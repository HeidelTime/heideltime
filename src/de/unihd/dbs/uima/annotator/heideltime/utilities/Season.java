package de.unihd.dbs.uima.annotator.heideltime.utilities;

public enum Season {
	SPRING("SP", 0), SUMMER("SU", 1), FALL("FA", 2), WINTER("WI", 3);
	protected String str;
	protected int off;

	private Season(String s, int o) {
		str = s;
		off = o;
	}

	@Override
	public String toString() {
		return str;
	}

	/**
	 * Ordinal representation, spring = 0, summer = 1, fall = 2, winter = 3
	 *
	 * @return Ordinal
	 */
	public int ord() {
		return off;
	}

	public static Season of(CharSequence s, int b) {
		if (b + 1 >= s.length())
			return null;
		char c1 = s.charAt(b), c2 = s.charAt(b + 1);
		if (c1 == 'S') {
			if (c2 == 'P')
				return SPRING;
			if (c2 == 'U')
				return SUMMER;
		} else if (c1 == 'F') {
			if (c2 == 'A')
				return FALL;
		} else if (c1 == 'W') {
			if (c2 == 'I')
				return WINTER;
		}
		return null;
	}

	public static Season of(CharSequence s) {
		return of(s, 0);
	}
}