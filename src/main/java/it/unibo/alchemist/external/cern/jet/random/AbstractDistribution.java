/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package it.unibo.alchemist.external.cern.jet.random;

import it.unibo.alchemist.external.cern.jet.random.engine.AbstractRandomEngine;
import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;

/**
 * Abstract base class for all random distributions.
 * 
 * A subclass of this class need to override method <tt>nextDouble()</tt> and,
 * in rare cases, also <tt>nextInt()</tt>.
 * <p>
 * Currently all subclasses use a uniform pseudo-random number generation engine
 * and transform its results to the target distribution. Thus, they expect such
 * a uniform engine upon instance construction.
 * <p>
 * {@link it.unibo.alchemist.external.cern.jet.random.engine.MersenneTwister} is recommended as uniform
 * pseudo-random number generation engine, since it is very strong and at the
 * same time quick. {@link #makeDefaultGenerator()} will conveniently construct
 * and return such a magic thing. You can also, for example, use
 * {@link cern.jet.random.engine.DRand}, a quicker (but much weaker) uniform
 * random number generation engine. Of course, you can also use other strong
 * uniform random number generation engines.
 * 
 * <p>
 * <b>Ressources on the Web:</b>
 * Check the Web version of the <a>
 * href="http://www.cern.ch/RD11/rkb/AN16pp/node1.html"> CERN Data Analysis
 * Briefbook </a>. This will clarify the definitions of most distributions.
 * Also consult the <A
 * HREF="http://www.statsoftinc.com/textbook/stathome.html"> StatSoft Electronic
 * Textbook</A> - the definite web book.
 * <p>
 * <b>Other useful ressources:</b>
 * <A HREF=
 * "http://www.stats.gla.ac.uk/steps/glossary/probability_distributions.html">
 * Another site </A> and <A
 * HREF="http://www.statlets.com/usermanual/glossary.htm"> yet another site
 * </A>describing the definitions of several distributions.
 * You may want to check out a <A
 * HREF="http://www.stat.berkeley.edu/users/stark/SticiGui/Text/gloss.htm">
 * Glossary of Statistical Terms</A>.
 * The GNU Scientific Library contains an extensive (but hardly readable) <A
 * HREF="http://sourceware.cygnus.com/gsl/html/gsl-ref_toc.html#TOC26"> list of
 * definition of distributions</A>.
 * Use this Web interface to <A
 * HREF="http://www.stat.ucla.edu/calculators/cdf"> plot all sort of
 * distributions</A>.
 * Even more ressources: <A
 * HREF="http://www.animatedsoftware.com/statglos/statglos.htm"> Internet
 * glossary of Statistical Terms</A>, <A
 * HREF="http://www.ruf.rice.edu/~lane/hyperstat/index.html"> a text book</A>,
 * <A HREF="http://www.stat.umn.edu/~jkuhn/courses/stat3091f/stat3091f.html">
 * another text book</A>.
 * Finally, a good link list <A
 * HREF="http://www.execpc.com/~helberg/statistics.html"> Statistics on the
 * Web</A>.
 * <p>
 * 
 * @see it.unibo.alchemist.external.cern.jet.random.engine
 * @see cern.jet.random.engine.Benchmark
 * @see cern.jet.random.Benchmark
 */
public abstract class AbstractDistribution extends cern.colt.AbstractPersistentObject
        implements cern.colt.function.DoubleFunction,
        cern.colt.function.IntFunction {
    /**
     * 
     */
    private static final long serialVersionUID = -5226193316569798444L;
    /**
     * 
     */
    private RandomEngine randomGenerator;

    /**
     * Makes this class non instantiable, but still let's others inherit from
     * it.
     */
    protected AbstractDistribution() {
        super();
    }

    /**
     * Equivalent to <tt>nextDouble()</tt>. This has the effect that
     * distributions can now be used as function objects, returning a random
     * number upon function evaluation.
     * 
     * @param dummy
     *            see parent
     * @return see parent
     */
    @Override
    public double apply(final double dummy) {
        return nextDouble();
    }

    /**
     * Equivalent to <tt>nextInt()</tt>. This has the effect that distributions
     * can now be used as function objects, returning a random number upon
     * function evaluation.
     * 
     * @param dummy
     *            see parent
     * @return see parent
     */
    @Override
    public int apply(final int dummy) {
        return nextInt();
    }

    /**
     * Returns a deep copy of the receiver; the copy will produce identical
     * sequences. After this call has returned, the copy and the receiver have
     * equal but separate state.
     * 
     * @return a copy of the receiver.
     */
    public AbstractDistribution clone() {
        final AbstractDistribution copy = (AbstractDistribution) super.clone();
        if (this.randomGenerator != null) {
            copy.randomGenerator = (RandomEngine) this.randomGenerator.clone();
        }
        return copy;
    }

    /**
     * Returns the used uniform random number generator.
     * 
     * @return the random generator
     */
    protected RandomEngine getRandomGenerator() {
        return randomGenerator;
    }

    /**
     * Constructs and returns a new uniform random number generation engine
     * seeded with the current time. Currently this is
     * {@link it.unibo.alchemist.external.cern.jet.random.engine.MersenneTwister}.
     * 
     * @return ask CERN staff :)
     */
    public static RandomEngine makeDefaultGenerator() {
        return AbstractRandomEngine.makeDefault();
    }

    /**
     * @return a random number from the distribution.
     */
    public abstract double nextDouble();

    /**
     * @return a random number from the distribution; returns
     *         <tt>(int) Math.round(nextDouble())</tt>. Override this method if
     *         necessary.
     */
    public int nextInt() {
        return (int) Math.round(nextDouble());
    }

    /**
     * Sets the uniform random generator internally used.
     * 
     * @param rng
     *            the random generator
     */
    protected void setRandomGenerator(final RandomEngine rng) {
        this.randomGenerator = rng;
    }
}
