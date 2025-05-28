package com.eh.digitalpathology.dicomreceiver.model;

public class DicomRequestDBObject {


    private String originalStudyInstanceUid;
    private String actualStudyInstanceUid;
    private String intermediateStoragePath;
    private String dicomInstanceReceivedTimestamp;
    private String enrichmentTimestamp;
    private String barcode;
    private String sopInstanceUid;
    private String seriesInstanceUid;

    // Getters and Setters
    public String getOriginalStudyInstanceUid() {
        return originalStudyInstanceUid;
    }

    public void setOriginalStudyInstanceUid(String originalStudyInstanceUid) {
        this.originalStudyInstanceUid = originalStudyInstanceUid;
    }

    public String getActualStudyInstanceUid() {
        return actualStudyInstanceUid;
    }

    public void setActualStudyInstanceUid(String actualStudyInstanceUid) {
        this.actualStudyInstanceUid = actualStudyInstanceUid;
    }

    public String getIntermediateStoragePath() {
        return intermediateStoragePath;
    }

    public void setIntermediateStoragePath(String intermediateStoragePath) {
        this.intermediateStoragePath = intermediateStoragePath;
    }

    public String getDicomInstanceReceivedTimestamp() {
        return dicomInstanceReceivedTimestamp;
    }

    public void setDicomInstanceReceivedTimestamp(String dicomInstanceReceivedTimestamp) {
        this.dicomInstanceReceivedTimestamp = dicomInstanceReceivedTimestamp;
    }

    public String getEnrichmentTimestamp() {
        return enrichmentTimestamp;
    }

    public void setEnrichmentTimestamp(String enrichmentTimestamp) {
        this.enrichmentTimestamp = enrichmentTimestamp;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSopInstanceUid() {
        return sopInstanceUid;
    }

    public void setSopInstanceUid(String sopInstanceUid) {
        this.sopInstanceUid = sopInstanceUid;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }
    @Override
    public String toString() {
        return "DicomRequestDBObject{" +
                "originalStudyInstanceUid='" + originalStudyInstanceUid + '\'' +
                ", actualStudyInstanceUid='" + actualStudyInstanceUid + '\'' +
                ", intermediateStoragePath='" + intermediateStoragePath + '\'' +
                ", dicomInstanceReceivedTimestamp='" + dicomInstanceReceivedTimestamp + '\'' +
                ", enrichmentTimestamp='" + enrichmentTimestamp + '\'' +
                ", barcode='" + barcode + '\'' +
                ", sopInstanceUid='" + sopInstanceUid + '\'' +
                ", seriesInstanceUid='" + seriesInstanceUid + '\'' +
                '}';
    }
}