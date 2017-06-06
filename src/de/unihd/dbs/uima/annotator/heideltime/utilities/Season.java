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

	public static Season of(String s) {
		switch (s) {
		case "SP":
			return SPRING;
		case "SU":
			return SUMMER;
		case "FA":
			return FALL;
		case "WI":
			return WINTER;
		}
		return null;
	}
}