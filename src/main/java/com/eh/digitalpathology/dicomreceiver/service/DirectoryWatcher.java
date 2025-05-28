package com.eh.digitalpathology.dicomreceiver.service;

import jakarta.annotation.PreDestroy;
import org.dcm4che3.tool.dcmqrscp.DcmQRSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Component
public class DirectoryWatcher {


    // terminal for storage-commitment ========================
    private String AE_STORAGE_CMT="STORESCP_CMT";
    private String AE_STORAGE_CMT_PORT = "11111";

    // bind source for storage-commitment
    private String AE_CMT_REQ ="LOCAL_AE";
    private String AE_CMT_REQ_PORT = "11115";

    private static final Logger log = LoggerFactory.getLogger( DirectoryWatcher.class.getName( ) );
    public static final String DICOM_RECEIVER = "dicom-receiver";


    private final FileProcessingService fileProcessingService;
    private ExecutorService executorService = Executors.newFixedThreadPool( Runtime.getRuntime( ).availableProcessors( ) );

    @Autowired
    public DirectoryWatcher ( FileProcessingService fileProcessingService ) {
        this.fileProcessingService = fileProcessingService;
    }


    public void runStorageCommitmentService(String basePath) throws InterruptedException {
        try {
            // Command-line arguments for the dcmqrscp command
            String[] dcmqrscpArgs = {
                    "dcmqrscp",
                    "-b", AE_STORAGE_CMT+":"+ AE_STORAGE_CMT_PORT, //storescp-commitment
                    "--bind", AE_CMT_REQ +"@localhost:"+ AE_CMT_REQ_PORT, //local source commitment-sender
                    "--stgcmt-same-assoc",
                    "--filepath","received/{00080018}.dcm",//{22000005} // barcode
                    //studytag/seriestag/soptag
                    "--dicomdir", "D:\\opt\\DICOMDIR",
                    //"--dicomdir", "\\opt\\received\\DICOMDIR",
                    "--ae-config", "D:\\NK-Work\\shischou\\dcm4che-5.33.0\\etc\\dcmqrscp\\ae.properties"
            };

//            String[] dcmqrscpArgs = { "dcmqrscp", "-b", AE_STORAGE_CMT+":"+AE_STORAGE_CMT_PORT, //storescp-commitment
//                    "--bind", AE_CMT_REQ+"@localhost:"+AE_CMT_REQ_PORT, //local source commitment-sender
//                    "--stgcmt-same-assoc" ,
//                    "--dicomdir", "D:\\NK-Work\\shischou\\SampleDICOMFile\\dicom_files_from_box\\DICOMDIR",
//                    "--ae-config", "D:\\NK-Work\\shischou\\dcm4che-5.33.0\\etc\\dcmqrscp\\ae.properties"
//            };

            // Log the start of the DICOM Query/Retrieve Service
            log.info("\t\t runQueryRetrieveService :: ######### Start Query/Retrieve Service with AE title: "+AE_STORAGE_CMT+":"+ AE_STORAGE_CMT_PORT +".");

            // Start the DICOM Query/Retrieve service
            DcmQRSCP.main(dcmqrscpArgs);

        } catch (Exception e) {
            // Log an error message if an exception occurs
            log.error("runQueryRetrieveService :: Unable to start DICOM Query/Retrieve service %s", e);
            Thread.currentThread( ).interrupt( );
        }
    }



    public void directoryLookup ( String fileStore, String intermediateStore ) throws InterruptedException {
        try ( WatchService watchService = FileSystems.getDefault( ).newWatchService( ) ) {
            Path path = Paths.get( fileStore );
            path.register( watchService, StandardWatchEventKinds.ENTRY_CREATE );
            log.info( "directoryLookup :: Watching directory: {}", path );
            WatchKey key;
            while ( ( key = watchService.take( ) ) != null ) {
                for ( WatchEvent< ? > event : key.pollEvents( ) ) {
                    if ( event.kind( ) == StandardWatchEventKinds.ENTRY_CREATE ) {
                        Path filePath = (Path) event.context( );

                        if ( filePath.toString( ).endsWith( ".part" ) || filePath.toString( ).endsWith( ".filepart" ) ) {
                            log.info( "directoryLookup :: ***************Ignoring file {}", filePath );
                            continue;
                        }
                        verifyInProgressReadWriteOnFile( fileStore, filePath.toFile( ).getName( ) );
                        log.info( "directoryLookup :: ==New file added: {}<=================================================", event.context( ) );
                        executorService.submit( ( ) -> fileProcessingService.processFile( event, fileStore, intermediateStore ) );
                    }
                }
                key.reset( );
            }
        } catch ( InterruptedException e ) {
            log.error( "directoryLookup :: file-watcher in InterruptedException error: {}", e.toString( ) );
            Thread.currentThread( ).interrupt( );
        } catch ( Exception e ) {
            log.error( "directoryLookup :: file-watcher in error: {}", e.toString( ) );
        }
    }

    private void verifyInProgressReadWriteOnFile ( String basepath, String file ) {
        log.info( "verifyInProgressReadWriteOnFile :: ....inside verifyInProgressReadWriteOnFile" );
        boolean isFileReady = false;
        try {
            while ( !isFileReady ) {
                Thread.sleep( 5000 );
                Path filePath = Paths.get( basepath, file );
                if ( filePath.toFile( ).exists( ) ) {
                    isFileReady = verifyLockOnFile( filePath ) && isFileFullyCopied( filePath );
                }
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread( ).interrupt( );
            log.error( "verifyInProgressReadWriteOnFile :: Thread was interrupted during sleep", e );
        }
    }

    private boolean verifyLockOnFile ( Path path ) {
        log.info( "Inside verifyLockOnFile for : {}", path );
        boolean isFileReady = false;
        try ( FileChannel fileChannel = FileChannel.open( path, StandardOpenOption.READ, StandardOpenOption.WRITE ) ) {
            // Try to acquire an exclusive lock on the file
            FileLock lock = fileChannel.tryLock( );
            if ( lock != null ) {
                log.info( "verifyLockOnFile :: File is not locked by another process : {}", path );
                // Release the lock
                lock.release( );
                isFileReady = true;
            } else {
                log.info( "verifyLockOnFile :: File is locked by another process : {}", path );
            }
        } catch ( Exception e ) {
            log.error( "verifyLockOnFile :: Exception occurred while processing file: {}", path, e );
        }
        return isFileReady;
    }

    private boolean isFileFullyCopied ( Path path ) {
        log.info( "Inside isFileFullyCopied for : {}", path );
        long previousSize = -1;
        long currentSize = path.toFile( ).length( );
        try {
            while ( previousSize != currentSize ) {
                previousSize = currentSize;
                Thread.sleep( 10000 );
                currentSize = path.toFile( ).length( );
            }
            log.info( "isFileFullyCopied :: File is fully copied : {}", path );
            return true;
        } catch ( InterruptedException e ) {
            Thread.currentThread( ).interrupt( );
            log.error( "isFileFullyCopied :: Thread was interrupted during sleep", e );
            return false;
        }
    }


    @PreDestroy
    public void shutdown ( ) {
        executorService.shutdown( );
        try {
            if ( !executorService.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                executorService.shutdownNow( );
                if ( !executorService.awaitTermination( 60, TimeUnit.SECONDS ) ) {
                    log.error( "ExecutorService did not terminate" );
                }
            }
        } catch ( InterruptedException ex ) {
            executorService.shutdownNow( );
            Thread.currentThread( ).interrupt( );
        }
    }
}

