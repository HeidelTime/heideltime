package de.unihd.dbs.uima.annotator.heideltime;

/**
 * Heideltime document types.
 */
public enum DocumentType {
	COLLOQUIAL("colloquial"), NEWS("news"), NARRATIVE("narrative"), SCIENTIFIC("scientific");
	String name;

	DocumentType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static DocumentType of(String s) {
		switch (s) {
		case "colloquial":
			return COLLOQUIAL;
		case "news":
			return NEWS;
		case "narrative":
		case "narratives":
			return NARRATIVE;
		case "scientific":
			return SCIENTIFIC;
		default:
			throw new IllegalArgumentException("Unknown document type: " + s);
		}
	}
}