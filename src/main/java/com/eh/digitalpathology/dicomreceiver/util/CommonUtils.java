package com.eh.digitalpathology.dicomreceiver.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class CommonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class.getName());

    @Value("${storescp.storage.path}")
    private String fileStorePath;


    public String getLocalStoragePath(){
        String receivedFiles = Paths.get(fileStorePath).toAbsolutePath().toString();
        logger.info("getLocalStoragePath :: Received Files Path: {}", receivedFiles);
        return receivedFiles;
    }

}
