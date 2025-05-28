package com.eh.digitalpathology.dicomreceiver.service;

import org.dcm4che3.tool.storescp.StoreSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReceiverService {
    private static final Logger log = LoggerFactory.getLogger(ReceiverService.class.getName());
    @Value("${storescp.command}")
    private String command;
    @Value("${storescp.bind}")
    private String bind;
    @Value("${storescp.aetitle}")
    private String aetitle;
    @Value("${storescp.aetitle.port}")
    private String port;

    public void runReceiverTerminal(String basePath) {
        try {
            String[] storescpArgs = {"storescp", "-b", aetitle + ":" + port, "--directory", basePath};
            log.info("\t\t runReceiverTerminal :: ######### Start {} as DICOM ReceiverService on port: {}", aetitle, port);
            StoreSCP.main(storescpArgs);
        } catch (Exception e) {
            log.error("runReceiverTerminal :: Unable to start dicom receiver %s", e);
        }
    }
}