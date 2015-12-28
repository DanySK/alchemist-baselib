/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist;

import java.io.File;

/**
 * Collection of Alchemist's global variables.
 * 
 */
@Deprecated
public final class Global {

    /**
     * Long size in bytes.
     */
    public static final byte LONG_SIZE = 8;
    /**
     * The base used for encoding of bytes to ASCII.
     */
    public static final byte ENCODING_BASE = 36;
    /**
     * System file separator.
     */
    public static final String SLASH = System.getProperty("file.separator");
    /**
     * Alchemist's temp dir.
     */
    public static final String PERSISTENTPATH = System.getProperty("user.home") + SLASH + ".alchemist";
    /**
     * Alchemist's options file.
     */
    public static final File OPTIONSFILE = new File(PERSISTENTPATH + SLASH + "options");
    /**
     * Alchemists temp dir.
     */
    public static final File PERSISTENTFILE = new File(PERSISTENTPATH);

    private Global() {
    }

}
