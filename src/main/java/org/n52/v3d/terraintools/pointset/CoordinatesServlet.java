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
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.terraintools.helper.PointsetValidation;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class CoordinatesServlet extends HttpServlet {

    protected void writeResponse(HttpServletResponse response, DriveSample driveSample)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/xml");
        out.println("<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>");
        //out.println("<?xml-stylesheet type=\"text/css\" href=\"terrainTools-style.css\"?>");
        out.println("<terrainToolsResponse>");
        out.println("  <userId>" + driveSample.getUserId() + "</userId>");
        out.println("  <applicationId>" + driveSample.getApplicationId() + "</applicationId>");
        out.println("  <projectId>" + driveSample.getProjectId() + "</projectId>");
        out.println("  <pointsetId>" + driveSample.getObjectId() + "</pointsetId>");
        out.println("</terrainToolsResponse>");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        RequestDispatcher rd = request.getRequestDispatcher("points.html");
        rd.forward(request, response);
    }

    protected File makeTemporaryFile(String data) throws IOException {
        File file = File.createTempFile("tmp-pointset", ".xyz");
        FileUtils.writeStringToFile(file, data);
        return file;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String requestType = request.getParameter("request");
        String data = request.getParameter("data");
        String project = request.getParameter("project");
        String pointsetName = request.getParameter("pointset");

        if ("newPointSet".equalsIgnoreCase(requestType)) {
            if (PointsetValidation.validatePointSet(request, response)) {
                File pointsetFile = makeTemporaryFile(data);
                try {
                    String tokenData = (String) request.getSession().getAttribute("token");
                    DriveSample driveSample = new DriveSample(project, pointsetName, pointsetFile, tokenData);
                    writeResponse(response, driveSample);
                }
                catch (Exception exception) {
                    out.println("Something bad happened with Google Drive! " + exception);
                }

                pointsetFile.delete();
            }
        }
        else {
            out.println("Illegal REQUEST parameter value.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
