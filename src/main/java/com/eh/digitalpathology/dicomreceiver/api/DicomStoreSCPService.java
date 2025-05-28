package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DicomStoreSCPService extends BasicCStoreSCP {

    private final Path storageDir = Paths.get("/opt/dicom/storage");

    public DicomStoreSCPService() {
        super("*");
    }

    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq,
                         PDVInputStream data, Attributes rsp) throws IOException {
        String sopInstanceUID = rq.getString(Tag.AffectedSOPInstanceUID);

        Files.createDirectories(storageDir);
        File file = storageDir.resolve(sopInstanceUID + ".dcm").toFile();

        Attributes dataset;
        try (DicomInputStream dis = new DicomInputStream(data)) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.URI);
            dataset = dis.readDataset(-1, -1);
        }

        try (DicomOutputStream dos = new DicomOutputStream(file)) {
            dos.writeDataset(dataset.createFileMetaInformation(pc.getTransferSyntax()), dataset);
        }

        DicomFileTracker.record(sopInstanceUID);
    }
}