package com.eh.digitalpathology.dicomreceiver.service;

import com.eh.digitalpathology.dicomreceiver.config.DBRestClient;
import com.eh.digitalpathology.dicomreceiver.exceptions.DbConnectorExeption;

import com.eh.digitalpathology.dicomreceiver.model.ApiResponse;
import com.eh.digitalpathology.dicomreceiver.model.DicomDirDocument;
import com.eh.digitalpathology.dicomreceiver.model.DicomRequestDBObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class.getName());

    private final DBRestClient dbRestClient;
    @Value("${db.connector.insert.uri}")
    String  uriInsert;

    @Autowired
    public DatabaseService(DBRestClient dbRestClient) {
        this.dbRestClient = dbRestClient;
    }


    public String insertDicomData(DicomRequestDBObject requestDBObject, String sourceServiceName ) throws DbConnectorExeption {
        log.info("insertDicomData :: Started inserting Dicom-Request-Object: {} \nwaiting for db reply..... ..........",requestDBObject);

        HttpHeaders headers = getHttpHeaders(sourceServiceName);
       try {
           return dbRestClient.exchange(HttpMethod.POST, uriInsert, requestDBObject, new ParameterizedTypeReference<ApiResponse<String>>() {
                   }, httpHeaders -> httpHeaders.putAll(headers))
                   .map(ApiResponse::status).block();
       } catch (Exception ex){
           throw new DbConnectorExeption("DB error", ex.getMessage());
       }

    }

    private static HttpHeaders getHttpHeaders(String serviceName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Service-Name", serviceName);
        return headers;
    }

    public String saveMetaDataInfo(DicomDirDocument dicomDirDocument) {
        try{
            return dbRestClient.exchange(HttpMethod.POST, "dicom/dicomdir", dicomDirDocument, new ParameterizedTypeReference<ApiResponse<String>>() {
            }, null).map(ApiResponse::status).block();
        } catch (Exception ex){
            throw new DbConnectorExeption("DB error", ex.getMessage());
        }
    }
}