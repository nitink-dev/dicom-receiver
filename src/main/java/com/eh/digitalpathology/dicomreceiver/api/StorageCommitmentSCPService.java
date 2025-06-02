package com.eh.digitalpathology.dicomreceiver.api;

import com.eh.digitalpathology.dicomreceiver.api.db.*;
import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

@Component
class StorageCommitmentSCPService implements DicomService {

    private static final Logger log = LoggerFactory.getLogger(StorageCommitmentSCPService.class);

    // Storage Commitment SOP Class UID (Push Model)
    public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS = "1.2.840.10008.1.20.1";
    // Storage Commitment SOP Instance UID (always fixed)
    public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_INSTANCE = "1.2.840.10008.1.20.1.1";

    private final DicomInstanceRepository dicomInstanceRepository;

    private final StorageCommitmentTrackerRepository storageCommitmentTrackerRepository;

    private final StorageCommitmentMappingRepository storageCommitmentMappingRepository;

    StorageCommitmentSCPService(DicomInstanceRepository dicomInstanceRepository, StorageCommitmentTrackerRepository storageCommitmentTrackerRepository, StorageCommitmentMappingRepository storageCommitmentMappingRepository) {
        this.dicomInstanceRepository = dicomInstanceRepository;
        this.storageCommitmentTrackerRepository = storageCommitmentTrackerRepository;
        this.storageCommitmentMappingRepository = storageCommitmentMappingRepository;
    }


    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, PDVInputStream data) throws IOException {
        if (dimse != Dimse.N_ACTION_RQ) {
            throw new DicomServiceException(Status.UnrecognizedOperation,
                    "Only N-ACTION is supported for Storage Commitment");
        }
        Attributes actionInfo;
        try (DicomInputStream dis = new DicomInputStream(data)) {
            actionInfo = dis.readDataset(-1, -1);
        }
        String transactionUID = actionInfo.getString(Tag.TransactionUID);
        Sequence refSOPSeq = actionInfo.getSequence(Tag.ReferencedSOPSequence);
        if (transactionUID == null || refSOPSeq == null) {
            throw new DicomServiceException(Status.MissingAttributeValue,
                    "Missing Transaction UID or Referenced SOP Sequence");
        }
        System.out.printf("Received N-ACTION for Transaction UID: %s with %d SOP Instances%n",
                transactionUID, refSOPSeq.size());
        Attributes eventInfo = new Attributes();
        eventInfo.setString(Tag.TransactionUID, VR.UI, transactionUID);
        Sequence successSeq = eventInfo.newSequence(Tag.ReferencedSOPSequence, refSOPSeq.size());
        Sequence failedSeq = eventInfo.newSequence(Tag.FailedSOPSequence, refSOPSeq.size());
        String seriesId = null;
        for (Attributes item : refSOPSeq) {
            // update database for commitment request status
            //DicomFileTracker.saveRecords(successSeq, record);
            String sopClassUID = item.getString(Tag.ReferencedSOPClassUID);
            String sopInstanceUID = item.getString(Tag.ReferencedSOPInstanceUID);
            if (sopClassUID == null || sopInstanceUID == null) {
                System.out.println("Missing SOP Class UID or Instance UID in a referenced item");
                continue;
            }
//            if (checkIfStored(sopClassUID, sopInstanceUID)) {
            // get record from database for sopInstanceUID
            Optional<DicomInstance> record = dicomInstanceRepository.findBySopInstanceUid(sopInstanceUID);

            if (record.isPresent()) {
                seriesId = seriesId == null ? record.get().getSeriesInstanceUid(): seriesId;
                Attributes successItem = new Attributes();
                successItem.setString(Tag.ReferencedSOPClassUID, VR.UI, sopClassUID);
                successItem.setString(Tag.ReferencedSOPInstanceUID, VR.UI, sopInstanceUID);
                successSeq.add(successItem);
            } else {
                Attributes failedItem = new Attributes();
                failedItem.setString(Tag.ReferencedSOPClassUID, VR.UI, sopClassUID);
                failedItem.setString(Tag.ReferencedSOPInstanceUID, VR.UI, sopInstanceUID);
                failedItem.setInt(Tag.FailureReason, VR.US, 0x0110); // Processing failure
                failedSeq.add(failedItem);
            }

            // update database for commitment request status
            if(seriesId != null)
                saveStorageCommitmentTracker(seriesId, sopInstanceUID, record.isPresent());
        }
        int eventTypeID = failedSeq.isEmpty() ? 1 : 2;
        System.out.printf("Sending N-EVENT-REPORT (eventTypeID = %d): %d success, %d failed%n",
                eventTypeID, successSeq.size(), failedSeq.size());



        try {
            as.neventReport(
                    STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS,
                    STORAGE_COMMITMENT_PUSH_MODEL_SOP_INSTANCE,
                    eventTypeID,
                    eventInfo,
                    null
            );
        } catch (InterruptedException e) {
            System.err.println("Interrupting thread for EventReport "+e);
            Thread.currentThread().interrupt();
        }
        Attributes rsp = Commands.mkNActionRSP(cmd, Status.Success);
        as.writeDimseRSP(pc, rsp, null);
        // update series for commitment response flag in database
        if (seriesId != null) {
            saveStorageCommitmentMapping(seriesId, failedSeq.isEmpty());
        }
    }
    @Override
    public void onClose(Association association) {
        log.info("Association closed with {}", association.getRemoteAET());
    }

    @Override
    public String[] getSOPClasses() {
        return new String[]{ UID.StorageCommitmentPushModel };
    }

    private static boolean checkIfStored(String sopClassUID, String sopInstanceUID) {
        File dicomFile = new File("D:\\opt\\dicom\\storage\\" + sopInstanceUID + ".dcm");
        if (!dicomFile.exists() || !dicomFile.isFile()) {
            System.out.println("File not found: " + dicomFile.getAbsolutePath());
            return false;
        }
        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            Attributes dataset = dis.readDataset(-1, -1);
            String fileSOPClassUID = dataset.getString(Tag.SOPClassUID);
            String fileSOPInstanceUID = dataset.getString(Tag.SOPInstanceUID);
            if (!sopClassUID.equals(fileSOPClassUID) || !sopInstanceUID.equals(fileSOPInstanceUID)) {
                System.out.println("SOP UID mismatch for file: " + dicomFile.getName());
                return false;
            }
            return true; // Valid and matches
        } catch (IOException e) {
            System.out.println("Failed to read DICOM file: " + dicomFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    private void saveStorageCommitmentTracker(String seriesId, String sopInstanceUID, boolean present) {
        StorageCommitmentTracker tracker = new StorageCommitmentTracker();
        tracker.setSopInstanceUid(sopInstanceUID);
        tracker.setSeriesInstanceUid(seriesId);
        tracker.setCmtRequestStatus("REQUESTED");

        if (present) {
            tracker.setCmtResponseStatus("SUCCESS");
        } else {
            tracker.setCmtResponseStatus("FAILURE");
        }
        tracker.setTimestamp(new Date());
        storageCommitmentTrackerRepository.save(tracker);
    }

    private void saveStorageCommitmentMapping(String seriesId, boolean cmtStatus) {
        StorageCommitmentMapping mapping = new StorageCommitmentMapping();
        mapping.setSeriesInstanceUid(seriesId);
        mapping.setStorageCmtStatus(cmtStatus);
        mapping.setUpdatedAt(new Date());
        storageCommitmentMappingRepository.save(mapping);
    }
}
