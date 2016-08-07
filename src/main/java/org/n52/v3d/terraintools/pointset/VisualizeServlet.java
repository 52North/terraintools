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
import java.io.StringWriter;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.triturus.examples.gridding.Gridding;
import org.n52.v3d.triturus.gisimplm.GmSimpleElevationGrid;
import org.n52.v3d.triturus.gisimplm.IoElevationGridReader;
import org.n52.v3d.triturus.gisimplm.IoElevationGridWriter;
import org.n52.v3d.triturus.vgis.VgPoint;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class VisualizeServlet extends HttpServlet {

    GmSimpleElevationGrid setGrid(GmSimpleElevationGrid grid) {
        for (int j = 0; j < grid.numberOfColumns(); j++) {
            for (int i = 0; i < grid.numberOfRows(); i++) {
                if (!grid.isSet(i, j)) {
                    grid.setValue(i, j, 0.0);
                }
            }
        }
        return grid;
    }

    protected File makeTemporaryFile(InputStream inputStream, String fileExtension) throws IOException {
        File pointsetFile = File.createTempFile("tmp-pointset", "." + fileExtension);
        OutputStream outputStream = new FileOutputStream(pointsetFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();

        return pointsetFile;
    }

    protected void doGridding(String pointsetPath, String elevationPath,
            double cellSize, short samplingMethod, String format) {
        Gridding gridding = new Gridding();
        gridding.setInputFile(pointsetPath);
        gridding.setOutputFile(elevationPath);
        gridding.setCellSize(cellSize);
        gridding.setSamplingMethod(samplingMethod);
        gridding.setOutputFormat(format);
        List<VgPoint> points = gridding.readPointCloud();
        GmSimpleElevationGrid elevGrid = gridding.performGridding(points);
        gridding.writeOutputFile(elevGrid);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        RequestDispatcher rd = request.getRequestDispatcher("visualize.html");
        rd.forward(request, response);
    }

    protected void writeGrid(String readerFormat, String pointsetPath,
            String visualizationPath, String writerFormat) {
        IoElevationGridReader reader = new IoElevationGridReader(readerFormat);
        GmSimpleElevationGrid grid = reader.readFromFile(pointsetPath);
        grid = setGrid(grid);
        IoElevationGridWriter writer = new IoElevationGridWriter(writerFormat);
        writer.writeToFile(grid, visualizationPath);
    }

    protected void addClicking(String visualizationPath) throws IOException {
        File visualizationFile = new File(visualizationPath);
        String content = FileUtils.readFileToString(visualizationFile);
        content = content.replace("<Shape>", "<Shape onClick=\"handleClick(event)\">");
        content = content.replace("</body>",
                "</body>\n"
                + "<script type=\"text/javascript\" src=\"https://rawgit.com/kamakshidasan/terraintools/master/src/main/resources/select.js\"></script>\n"
                + "<div id=\"insert\"></div>"
        );
        FileUtils.writeStringToFile(visualizationFile, content);
    }

    protected void writeResponse(HttpServletResponse response,
            DriveSample driveSample, InputStream inputStream, String format)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        String visualizationId = driveSample.getObjectId();
        inputStream = DriveSample.downloadFile(DriveSample.drive, visualizationId);

        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        String output = writer.toString();
        output = output.replace("<head>",
                "<head>\n"
                + "<meta name=\"visualizationId\" content=\"" + visualizationId + "\">"
        );
        out.println(output);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pointsetId = request.getParameter("pointset");
        String visualizationName = request.getParameter("visualization");
        String format = request.getParameter("format");
        short samplingMethod = Short.parseShort(request.getParameter("method"));
        double cellSize = Double.parseDouble(request.getParameter("cellsize"));

        InputStream inputStream = DriveSample.downloadFile(
                DriveSample.drive, pointsetId);
        String fileExtension = FilenameUtils.getExtension(
                DriveSample.getDownloadFileName());

        File pointsetFile = makeTemporaryFile(inputStream, fileExtension);
        String pointsetPath = pointsetFile.getPath();
        String visualizationPath = pointsetFile.getParent() + pointsetFile.separator + visualizationName;

        if (fileExtension.equalsIgnoreCase("xyz")) {
            doGridding(pointsetPath, visualizationPath, cellSize, samplingMethod, format);
        }
        else if (fileExtension.equalsIgnoreCase("asc") || fileExtension.equalsIgnoreCase("grd")) {
            writeGrid(IoElevationGridReader.ARCINFO_ASCII_GRID, pointsetPath, visualizationPath, format);
        }
        else if (fileExtension.equalsIgnoreCase("x3d") || fileExtension.equalsIgnoreCase("x3dom")
                || fileExtension.equalsIgnoreCase("html")) {
            writeGrid(IoElevationGridReader.X3DOM, pointsetPath, visualizationPath, format);
        }

        addClicking(visualizationPath);

        File visualizationFile = new File(visualizationPath);

        String tokenData = (String) request.getSession().getAttribute("token");
        DriveSample driveSample = new DriveSample(DriveSample.PROJECT_FOLDER_NAME, visualizationName, visualizationFile, tokenData);

        writeResponse(response, driveSample, inputStream, format);

        pointsetFile.delete();
        visualizationFile.delete();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
