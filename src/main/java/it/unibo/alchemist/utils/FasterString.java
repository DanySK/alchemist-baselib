/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.utils;

import static org.danilopianini.lang.Constants.DJB2_MAGIC;
import static org.danilopianini.lang.Constants.DJB2_SHIFT;
import static org.danilopianini.lang.Constants.DJB2_START;
import it.unibo.alchemist.Global;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * This class wraps java.lang.String and provides faster equals(). Used
 * internally to ensure better performances. This class guarantees a 100%
 * correct comparison for all the Strings up to 8 characters long.
 * 
 * @author Danilo Pianini
 * 
 */
public class FasterString implements Cloneable, Serializable, Comparable<FasterString>, CharSequence {

	private static final long serialVersionUID = -3490623928660729120L;
	private String base;
	private long hash;
	private int hash32;
	private final String s;

	/**
	 * Clones this object.
	 * 
	 * @param string
	 *            the template for the clone
	 */
	public FasterString(final FasterString string) {
		s = string.s;
		hash = string.hash;
		hash32 = string.hash32;
	}

	/**
	 * @param string
	 *            the String to wrap
	 */
	public FasterString(final String string) {
		Objects.requireNonNull(string);
		s = string;
	}

	@Override
	public char charAt(final int index) {
		return s.charAt(index);
	}

	@Override
	public FasterString clone() {
		/*
		 * State cannot change, no need to deep copy anything.
		 */
		return this;
	}

	@Override
	public int compareTo(final FasterString o) {
		return s.compareTo(o.s);
	}

	/**
	 * djb2.
	 */
	private void computeHashes() {
		hash = DJB2_START;
		hash32 = DJB2_START;
		final byte[] bytes = s.getBytes();
		for (final byte b : bytes) {
			hash = ((hash << DJB2_SHIFT) + hash) + b;
			hash32 = hash32 * DJB2_MAGIC ^ b;
		}
	}

	/**
	 * Overloaded method.
	 * 
	 * @param fs
	 *            the FasterString to compare to
	 * @return true if equals
	 */
	public boolean equals(final FasterString fs) {
		return (hashCode() == fs.hashCode()) && (hash == fs.hash) && (s.length() == fs.s.length());
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof FasterString) {
			return equals((FasterString) o);
		}
		return false;
	}

	/**
	 * @return a 64bit hash, computed with DJB2
	 */
	public long hash64() {
		if (hash == 0) {
			computeHashes();
		}
		return hash;
	}

	@Override
	public int hashCode() {
		if (hash32 == 0 && hash == 0) {
			computeHashes();
		}
		return hash32;
	}

	/**
	 * @return A Base64 encoded version of the hash
	 */
	public String hashToString() {
		if (base == null) {
			/*
			 * If hash32 is negative, it is necessary to sum 1. This is because
			 * -Integer.MIN_VALUE is equal to Integer.MIN_VALUE.
			 */
			final int h32 = hashCode() > 0 ? hash32 : -(hash32 + 1);
			final long h64 = hash > 0 ? hash : -(hash + 1);
			base = Integer.toString(h32, Global.ENCODING_BASE) + Long.toString(h64, Global.ENCODING_BASE);
		}
		return base;
	}

	@Override
	public int length() {
		return s.length();
	}

	@Override
	public CharSequence subSequence(final int start, final int end) {
		return s.subSequence(start, end);
	}

	@Override
	public String toString() {
		return s;
	}

	@Override
	public IntStream chars() {
		return s.chars();
	}

	@Override
	public IntStream codePoints() {
		return s.codePoints();
	}

}
