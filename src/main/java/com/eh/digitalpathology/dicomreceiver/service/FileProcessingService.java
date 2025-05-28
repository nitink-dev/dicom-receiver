package com.eh.digitalpathology.dicomreceiver.service;

import com.eh.digitalpathology.dicomreceiver.model.DicomRequestDBObject;
import com.eh.digitalpathology.dicomreceiver.model.ReqGeneratorNotificationMsg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

@Service
public class FileProcessingService {
    private final DicomExtractorService dicomExtractorService;
    private final DatabaseService databaseService;
    private final EventNotificationService eventNotificationService;
    private final DicomDirService dicomDirService;
    public static final String DICOM_RECEIVER = "dicom-receiver";
    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class.getName());

    @Value("${remote.kafka.receiver.topic}")
    private String receiverToReqGenerator;

    public FileProcessingService(DicomExtractorService dicomExtractorService, DatabaseService databaseService, EventNotificationService eventNotificationService, DicomDirService dicomDirService) {
        this.dicomExtractorService = dicomExtractorService;
        this.databaseService = databaseService;
        this.eventNotificationService = eventNotificationService;
        this.dicomDirService = dicomDirService;
    }

    public void processFile(WatchEvent<?> event, String fileStore, String intermediateStore)  {
        log.info("processFile :: ===============================>Process File thread Started:{}", Thread.currentThread().getName());
        Path dir = Paths.get(fileStore);
        Path filePath = dir.resolve((Path) event.context());
        log.info("processFile :: ===============================>\n======>File received by dicom-file-watcher======>\n {}", filePath);
        if (Files.exists(filePath)) {
            log.info("processFile :: File exists & ready to process: {}", filePath.getFileName());
            log.info("processFile :: *********************\n*************\nProcessing file at: {}", filePath);
            try {
                if (filePath.getFileName().toString().contains("DICOMDIR")){
                    log.info("processFile :: dicom dir service will be called");
                    dicomDirService.fetchAndStoreMetaData(filePath);

                }else {
                    DicomRequestDBObject dicomRequestDBObject = dicomExtractorService.extract(intermediateStore, filePath.toString());
                    String status = databaseService.insertDicomData(dicomRequestDBObject, DICOM_RECEIVER);
                    log.info("processFile :: response from insertDicomData(): {}", status);
                    if ("success".equalsIgnoreCase(status)) {
                        log.info("processFile :: Data insertion successful !!!");
                        notifyKafka(dicomRequestDBObject);
                    } else {
                        log.info("processFile :: KAFKA NOT NOTIFIED!!!.. ");
                    }
                }
            } catch (Exception e) {
                log.error("processFile :: Exception occurred while processing file: {}", e.getMessage());
            }
        }
    }

    public void notifyKafka(DicomRequestDBObject requestDBObject) {
        log.info("notifyKafka :: =======>Sending notification to KAFKA: {}", requestDBObject);
        ReqGeneratorNotificationMsg msg = new ReqGeneratorNotificationMsg( requestDBObject.getBarcode(), requestDBObject.getSopInstanceUid(), requestDBObject.getSeriesInstanceUid());
        ObjectMapper objectMapper = new ObjectMapper( );
        String msgString = null;
        try {
            msgString = objectMapper.writeValueAsString( msg );
        } catch ( JsonProcessingException e ) {
            log.error("unable to convert object to string :: {}", e.getMessage());
        }
        eventNotificationService.sendEvent(receiverToReqGenerator, msgString);
    }


}