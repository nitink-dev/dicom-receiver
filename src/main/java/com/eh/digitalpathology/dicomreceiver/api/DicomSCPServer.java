package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

@Service
public class DicomSCPServer {
    private final Device device;
    private final ApplicationEntity ae;
    private final Connection conn;
    private static final String VL_WHOLE_SLIDE_MICROSCOPY_IMAGE_STORAGE = "1.2.840.10008.5.1.4.1.1.77.1.6";

    // Storage Commitment SOP Class UID (Push Model)
    public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS = "1.2.840.10008.1.20.1";
    // Storage Commitment SOP Instance UID (always fixed)
    public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_INSTANCE = "1.2.840.10008.1.20.1.1";

    @Autowired
    private final StorageCommitmentSCPService storageCommitmentSCPService;
    @Autowired
    private final DicomStoreSCPService dicomStoreSCPService;

    public DicomSCPServer(StorageCommitmentSCPService storageCommitmentSCPService, DicomStoreSCPService dicomStoreSCPService) throws IOException {
        this.storageCommitmentSCPService = storageCommitmentSCPService;
        this.dicomStoreSCPService = dicomStoreSCPService;
        device = new Device("dicom-scp");
        ae = new ApplicationEntity("EH_ENRICH");
        conn = new Connection();


        device.setExecutor(Executors.newCachedThreadPool());
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.addConnection(conn);

        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(dicomStoreSCPService);
        serviceRegistry.addDicomService(storageCommitmentSCPService);
        device.setDimseRQHandler(serviceRegistry);

        // Define SOP class support

        ae.addTransferCapability(new TransferCapability(null,
                UID.VLWholeSlideMicroscopyImageStorage,
                TransferCapability.Role.SCP,
                new String[]{
                        UID.ExplicitVRLittleEndian,
                        UID.ImplicitVRLittleEndian,
                        UID.JPEGBaseline8Bit
                }));

        ae.addTransferCapability(new TransferCapability(null,
                UID.StorageCommitmentPushModel,
                TransferCapability.Role.SCP,
                new String[]{
                        UID.ImplicitVRLittleEndian,
                        UID.ExplicitVRLittleEndian,
                }));

        ae.addTransferCapability(new TransferCapability(null,
                STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian));



        conn.setPort(2576);
    }

    public void start() throws IOException, InterruptedException, GeneralSecurityException {
        device.bindConnections();
    }
}
