package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * Class for more efficient matching.
 *
 * This class applies a regular expression only at known token boundary positions, rather than attempting to match at any character.
 * 
 * @author Erich Schubert
 */
public class TokenBoundaryMatcher {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(TokenBoundaryMatcher.class);

	/** Enable pattern profiling, to identify unusually slow patterns */
	private static final boolean PROFILE_REGEXP = false;

	/** Data storage for profiling */
	private HashMap<String, Long> profileData = PROFILE_REGEXP ? new HashMap<String, Long>() : null;

	/** Storage for valid starting positions */
	IntArrayList startpos = new IntArrayList();

	/** Storage for valid end positions */
	IntArrayList endpos = new IntArrayList();

	/**
	 * Simplify a string by doing some character substitutions.
	 * 
	 * This intentionally does not change the length, to preserve offsets!
	 *
	 * @param in
	 *                Input text
	 * @return Simplified text
	 */
	public static CharSequence simplifyString(CharSequence in) {
		StringBuilder buf = new StringBuilder(in);
		final int len = in.length();
		for (int i = 0; i < len; i++) {
			char c = buf.charAt(i);
			if (c == '\t' || c == '\u00A0' || (c >= '\u2000' && c <= '\u200D') || c == '\u202F' || c == '\u205F' || c == '\u2060' || c == '\u3000' || c == '\uFEFF') {
				// Normalize whitespace (but leave \n\r)
				buf.setCharAt(i, ' ');
			} else if (c >= '\u2011' && c <= '\u2014') {
				// Normalize unicode hyphens:
				buf.setCharAt(i, '-');
			} else if (c == '\u000B' || c == '\u000C' || c == '\u0085' || c == '\u2028' || c == '\u2029') {
				// Unusual line and paragraph breaks.
				buf.setCharAt(i, '\n');
			}
			// TODO: add double-width arabic digits? But they are 2 chars wide.
		}
		return buf;
	}

	/**
	 * Build a list of admissible token boundaries, for faster matching.
	 *
	 * If a token begins or ends with [.,/-], this character will be an optional match.
	 *
	 * @param startpos
	 *                Output valid starting positions
	 * @param endpos
	 *                Output valid end positions
	 * @param s
	 *                Sentence
	 * @param jcas
	 *                JCas
	 * @param coveredText
	 *                Text covered by sentence
	 */
	public void tokenBoundaries(CharSequence coveredText, Sentence s, JCas jcas) {
		startpos.clear();
		endpos.clear();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		final int offset = s.getBegin();
		startpos.add(0); // s.getBegin() - offset
		for (FSIterator<Token> iterToken = tokens.subiterator(s); iterToken.hasNext();) {
			Token t = iterToken.next();
			int begin = t.getBegin() - offset, end = t.getEnd() - offset;
			if (begin == end)
				continue;
			// Allow begin and end for anchoring:
			if (checkBegin(coveredText, begin))
				startpos.add(begin);
			// Note, begin < end!
			char first = coveredText.charAt(begin);
			if (first == '.' || first == ',' || first == '/' || first == '-')
				if (checkBegin(coveredText, begin + 1))
					startpos.add(begin + 1);
			int lastp = end - 1;
			if (begin < lastp) { // Avoid checking one-char tokens twice.
				char lastc = coveredText.charAt(lastp);
				if (lastc == '.' || lastc == ',' || lastc == '/' || lastc == '-')
					if (checkEnd(coveredText, lastp))
						endpos.add(lastp);
				// Stanford produces tokens like "2016/2017".
				if (isDigit(first) && isDigit(lastc)) {
					int left = begin + 1, right = lastp - 1;
					while(left < right && isDigit(coveredText.charAt(left)))
						++left;
					while(left < right && isDigit(coveredText.charAt(right)))
						--right;
					if (left == right) {
						char sep = coveredText.charAt(left);
						if (sep == '/' /* || sep == '-' || sep == '.' || sep == ',' */) {
							endpos.add(left);
							startpos.add(left + 1);
						}
					}
				}
			}
			if (checkEnd(coveredText, end))
				endpos.add(end);
		}
		endpos.add(s.getEnd() - offset);
		startpos.sortRemoveDuplicates();
		endpos.sortRemoveDuplicates();
		if (LOG.isTraceEnabled())
			LOG.trace("Token boundaries: {}", debugTokenBoundaries(coveredText));
	}

	/**
	 * Produce a debug representation of the token boundaries.
	 * 
	 * @param cov
	 *                Covered text
	 * @param startpos
	 *                Start positions
	 * @param endpos
	 *                End positions
	 * @return String buffer
	 */
	public StringBuilder debugTokenBoundaries(CharSequence cov) {
		final int l = cov.length(), sl = startpos.size(), el = endpos.size();
		assert (endpos.size() == 0 || endpos.get(0) > 0);
		StringBuilder buf = new StringBuilder(l + sl + el);
		for (int c = 0, s = 0, e = 0; c < l; c++) {
			if (s < sl && c == startpos.get(s)) {
				buf.append('»');
				++s;
			}
			buf.append(cov.charAt(c));
			if (e < el && c + 1 == endpos.get(e)) {
				buf.append('«');
				++e;
			}
		}
		return buf;
	}

