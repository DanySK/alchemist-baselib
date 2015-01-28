package it.unibo.alchemist.test;

import static org.junit.Assert.fail;
import it.unibo.alchemist.utils.FasterString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author Danilo Pianini
 *
 */
public class TestFasterString {

	private static final byte MAX_LEN = 3;
	private static final char MIN_CHAR = ' ';
	private static final char MAX_CHAR = '~';
	
	/**
	 * Ensures no collisions for strings three characters long.
	 */
	@Test
	public void testEqualsFasterString() {
		final Map<FasterString, FasterString> set = new HashMap<>();
		for (byte len = 1; len <= MAX_LEN; len++) {
			final char[] cur = new char[len];
			Arrays.fill(cur, MIN_CHAR);
			int pos = 0;
			while (pos < len) {
				final FasterString fs = new FasterString(new String(cur));
				final FasterString existing = set.get(fs);
				if (existing != null) {
					fail(fs + " collides with " + existing);
				} else {
					set.put(fs, fs);
				}
				cur[0]++;
				if (pos < len && cur[pos] > MAX_CHAR) {
					while (pos < len && cur[pos] >= MAX_CHAR) {
						cur[pos++] = MIN_CHAR;
					}
					if (pos < len) {
						cur[pos]++;
						pos = 0;
					}
				}
			}
		}
	}

}
