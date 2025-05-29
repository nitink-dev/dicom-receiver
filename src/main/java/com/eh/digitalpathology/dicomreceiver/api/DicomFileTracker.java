package com.eh.digitalpathology.dicomreceiver.api;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DicomFileTracker {
    private static final AtomicInteger counter = new AtomicInteger();


    public static void record(String sopInstanceUID) {
        System.out.println("Received: " + sopInstanceUID);
        counter.incrementAndGet();
        System.out.println("Count Score: "+ counter);
    }

    public static void saveRecords(Sequence successSeq){
        List<String> sopInstanceUIDs = new ArrayList<>();

        for (Attributes item : successSeq) {
            String sopInstanceUID = item.getString(Tag.ReferencedSOPInstanceUID);
            if (sopInstanceUID != null) {
                sopInstanceUIDs.add(sopInstanceUID);
            }
        }

        int count = sopInstanceUIDs.size();
        System.out.println("Storing Storage Commitment success records:");
        for (String uid : sopInstanceUIDs) {
            System.out.println(" - SOP Instance UID: " + uid);
        }

        System.out.println("Total committed SOP Instances: "+ count);

    }

    public static int getCount() {
        return counter.get();
    }
}