	/**
	 * Check the beginning of a token. Disallow 0123.2016 to match 2016.
	 * 
	 * @param cov
	 *                Text
	 * @param begin
	 *                Position
	 * @return {@code false} if a bad position
	 */
	private static boolean checkBegin(CharSequence cov, int begin) {
		final int len = cov.length();
		if (begin >= len)
			return false;
		// Position 0 is always allowed.
		if (begin == 0)
			return true;
		// Check digits:
		final char curr = cov.charAt(begin);
		if (isDigit(curr)) {
			// Check previous character:
			final char prev = cov.charAt(begin - 1);
			// get rid of expressions if there is a character or symbol ($+) directly in front of the expression
			if (prev == '$' || prev == '€' || prev == '+' || Character.isAlphabetic(prev))
				return false;
			if (begin == 1)
				return true;
			// Check two characters: [0-9]\.
			if (prev == '.' && isDigit(cov.charAt(begin - 2)))
				return false;
		} else if (curr == '.' || curr == ',') {
			if (begin + 1 < len && isDigit(cov.charAt(begin - 1)) && isDigit(cov.charAt(begin + 1)))
				return false; // Looks like a decimal point
		}
		return true;
	}

	/**
	 * Check the end position of a token. Disallow 2016.12345 to match 2016.
	 * 
	 * @param cov
	 *                Text
	 * @param end
	 *                Position
	 * @return {@code false} if a bad position
	 */
	private static boolean checkEnd(CharSequence cov, int end) {
		final int len = cov.length();
		if (end == 0 || end > len)
			return false; // would be empty, or invalid
		// End is always allowed.
		if (end == len)
			return true;
		// Current character_
		final char curr = cov.charAt(end - 1);
		if (isDigit(curr)) {
			final char succ = cov.charAt(end);
			if (succ == '%' || succ == '(' || succ == '°' || succ == '$' || succ == '€' || Character.isAlphabetic(succ))
				return false;
			if (end + 1 < len && (succ == '.' || succ == ','))
				if (isDigit(cov.charAt(end + 1)))
					return false; // Looks like a decimal point
		}
		return true;
	}

	/**
	 * In contrast to {@link Character#isDigit}, we only allow ascii digits here.
	 * 
	 * (The other digits require two chars!)
	 *
	 * @param c
	 *                Character
	 * @return {@code true} if '0'-'9'
	 */
	public static boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}

	/**
	 * Find the next match beginning and ending at token boundaries.
	 *
	 * @param start
	 *                Search position
	 * @param m
	 *                Matcher
	 * @param key
	 *                Key (for performance logging)
	 * @return Search position to continue, or {@code -1} if no match.
	 */
	public int matchNext(int start, Matcher m, String key) {
		assert (start >= 0);
		long begin = PROFILE_REGEXP ? System.nanoTime() : 0;
		final int slen = startpos.size();
		if (start >= slen || endpos.size() == 0)
			return -1;
		final int epos = endpos.get(endpos.size() - 1);
		while (start < slen) {
			if (m.region(startpos.get(start), epos).lookingAt()) {
				// Match. Ensure end matched a token boundary:
				final int mend = m.end();
				if (endpos.binarySearch(mend) >= 0) {
					// Good match.
					// Find position to continue matching:
					int etok = start + 1;
					while (etok < slen && startpos.get(etok) < mend)
						++etok;
					if (PROFILE_REGEXP) {
						long dur = System.nanoTime() - begin;
						Long old = profileData.get(key);
						profileData.put(key, dur + (old != null ? old.longValue() : 0L));
					}
					return etok;
				}
			}
			++start;
		}
		if (PROFILE_REGEXP) {
			long dur = System.nanoTime() - begin;
			Long old = profileData.get(key);
			profileData.put(key, dur + (old != null ? old.longValue() : 0L));
		}
		return -1; // No match
	}

	/**
	 * Output profiling data, if enabled.
	 */
	public void logProfilingData() {
		if (!PROFILE_REGEXP)
			return;
		long sum = 0;
		for (Long v : profileData.values())
			sum += v;
		double avg = sum / (double) profileData.size();

		StringBuilder buf = new StringBuilder();
		buf.append("Profiling data:\n");
		buf.append("Average: ").append(avg).append("\n");
		buf.append("Rules with above average cost:\n");
		for (Map.Entry<String, Long> ent : profileData.entrySet()) {
			long v = ent.getValue();
			if (v > 2 * avg)
				buf.append(v).append('\t').append(ent.getKey()).append('\t').append(v / avg).append("\n");
		}
		LOG.warn(buf.toString());
	}
}
