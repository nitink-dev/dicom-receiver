package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "dicom_instances")

public class DicomInstance {

    @Id
    private String id;

    private String sopInstanceUid;
    private String originalStudyInstanceUid;
    private String seriesInstanceUid;
    private String actualStudyInstanceUid;
    private String barcode;
    private String intermediateStoragePath;
    private String processingStatus;
    private Instant dicomInstanceReceivedTimestamp;
    private Instant enrichmentTimestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSopInstanceUid() {
        return sopInstanceUid;
    }

    public void setSopInstanceUid(String sopInstanceUid) {
        this.sopInstanceUid = sopInstanceUid;
    }

    public String getOriginalStudyInstanceUid() {
        return originalStudyInstanceUid;
    }

    public void setOriginalStudyInstanceUid(String originalStudyInstanceUid) {
        this.originalStudyInstanceUid = originalStudyInstanceUid;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public String getActualStudyInstanceUid() {
        return actualStudyInstanceUid;
    }

    public void setActualStudyInstanceUid(String actualStudyInstanceUid) {
        this.actualStudyInstanceUid = actualStudyInstanceUid;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getIntermediateStoragePath() {
        return intermediateStoragePath;
    }

    public void setIntermediateStoragePath(String intermediateStoragePath) {
        this.intermediateStoragePath = intermediateStoragePath;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public Instant getDicomInstanceReceivedTimestamp() {
        return dicomInstanceReceivedTimestamp;
    }

    public void setDicomInstanceReceivedTimestamp(Instant dicomInstanceReceivedTimestamp) {
        this.dicomInstanceReceivedTimestamp = dicomInstanceReceivedTimestamp;
    }

    public Instant getEnrichmentTimestamp() {
        return enrichmentTimestamp;
    }

    public void setEnrichmentTimestamp(Instant enrichmentTimestamp) {
        this.enrichmentTimestamp = enrichmentTimestamp;
    }
}
