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
package org.n52.v3d.terraintools.pointset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.terraintools.helper.FloodingHelper;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class FloodingServlet extends HttpServlet {

    private String floodingPath = ""; //data/test.html
    private int point_x = 0, point_y = 0;
    private double waterLevel = 10.0;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        FloodingHelper floodingHelper = new FloodingHelper();
        String outputFlooding = floodingHelper.createFlooding(floodingPath, point_x, point_y, waterLevel);
        String result = floodingHelper.combineFlooding(floodingPath, outputFlooding);
        writeResponse(request, response, result);

    }

    protected void writeResponse(HttpServletRequest request, HttpServletResponse response, String result)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        InputStream inputStream = new FileInputStream(result);
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        String x3dom = writer.toString();
        out.println(x3dom);

        new File(result).delete();
    }

    protected File makeTemporaryFile(InputStream inputStream) throws IOException {
        File floodingFile = File.createTempFile("tmp-flooding", ".html");
        OutputStream outputStream = new FileOutputStream(floodingFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();

        return floodingFile;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String position = request.getParameter("position");
        waterLevel = Double.parseDouble(request.getParameter("waterlevel"));
        String visualizationId = request.getParameter("objId");
        
        String[] point = position.split(",");
        point_x = Integer.parseInt(point[0]);
        point_y = Integer.parseInt(point[1]);

        InputStream inputStream = DriveSample.downloadFile(DriveSample.drive, visualizationId);
        File floodingFile = makeTemporaryFile(inputStream);
        floodingPath = floodingFile.getPath();
        processRequest(request, response);
    }

}
