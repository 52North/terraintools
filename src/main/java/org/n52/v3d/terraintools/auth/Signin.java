/**
 * ﻿Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
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
package org.n52.v3d.terraintools.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class Signin {
    /*
     * Default HTTP transport to use to make HTTP requests.
     */

    public static final HttpTransport TRANSPORT = new NetHttpTransport();

    /*
     * Default JSON factory to use to deserialize JSON.
     */
    public static final JacksonFactory JSON_FACTORY = new JacksonFactory();

    /*
     * Gson object to serialize JSON responses to requests to this servlet.
     */
    public static final Gson GSON = new Gson();

    /*
     * Creates a client secrets object from the client_secrets.json file.
     */
    public static GoogleClientSecrets clientSecrets;

    static {
        try {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(Signin.class.getResourceAsStream("/client_secrets.json")));
            System.out.println("Static serving from signin"+ clientSecrets);
        }
        catch (IOException e) {
            throw new Error("No client_secrets.json found", e);
        }
    }

    /*
     * This is the Client ID that you generated in the API Console.
     */
    public static final String CLIENT_ID = clientSecrets.getWeb().getClientId();

    /*
     * This is the Client Secret that you generated in the API Console.
     */
    public static final String CLIENT_SECRET = clientSecrets.getWeb().getClientSecret();

    /*
     * Optionally replace this with your application's name.
     */
    public static final String APPLICATION_NAME = "Google+ Java Quickstart";

}
