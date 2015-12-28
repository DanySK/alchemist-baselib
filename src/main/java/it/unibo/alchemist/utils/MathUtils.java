/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.utils;

import it.unibo.alchemist.external.cern.jet.random.engine.MersenneTwister;
import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;

import java.util.Date;

import static org.apache.commons.math3.util.FastMath.nextAfter;
import static org.apache.commons.math3.util.FastMath.sqrt;
import static org.apache.commons.math3.util.FastMath.floor;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.abs;
import static org.apache.commons.math3.util.FastMath.pow;
import static org.apache.commons.math3.util.FastMath.exp;
import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.apache.commons.math3.util.FastMath.log;

/**
 *         A collection of static methods useful for some mathematical
 *         computation.
 * 
 */
public final class MathUtils {

    /**
     * The Avogadro's number.
     */
    public static final double AVOGADRO = 6.02214129e23;
    /**
     * 360.
     */
    public static final int DEGREES_IN_CIRCLE = 360;
    /**
     * Relative precision value under which two double values are considered to
     * be equal by fuzzyEquals.
     */
    public static final double DOUBLE_EQUALITY_EPSILON = 10e-12;
    /**
     * The Boltzmann's constant.
     */
    public static final double K_BOLTZMANN = 1.3806488e-23;
    /**
     * Cache for ln(2).
     */
    private static final double LOG_2 = Math.log(2);
    /**
     * Maximum allowed factorial (double).
     */
    private static final int MAXFACTDOUBLE = 170, LANCZOS_G = 7;
    /**
     * Maximum allowed factorial (long).
     */
    private static final int MAXFACTLONG = 20;
    /**
     * Coefficients for Lanczos.
     */
    private static final double[] P = { 0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7 };
    /**
     * Lanczos internal paramenter.
     */
    private static final double POINTFIVE = 0.5d;

    /**
     * Radians to degrees conversion factor.
     */
    public static final double RAD_TO_DEG = DEGREES_IN_CIRCLE / (2 * Math.PI);
    /**
     * Internal RNG.
     */
    private static final RandomEngine RG = new MersenneTwister(new Date());
    /**
     * Factorial cache (double).
     */
    private static final double[] VFACTD = new double[MAXFACTDOUBLE];
    /**
     * Factorial cache (long).
     */
    private static final long[] VFACTL = new long[MAXFACTLONG];

