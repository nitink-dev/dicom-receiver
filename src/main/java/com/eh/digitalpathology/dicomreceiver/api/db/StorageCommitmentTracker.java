package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "storageCommitmentTracker")
public class StorageCommitmentTracker {
    @Id
    private String id;
    private String sopInstanceUid;
    private String seriesInstanceUid;
    private String cmtRequestStatus;   // REQUESTED / NOT_FOUND
    private String cmtResponseStatus;  // SUCCESS / FAILURE
    private Date timestamp;

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

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public String getCmtRequestStatus() {
        return cmtRequestStatus;
    }

    public void setCmtRequestStatus(String cmtRequestStatus) {
        this.cmtRequestStatus = cmtRequestStatus;
    }

    public String getCmtResponseStatus() {
        return cmtResponseStatus;
    }

    public void setCmtResponseStatus(String cmtResponseStatus) {
        this.cmtResponseStatus = cmtResponseStatus;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
