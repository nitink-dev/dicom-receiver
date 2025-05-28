package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.*;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

@Component
class StorageCommitmentSCPService implements DicomService {

    private static final Logger log = LoggerFactory.getLogger(StorageCommitmentSCPService.class);

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, PDVInputStream data) throws IOException {

        if (dimse != Dimse.N_ACTION_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        log.info("Received N-ACTION-RQ");

        Attributes actionInfo;
        try (DicomInputStream dis = new DicomInputStream(data)) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            actionInfo = dis.readDataset(-1, -1);
        }

        String transactionUID = actionInfo.getString(Tag.TransactionUID);
        Sequence refSeq = actionInfo.getSequence(Tag.ReferencedSOPSequence);

        Attributes eventDataset = new Attributes();
        eventDataset.setString(Tag.TransactionUID, VR.UI, transactionUID);
        Sequence successSeq = eventDataset.newSequence(Tag.ReferencedSOPSequence, refSeq.size());

        for (Attributes ref : refSeq) {
            String sopInstanceUID = ref.getString(Tag.ReferencedSOPInstanceUID);
            File file = Paths.get("/opt/dicom/storage", sopInstanceUID + ".dcm").toFile();
            if (file.exists()) {
                successSeq.add(ref);
            }
        }

        Attributes rsp = Commands.mkNActionRSP(cmd, Status.Success);
        as.writeDimseRSP(pc, rsp);

        log.info("Sent N-ACTION-RSP. Preparing to send N-EVENT-REPORT");

        // Send N-EVENT-REPORT to SCU
        ApplicationEntity ae = as.getApplicationEntity();
        Device device = ae.getDevice();

        Connection conn = new Connection();
        ae.addConnection(conn);

        AAssociateRQ rq = new AAssociateRQ();
        rq.setCalledAET("LOCAL_AE");
        rq.setCallingAET("EH_ENRICH");
        rq.addPresentationContext(new PresentationContext(1,
                UID.StorageCommitmentPushModel,
                UID.ImplicitVRLittleEndian));

        Connection remoteConn = new Connection();
        remoteConn.setHostname("localhost");
        remoteConn.setPort(11115); // SCU port
        Association assoc = null;
        try {
            assoc = ae.connect(remoteConn, rq);

            assoc.neventReport(
                    UID.StorageCommitmentPushModel,
                    UID.StorageCommitmentPushModelInstance,
                    1, // Event Type ID
                    eventDataset,
                    null);

            assoc.waitForOutstandingRSP();
            assoc.release();
            log.info("N-EVENT-REPORT sent and association released");
        } catch (Exception e) {
            log.error("Failed to send N-EVENT-REPORT", e);
        } finally {
            if (assoc != null && assoc.isReadyForDataTransfer()) {
                try {
                    assoc.release();
                } catch (IOException ioException) {
                    log.warn("Failed to release association in finally block", ioException);
                }
            }
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


}
