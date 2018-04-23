package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * Abstract class for all Resource Managers to inherit from. Contains basic functionality such as file system access and some private members.
 *
 */
public abstract class GenericResourceManager {
	// language for the utilized resources
	protected final String LANGUAGE;
	// kind of resource -- e.g. repattern, normalization, rules
	protected String resourceType;

	/**
	 * Instantiates the Resource Manager with a resource type
	 * 
	 * @param resourceType
	 *                kind of resource to represent
	 */
	protected GenericResourceManager(String resourceType, String language) {
		this.resourceType = resourceType;
		this.LANGUAGE = language;
	}

	private static final Pattern WHITESPACE = Pattern.compile("(?: |\\\\[sS])");

	public static String replaceSpaces(String inText) {
		Matcher m = WHITESPACE.matcher(inText);
		if (!m.find())
			return inText;
		final int len = inText.length();
		StringBuilder buf = new StringBuilder();
		int lastpos = 0;
		do {
			int start = m.start(), end = m.end();
			final char lastchar = inText.charAt(end - 1);
			assert (lastchar == ' ' || lastchar == 's' || lastchar == 'S');
			boolean negative = lastchar == 'S';
			boolean chargroup = false;
			String extra = "+"; // By default, insert a plus.
			if (end < len) {
				char next = inText.charAt(end);
				if (next == '?' || next == '*' || next == '+' || next == '{')
					extra = null; // Preserve
				if (next == ']' && start > 0 && inText.charAt(start - 1) == '[') {

				}
			}
			for (int s = end; s < len; s++) {
				char next = inText.charAt(s);
				if (next == '[' && inText.charAt(s - 1) != '\\')
					break; // Supposedly not in a character group.
				if (next == ']' && inText.charAt(s - 1) != '\\') {
					chargroup = true;
					break;
				}
			}
			buf.append(inText, lastpos, start);
			if (chargroup) {
				// buf.append(negative ? "\\P{javaWhitespace}" : "\\p{javaWhitespace}");
				buf.append(negative ? "\\S" : "\\s");
			} else {
				// buf.append(negative ? "[\\P{javaWhitespace}]" : "[\\p{javaWhitespace}]");
				buf.append(negative ? "\\S" : "\\s");
				if (extra != null)
					buf.append(extra);
			}
			lastpos = end;
		} while (m.find());
		if (lastpos < len)
			buf.append(inText, lastpos, len);
		return buf.toString();
	}
}
