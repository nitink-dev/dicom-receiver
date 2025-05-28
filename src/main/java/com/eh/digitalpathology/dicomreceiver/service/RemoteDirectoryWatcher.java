package com.eh.digitalpathology.dicomreceiver.service;

import com.eh.digitalpathology.dicomreceiver.constants.WatchDirectoryConstant;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import net.idauto.oss.jcifsng.vfs2.provider.SmbFileSystemConfigBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class RemoteDirectoryWatcher {
    private static final Logger logger = LoggerFactory.getLogger(RemoteDirectoryWatcher.class);

    @Value("${sharedfolder.servername}")
    private String serverName;

    @Value("${sharedfolder.sharepath}")
    private String sharePath;

    @Value("${jcifs.smb.client.minVersion}")
    private String smbMinVersion;

    @Value("${jcifs.smb.client.maxVersion}")
    private String smbMaxVersion;

    @Value("${sharedfolder.username}")
    private String username;

    @Value("${sharedfolder.password}")
    private String password;

    @Value("${directorywatcher.running.status}")
    private boolean running ;


    private final RemoteDirectoryService remoteDirectoryService;

    @Autowired
    public RemoteDirectoryWatcher(RemoteDirectoryService remoteDirectoryService) {
        this.remoteDirectoryService = remoteDirectoryService;
    }

    public void watchSharedDirectory() {
        logger.info("watchSharedDirectory :: Inside watchSharedDirectory method... ");
        try{
            // jcifs configuration to enable support for SMB2 & SMB3
            Properties jcifsProperties = new Properties();
            jcifsProperties.setProperty(WatchDirectoryConstant.SMB_MIN_VERSION, smbMinVersion);
            jcifsProperties.setProperty(WatchDirectoryConstant.SMB_MAX_VERSION, smbMaxVersion);
            CIFSContext jcifsContext = new BaseContext(new PropertyConfiguration(jcifsProperties));
            FileSystemOptions options = new FileSystemOptions();

            // Set up authentication
            if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)){
                StaticUserAuthenticator auth = new StaticUserAuthenticator("", username, password);
                DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, auth);
            }

            SmbFileSystemConfigBuilder.getInstance().setCIFSContext(options, jcifsContext);
            String smburl = new URIBuilder()
                    .setScheme(WatchDirectoryConstant.SMB)
                    .setHost(serverName)
                    .setPath(sharePath).toString();

            resolveSharedFolder(smburl, options);
        }catch(Exception e ){
            logger.error("watchSharedDirectory :: Exception occured while watching network shared folder for servername : {}  &  sharepath : {} , Exception : " , serverName, sharePath, e);
        }
    }

    private void resolveSharedFolder(String smburl, FileSystemOptions options) throws FileSystemException {
        logger.info("resolveSharedFolder :: Inside resolveSharedFolder method for smburl : {}", smburl);
        FileSystemManager fsManager = VFS.getManager();
        try(FileObject remoteDirectory = fsManager.resolveFile(smburl, options)){
            //	FileListener to handle file events
            DefaultFileMonitor fm = new DefaultFileMonitor(new FileListener() {
                @Override
                public void fileCreated(FileChangeEvent event) {
                    logger.info("resolveSharedFolder :: File created : {}", event.getFileObject().getName());
                        remoteDirectoryService.processFileEvent(event, remoteDirectory, username, password, serverName, sharePath);

                }

                @Override
                public void fileChanged(FileChangeEvent event) {
                    logger.info("resolveSharedFolder :: Ignoring file changed : {}", event.getFileObject().getName() );

                }

                @Override
                public void fileDeleted(FileChangeEvent event) {
                    // As DirectoryWatcher we are not handling file deletion events
                    logger.info("resolveSharedFolder :: Ignoring file deleted : {}", event.getFileObject().getName() );
                }
            });
            fm.setRecursive(true);
            fm.addFile(remoteDirectory);
            fm.start();

            logger.info("resolveSharedFolder :: Monitoring directory: {}", remoteDirectory.getName() );
            // Keep the program running to monitor the shared network directory indefinitely
            while (running) {
                Thread.sleep(5000);
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            logger.error("resolveSharedFolder :: Thread was interrupted during sleep", e);
        }
    }

}
