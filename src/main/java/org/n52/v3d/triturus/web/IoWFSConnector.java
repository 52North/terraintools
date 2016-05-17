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
package org.n52.v3d.triturus.web;

import org.n52.v3d.triturus.core.T3dNotYetImplException;

/** 
 * todo engl. JavaDoc
 * �ber diese Klasse ist der Zugriff auf OGC-konforme Web Feature Services (WFS) m�glich.<p>
 * <i>Bem.: Diese Klasse ist noch nicht implementiert.</i><p>
 * @author Benno Schmidt
 */
public class IoWFSConnector
{
    private WFSRequestConfig mRequCfg;
    private IoURLReader mConn;
    
    /**
     * Konstruktor.<p>
     * @param pRequestConfig initiale WFS-Request-Konfiguration
     */
    public IoWFSConnector(WFSRequestConfig pRequestConfig) 
    {
    	mConn = new IoURLReader("http", "");
    	mRequCfg = pRequestConfig;	
    }
    
    /**
     * setzt die WFS-Request-Konfiguration.<p>
     * @param pRequestConfig Request-Konfiguration
     */
    public void setRequestConfiguration(WFSRequestConfig pRequestConfig) {
    	mRequCfg = pRequestConfig;
    }

    /**
     * liefert die aktuelle WFS-Request-Konfiguration.<p>
     * @return Request-Konfiguration
     */
    public WFSRequestConfig getRequestConfiguration() {
    	return mRequCfg;
    }
    
    /** <b>TODO</b> */
    public void getFeatures()
    {
    	throw new T3dNotYetImplException();
    }

    /**
     * liefert den Objekt-internen Konnektor, �ber den die Web-Verbindung aufgebaut wird.<p>
     * @see IoURLReader
     * @return <tt>IoURLReader</tt>-Objekt
     */
    public IoURLReader connector() {
        return mConn;
    }
}