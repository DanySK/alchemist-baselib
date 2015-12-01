//   BuildProperties.java
//   Java Spatial Index Library
//   Copyright (C) 2012 Aled Morris <aled@sourceforge.net>
// //  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
// 
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package it.unibo.alchemist.external.com.infomatiq.jsi;

import it.unibo.alchemist.utils.L;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
//CHECKSTYLE:OFF
/**
 * Allows build properties to be retrieved at runtime. Currently, version and
 * scmRevisionId are implemented.
 */
public final class BuildProperties implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2807739587363734658L;

    private static final BuildProperties INSTANCE = new BuildProperties();

    private final String version;
    private final String scmRevisionId;

    private BuildProperties() {
        final Properties p = new Properties();
        try {
            p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("build.properties"));
        } catch (IOException e) {
            L.error(e);
        }
        version = p.getProperty("version", "");
        scmRevisionId = p.getProperty("scmRevisionId", "");
    }

    /**
     * Version number as specified in pom.xml
     */
    public static String getVersion() {
        return INSTANCE.version;
    }

    /**
     * SCM revision ID. This is the git commit ID.
     */
    public static String getScmRevisionId() {
        return INSTANCE.scmRevisionId;
    }
}
