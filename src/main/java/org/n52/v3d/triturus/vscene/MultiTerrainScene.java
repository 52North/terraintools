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
package org.n52.v3d.triturus.vscene;

import org.n52.v3d.triturus.core.T3dNotYetImplException;
import org.n52.v3d.triturus.gisimplm.GmEnvelope;
import org.n52.v3d.triturus.gisimplm.GmPoint;
import org.n52.v3d.triturus.t3dutil.MpHypsometricColor;
import org.n52.v3d.triturus.t3dutil.T3dColor;
import org.n52.v3d.triturus.t3dutil.T3dSymbolInstance;
import org.n52.v3d.triturus.t3dutil.T3dVector;
import org.n52.v3d.triturus.vgis.VgElevationGrid;
import org.n52.v3d.triturus.vgis.VgEnvelope;
import org.n52.v3d.triturus.vgis.VgPoint;

import java.util.ArrayList;

/**
 * Specification of a scene that consists of an arbitrary number of grid-based <i>digital elevation models</i> (DEMs),
 * an arbitrary number of <i>marker objects</i>, <i>viewpoint definitions</i>, and </i>light-sources</i>.
 * <p>
 * Examples for typical use cases:
 * <ul>
 *     <li>Visualization of > 1 (neighboring) elevation models</li>
 *     <li>Visualization of elevation models referring to different thematic aspects (e.g., terrain surface and
 *     groundwater levels)</li>
 * </ul>
 * <p>
 * From the elevation models' spatial extents, a normalized 3D bounding-box will be calculated dynamically (as for
 * <tt>VsSimpleScene</tt></tt>s, see description there):
 * <ul>
 *     <li>In the x-y-plane, this will be a subset of the range -1 &lt;= x' &lt;= +1, -1 &lt;= y' &lt;= +1, whereat the
 *     normalized coordinates are symbolized using an apostrophe (').</li>
 *     <li>The extent in z-direction will be computed from the elevation-values in a way, that vertical height-scale
 * and horizontal scale (x-y-plane) correspond (no exaggeration factor!).</li>
 * </ul>
 * <p>
 * For <tt>MultiTerrainScene</tt>s, viewpoint and light-source definitions can be specified easily, since the
 * helper-method <tt>denorm()</tt> allows to specify parameters using normalized coordinates. Hence, viewpoints and
 * light-source parameters are <i>invariant with respect to the set exaggeration-value.</i>
 *
 * @see VsScene
 * @see VsSimpleScene
 * @author Benno Schmidt
 * @since Triturus ver 1.1
 */
