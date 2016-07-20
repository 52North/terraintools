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
package org.n52.v3d.terraintools.pointset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String visualizationId = (String) request.getSession().getAttribute("visualizationId");

        InputStream inputStream = DriveSample.downloadFile(DriveSample.drive, visualizationId);

        File pointsetFile = File.createTempFile("tmp-visualization", ".html");
        OutputStream outputStream = new FileOutputStream(pointsetFile);
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();

        IoElevationGridReader reader = new IoElevationGridReader(IoElevationGridReader.X3DOM);
        GmSimpleElevationGrid grid = null;
        try {
            grid = reader.readFromFile(pointsetFile.getPath());
        }
        catch (T3dException e) {
            e.printStackTrace();
        }

        String position = request.getParameter("pos");

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

        VgLineString defLine = new GmLineString(pointString);

        FltElevationGrid2Profile proc = new FltElevationGrid2Profile();
        VgProfile prof = proc.transform(grid, defLine);

        String visualizationName = "cross-section.svg";
        String visualizationPath = pointsetFile.getParent() + pointsetFile.separator + visualizationName;
        
        IoProfileWriter lWriter = new IoProfileWriter(IoProfileWriter.SVG);
        lWriter.writeToFile(prof, visualizationPath);

        File elevationFile = new File(visualizationPath);
        
        String tokenData = (String) request.getSession().getAttribute("token");
        DriveSample driveSample = new DriveSample(DriveSample.PROJECT_FOLDER_NAME, visualizationName, elevationFile, tokenData);

        response.setContentType("text/html");
        String crossSectionId = driveSample.getObjectId();
        inputStream = DriveSample.downloadFile(DriveSample.drive, crossSectionId);
        
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer);
        String x3dom = writer.toString();
        out.println(x3dom);

        request.getSession().setAttribute("crossSectionId", crossSectionId);
        pointsetFile.delete();
        elevationFile.delete();
        
    }

}
