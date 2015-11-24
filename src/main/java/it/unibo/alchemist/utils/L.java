/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.utils;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 */
public final class L {

    private static boolean gui;

    private static final Logger LOGGER = Logger.getLogger("it.unibo.alchemist");

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.INFO);
        final Handler defaultHandler = new StreamHandler(System.out, new SimpleFormatter());
        defaultHandler.setLevel(LOGGER.getLevel());
        LOGGER.addHandler(defaultHandler);
    }

    /**
     * @param h
     *            the handler to attach
     */
    public static void addHandler(final Handler h) {
        h.setLevel(Level.ALL);
        LOGGER.addHandler(h);
    }

    /**
     * @param h
     *            the handler to detach
     */
    public static void removeHandler(final Handler h) {
        LOGGER.removeHandler(h);
    }

    /**
     * @param s
     *            the String to log
     */
    public static void debug(final String s) {
        log(Level.FINEST, s);
    }

    /**
     * @param s
     *            the String to log
     */
    public static void error(final String s) {
        log(Level.SEVERE, s);
    }

    /**
     * Logs a String describing the error followed by the stacktrace.
     * 
     * @param s
     *            the String to log
     * @param e
     *            the Throwable to get and print the stacktrace
     */
    public static void error(final String s, final Throwable e) {
        error(s + "\n" + ExceptionUtils.getStackTrace(e));
    }

    /**
     * @param e
     *            the Throwable to get and print the stacktrace
     */
    public static void error(final Throwable e) {
        log(Level.SEVERE, ExceptionUtils.getStackTrace(e));
    }

    private static void flush() {
        for (final Handler h : getHandlers()) {
            if (h instanceof StreamHandler) {
                ((StreamHandler) h).flush();
            }
        }
    }

    /**
     * @return the current Handlers, within an array
     */
    public static Handler[] getHandlers() {
        return LOGGER.getHandlers();
    }

    /**
     * @return the current logging level
     */
    public static Level getLevel() {
        return LOGGER.getLevel();
    }

    /**
     * @param s
     *            the String to log
     */
    public static void log(final String s) {
        log(Level.INFO, s);
    }

    /**
     * @param enabled
     *            true if you want a message to pop up in case of errors
     */
    public static void setGUIEnabled(final boolean enabled) {
        gui = enabled;
    }

    /**
     * @param l
     *            the logging level
     */
    public static void setLoggingLevel(final Level l) {
        LOGGER.setLevel(l);
    }

    /**
     * @param s
     *            the String to log
     */
    public static void warn(final String s) {
        log(Level.WARNING, s);
    }

    /**
     * @param e
     *            the Throwable to get and print the stacktrace
     */
    public static void warn(final Throwable e) {
        log(Level.WARNING, ExceptionUtils.getStackTrace(e));
    }

    private static void log(final Level level, final String message) {
        LOGGER.log(level, message);
        flush();
        if (gui && level.equals(Level.SEVERE)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private L() {
    }

}
