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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.v3d.terraintools.helper;

import java.io.File;
import java.io.IOException;
import org.n52.v3d.triturus.core.T3dException;
import org.n52.v3d.triturus.gisimplm.FltElevationGridFloodFill;
import org.n52.v3d.triturus.gisimplm.GmPoint;
import org.n52.v3d.triturus.gisimplm.GmSimpleElevationGrid;
import org.n52.v3d.triturus.gisimplm.IoElevationGridReader;
import org.n52.v3d.triturus.gisimplm.IoElevationGridWriter;
import org.n52.v3d.triturus.t3dutil.MpHypsometricColor;
import org.n52.v3d.triturus.t3dutil.MpSimpleHypsometricColor;
import org.n52.v3d.triturus.t3dutil.T3dColor;
import org.n52.v3d.triturus.t3dutil.T3dSymbolDef;
import org.n52.v3d.triturus.t3dutil.symboldefs.T3dSphere;
import org.n52.v3d.triturus.vgis.VgPoint;
import org.n52.v3d.triturus.visx3d.VrmlX3dSceneGenerator;
import org.n52.v3d.triturus.vscene.MultiTerrainScene;

/**
 *
 * @author Adhitya
 */
public class FloodingHelper {

    public String createFlooding(String initialGrid, int point_x, int point_y, double waterLevel) throws IOException {
        File floodingFile = File.createTempFile("tmp-test-flood", ".asc");
        String outputFile = floodingFile.getPath();
        IoElevationGridReader reader = new IoElevationGridReader(
                IoElevationGridReader.X3DOM);
        IoElevationGridWriter writer = new IoElevationGridWriter(
                IoElevationGridWriter.ARCINFO_ASCII_GRID);

        GmSimpleElevationGrid srcGrd, targetGrd;

        try {
            // Read the elevation grid from file:
            srcGrd = reader.readFromFile(initialGrid);

            VgPoint seedPoint = new GmPoint(srcGrd.envelope().getCenterPoint());

            // Set water-level meters above ground:
            seedPoint.setZ(
                    srcGrd.getValue(point_x, point_y)
                    + waterLevel);

            FltElevationGridFloodFill flt = new FltElevationGridFloodFill();
            targetGrd = (GmSimpleElevationGrid) flt.transform(srcGrd, seedPoint);

            // Write result:
            writer.writeToFile(targetGrd, outputFile);
        }
        catch (T3dException e) {
            e.printStackTrace();
        }
        return outputFile;
    }

    public String combineFlooding(String initialGrid, String floodedGrid) throws IOException {

        IoElevationGridReader reader1 = new IoElevationGridReader(IoElevationGridReader.X3DOM);
        IoElevationGridReader reader2 = new IoElevationGridReader("ArcIGrd");

        try {
            // Read elevation grids:
            GmSimpleElevationGrid grid1 = reader1.readFromFile(initialGrid);

            GmSimpleElevationGrid grid2 = reader2.readFromFile(floodedGrid);

            // Define marker symbol:
            T3dSymbolDef sym = new T3dSphere(100.);
            T3dColor col = new T3dColor(0, 1, 1);

            // Construct 3D scene:
            MultiTerrainScene s = new MultiTerrainScene();
            s.addTerrain(grid1);
            s.addTerrain(grid2);
            s.setDefaultExaggeration(8.0);

            for (int i = 0; i < s.getTerrains().size(); i++) {
                this.prepare((GmSimpleElevationGrid) s.getTerrains().get(i)); // just replaces NODATA values...
            }

            MpHypsometricColor colMapper = this.defineReliefColoring();
            s.setHypsometricColorMapper(colMapper);

            // Export scene as VRML file:
            VrmlX3dSceneGenerator res = new VrmlX3dSceneGenerator(s);

            File resultFile = File.createTempFile("tmp-test-result", ".html");
            String file = resultFile.getPath();
            res.writeToX3domFile(file);

            new File(floodedGrid).delete();

            return file;
        }
        catch (T3dException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void prepare(GmSimpleElevationGrid grid) {
        // If some grid cell's have NODATA values, assign values...
        for (int j = 0; j < grid.numberOfColumns(); j++) {
            for (int i = 0; i < grid.numberOfRows(); i++) {
                if (!grid.isSet(i, j)) {
                    grid.setValue(i, j, grid.minimalElevation());
                }
            }
        }
    }

    protected MpHypsometricColor defineReliefColoring() {
        MpHypsometricColor colMapper = new MpSimpleHypsometricColor();
        double elev[] = {30., 80, 130, 180.};
        T3dColor cols[] = {
            new T3dColor(0.0f, 0.8f, 0.0f), // green
            new T3dColor(1.0f, 1.0f, 0.5f), // pale yellow
            //new T3dColor(0f, 0f, 0.8f), // blue
            new T3dColor(0.78f, 0.27f, 0.0f), // brown
            new T3dColor(0.82f, 0.2f, 0.0f) // red/brown
        };
        ((MpSimpleHypsometricColor) colMapper).setPalette(elev, cols, true);
        return colMapper;
    }
}
