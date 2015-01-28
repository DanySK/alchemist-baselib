/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.utils;

import it.unibo.alchemist.Global;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.IntStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * This class wraps java.lang.String and provides faster equals(). Used
 * internally to ensure better performances. The faster comparison is realized
 * by computing a hash internally (currently using Murmur 3), so be aware that
 * collisions may happen.
 * 
 * @author Danilo Pianini
 * 
 */
public class FasterString implements Cloneable, Serializable, Comparable<FasterString>, CharSequence {

	private static final long serialVersionUID = -3490623928660729120L;
	private static final Charset CHARSET = Charset.forName("UTF8");
	private static final HashFunction HASHF = Hashing.murmur3_128();
	private String base;
	private transient HashCode hash;
	private long hash64;
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
		hash64 = string.hash64;
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
		hash = HASHF.hashBytes(s.getBytes(CHARSET));
		hash32 = hash.asInt();
		hash64 = hash.asLong();
	}

	/**
	 * Overloaded method.
	 * 
	 * @param fs
	 *            the FasterString to compare to
	 * @return true if equals
	 */
	public boolean equals(final FasterString fs) {
		return hashCode() == fs.hashCode()
				&& s.length() == fs.s.length()
				&& hash64 == fs.hash64
				&& hash.equals(fs.hash);
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
		if (hash == null) {
			computeHashes();
		}
		return hash64;
	}

	@Override
	public int hashCode() {
		if (hash == null) {
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
			final long h64 = hash64 > 0 ? hash64 : -(hash64 + 1);
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
