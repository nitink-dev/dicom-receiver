package com.eh.digitalpathology.dicomreceiver.api;

import org.springframework.stereotype.Component;

@Component
public class DicomServerBootstrap {
    public static void main(String[] args) throws Exception {
        DicomSCPConfig config = new DicomSCPConfig(); // use Spring or manual config
        DicomSCPServer server = new DicomSCPServer(config);
        server.start();
        System.out.println("DICOM SCP started on port " + config.getPort());
    }
}