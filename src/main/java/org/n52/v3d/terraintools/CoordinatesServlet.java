/**
 * Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
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
package org.n52.v3d.terraintools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.n52.v3d.triturus.gisimplm.GmPoint;

/**
 *
 * @author Adhitya Kamakshidasan
 */
public class CoordinatesServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        //HTML to be printed on-screen
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>CoordinatesServlet</title>");
        out.println("</head>");
        out.println("<body>");

        String data = request.getParameter("textarea");
        ArrayList<GmPoint> points = new ArrayList(); //Arraylist for storing all points
        
        String [] lines = data.split("\\n"); //Split the lines with respect to new-line
        
        for (int i=0; i<lines.length;i++) { //Go through each line
            String line = lines[i]; //Get current line
            line = line.trim(); // Remove leading and trailing spaces
            line = line.replaceAll(" +", " "); // Remove extra spaces that may have been there in-between
            line = line.replaceAll(" ", ","); //Convert space separated values to comma separated values
            try {
                GmPoint point = new GmPoint(line); //Convert line to a GmPoint!
                points.add(point); //Add the point to the ArrayList
            } catch (Exception exception) {
                out.println("<p style=\"color:red\"><b>An error was found on line "
                        + i + ": " + line + "</b></p>");
            }
        }

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

}
