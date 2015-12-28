package it.unibo.alchemist.external.cern.jet.random.engine;

import java.io.Serializable;

import org.apache.commons.math3.random.RandomGenerator;

import cern.colt.function.DoubleFunction;
import cern.colt.function.IntFunction;

/**
 * Interface for uniform pseudo-random number generating engines.
 * 
 */
@Deprecated
public interface RandomEngine extends DoubleFunction, IntFunction, RandomGenerator, Serializable {

    /**
     * @return the initial seed.
     */
    int getSeed();

    /**
     * @return an exact copy of this {@link RandomEngine}
     */
    RandomEngine clone();

}
