/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package it.unibo.alchemist.external.cern.jet.random;

/**
 * Abstract base class for all continous distributions.
 * 
 */
@Deprecated
public abstract class AbstractContinousDistribution extends AbstractDistribution {
    /**
     * 
     */
    private static final long serialVersionUID = -6076386102892935656L;

    /**
     * Makes this class non instantiable, but still let's others inherit from
     * it.
     */
    protected AbstractContinousDistribution() {
        super();
    }

}
