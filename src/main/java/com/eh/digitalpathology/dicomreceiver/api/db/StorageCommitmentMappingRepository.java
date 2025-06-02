package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StorageCommitmentMappingRepository extends MongoRepository<StorageCommitmentMapping, String> {
    Optional<StorageCommitmentMapping> findBySeriesInstanceUid(String seriesInstanceUid);
}
