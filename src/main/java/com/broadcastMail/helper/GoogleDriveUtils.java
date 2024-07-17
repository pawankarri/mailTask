//package com.broadcastMail.helper;
//
//import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.googleapis.util.Utils;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.DriveScopes;
//import com.google.api.services.drive.model.File;
//
//import java.io.*;
//import java.security.GeneralSecurityException;
//import java.util.Collections;
//
//public class GoogleDriveUtils {
//    private static final String APPLICATION_NAME = "mass-mails";
//
//    public static InputStream downloadFileFromDrive(String username, String password, String filename) throws IOException, GeneralSecurityException {
//        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream("fetch_appraisals/"+filename))
//                .createScoped(Collections.singleton(DriveScopes.DRIVE));
//
//        Drive driveService = new Drive.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                Utils.getDefaultJsonFactory(),
//                credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//
//        // Search for the file by name in the specified folder
//        String query = "name='" + filename + "'";
//        Drive.Files.List request = driveService.files().list().setQ(query);
//        com.google.api.services.drive.model.FileList files = request.execute();
//        if (files.getFiles().isEmpty()) {
//            throw new IOException("File not found in Google Drive: " + filename);
//        }
//        File driveFile = files.getFiles().get(0);
//        InputStream inputStream = driveService.files().get(driveFile.getId()).executeMediaAsInputStream();
//        return inputStream;
//    }
//}