    /**
     * Compares two double values, taking care of computing a relative error
     * tolerance threshold.
     * 
     * @param a
     *            first double
     * @param b
     *            second double
     * @return true if the double are equals with a precision order of
     *         DOUBLE_EQUALITY_EPSILON
     */
    public static boolean exactEquals(final double a, final double b) {
        return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b);
    }

    /**
     * This is a chached version of the factorial. It means it is slow for the
     * first call, but extremely fast after that. It relies on the idea that
     * normally inside Alchemist a factorial is called often with the same
     * numbers (e.g. for the rate contribution computation of chemical reactions
     * which involve more molecules of the same compound). Due to limited
     * precision of double, you may experience precision loss with high numbers.
     * This method returns meaningful results up to 170!. Over this limit, you
     * get Double.POSITIVE_INFINITY.
     * 
     * @param n
     *            the number to compute
     * @return n!
     */
    public static double factDouble(final int n) {
        if (n == 0) {
            return 1;
        }
        if (n > MAXFACTDOUBLE) {
            return Double.POSITIVE_INFINITY;
        }
        if (VFACTD[n] != 0d) {
            return VFACTD[n];
        }
        final double res = factDouble(n - 1) * n;
        VFACTD[n] = res;
        return res;
    }

    /**
     * This is a chached version of the factorial. It means it is slow for the
     * first call, but extremely fast after that. It relies on the idea that
     * normally inside Alchemist a factorial is called often with the same
     * numbers (e.g. for the rate contribution computation of chemical reactions
     * which involve more molecules of the same compound). Due to limited length
     * of long, this method works up to 20!.
     * 
     * @param n
     *            the number to compute
     * @return n!
     */
    public static long factLong(final int n) {
        if (n > MAXFACTLONG) {
            throw new IllegalArgumentException("Maximum allowed value for this is 20. You tried with " + n);
        }
        if (n == 0) {
            return 1;
        }
        if (VFACTL[n] != 0) {
            return VFACTL[n];
        }
        final long res = factLong(n - 1) * n;
        VFACTL[n] = res;
        return res;
    }

    /**
     * Given a long, this functions computes in which position the first zero
     * appears, counting from the least significant bit.
     * 
     * @param k
     *            the long number you wish to know where the first 1 occurs
     * @return the first zero in a binary long number
     */
    public static int firstOnePosition(final long k) {
        return (int) floor(log2(k));
    }

    /**
     * @param target
     *            the number
     * @param min
     *            the minimum
     * @param max
     *            the maximum
     * @return min if target < min, max if target > max, target otherwise
     */
    public static double forceRange(final double target, final double min, final double max) {
        if (target < min) {
            return min;
        }
        if (target > max) {
            return max;
        }
        return target;
    }

    /**
     * Compares two double values, taking care of computing a relative error
     * tolerance threshold.
     * 
     * @param a
     *            first double
     * @param b
     *            second double
     * @return true if the double are equals with a precision order of
     *         DOUBLE_EQUALITY_EPSILON
     */
    public static boolean fuzzyEquals(final double a, final double b) {
        return abs(a - b) <= DOUBLE_EQUALITY_EPSILON * max(abs(a), abs(b));
    }

    /**
     * Compares two double values, taking care of computing a relative error
     * tolerance threshold.
     * 
     * @param a
     *            first double
     * @param b
     *            second double
     * @return true if a > b, or if fuzzyEquals(a, b).
     */
    public static boolean fuzzyGreaterEquals(final double a, final double b) {
        return a >= b || fuzzyEquals(a, b);
    }

    /**
     * Just an application of Pythagore's theorem. If two arrays of different
     * size are passed, the function throws an {@link IllegalArgumentException}.
     * 
     * @param a
     *            an array containing the coordinates of the first point
     * @param b
     *            an array containing the coordinates of the second point
     * @param <N>
     *            the type of the coordinates
     * @return the euclidean distance between the two points.
     */
    public static <N extends Number> double getEuclideanDistance(final double[] a, final double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must have the same size.");
        }
        double squaresum = 0d;
        /* Sum the squares of the distances for each dimension */
        for (int i = 0; i < a.length; i++) {
            final double k = a[i] - b[i];
            squaresum += k * k;
        }
        /* Then use the square root */
        return Math.sqrt(squaresum);
    }

    /**
     * Return whether a point is inside an ellipse or not.
     * 
     * @param a2
     *            Square of major axis
     * @param b2
     *            Square of minor axis
     * @param x
     *            X position of the point to test
     * @param y
     *            Y position of the point to test
     * @return true if the point is inside the ellipse
     */
    public static boolean isInEllipse(final double a2, final double b2, final double x, final double y) {
        return x * x / a2 + y * y / b2 < 1;
    }

    /**
     * This method calculates the Gamma function Γ(x) using the Lanczos
     * approximation.
     * 
     * @param xp
     *            the variable for Γ(x)
     * @return the Gamma function value with Lanczos approximation
     */
    public static double lanczosGamma(final double xp) {
        double x = xp;
        if (x < POINTFIVE) {
            return PI / (sin(PI * x) * lanczosGamma(1 - x));
        }

        x -= 1;
        double a = P[0];
        final double t = x + LANCZOS_G + POINTFIVE;
        for (int i = 1; i < P.length; i++) {
            a += P[i] / (x + i);
        }

        return sqrt(2 * Math.PI) * pow(t, x + POINTFIVE) * exp(-t) * a;
    }

    /**
     * A simple function to compute the base 2 logarithm.
     * 
     * @param v
     *            the number whose logarithm must be computed
     * @return log_2(v)
     */
    public static double log2(final double v) {
        return log(v) / LOG_2;
    }

    /**
     * @param val the value
     * @param v1 first value to compare to
     * @param v2 second value to compare to
     * @return v1 if val is closer to v1 than to v2, v2 otherwise
     */
    public static double nearest(final double val, final double v1, final double v2) {
        if (abs(v1 - val) < abs(v2 - val)) {
            return v1;
        }
        return v2;
    }

    /**
     * Equivalent of nextUp, but with opposite direction.
     * 
     * @param d the double you want to get the previous value
     * @return the double closest to the parameter in direction of negative infinity
     */
    public static double nextDown(final double d) {
        return nextAfter(d, Double.NEGATIVE_INFINITY);
    }

    /**
     * Fast method to get a new random integer.
     * 
     * @return a random integer in the interval [Integer.MIN_VALUE,
     *         Integer.MAX_VALUE]
     */
    @Deprecated
    public static int randomInt() {
        return RG.nextInt();
    }

    /**
     * This method calculates the Gamma function Γ(x) using the Stirling
     * approximation.
     * 
     * @param x
     *            the variable for Γ(x)
     * @return the Gamma function value with Stirling approximation
     */
    public static double stirlingGamma(final double x) {
        return sqrt(2d * Math.PI / x) * pow((x / Math.E), x);
    }

    /**
     * Disable default constructor.
     */
    private MathUtils() {
    }


}
