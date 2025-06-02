package com.eh.digitalpathology.dicomreceiver.api.db;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DicomInstanceRepository extends MongoRepository<DicomInstance, String> {
    Optional<DicomInstance> findBySopInstanceUid(String sopInstanceUid);
}
