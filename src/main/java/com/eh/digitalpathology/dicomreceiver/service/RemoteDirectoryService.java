package com.eh.digitalpathology.dicomreceiver.service;

import com.eh.digitalpathology.dicomreceiver.util.CommonUtils;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class RemoteDirectoryService {

    private static final Logger logger = LoggerFactory.getLogger( RemoteDirectoryService.class );

    @Value( "${max.retries}" )
    private long maxRetries;


    private final CommonUtils commonUtils;

    public RemoteDirectoryService ( CommonUtils commonUtils ) {
        this.commonUtils = commonUtils;
    }

    private ExecutorService remoteWatcherExecutorService = Executors.newFixedThreadPool( Runtime.getRuntime( ).availableProcessors( ) );

    public void processFileEvent ( FileChangeEvent event, FileObject remoteDirectory, String username, String password,
                                   String serverName, String sharedPath ) {
        logger.info( "processFileEvent :: Inside processFileEvent method for : {}", event );
        try {
            String relativeFilePath = getRelativeFilePath( event.getFileObject( ).getName( ).toString( ), remoteDirectory.toString( ) );
            if ( event.getFileObject( ).isFile( ) ) {
                remoteWatcherExecutorService.submit( ( ) -> establishConnectionAndProcessFile( relativeFilePath, event.getFileObject( ).getName( ).getBaseName( ),
                        username, password, serverName, sharedPath ) );
            }
        } catch ( Exception e ) {
            logger.error( "processFileEvent :: Exception occurred while processing FileEvent : ", e );
        }
    }

    // Verify File Status and Copy file to local storage
    private void establishConnectionAndProcessFile ( String remoteFileRelativePath, String remoteFileName, String username,
                                                     String password, String servername, String sharepath ) {
        logger.info( "establishConnectionAndProcessFile :: Inside establishConnectionAndProcessFile method for : {}", remoteFileRelativePath );
        AuthenticationContext authContext;
        if ( !StringUtils.isEmpty( username ) && !StringUtils.isEmpty( password ) ) {
            authContext = new AuthenticationContext( username, password.toCharArray( ), "" );
        } else {
            authContext = AuthenticationContext.anonymous( );
        }

        int attempt = 0;
        boolean success = false;
        while ( attempt < maxRetries && !success ) {
            attempt++;
            try ( SMBClient client = new SMBClient( );
                  Connection connection = client.connect( servername );
                  Session session = connection.authenticate( authContext );
                  DiskShare share = (DiskShare) session.connectShare( sharepath ) ) {
                logger.debug( "establishConnectionAndProcessFile :: SMB Dialect in use: {}", connection.getNegotiatedProtocol( ).getDialect( ) );
                logger.info( "establishConnectionAndProcessFile :: Connection successfully established for file transfer using the SMB protocol for : {}", remoteFileRelativePath );
                verifyAndProcessFile( share, remoteFileRelativePath, remoteFileName );
                success = true;
            } catch ( Exception e ) {
                logger.error( "establishConnectionAndProcessFile :: Attempt {} - Exception occurred while establishing connection with network shared folder: ", attempt, e );
                if ( attempt >= maxRetries ) {
                    logger.error( "establishConnectionAndProcessFile :: Max retries reached. Failed to establish connection and copy file: {}", remoteFileName );
                } else {
                    logger.error( "establishConnectionAndProcessFile :: Retrying... (Attempt {}/{}) for remoteFileName : {}", attempt, maxRetries, remoteFileName );
                }
            }
        }
    }

    private void verifyAndProcessFile ( DiskShare share, String remoteFileRelativePath, String remoteFileName ) {
        Set< AccessMask > accessMask = EnumSet.of( AccessMask.GENERIC_READ );
        Set< FileAttributes > attributes = EnumSet.noneOf( FileAttributes.class );
        Set< SMB2ShareAccess > shareAccesses = EnumSet.of( SMB2ShareAccess.FILE_SHARE_READ );
        SMB2CreateDisposition createDisposition = SMB2CreateDisposition.FILE_OPEN;
        Set< SMB2CreateOptions > createOptions = EnumSet.noneOf( SMB2CreateOptions.class );
        try {
            boolean isFileReady = false;
            while ( !isFileReady ) {
                Thread.sleep( 10000 ); // Wait for 10 seconds
                File remoteFile = verifyLockonFile( share, remoteFileRelativePath, accessMask, attributes, shareAccesses, createDisposition, createOptions );
                if ( remoteFile != null ) {
                    long initialSize = remoteFile.getFileInformation( ).getStandardInformation( ).getEndOfFile( );
                    Thread.sleep( 10000 ); // Wait for 10 seconds
                    long finalSize = remoteFile.getFileInformation( ).getStandardInformation( ).getEndOfFile( );
                    if ( initialSize == finalSize ) {
                        isFileReady = true;
                        copyRemoteFileToLocal( remoteFile, remoteFileName );
                    }
                }
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread( ).interrupt( );
            logger.error( "verifyAndProcessFile :: Thread was interrupted during sleep", e );
        }
    }

    private File verifyLockonFile ( DiskShare share, String remoteFilePath, Set< AccessMask > accessMask,
                                    Set< FileAttributes > attributes, Set< SMB2ShareAccess > shareAccesses,
                                    SMB2CreateDisposition createDisposition, Set< SMB2CreateOptions > createOptions ) {
        logger.info( "verifyLockonFile :: Inside verifyLockonFile method for : {}", remoteFilePath );
        File remoteFile = null;
        try {
            remoteFile = share.openFile( remoteFilePath, accessMask, attributes, shareAccesses, createDisposition, createOptions );
            logger.info( "verifyLockonFile :: File is NOT locked. No active reads/writes ------------- : {} ", remoteFilePath );
        } catch ( Exception e ) {
            logger.error( "verifyLockonFile :: File is LOCKED! Another process is reading/writing ------------- : {} ", remoteFilePath );
            logger.error( e.getMessage( ) );
        }
        return remoteFile;
    }


    private void copyRemoteFileToLocal ( File remoteFile, String remoteFileName ) {
        logger.info( "copyRemoteFileToLocal:: Inside copyRemoteFileToLocal method: {}", remoteFile );
        int attempt = 0;
        boolean success = false;
        String receivedFiles = commonUtils.getLocalStoragePath( );
        if ( remoteFileName.equalsIgnoreCase( "DICOMDIR" ) ) {
            remoteFileName = remoteFileName + "_" + Instant.now( ).toString( );
        }
        Path localStorageFile = Paths.get( receivedFiles, remoteFileName );
        while ( attempt < maxRetries && !success ) {
            attempt++;
            try ( InputStream smbInputStream = remoteFile.getInputStream( );
                  FileChannel localFileChannel = FileChannel.open( localStorageFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE ) ) {
                byte[] buffer = new byte[ 1048576 ];
                int bytesRead;
                while ( ( bytesRead = smbInputStream.read( buffer ) ) != -1 ) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap( buffer, 0, bytesRead );
                    localFileChannel.write( byteBuffer );
                }
                logger.info( "copyRemoteFileToLocal :: File copied successfully: {}", remoteFileName );
                success = true; // Mark success if no exception occurs
            } catch ( Exception e ) {
                logger.error( "copyRemoteFileToLocal :: Attempt {} - Exception occurred while copying file from remote shared folder to local for file {}. Exception: ", attempt, remoteFileName, e );
                if ( attempt >= maxRetries ) {
                    logger.error( "copyRemoteFileToLocal :: Max retries reached. Failed to copy file: {}", remoteFileName );
                } else {
                    logger.info( "copyRemoteFileToLocal :: Retrying... (Attempt {}/{}) for remoteFileName: {}", attempt, maxRetries, remoteFileName );
                }
            }
        }
    }


    private String getRelativeFilePath ( String fullFilePath, String sharePath ) {
        logger.info( "Inside getRelativeFilePath method for : {} ", fullFilePath );
        if ( fullFilePath.startsWith( sharePath ) ) {
            return fullFilePath.substring( sharePath.length( ) );
        }
        return fullFilePath;
    }

    @PreDestroy
    public void shutdown ( ) {
        remoteWatcherExecutorService.shutdown( );
        try {
            if ( !remoteWatcherExecutorService.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                remoteWatcherExecutorService.shutdownNow( );
                if ( !remoteWatcherExecutorService.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                    logger.error( "ExecutorService did not terminate" );
                }
            }
        } catch ( InterruptedException ex ) {
            remoteWatcherExecutorService.shutdownNow( );
            Thread.currentThread( ).interrupt( );
        }
    }
}
