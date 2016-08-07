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
import org.n52.v3d.triturus.core.T3dException;
import org.n52.v3d.triturus.gisimplm.FltElevationGrid2Profile;
import org.n52.v3d.triturus.gisimplm.GmLineString;
import org.n52.v3d.triturus.gisimplm.GmSimpleElevationGrid;
import org.n52.v3d.triturus.gisimplm.IoElevationGridReader;
import org.n52.v3d.triturus.gisimplm.IoProfileWriter;
import org.n52.v3d.triturus.vgis.VgLineString;
import org.n52.v3d.triturus.vgis.VgPoint;
import org.n52.v3d.triturus.vgis.VgProfile;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class CrossSectionServlet extends HttpServlet {

    protected File makeTemporaryFile(InputStream inputStream) throws IOException {
        File pointsetFile = File.createTempFile("tmp-visualization", ".html");
        OutputStream outputStream = new FileOutputStream(pointsetFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();

        return pointsetFile;
    }

    protected GmSimpleElevationGrid readGrid(File pointsetFile) {
        IoElevationGridReader reader = new IoElevationGridReader(IoElevationGridReader.X3DOM);
        GmSimpleElevationGrid grid = null;
        try {
            grid = reader.readFromFile(pointsetFile.getPath());
        }
        catch (T3dException e) {
            e.printStackTrace();
        }
        return grid;
    }

    protected String getPointString(GmSimpleElevationGrid grid, String position) {
        String[] points = position.split(",");

        String pointString = "";
        for (int i = 0; i < (points.length) / 2; i++) {
            int j = 2 * i;
            int k = j + 1;
            int x = Integer.parseInt(points[j]);
            int y = Integer.parseInt(points[k]);
            VgPoint point = grid.getPoint(x, y);
            String value = point.getX() + "," + point.getY() + "," + point.getZ();
            if (i == 0) {
                pointString = value;
            }
            else {
                pointString = pointString + "," + value;
            }
        }
        return pointString;
    }

    protected void writeCrossSection(GmSimpleElevationGrid grid, VgLineString defLine, String crossSectionPath) {
        FltElevationGrid2Profile proc = new FltElevationGrid2Profile();
        VgProfile prof = proc.transform(grid, defLine);

        IoProfileWriter lWriter = new IoProfileWriter(IoProfileWriter.SVG);
        lWriter.writeToFile(prof, crossSectionPath);
    }

    protected String getCrossSectionPath(String name, File pointsetFile) {
        return pointsetFile.getParent() + pointsetFile.separator + name;
    }

    protected void writeResponse(HttpServletResponse response, DriveSample driveSample, InputStream inputStream)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String crossSectionId = driveSample.getObjectId();
        inputStream = DriveSample.downloadFile(DriveSample.drive, crossSectionId);

        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        String x3dom = writer.toString();
        out.println(x3dom);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String visualizationId = request.getParameter("objId");
        String position = request.getParameter("pos");

        InputStream inputStream = DriveSample.downloadFile(DriveSample.drive, visualizationId);

        File pointsetFile = makeTemporaryFile(inputStream);
        GmSimpleElevationGrid grid = readGrid(pointsetFile);
        String pointString = getPointString(grid, position);
        VgLineString defLine = new GmLineString(pointString);

        String crossSectionName = "cross-section.svg";
        String crossSectionPath = getCrossSectionPath(crossSectionName, pointsetFile);

        writeCrossSection(grid, defLine, crossSectionPath);

        File elevationFile = new File(crossSectionPath);
        String tokenData = (String) request.getSession().getAttribute("token");
        DriveSample driveSample = new DriveSample(DriveSample.PROJECT_FOLDER_NAME, crossSectionName, elevationFile, tokenData);
        
        writeResponse(response, driveSample, inputStream);

        pointsetFile.delete();
        elevationFile.delete();

    }

}
