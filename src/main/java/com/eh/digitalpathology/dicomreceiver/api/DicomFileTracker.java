package com.eh.digitalpathology.dicomreceiver.api;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DicomFileTracker {
    private static final AtomicInteger counter = new AtomicInteger();

    public static void record(String sopInstanceUID) {
        System.out.println("Received: " + sopInstanceUID);
        counter.incrementAndGet();
        System.out.println("Count Score: "+ counter);
    }

    public static int getCount() {
        return counter.get();
    }
}