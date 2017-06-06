package de.unihd.dbs.uima.annotator.heideltime.utilities;

public class ParseInteger {
	private ParseInteger() {
		// Utility class, use static methods.
	}

	/**
	 * Parse an integer within a string.
	 * 
	 * This is derived from {@link Integer#parseInt(String)}, but allows using subsequences of an arbitrary CharSequence.
	 * 
	 * @param s
	 *                String
	 * @param b
	 *                Start
	 * @param e
	 *                End (exclusive)
	 * @return Integer
	 */
	public static int parseInt(CharSequence s, int b, int e) {
		if (s == null)
			throw new NumberFormatException("null");
		if (e <= b)
			throw new NumberFormatException("Empty string");

		int result = 0;
		boolean negative = false;
		int limit = -Integer.MAX_VALUE;
		int multmin = limit / 10;
		int digit;

		int i = b;
		char firstChar = s.charAt(i);
		if (firstChar < '0') { // Possible leading "+" or "-"
			if (firstChar == '-') {
				negative = true;
				limit = Integer.MIN_VALUE;
			} else if (firstChar != '+')
				throw new NumberFormatException("For input string: \"" + s.subSequence(b, e) + "\"");
			i++;
			if (i == e) // Cannot have lone "+" or "-"
				throw new NumberFormatException("lone + or -");
		}
		while (i < e) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			digit = Character.digit(s.charAt(i++), 10);
			if (digit < 0)
				throw new NumberFormatException("For input string: \"" + s.subSequence(b, e) + "\"");
			if (result < multmin)
				throw new NumberFormatException("For input string: \"" + s.subSequence(b, e) + "\"");
			result *= 10;
			if (result < limit + digit)
				throw new NumberFormatException("For input string: \"" + s.subSequence(b, e) + "\"");
			result -= digit;
		}
		return negative ? result : -result;
	}

	public static int parseInt(CharSequence s) {
		return parseInt(s, 0, s.length());
	}

	public static int parseIntAt(CharSequence s, int b) {
		return parseInt(s, b, s.length());
	}
}
