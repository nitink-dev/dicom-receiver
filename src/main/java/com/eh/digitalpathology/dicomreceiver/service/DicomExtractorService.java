package com.eh.digitalpathology.dicomreceiver.service;

import com.eh.digitalpathology.dicomreceiver.exceptions.DicomAttributesException;
import com.eh.digitalpathology.dicomreceiver.model.DicomRequestDBObject;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@Service
public class DicomExtractorService {
    private static final Logger log = LoggerFactory.getLogger( DicomExtractorService.class.getName( ) );

    @Value("${dicom.barcode.generation.enable}")
    private boolean enableBarcodeGeneration;

    public DicomRequestDBObject extract ( String finalPath, String fileName ) throws DicomAttributesException {
        DicomRequestDBObject dicomRequestDBObject = new DicomRequestDBObject( );
        File dicomFile = new File( fileName );

        try ( DicomInputStream dicomInputStream = new DicomInputStream( dicomFile ) ) {

            log.info( "extract:: ************ Started extracting file : {}", dicomFile );
            // Read the DICOM dataset
            Attributes attributes = dicomInputStream.readDataset( Tag.PixelData );

            checkAttributeNull( dicomRequestDBObject, attributes.getString( Tag.SOPInstanceUID ), "SOPInstanceUID" );
            checkAttributeNull( dicomRequestDBObject, attributes.getString( Tag.SeriesInstanceUID ), "SeriesInstanceUID" );


            if ( attributes.getString( Tag.BarcodeValue ) == null && enableBarcodeGeneration ) {
                String barcodeValue = generateShortBarcode( attributes.getString( Tag.StudyInstanceUID ), attributes.getString( Tag.SeriesInstanceUID ) );
                attributes.setString( Tag.BarcodeValue, VR.LO, barcodeValue );
                log.info( "Generated and set new barcode: {}", barcodeValue );
            }

            checkAttributeNull( dicomRequestDBObject, attributes.getString( Tag.BarcodeValue ), "Barcode" );

            String path = moveFileToTempStore( finalPath, dicomFile, attributes.getString( Tag.SOPInstanceUID ), attributes.getString( Tag.StudyInstanceUID ), attributes.getString( Tag.SeriesInstanceUID ) );
            dicomRequestDBObject.setIntermediateStoragePath( path );
            log.info( "extract :: Created DB object to save: {}", dicomRequestDBObject );
            // Get the current timestamp in ISO 8601 format
            String currentTimestamp = DateTimeFormatter.ISO_INSTANT.withZone( ZoneOffset.UTC ).format( Instant.now( ) );
            dicomRequestDBObject.setOriginalStudyInstanceUid( attributes.getString( Tag.StudyInstanceUID ) );
            dicomRequestDBObject.setDicomInstanceReceivedTimestamp( currentTimestamp );

        } catch ( IOException e ) {
            throw new DicomAttributesException( "BAD_REQUEST", e.getMessage( ) );
        } finally {
            try {
                Files.deleteIfExists( dicomFile.toPath( ) );
            } catch ( IOException e ) {
                log.error( "extract :: Unable to delete file : {}", e.getMessage( ) );
            }
        }

        log.info( " Extracting and moving file done for {}", dicomFile.getName( ) );

        return dicomRequestDBObject;
    }


    private static void checkAttributeNull ( DicomRequestDBObject dicomRequestDBObject, String attributeString, String tagName ) {
        log.info( "{}: {}", tagName, attributeString );
        if ( null != attributeString ) {
            switch ( tagName ) {
                case "Barcode" -> dicomRequestDBObject.setBarcode( attributeString );
                case "SOPInstanceUID" -> dicomRequestDBObject.setSopInstanceUid( attributeString );
                case "SeriesInstanceUID" -> dicomRequestDBObject.setSeriesInstanceUid( attributeString );
                default -> throw new DicomAttributesException( "INVALID ATTRIBUTE", tagName + " is NULL" );
            }
        } else {
            throw new DicomAttributesException( "INVALID ATTRIBUTE", tagName + " is NULL" );
        }
    }

    private static String moveFileToTempStore ( String finalPath, File dicomFile, String sopInstanceID, String studyID, String seriesID ) throws DicomAttributesException {
        log.info( "moveFileToTempStore :: =====================Creating file server path for current file=============================================>" );
        String outputPath = Paths.get( finalPath, studyID, seriesID, sopInstanceID + ".dcm" ).toString( );
        log.info( "moveFileToTempStore :: File server path for file: {} ", outputPath );

        try {
            File output = new File( outputPath );
            Files.createDirectories( output.getParentFile( ).toPath( ) );
            Files.copy( dicomFile.toPath( ), Paths.get( outputPath ), StandardCopyOption.REPLACE_EXISTING );
            log.info( "moveFileToTempStore :: File moved to file-server {}", output.getParentFile( ).toPath( ) );
            log.info( "moveFileToTempStore :: DCM File: {} ", outputPath );
        } catch ( Exception fe ) {
            throw new DicomAttributesException( "BAD_REQUEST", fe.getMessage( ) );
        }
        return outputPath;
    }


    private static String generateShortBarcode ( String studyUID, String seriesUID ) {
        try {
            String input = studyUID + seriesUID;
            MessageDigest md = MessageDigest.getInstance( "SHA-256" );
            byte[] hash = md.digest( input.getBytes( StandardCharsets.UTF_8 ) );

            // Convert to positive BigInteger and then to Base36
            String base36 = new BigInteger( 1, hash ).toString( 36 ).toUpperCase( );

            // Take the first 4 characters and prefix with "BC"
            return "BC-" + base36.substring( 0, 4 );
        } catch ( NoSuchAlgorithmException e ) {
            throw new DicomAttributesException( "SHA-256 algorithm not found", e.getMessage() );
        }
    }


}