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
package org.n52.v3d.triturus.gisimplm;

import org.n52.v3d.triturus.core.IoObject;
import org.n52.v3d.triturus.core.T3dException;
import org.n52.v3d.triturus.core.T3dNotYetImplException;
import org.n52.v3d.triturus.vgis.VgEnvelope;
import org.n52.v3d.triturus.vgis.VgPoint;

import java.io.*;
import java.util.ArrayList;

/**
 * Import of files that contain point coordinates (special simple ASCII format)<br /><br />
 *  <i>German:</i> Einlesen von Punktdateien. In den Punktdateien stehen zeilenweise x-, y- und z-Koordinaten durch
 * Leerzeichen voneinander getrennt.
 * @author Benno Schmidt
 */
public class IoPointListReader extends IoObject
{
    private String mLogString = "";

    private String mFormat;
    private ArrayList mPointList = null;

    private VgEnvelope mSpatialFilter = null;

    /**
     * Identifier to be used to process plain ASCII files holding XYZ triples.
     */
    public static final String PLAIN = "Plain";

    /**
     * Constructor.<br /><br />
     * <i>German:</i> Konstruktor. Als Parameter ist der Dateiformattyp zu setzen. Wird dieser nicht unterst&uuml;tzt,
     * wird sp&auml;ter w&auml;hrend des Lesevorgangs eine Ausnahme geworfen.<br />
     * Es werden die folgenden Formate unterst�tzt:<br />
     * <ul>
     * <li><i>Plain:</i> ASCII-Datei, zeilenweise x, y und z separiert durch Blank</li>
     * <li><b>... weitere Typen insb. Vermessungsformate -> Benno</b></li>
     * </ul>
     * @param pFormat Format-string, e.g. "Plain"
     * @see IoPointListReader#PLAIN
     */
    public IoPointListReader(String pFormat) {
        mLogString = this.getClass().getName();
        this.setFormatType(pFormat);
    }

    public String log() {
        return mLogString;
    }

    /** 
     * sets the format type.
     * @param pFormat Format-type (e.g. "Plain")
     * @see IoPointListReader#PLAIN
     */
    public void setFormatType(String pFormat)
    {
        mFormat = pFormat;
    }

    /**
     * reads in a set of 3-d points from a file.<br /><br />
     * <i>German:</i> liest eine Menge von 3D-Punkten einer Datei ein. Wird der spezifizierte Formattyp nicht
     * unterst&uuml;tzt, wirft die Methode eine <tt>T3dNotYetImplException</tt>.<p>
     * @param pFilename File name (complete path)
     * @return <tt>ArrayList</tt> consisting of <tt>VgPoint</tt> objects
     * @throws org.n52.v3d.triturus.core.T3dException
     * @throws org.n52.v3d.triturus.core.T3dNotYetImplException
     */
    public ArrayList readFromFile(String pFilename) throws T3dException, T3dNotYetImplException
    {
        int i = 0;
        if (mFormat.equalsIgnoreCase("Plain")) i = 1;
        // --> hier ggf. weitere Typen erg�nzen...

        try {
            switch (i) {
                case 1: this.readPlainAscii(pFilename); break;
                // --> hier ggf. weitere Typen erg�nzen...

                default: throw new T3dNotYetImplException("Unsupported file format");
            }
        }
        catch (T3dException e) {
            throw e;
        }

        return mPointList;
    }

