package com.eh.digitalpathology.dicomreceiver.model;

public class DicomDirDocument {

    private String studyId;
    private String seriesId;
    private int imageCount;
    private byte[] dicomDirFile;

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public String getStudyId() {
        return studyId;
    }

    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public byte[] getDicomDirFile() {
        return dicomDirFile;
    }

    public void setDicomDirFile(byte[] dicomDirFile) {
        this.dicomDirFile = dicomDirFile;
    }
}
