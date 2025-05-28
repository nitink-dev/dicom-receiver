package com.eh.digitalpathology.dicomreceiver.api;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DicomSCPConfig {
    private final String aeTitle = "EH_ENRICH";
    private final int port = 2575;
    private final Path storagePath = Paths.get("/opt/dicom/storage");

    public String getAETitle() { return aeTitle; }
    public int getPort() { return port; }

    public Path getStoragePath() {
        return storagePath;
    }
}