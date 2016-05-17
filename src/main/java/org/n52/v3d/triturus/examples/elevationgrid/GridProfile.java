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
package org.n52.v3d.triturus.examples.elevationgrid;

import org.n52.v3d.triturus.core.T3dException;
import org.n52.v3d.triturus.gisimplm.*;
import org.n52.v3d.triturus.vgis.VgLineString;
import org.n52.v3d.triturus.vgis.VgProfile;

/**
 * Triturus example application: Generates a cross-section for a given elevation grid.
 * @author Benno Schmidt
 * @see GridProfileApp
 */
public class GridProfile
{
	public static void main(String args[])
	{
        // Read the elevation grid from file:
		IoElevationGridReader reader = new IoElevationGridReader(IoElevationGridReader.ARCINFO_ASCII_GRID);
		GmSimpleElevationGrid grid = null;
		try {
			grid = reader.readFromFile("/data/example_dem.asc");
		}
		catch (T3dException e) {
			e.printStackTrace();
		}

        // This is just some control output:
    	System.out.println(grid);
        System.out.print("The elevation grid's bounding-box: ");
		System.out.println(grid.envelope().toString());

        // Give definition-line (sequence of x, y, z coordinates, z will be ignored):
		VgLineString defLine = new GmLineString("2670740,5811200,0,2670700,5811000,0");
		System.out.println(defLine); // control output
		
		// Generate cross-section:
		FltElevationGrid2Profile proc = new FltElevationGrid2Profile();
		VgProfile prof = proc.transform(grid, defLine);

		// Cross-section output...
		// to console:
        for (int i = 0; i < prof.numberOfTZPairs(); i++)
            System.out.println((prof.getTZPair(i))[0] + ", " + (prof.getTZPair(i))[1]);
        // to SVG:
        System.out.println("Writing SVG-file...");
        IoProfileWriter lWriter = new IoProfileWriter(IoProfileWriter.SVG);
        lWriter.writeToFile(prof, "/temp/cross-sec-1.svg");
        // to ASCII-file:
        System.out.println("Exporting to ASCII-file...");
        lWriter.setFormatType(IoProfileWriter.ACGEO);
        lWriter.writeToFile(prof, "/temp/cross-sec-1.prf");
    }
}
