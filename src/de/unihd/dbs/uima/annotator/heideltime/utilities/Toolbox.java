package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Toolbox class contains methods with functionality that you would also
 * find outside the context of HeidelTime's specific skillset; i.e. they do
 * not require the CAS context, but are 'useful code snippets'.
 *
 * @author jannik stroetgen
 */
public class Toolbox {
	/**
	 * Find all the matches of a pattern in a charSequence and return the
	 * results as list.
	 * 
	 * @param pattern Pattern to be matched
	 * @param s String to be matched against
	 * @return Iterable List of MatchResults
	 */
	public static Iterable<MatchResult> findMatches(Pattern pattern, CharSequence s) {
		Matcher m = pattern.matcher(s);
		if (m.find()) {
			List<MatchResult> results = new ArrayList<MatchResult>();
			results.add(m.toMatchResult()); // First match
			while(m.find()) // Subsequent matches
				results.add(m.toMatchResult());
			return results;
		}
		return Collections.emptyList();
	}
}
