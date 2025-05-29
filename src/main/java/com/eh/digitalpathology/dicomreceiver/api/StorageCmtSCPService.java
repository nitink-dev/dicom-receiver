package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.net.service.DicomServiceRegistry;

import java.io.File;
import java.io.IOException;

    public class StorageCmtSCPService {

        // Storage Commitment SOP Class UID (Push Model)
        public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS = "1.2.840.10008.1.20.1";
        // Storage Commitment SOP Instance UID (always fixed)
        public static final String STORAGE_COMMITMENT_PUSH_MODEL_SOP_INSTANCE = "1.2.840.10008.1.20.1.1";

        public static void installStorageCommitment() throws Exception {

            Device device = new Device("stgcmtscp");
            ApplicationEntity ae = new ApplicationEntity("STGCMT_SCP");
            Connection conn = new Connection();

            device.addConnection(conn);
            device.addApplicationEntity(ae);
            ae.addConnection(conn);
            ae.addTransferCapability(new TransferCapability(null,STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS,TransferCapability.Role.SCP, UID.ImplicitVRLittleEndian));
            DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
            serviceRegistry.addDicomService(new BasicCEchoSCP());
            serviceRegistry.addDicomService(new DicomService() {
                @Override
                public void onClose(Association association) {
                }
                @Override
                public String[] getSOPClasses() {
                    return new String[]{STORAGE_COMMITMENT_PUSH_MODEL_SOP_CLASS};
                }
                @Override
                public void onDimseRQ(Association as, PresentationContext pc,
                                      Dimse dimse, Attributes cmd, PDVInputStream data)
                        throws IOException {
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
                    for (Attributes item : refSOPSeq) {
                        String sopClassUID = item.getString(Tag.ReferencedSOPClassUID);
                        String sopInstanceUID = item.getString(Tag.ReferencedSOPInstanceUID);
                        if (sopClassUID == null || sopInstanceUID == null) {
                            System.out.println("Missing SOP Class UID or Instance UID in a referenced item");
                            continue;
                        }
                        if (checkIfStored(sopClassUID, sopInstanceUID)) {
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
                }

            });
            ae.setDimseRQHandler(serviceRegistry);
            device.bindConnections();
            System.out.println("Storage Commitment SCP is running on AE: STGCMT_SCP");
        }


        private static boolean checkIfStored(String sopClassUID, String sopInstanceUID) {
            File dicomFile = new File("/your/dicom/storage/path/" + sopInstanceUID + ".dcm");
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

    }


