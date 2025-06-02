package com.eh.digitalpathology.dicomreceiver.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DicomServerBootstrap implements CommandLineRunner {

    private final DicomSCPServer dicomSCPServer;

    public DicomServerBootstrap(DicomSCPServer dicomSCPServer) {
        this.dicomSCPServer = dicomSCPServer;
    }

    public static void main(String[] args) {
        SpringApplication.run(DicomServerBootstrap.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        dicomSCPServer.start();
        System.out.println("DICOM SCP started");
    }
}
