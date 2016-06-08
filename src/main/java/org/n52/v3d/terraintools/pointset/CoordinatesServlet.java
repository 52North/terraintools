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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.n52.v3d.terraintools.drive.DriveSample;
import org.n52.v3d.triturus.examples.gridding.Gridding;
import org.n52.v3d.triturus.gisimplm.GmPoint;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class CoordinatesServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Terrain Points</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<form action='points' method='POST'>");
        out.println("Project Name: <input type='text' name='project' value='GSoC_Testing'/>");
        out.println("<br>");
        out.println("<br>");
        out.println("File Name: <input type='text' name='pointset' value='PointSet.xyz'/>");
        out.println("<br>");
        out.println("<br>");
        out.println("Enter your coordinates");
        out.println("<br>");
        out.println("<br>");
        out.println("<input type='hidden' name='request' value='newPointSet'>");
        out.println("<textarea name='data' rows='5' cols='50'></textarea>");
        out.println("<br>");
        out.println("<br>");
        out.println("<input type='submit' value='New Point Set'>");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }

    protected boolean validatePointSet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        boolean valid = true;
        String data = request.getParameter("data");
        ArrayList<GmPoint> points = new ArrayList();
        String[] lines = data.split("\\n");

        for (int i = 0; i < lines.length && valid == true; i++) {
            String line = lines[i];
            line = line.trim();
            line = line.replaceAll(" +", " ");
            line = line.replaceAll(" ", ",");
            try {
                GmPoint point = new GmPoint(line);
                points.add(point);
            }
            catch (Exception exception) {
                out.println("<p style=\"color:red\"><b>An error was found on line "
                        + i + ": " + line + "</b></p>");
                valid = false;
            }
        }

        if (!valid) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>CoordinatesServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h2>The following points were found: </h2>");
            out.println("<ul>");
            for (GmPoint point : points) {
                out.println("<li>" + point.toString() + "</li>");
            }
            out.println("</ul>");
            out.println("<h2>Number of coordinate points: " + points.size() + "</h2>");
            out.println("</body>");
            out.println("</html>");
        }
        return valid;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        //HttpStandardResponse httpStandardResponse = new HttpStandardResponse();

        String requestType = request.getParameter("request");
        String data = request.getParameter("data");
        String project = request.getParameter("project");
        String pointsetName = request.getParameter("pointset");

        if ("newPointSet".equalsIgnoreCase(requestType)) {
            if (validatePointSet(request, response)) {
                File file = File.createTempFile("tmp-pointset", ".xyz");
                FileWriter writer = new FileWriter(file);
                writer.write(data);
                writer.flush();
                writer.close();

                try {
                    String tokenData = (String) request.getSession().getAttribute("token");
                    DriveSample driveSample = new DriveSample(project, pointsetName, file, tokenData);
                    response.setContentType("text/xml");
                    out.println("<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>");
                    //out.println("<?xml-stylesheet type=\"text/css\" href=\"terrainTools-style.css\"?>");
                    out.println("<terrainToolsResponse>");
                    out.println("  <userId>"+driveSample.getUserId()+"</userId>");
                    out.println("  <applicationId>"+driveSample.getApplicationId()+"</applicationId>");
                    out.println("  <projectId>"+driveSample.getProjectId()+"</projectId>");
                    out.println("  <pointsetId>"+driveSample.getObjectId()+"</pointsetId>");
                    out.println("</terrainToolsResponse>");
                    out.println();
                    request.getSession().setAttribute("userId",driveSample.getUserId());
                    request.getSession().setAttribute("applicationId",driveSample.getApplicationId());
                    request.getSession().setAttribute("projectId",driveSample.getProjectId());
                    request.getSession().setAttribute("pointsetId",driveSample.getObjectId());
                }
                catch (Exception exception) {
                    out.println("Something bad happened with Google Drive! "+exception);
                    //httpStandardResponse.sendException("Something bad happened with Google Drive! "+exception, response);
                }

                file.delete();
            }
        }
        else {
            out.println("Illegal REQUEST parameter value.");
            //httpStandardResponse.sendException("Illegal REQUEST parameter value.", response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}