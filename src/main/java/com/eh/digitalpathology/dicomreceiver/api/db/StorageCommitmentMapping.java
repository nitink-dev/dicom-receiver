package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "storageCommitmentMapping")
public class StorageCommitmentMapping {
    @Id
    private String id;
    private String seriesInstanceUid;
    private boolean storageCmtStatus;
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public boolean isStorageCmtStatus() {
        return storageCmtStatus;
    }

    public void setStorageCmtStatus(boolean storageCmtStatus) {
        this.storageCmtStatus = storageCmtStatus;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