    private void readPlainAscii(String pFilename) throws T3dException
    {
// TODO: Separator variabel machen ebenso wie Reihenfolge x y z etc.; Kennung?
        String line = "";
        int lineNumber = 0;

        mPointList = new ArrayList();

        try {
            FileReader lFileRead = new FileReader(pFilename);
            BufferedReader lDatRead = new BufferedReader(lFileRead);

            String tok1, tok2, tok3;
            double x, y, z;
            VgPoint pt = null;

            line = lDatRead.readLine();
            while (line != null) {
                lineNumber++;

                tok1 = this.getStrTok(line, 1, " ");
                tok2 = this.getStrTok(line, 2, " ");
                tok3 = this.getStrTok(line, 3, " ");

                x = this.toDouble(tok1);
                y = this.toDouble(tok2);
                z = this.toDouble(tok3);

                pt = new GmPoint(x, y, z);

                if (mSpatialFilter != null) {
                    if (mSpatialFilter.contains(pt))
                        mPointList.add(pt);
                } else
                    mPointList.add(pt);

                line = lDatRead.readLine();
                //if (lineNumber % 1000000 == 0) System.out.println("lineNumber = " + lineNumber);
            }
            lDatRead.close();
        }
        catch (FileNotFoundException e) {
            throw new T3dException("Could not access file \"" + pFilename + "\".");
        }
        catch (IOException e) {
            throw new T3dException(e.getMessage());
        }
        catch (T3dException e) {
            throw new T3dException(e.getMessage());
        }
        catch (Exception e) {
            throw new T3dException("Parser error in \"" + pFilename + "\":" + lineNumber);
        }
System.out.println("lineNumber = " + lineNumber);
    } // readPlainAscii()

    /**
     * defines a spatial filter.<br /><br />
     * <i>German:</i> setzt einen r&auml;umlichen Filter f&uuml;r die einzulesenden Punkte. Punkte, die au&szlig;erhalb
     * der durch <tt>pFilter</tt> gegebenen Bounding-Box liegen, werden nicht ber&uuml;cksichtigt.<br />
     * Soll keine r&auml;umliche Filterung erfolgen, ist der Wert <i>null</i> als Parameter zu setzen (Voreinstellung).
     * <br />
     * Bem.: Die z-Werte der Bounding-Box sind auf hinreichend kleine/gro&szlig;e Werte zu setzen.
     * @param pFilter Bounding-Box
     */
    public void setSpatialFilter(VgEnvelope pFilter) {
        mSpatialFilter = pFilter;
    }

    /**
     * gives the set spatial filter.<br /><br />
     * <i>German:</i> liefert den gesetzten r&auml;umlichen Filter. Punkte, die au&szlig;erhalb der Bounding-Box des
     * Filters liegen, werden beim Einlesen nicht ber&uuml;cksichtigt.<br />
     * Falls kein r&auml;umlicher Filter gesetzt ist, wird der Wert <i>null</i> zur&uuml;ckgegeben.
     * @return 3-D bounding-Box (if a spatial filter is set, else <i>null</i>)
     */
    public VgEnvelope getSpatialFilter() {
        return mSpatialFilter;
    }

    // private Helfer, ben�tigt in readPlainAscii():

    // Extraktion des i-ten Tokens (i >= 1!, i max. = 4) aus einem String ('pSep" als Trenner):
    private String getStrTok(String pStr, int i, String pSep) throws T3dException
    {
        ArrayList lStrArr = new ArrayList(); 
        lStrArr.add(pStr);
        int i0 = 0, i1 = 0, k = 0;
        while (k < 4 && i1 >= 0) {
           i1 = pStr.indexOf(pSep, i0);
           if (i1 >= 0) {
               if (k == 0)
                   lStrArr.set(0, pStr.substring(i0, i1));
               else
                   lStrArr.add(pStr.substring(i0, i1));
               i0 = i1 + 1;
               k++;
           }
        }
        if (k <= 3)
            lStrArr.add(pStr.substring(i0));
        if (i < 1 || i > 4)
            throw new T3dException("Logical parser error.");
        return (String) lStrArr.get(i - 1);
    } 

    // Konvertierung String -> Gleitpunktzahl:
    private double toDouble(String pStr) 
    {
pStr = pStr.replaceAll(",", "."); // todo: falls, als Dezimalpunkt
        return Double.parseDouble(pStr);
    } 

    // Konvertierung String -> Ganzzahl:
    private int toInt(String pStr)
    {
        return Integer.parseInt(pStr);
    } 
}
