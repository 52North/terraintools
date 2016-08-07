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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.triturus.examples.gridding.Gridding;
import org.n52.v3d.triturus.gisimplm.GmSimpleElevationGrid;
import org.n52.v3d.triturus.gisimplm.IoElevationGridWriter;
import org.n52.v3d.triturus.vgis.VgPoint;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class ElevationServlet extends HttpServlet {

    protected void doGridding(String pointsetPath, String elevationPath,
            double cellSize, short samplingMethod) {
        Gridding gridding = new Gridding();
        gridding.setInputFile(pointsetPath);
        gridding.setOutputFile(elevationPath);
        gridding.setCellSize(cellSize);
        gridding.setSamplingMethod(samplingMethod);
        gridding.setOutputFormat(IoElevationGridWriter.ARCINFO_ASCII_GRID);
        List<VgPoint> points = gridding.readPointCloud();
        GmSimpleElevationGrid elevGrid = gridding.performGridding(points);
        gridding.writeOutputFile(elevGrid);
    }

    protected void writeResponse(HttpServletResponse response, DriveSample driveSample, String pointsetId)
            throws ServletException, IOException {
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        out.println("<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>");
        //out.println("<?xml-stylesheet type=\"text/css\" href=\"terrainTools-style.css\"?>");
        out.println("<terrainToolsResponse>");
        out.println("  <userId>" + driveSample.getUserId() + "</userId>");
        out.println("  <applicationId>" + driveSample.getApplicationId() + "</applicationId>");
        out.println("  <projectId>" + driveSample.getProjectId() + "</projectId>");
        out.println("  <pointsetId>" + pointsetId + "</pointsetId>");
        out.println("  <elevationId>" + driveSample.getObjectId() + "</elevationId>");
        out.println("</terrainToolsResponse>");
    }

    protected File makeTemporaryFile(InputStream inputStream) throws IOException {
        File pointsetFile = File.createTempFile("tmp-pointset", ".xyz");
        OutputStream outputStream = new FileOutputStream(pointsetFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();
        return pointsetFile;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        RequestDispatcher rd = request.getRequestDispatcher("elevation.html");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String requestType = request.getParameter("request");
        String pointsetId = request.getParameter("pointset");
        String elevationName = request.getParameter("elevation");
        short samplingMethod = Short.parseShort(request.getParameter("method"));
        double cellSize = Double.parseDouble(request.getParameter("cellsize"));

        if (requestType.equalsIgnoreCase("newElevationGrid")) {

            InputStream inputStream = DriveSample.downloadFile(DriveSample.drive, pointsetId);

            File pointsetFile = makeTemporaryFile(inputStream);

            String pointsetPath = pointsetFile.getPath();
            String elevationPath = pointsetFile.getParent() + pointsetFile.separator + elevationName;

            doGridding(pointsetPath, elevationPath, cellSize, samplingMethod);

            File elevationFile = new File(elevationPath);

            String tokenData = (String) request.getSession().getAttribute("token");
            DriveSample driveSample = new DriveSample(DriveSample.PROJECT_FOLDER_NAME, elevationName, elevationFile, tokenData);
            writeResponse(response, driveSample, pointsetId);

            pointsetFile.delete();
            elevationFile.delete();
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
