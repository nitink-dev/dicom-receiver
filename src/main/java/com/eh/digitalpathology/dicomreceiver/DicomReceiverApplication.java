package com.eh.digitalpathology.dicomreceiver;


import com.eh.digitalpathology.dicomreceiver.service.DirectoryWatcher;
import com.eh.digitalpathology.dicomreceiver.service.ReceiverService;
import com.eh.digitalpathology.dicomreceiver.service.RemoteDirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class DicomReceiverApplication implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DicomReceiverApplication.class.getName());
    private final ReceiverService receiverService;
    private final DirectoryWatcher directoryWatcher;
    @Value("${storescp.storage.path}")
    private String fileStorePath;
    @Value("${file.server.path}")
    private String intermediateFileServer;
    @Value("${sharedfolder.enableRemoteDirectoryWatcher}")
    private boolean  enableRemoteDirectoryWatcher;

    private RemoteDirectoryWatcher remoteDirectoryWatcher;

    @Autowired
    public DicomReceiverApplication(ReceiverService receiverService, DirectoryWatcher directoryWatcher, RemoteDirectoryWatcher remoteDirectoryWatcher) {
        this.receiverService = receiverService;
        this.directoryWatcher = directoryWatcher;
        this.remoteDirectoryWatcher = remoteDirectoryWatcher;

    }

    public static void main(String[] args) {
        SpringApplication.run(DicomReceiverApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(4); // Adjust the number of threads as needed

        // Get the root folder

        Files.createDirectories(Paths.get(fileStorePath));
        Files.createDirectories(Paths.get(intermediateFileServer));
        String receivedFiles = Paths.get(fileStorePath).toAbsolutePath().toString();
        log.info("run :: receivedFiles :: {}", receivedFiles);
        String intermediateFileStorage = Paths.get(intermediateFileServer).toAbsolutePath().toString();
        log.info("run :: intermediateFileStorage :: {}", intermediateFileStorage);
        executorService.submit(() -> {
            log.info("run :: ===================>Starting receiverService terminal service...");
            try {
                receiverService.runReceiverTerminal(receivedFiles);
                log.info("run :: ReceiverService terminal service completed.");
            } catch (Exception e) {
                log.error("run :: Error running receiverService terminal: ", e);
            }
        });


//        executorService.submit(() -> {
//            log.info("run :: ===========>Starting local directory watcher service...");
//            try {
//                directoryWatcher.runStorageCommitmentService(receivedFiles);
//                log.info("run :: Directory watcher service completed.");
//            } catch (InterruptedException ie) {
//                log.error("run :: Error running directory watcher: ", ie);
//                Thread.currentThread().interrupt(); // Re-interrupt the current thread
//            }
//        });

        executorService.submit(() -> {
            log.info("run :: ===========>Starting local directory watcher service...");
            try {
                directoryWatcher.directoryLookup(receivedFiles, intermediateFileStorage);
                log.info("run :: Directory watcher service completed.");
            } catch (InterruptedException ie) {
                log.error("run :: Error running directory watcher: ", ie);
                Thread.currentThread().interrupt(); // Re-interrupt the current thread
            }
        });

        log.info("run :: enableRemoteDirectoryWatcher: {}", enableRemoteDirectoryWatcher);
        if(enableRemoteDirectoryWatcher) {
            executorService.submit(() -> {
                log.info("run :: ============>Starting remote directory watcher service...");
                try {
                    remoteDirectoryWatcher.watchSharedDirectory();
                    log.info("run :: Remote directory watcher service completed.");
                } catch (Exception e) {
                    log.error("run :: Error running remote directory watcher: ", e);
                }
            });
        }else{
            log.info("run :: ===============> Disabled remote directory watcher service...");
        }
        executorService.shutdown();
    }
}

