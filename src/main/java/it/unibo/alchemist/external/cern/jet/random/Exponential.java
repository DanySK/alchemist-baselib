/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package it.unibo.alchemist.external.cern.jet.random;

import org.apache.commons.math3.util.FastMath;

import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;

/**
 * Exponential Distribution (aka Negative Exponential Distribution); See the <A
 * HREF=
 * "http://www.cern.ch/RD11/rkb/AN16pp/node78.html#SECTION000780000000000000000"
 * > math definition</A> <A
 * HREF="http://www.statsoft.com/textbook/glose.html#Exponential%20Distribution">
 * animated definition</A>.
 * <p>
 * <tt>p(x) = lambda*exp(-x*lambda)</tt> for <tt>x &gt;= 0</tt>,
 * <tt>lambda &gt; 0</tt>.
 * <p>
 * Instance methods operate on a user supplied uniform random number generator;
 * they are unsynchronized.
 * 
 * Static methods operate on a default uniform random number generator; they are
 * synchronized.
 * <p>
 * 
 */
public class Exponential extends AbstractContinousDistribution {
    /**
     * 
     */
    private static final long serialVersionUID = 7264179718362128152L;

    /**
     * 
     */
    private double lambda;

    /**
     * The uniform random number generated shared by all <b>static</b> methods.
     */
    private static Exponential shared = new Exponential(1.0,
            makeDefaultGenerator());

    /**
     * Constructs a Negative Exponential distribution.
     * 
     * @param l
     *            lambda
     * @param randomGenerator
     *            randomGenerator
     */
    public Exponential(final double l, final RandomEngine randomGenerator) {
        this(randomGenerator);
        setState(l);
    }

    /**
     * Builds a new Negative Exponential distribution with lambda = 0.
     * 
     * @param randomGenerator
     *            The random engine to use for random generation
     */
    public Exponential(final RandomEngine randomGenerator) {
        super();
        setRandomGenerator(randomGenerator);
    }

    /**
     * Returns the cumulative distribution function.
     * 
     * @param x
     *            x
     * @return the result
     */
    public double cdf(final double x) {
        if (x <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-x * lambda);
    }

    /**
     * Returns a random number from the distribution.
     * @return the result
     */
    public double nextDouble() {
        return nextDouble(lambda);
    }

    /**
     * Returns a random number from the distribution; bypasses the internal
     * state.
     * @param l
     *            lambda
     * @return the result
     */
    public double nextDouble(final double l) {
        return -FastMath.log1p(-getRandomGenerator().nextDouble()) / l;
    }

    /**
     * Returns the probability distribution function.
     * @param x
     *            x
     * @return the result
     */
    public double pdf(final double x) {
        if (x < 0.0) {
            return 0.0;
        }
        return lambda * Math.exp(-x * lambda);
    }

    /**
     * @param l
     *            lambda
     * Sets the mean.
     */
    public final void setState(final double l) {
        this.lambda = l;
    }

    /**
     * Returns a random number from the distribution with the given lambda.
     * 
     * @param lambda
     *            lambda
     * @return next double
     */
    public static double staticNextDouble(final double lambda) {
        synchronized (shared) {
            return shared.nextDouble(lambda);
        }
    }

    /**
     * Returns a String representation of the receiver.
     * @return the result
     */
    public String toString() {
        return this.getClass().getName() + "(" + lambda + ")";
    }

    /**
     * Sets the uniform random number generated shared by all <b>static</b>
     * methods.
     * 
     * @param randomGenerator
     *            the new uniform random number generator to be shared.
     */
    @SuppressWarnings("unused")
    private static void xstaticSetRandomGenerator(
            final RandomEngine randomGenerator) {
        synchronized (shared) {
            shared.setRandomGenerator(randomGenerator);
        }
    }
}
