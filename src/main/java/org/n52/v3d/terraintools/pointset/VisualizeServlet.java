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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.triturus.examples.gridding.Gridding;
import org.n52.v3d.triturus.gisimplm.GmPoint;
import org.n52.v3d.triturus.gisimplm.GmSimpleElevationGrid;
import org.n52.v3d.triturus.vgis.VgPoint;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class VisualizeServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        RequestDispatcher rd = request.getRequestDispatcher("visualize.html");  
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String pointsetId = request.getParameter("pointset");
        String visualizationName = request.getParameter("visualization");
        String format = request.getParameter("format");

        InputStream inputStream = DriveSample.downloadFile(DriveSample.drive, pointsetId);

        File pointsetFile = File.createTempFile("tmp-pointset", ".xyz");
        OutputStream outputStream = new FileOutputStream(pointsetFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();

        String pointsetPath = pointsetFile.getPath();
        String visualizationPath = pointsetFile.getParent() + pointsetFile.separator + visualizationName;

        Gridding gridding = new Gridding();
        gridding.setInputFile(pointsetPath);
        gridding.setOutputFile(visualizationPath);
        gridding.setOutputFormat(format);
        List<VgPoint> points = gridding.readPointCloud();
        GmSimpleElevationGrid elevGrid = gridding.performGridding(points);
        gridding.writeOutputFile(elevGrid);
        
        File elevationFile = new File(visualizationPath);
        
        // Allow clicking option
        String content = FileUtils.readFileToString(elevationFile);
        content = content.replace("<Shape>", "<Shape onClick=\"handleClick(event)\">");
        content = content.replace("</body>", 
            "</body>\n" +
            "<script type=\"text/javascript\" src=\"https://rawgit.com/kamakshidasan/terraintools/master/src/main/resources/select.js\"></script>\n" +
            "<div id=\"insert\"></div>"    
        );
        FileUtils.writeStringToFile(elevationFile, content);

        String tokenData = (String) request.getSession().getAttribute("token");
        DriveSample driveSample = new DriveSample(DriveSample.PROJECT_FOLDER_NAME, visualizationName, elevationFile, tokenData);

        response.setContentType("text/html");
        String visualizationId = driveSample.getObjectId();
        inputStream = DriveSample.downloadFile(DriveSample.drive, visualizationId);
        
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        String x3dom = writer.toString();
        out.println(x3dom);

        request.getSession().setAttribute("visualizationId", visualizationId);
        pointsetFile.delete();
        elevationFile.delete();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
