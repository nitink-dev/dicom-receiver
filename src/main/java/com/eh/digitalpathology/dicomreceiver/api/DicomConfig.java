package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

@Configuration
public class DicomConfig {

    @Bean
    public Device dicomDevice(StorageCommitmentSCPService scpService) throws IOException, GeneralSecurityException {
        Device device = new Device("EH_ENRICH_DEVICE");
        ApplicationEntity ae = new ApplicationEntity("EH_ENRICH");
        Connection conn = new Connection();
        conn.setPort(2575); // SCP port

        ae.addConnection(conn);
        ae.setAssociationAcceptor(true);

        // Add Transfer Capability
        ae.addTransferCapability(new TransferCapability(null,
                UID.StorageCommitmentPushModel,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian));

        // Register SCP service
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        serviceRegistry.addDicomService(scpService);
        ae.setDimseRQHandler(serviceRegistry);

        device.addConnection(conn);
        device.addApplicationEntity(ae);
        device.setExecutor(Executors.newSingleThreadExecutor());
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

        device.bindConnections();
        return device;
    }
}