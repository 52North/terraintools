/**
 * ﻿Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 * - Apache License, version 2.0 - Apache Software License, version 1.0 - GNU
 * Lesser General Public License, version 3 - Mozilla Public License, versions
 * 1.0, 1.1 and 2.0 - Common Development and Distribution License (CDDL),
 * version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders if
 * the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package org.n52.v3d.terraintools.drive;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.n52.v3d.terraintools.auth.Signin.CLIENT_ID;
import static org.n52.v3d.terraintools.auth.Signin.CLIENT_SECRET;
import static org.n52.v3d.terraintools.auth.Signin.JSON_FACTORY;
import static org.n52.v3d.terraintools.auth.Signin.TRANSPORT;

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
    public static String PROJECT_FOLDER_NAME = "TestingProject";
    private static String PROJECT_FOLDER_ID;

    private static final String UPLOAD_FILE_PATH = RelativePaths.DATA_XYZ;
    private static java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);
    private static String UPLOAD_FILE_NAME = UPLOAD_FILE.getName();
    private static String UPLOAD_FILE_ID;

    private static String USER_ID;

    public static Drive drive;

    public DriveSample(String project, String fileName, java.io.File file, String tokenData) {
        try {
            init(tokenData);
            About about = drive.about().get().execute();
            USER_ID = about.getName();

            PROJECT_FOLDER_NAME = project;
            UPLOAD_FILE_NAME = fileName;
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
        catch (Exception exception) {
            System.err.println(exception);
        }
    }

    public String getApplicationId() {
        return APPLICATION_FOLDER_ID;
    }

    public String getProjectId() {
        return PROJECT_FOLDER_ID;
    }

    public String getObjectId() {
        return UPLOAD_FILE_ID;
    }

    public String getUserId() {
        return USER_ID;
    }

    public static void init(String tokenData) throws IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(JSON_FACTORY)
                .setTransport(TRANSPORT)
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET).build()
                .setFromTokenResponse(JSON_FACTORY.fromString(
                                tokenData, GoogleTokenResponse.class));
        drive = new Drive.Builder(TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
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

    public static InputStream downloadFile(Drive service, String fileId) {

        try {
            File file = drive.files().get(fileId).execute();
            System.out.println("Title: " + file.getTitle());
            if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
                HttpResponse resp
                        = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                        .execute();
                return resp.getContent();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFileName(Drive service, String fileId) throws IOException {
        try {
            File file = drive.files().get(fileId).execute();
            return file.getTitle();
        }
        catch (Exception exception) {
            return null;
        }
    }

}
