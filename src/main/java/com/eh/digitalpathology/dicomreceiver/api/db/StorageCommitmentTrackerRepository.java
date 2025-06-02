package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StorageCommitmentTrackerRepository extends MongoRepository<StorageCommitmentTracker, String> {
    List<StorageCommitmentTracker> findBySeriesInstanceUid(String seriesInstanceUid);
    Optional<StorageCommitmentTracker> findBySopInstanceUid(String sopInstanceUid);
}
