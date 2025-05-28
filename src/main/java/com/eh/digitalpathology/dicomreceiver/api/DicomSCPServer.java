package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

@Component
public class DicomSCPServer {
    private final Device device;
    private final ApplicationEntity ae;
    private final Connection conn;
    private static final String VL_WHOLE_SLIDE_MICROSCOPY_IMAGE_STORAGE = "1.2.840.10008.5.1.4.1.1.77.1.6";


    public DicomSCPServer(DicomSCPConfig config) throws IOException {
        device = new Device("dicom-scp");
        ae = new ApplicationEntity(config.getAETitle());
        conn = new Connection();


        device.setExecutor(Executors.newCachedThreadPool());
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.addConnection(conn);

        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new DicomStoreSCPService());
        serviceRegistry.addDicomService(new StorageCommitmentSCPService());
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


        conn.setPort(config.getPort());
    }

    public void start() throws IOException, InterruptedException, GeneralSecurityException {
        device.bindConnections();
    }
}