/*abstract*/ public class MultiTerrainScene extends VsScene
// todo Maybe this class should be abstract in the future?
{
    private ArrayList<VgElevationGrid> mTerrains = null;
    private VgEnvelope mBBox = null; // A 'null' value will be used to indicate BBox-calculation is necessary.

    private ArrayList<T3dSymbolInstance> mMarkers = null;

    private double mScale; // Scaling factor used for geo-coordinate normalization
    private T3dVector mOffset = new T3dVector(); // Translation vector used for geo-coordinate normalization
    private double mAspect; // Aspect ratio y-extent : x-extent

    private T3dColor mDefaultReliefColor = new T3dColor(0.f,1.f,0.f);
    private T3dColor mBackgroundColor = new T3dColor(0.f,0.f,0.f);
    private T3dColor mBBoxColor = new T3dColor(1.f,1.f,1.f);
    private boolean mDrawBBox = false;
    private MpHypsometricColor mHypsometricColMap = null;


    /**
     * adds an elevation model to be visualized as relief ("terrain") to the current scene.
     * <p>
     * You can not add more than one elevation-model to the <tt>VsSimpleScene</tt>.
     * <p>
     * Note, that the elevation-model must be a grid-based model. E.g., <tt>MultiTerrainScene</tt> descriptions do
     * not support triangulated irregular networks (TINs) yet (although it should be easy to implement this).
     *
     * @param pTerrain Elevation-model
     */
    public void addTerrain(VgElevationGrid pTerrain)
    {
        if (mTerrains == null)
            mTerrains = new ArrayList<VgElevationGrid>();

        if (pTerrain != null) {
            mTerrains.add(pTerrain);

            // Determine new Bounding-box:
            VgEnvelope lBBox = new GmEnvelope(
                    pTerrain.getGeometry().envelope().getXMin(),
                    pTerrain.getGeometry().envelope().getXMax(),
                    pTerrain.getGeometry().envelope().getYMin(),
                    pTerrain.getGeometry().envelope().getYMax(),
                    pTerrain.minimalElevation(),
                    pTerrain.maximalElevation());

            if (mBBox == null) {
                // Since mTerrains must not be empty (after calls ti removeTerrain()),
                // mBBox = lBBox is not sufficient!
                mBBox = this.envelope();
            }

            mBBox.letContainEnvelope(lBBox);

            this.calculateNormTransformation();
        }
    }

    /**
     * gets the elevation-models to be visualized.
     *
     * @return List of elevation-models, or <i>null</i> if no elevation-models have been added to the scene
     */
    public ArrayList<VgElevationGrid> getTerrains() {
        return mTerrains;
    }

    /**
     * removes an elevation-model from the current scene.
     *
     * @param pTerrain Elevation-model object to be removed
     */
    public void removeTerrain(VgElevationGrid pTerrain)
    {
        mTerrains.remove(pTerrain);
        mBBox = null; // i.e., BBox is invalid, requires calculation
    }

    /**
     * returns the 3D bounding-box with respect to the elevation-grids that are part of the scene. Note that marker
     * locations will be ignored.
     *
     * @return Bounding-box or <i>null</i>, if no elevation-grids are part of the scene
     */
    public VgEnvelope envelope()
    {
        if (mBBox != null)
            return mBBox;

        if (mTerrains == null || mTerrains.size() <= 0)
            return null;

        // Else calculate bounding-box:
        VgEnvelope bb2d, bb;
        for (VgElevationGrid t : mTerrains) {
            bb2d = t.getGeometry().envelope();
            bb = new GmEnvelope(
                    bb2d.getXMin(),
                    bb2d.getXMax(),
                    bb2d.getYMin(),
                    bb2d.getYMax(),
                    t.minimalElevation(),
                    t.maximalElevation());

            if (mBBox == null) {
                mBBox = bb;
            } else {
                mBBox.letContainEnvelope(bb);
            }
        }

        this.calculateNormTransformation();

        return mBBox;
    }

    /**
     * adds a marker to be visualized to the current scene.
     *
     * @param pMarker Marker specification
     */
    public void addMarker(T3dSymbolInstance pMarker)
    {
        if (mMarkers == null)
            mMarkers = new ArrayList<T3dSymbolInstance>();

        if (pMarker != null)
            mMarkers.add(pMarker);
    }

    /**
     * gets the markers to be visualized.
     *
     * @return List of markers, or <i>null</i> if no markers have been added to the scene
     */
    public ArrayList<T3dSymbolInstance> getMarkers() {
        return mMarkers;
    }

    /**
     * removes a marker from the current scene.
     *
     * @param pMarker Marker object to be removed
     */
    public void removeMarker(T3dSymbolInstance pMarker)
    {
        // TODO Method has not been tested yet...
        mMarkers.remove(pMarker);
    }

    /**
     * returns a position referring to the normalized coordinate-space for a position in geo-coordinate-space. I.e.,
     * this method transforms geo-coordinates into (<tt>MultiTerrainScene</tt>-specific) normalized coordinates.
     * <p>
     * For the result point, the assertion -1 &lt;= x' &lt; +1, -1 &lt;= y' &lt; +1 must hold, if and only if the
     * position is inside the model's bounding-box.
     *
     * @param pGeoPos Georeferenced point
     * @return Point in normalized coordinate space
     * @see MultiTerrainScene#denorm
     */
    public T3dVector norm(VgPoint pGeoPos)
    {
        // todo: Method is identical to VsSimpleScene#norm - maybe this should be refactored...

        return new T3dVector(
            pGeoPos.getX() * mScale + mOffset.getX(),
            pGeoPos.getY() * mScale + mOffset.getY(),
            pGeoPos.getZ() * mScale);
    }

    /**
     * returns a position referring to the geo-coordinate-space for a position in the normalized coordinate-space.
     * I.e., this method transforms (<tt>MultiTerrainScene</tt>-specific) normalized coordinates into geo-coordinates.
     *
	 * @param pNormPos Georeferenced point
	 * @return Point in normalized coordinate space
	 * @see MultiTerrainScene#norm
	 */
	public VgPoint denorm(T3dVector pNormPos)
	{
        // todo: Method is identical to VsSimpleScene#denorm - maybe this should be refactored...

		return new GmPoint(
		    (pNormPos.getX() - mOffset.getX()) / mScale,
		    (pNormPos.getY() - mOffset.getY()) / mScale,
		    pNormPos.getZ() / mScale);
	}

    private void calculateNormTransformation()
	{
        // todo: Method is rather identical to VsSimpleScene#calculateNormTransformation - maybe this should be refactored...

        VgEnvelope envXY = this.envelope();
		double xMinGeo = envXY.getXMin();
		double xMaxGeo = envXY.getXMax();
		double yMinGeo = envXY.getYMin();
		double yMaxGeo = envXY.getYMax();

        double dx = xMaxGeo - xMinGeo;
        double dy = yMaxGeo - yMinGeo;
        mAspect = dy/dx;

        if (Math.abs(dx) > Math.abs(dy)) {
        	mScale = 2./dx;
        	mOffset.setX(-(xMinGeo + xMaxGeo)/ dx);
        	mOffset.setY(-(yMinGeo + yMaxGeo)/ dx);
        }
        else {
        	mScale = 2./dy;
        	mOffset.setX(-(xMinGeo + xMaxGeo)/ dy);
        	mOffset.setY(-(yMinGeo + yMaxGeo)/ dy);
        }
	}

    /**
     * returns the scale factor that corresponds to the normalization-transformation.
     * <p>
     * Note: The normalization will be performed applying the scaling <tt>this.getScale()</tt> to geo-coordinates
     * first, and then the translation <tt>this.getOffset()</tt>.
     *
     * @return Scaling factor
     * @see VsSimpleScene#getOffset
     */
    public double getScale() {
        // todo: Method is identical to VsSimpleScene#getScale - maybe this should be refactored...
        return mScale;
    }

    /**
     * returns the aspect-ratio of y-extent and x-extent of the elevation-model.
     *
     * @return Aspect-ratio
     */
    public double getAspect() {
        // todo: Method is identical to VsSimpleScene#getAspect - maybe this should be refactored...
        return mAspect;
    }

    /**
     * returns the translation vector that corresponds to the normalization-transformation.
     * <p>
     * Note: The normalization will be performed applying the scaling <tt>this.getScale()</tt> to geo-coordinates
     * first, and then the translation <tt>this.getOffset()</tt>.
     *
     * @return Translation vector
     * @see VsSimpleScene#getScale
     */
    public T3dVector getOffset() {
        // TODO: Method is identical to VsSimpleScene#getOffset - maybe this should be refactored...
        return mOffset;
    }

    /**
     * gets the minimum z'-value of the elevation-model <tt>this.getTerrain</tt> with respect to normalized coordinate
     * space.
     *
     * @return normalized z'-coordinate for the minimum elevation-value inside the <tt>VsSimpleScene</tt>
     */
    public double normZMin() {
        return this.envelope().getZMin() * mScale;
    }

    /**
     * gets the maximum z'-value of the elevation-model <tt>this.getTerrain</tt> with respect to normalized coordinate
     * space.
     *
     * @return normalized z'-coordinate for the maximum elevation-value inside the <tt>VsSimpleScene</tt>
     */
    public double normZMax() {
        return this.envelope().getZMax() * mScale;
    }

    /**
     * sets the scene's background-color. By default, a black background is set.
     *
     * @param pColor Background-color
     */
    public void setBackgroundColor(T3dColor pColor) {
        mBackgroundColor = pColor;
    }

    /**
     * gets the scene's background-color.
     *
     * @return Background-color
     */
    public T3dColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * sets the relief shapes' default color. By default, a green coloring will be used. Note that this value will
     * ignored, if a color-mapper is assigned to the scene.
     *
     * @see this#setHypsometricColorMapper(org.n52.v3d.triturus.t3dutil.MpHypsometricColor)
     * @param pColor Relief-color
     */
    public void setDefaultReliefColor(T3dColor pColor) {
        mDefaultReliefColor = pColor;
    }

    /**
     * gets the relief shapes' default color.
     *
     * @return Relief-color
     */
    public T3dColor getDefaultReliefColor() {
        return mDefaultReliefColor;
    }

    /**
     * sets the bounding-box color. By default, a white bounding-box will be drawn.
     *
     * @see this#drawBBoxShape
     * @param pColor Bounding-box color
     */
    public void setBBoxColor(T3dColor pColor) {
        mBBoxColor = pColor;
    }

    /**
     * gets the bounding-box color.
     *
     * @return Bounding-box color
     */
    public T3dColor getBBoxColor() {
        return mBBoxColor;
    }

    /**
     * controls, if the 3D-bounding-box corresponding to the elevation-model will be visible.
     *
     * @param pDrawBBox <i>true</i>, to add the bounding-box shape to the scene
     */
    public void drawBBoxShape(boolean pDrawBBox) {
        mDrawBBox = pDrawBBox;
    }

    protected boolean drawBBox() {
        return mDrawBBox;
    }

    /**
     * enables the usage of a hypsometric color map for terrain visualization.
     *
     * @param pColMap Hypsometric color-assignment or <i>null</i>, if no colouring shall be carried out
     */
    public void setHypsometricColorMapper(MpHypsometricColor pColMap) {
    	mHypsometricColMap = pColMap;
    }

    /**
     * gets the hypsometric color mapper that will be used to color the relief color.
     *
     * @return color.amapper object, or <i>null</i>, if no coloring will be used
     */
    public MpHypsometricColor getHypsometricColorMapper() {
        return mHypsometricColMap;
    }

    public Object generateScene() {
        // ???
        return new T3dNotYetImplException(); // todo ???
    }
}
