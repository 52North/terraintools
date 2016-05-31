/**
 * Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.v3d.terraintools.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.n52.v3d.terraintools.helper.RelativePaths;

// Adhitya: https://github.com/google/google-api-java-client-samples/tree/master/drive-cmdline-sample
public class DriveSample {

    /**
     * Be sure to specify the name of your application. If the application name
     * is {@code null} or blank, the application will log a warning. Suggested
     * format is "MyCompany-ProductName/1.0".
     */
    private static final String APPLICATION_NAME = "AdhityaTestingDrive";
    private static final String APPLICATION_FOLDER_NAME = "52n-terraintools";
    private static String APPLICATION_FOLDER_ID;
    private static String PROJECT_FOLDER_NAME = "TestingProject";
    private static String PROJECT_FOLDER_ID;
    
    private static final String UPLOAD_FILE_PATH = RelativePaths.DATA_XYZ;
    private static java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);
    private static String UPLOAD_FILE_NAME = UPLOAD_FILE.getName();
    private static String UPLOAD_FILE_ID;
    
    private static String USER_ID;
    
    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR
            = new java.io.File(System.getProperty("user.home"), ".store/drive_sample");

    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to
     * make it a single globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global Drive API client.
     */
    private static Drive drive;

    public DriveSample(String project, String pointsetName, java.io.File file) throws Exception {
        init();
        
        About about = drive.about().get().execute();
        USER_ID = about.getName();
        
        PROJECT_FOLDER_NAME = project;
        UPLOAD_FILE_NAME = pointsetName;
        UPLOAD_FILE = file;

        View.header1("Retrieving application folder");
        File applicationFolder = retrieveApplicationFolder();
        System.out.println("Application Folder ID: " + APPLICATION_FOLDER_ID);

        View.header1("Retrieving project folder");
        File projectFolder = retrieveProjectFolder();
        System.out.println("Project Folder ID: " + PROJECT_FOLDER_ID);

        View.header1("Starting Resumable Media Upload");
        File uploadedFile = uploadFile(projectFolder, true);

        View.header1("Success!");
    }
    
    public String getApplicationId(){
        return APPLICATION_FOLDER_ID;
    }
    
    public String getProjectId(){
        return PROJECT_FOLDER_ID;
    }
    
    public String getPointsetId(){
        return UPLOAD_FILE_ID;
    }
    
    public String getUserId(){
        return USER_ID;
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize() throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(DriveSample.class.getResourceAsStream("/client_secrets.json")));
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static void init() throws Exception {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        // authorization
        Credential credential = authorize();
        // set up the global Drive instance
        drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    public static void main(String[] args) {
        try {
            init();
            // run commands
            View.header1("Retrieving application folder");
            File applicationFolder = retrieveApplicationFolder();
            System.out.println("Application Folder ID: " + APPLICATION_FOLDER_ID);

            View.header1("Retrieving project folder");
            File projectFolder = retrieveProjectFolder();
            System.out.println("Project Folder ID: " + PROJECT_FOLDER_ID);

            View.header1("Starting Resumable Media Upload");
            File uploadedFile = uploadFile(projectFolder, true);

            View.header1("Success!");
        }
        catch (Exception exception) {
            System.err.println(exception);
        }
    }

    // https://developers.google.com/drive/v2/reference/files/list#examples
    private static File retrieveApplicationFolder() throws IOException {
        List<File> result = new ArrayList();
        Files.List request = drive.files().list();
        File folder;
        FileList files = request
                .setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and title='" + APPLICATION_FOLDER_NAME + "'")
                .execute();
        result.addAll(files.getItems());
        if (!result.isEmpty()) {
            folder = result.get(0);
        }
        else {
            folder = createFolder(APPLICATION_FOLDER_NAME);
        }
        APPLICATION_FOLDER_ID = folder.getId();
        return folder;
    }

    private static File retrieveProjectFolder() throws IOException {
        List<File> result = new ArrayList();
        Files.List request = drive.files().list();
        File folder;
        FileList files = request
                .setQ("mimeType='application/vnd.google-apps.folder' and "
                        + "trashed=false and title='" + PROJECT_FOLDER_NAME + "' and "
                        + "'" + APPLICATION_FOLDER_ID + "' in parents")
                .execute();
        result.addAll(files.getItems());
        if (!result.isEmpty()) {
            folder = result.get(0);
        }
        else {
            View.header1("Creating project in application folder");
            folder = createSubFolder(PROJECT_FOLDER_NAME, APPLICATION_FOLDER_ID);
        }
        PROJECT_FOLDER_ID = folder.getId();
        return folder;
    }

    private static File createFolder(String title) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setTitle(title);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = drive.files().insert(fileMetadata).setFields("id").execute();
        return file;
    }

    private static File createSubFolder(String title, String parentId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setTitle(title);
        fileMetadata.setParents(Arrays.asList(new ParentReference().setId(parentId)));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = drive.files().insert(fileMetadata).setFields("id").execute();
        return file;
    }

    /**
     * Uploads a file using either resumable or direct media upload.
     */
    private static File uploadFile(File parent, boolean useDirectUpload) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setTitle(UPLOAD_FILE_NAME);

        FileContent mediaContent = new FileContent("text/plain", UPLOAD_FILE);

        fileMetadata.setParents(Arrays.asList(new ParentReference().setId(parent.getId())));

        Drive.Files.Insert insert = drive.files().insert(fileMetadata, mediaContent);
        MediaHttpUploader uploader = insert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(useDirectUpload);
        uploader.setProgressListener(new FileUploadProgressListener());
        File file = insert.execute();
        UPLOAD_FILE_ID = file.getId();
        return file;
    }
}
