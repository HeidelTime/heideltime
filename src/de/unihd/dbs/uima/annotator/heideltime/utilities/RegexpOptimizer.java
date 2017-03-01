package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to optimize constructed regexps for performance.
 * 
 * This is currently an ugly hack, and only supports a very limited subset of regular expressions.
 * 
 * In particular, only non-capturing groups are supported. It even has a hard-coded list (see method {@code #isSimple} of characters permitted.
 * 
 * Don't use it on regexps that expand massively, such as phone numbers!
 * 
 * This needs to eventually be rewritten into a more general tool, and with a proper regexp parser.
 * 
 * @author Erich Schubert
 */
public class RegexpOptimizer {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(RegexpOptimizer.class);

	/**
	 * Class when unsupported constructs are used in the optimizer.
	 * 
	 * @author Erich Schubert
	 */
	public static class OptimizerException extends Exception {
		/** Serialization version */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param m
		 *                Error message.
		 */
		public OptimizerException(String m) {
			super(m);
		}
	}

	@FunctionalInterface
	public static interface Consumer {
		void accept(CharSequence str) throws OptimizerException;
	}

	public static void expandPatterns(String s, Consumer out) throws OptimizerException {
		expandPatterns(s, 0, s.length(), new StringBuilder(), out);
	}

	public static void expandPatterns(String s, int i, int len, StringBuilder b, Consumer out) throws OptimizerException {
		if (i >= len) {
			out.accept(b);
			return;
		}
		char cur = s.charAt(i);
		if (isSimple(cur)) {
			int l = b.length();
			b.append(cur);
			expandPatterns(s, i + 1, len, b, out);
			b.setLength(l);
			return;
		}
		if (cur == '\\') {
			// Escape character.
			if (i == len)
				throw new OptimizerException("Last character was an escape in");
			int l = b.length();
			b.append(cur).append(s.charAt(i + 1));
			expandPatterns(s, i + 2, len, b, out);
			b.setLength(l);
			return;
		}
		if (cur == '?') {
			// Previous character was optional.
			int l = b.length();
			if (l == 0)
				throw new OptimizerException("First character was a question mark");
			if (isSimple(b.charAt(l - 1)) && (l == 1 || isSimple(b.charAt(l - 2)))) {
				// Expand with
				expandPatterns(s, i + 1, len, b, out);
				b.setLength(l - 1);
				// Expand without.
				expandPatterns(s, i + 1, len, b, out);
				b.setLength(l - 1);
			} else {
				b.append('?');
				expandPatterns(s, i + 1, len, b, out);
				b.setLength(l - 1);
			}
			return;
		}
		if (cur == '[') {
			int end = i + 1, nextp = -1;
			boolean simple = true;
			String optional = null;
			for (; end < len; end++) {
				char next = s.charAt(end);
				if (next == ']') {
					nextp = end + 1;
					if (end + 1 < len && s.charAt(end + 1) == '?') {
						optional = "?";
						nextp++;
						// Possessive
						if (end + 2 < len && s.charAt(end + 2) == '+') {
							optional = "?+";
							nextp++;
						}
					}
					break;
				}
				if (next == '-')
					if (end != i + 1 && (end + 1 < len && s.charAt(end + 1) != ']'))
						simple = false;
				if (next == '[')
					throw new OptimizerException("Nested [");
				if (next == '\\')
					throw new OptimizerException("Escaped chars");
			}
			if (end >= len || s.charAt(end) != ']')
				throw new OptimizerException("Did not find matching []");
			final int l = b.length();
			if (simple) {
				// Expand simple character ranges:
				if (optional != null) {
					// FIXME: retain possessive?
					expandPatterns(s, nextp, len, b, out);
					b.setLength(l);
				}
				for (int j = i + 1; j < end; j++) {
					char c = s.charAt(j);
					if (c == '.')
						b.append('\\');
					b.append(c);
					expandPatterns(s, nextp, len, b, out);
					b.setLength(l);
				}
			} else {
				assert (s.charAt(i) == '[' && s.charAt(end) == ']') : s.substring(i, nextp);
				if (end - i < 4) {
					// System.err.println("*****X " + s.substring(i, nextp) + " " + optional);
				} else if (end - i == 4) {
					// System.err.println("****** " + s.substring(i, nextp) + " " + optional);
					char c1 = s.charAt(i + 1), c2 = s.charAt(i + 2), c3 = s.charAt(i + 3);
					// Expand small ranges
					if (c2 == '-' && isSimple(c1) && isSimple(c3) && c1 < c3 && c3 - c1 < 10) {
						if (optional != null) {
							// FIXME: retain possessive?
							expandPatterns(s, nextp, len, b, out);
							b.setLength(l);
						}
						for (char c = c1; c <= c3; c++) {
							if (!isSimple(c)) {
								throw new OptimizerException("Non-simple char in char range: " + c);
							}
							b.append(c);
							expandPatterns(s, nextp, len, b, out);
							b.setLength(l);
						}
					}
					return;
				}
				// We simply copy&paste more complex character ranges
				for (int j = i; j < nextp; j++)
					b.append(s.charAt(j));
				expandPatterns(s, nextp, len, b, out);
				b.setLength(l);
			}
			return;
		}
		if (cur == '(') {
			int end = i + 1, begin = i + 1, nextp = -1;
			int depth = 1;
			boolean simple = true;
			String optional = null;
			for (int r = 0; end < len; end++, r++) {
				char next = s.charAt(end);
				if (r == 0) {
					if (next != '?')
						throw new OptimizerException("Non-optional group");
					++begin;
					continue;
				}
				if (r == 1) {
					if (next != ':')
						throw new OptimizerException("Non-optional group");
					++begin;
					continue;
				}
				if (next == ')' && --depth == 0) {
					nextp = end + 1;
					// Trailing modifiers
					if (end + 1 < len && s.charAt(end + 1) == '?') {
						optional = "?";
						nextp++;
						// Possessive
						if (end + 2 < len && s.charAt(end + 2) == '+') {
							optional = "?+";
							nextp++;
						}
					}
					break;
				}
				if (next == '\\') {
					simple = false;
					if (end + 1 == len)
						throw new OptimizerException("Escape at end of group?!?");
					++end;
				}
				if (next == '[' || next == '?' || next == '*' || next == '\\') {
					simple = false;
					// throw new ExpansionException("Special char " + next + " in group");
				}
				if (next == '(') {
					++depth;
					simple = false;
				}
			}
			if (end >= len || s.charAt(end) != ')')
				throw new OptimizerException("Did not find matching '()'");
			if (simple) {
				int l = b.length();
				if (optional != null) {
					expandPatterns(s, nextp, len, b, out);
					b.setLength(l);
				}
				for (int j = begin; j < end; j++) {
					char c = s.charAt(j);
					if (c == '|') {
						expandPatterns(s, nextp, len, b, out);
						b.setLength(l);
						continue;
					}
					b.append(c);
				}
				expandPatterns(s, nextp, len, b, out);
				b.setLength(l);
				return;
			}
			// Non-simple expansion:
			// LOG.trace("Need to expand: " + s.substring(begin - 3, begin) + ">>" + s.substring(begin, end) + "<<" + s.substring(end, nextp));
			assert (depth == 0);
			depth = 0;
			int l = b.length();
			if (optional != null) {
				expandPatterns(s, nextp, len, b, out);
				b.setLength(l);
			}
			final int cont = nextp; // Make effectively final.
			int prev = begin;
			for (int j = begin; j < end; j++) {
				char c = s.charAt(j);
				if (c == '|' && depth == 0) {
					// LOG.trace("Need to expand: " + s.substring(prev, j));
					expandPatterns(s, prev, j, new StringBuilder(), x -> {
						// LOG.trace("Recursive expansion to: " + x);
						b.append(x);
						expandPatterns(s, cont, len, b, out);
						b.setLength(l);
					});
					prev = j + 1;
				} else if (c == '(')
					++depth;
				else if (c == ')')
					--depth;
				else if (c == '\\')
					++j;
			}
			if (depth != 0)
				throw new OptimizerException("Could not close () group.");
			expandPatterns(s, prev, end, new StringBuilder(), x -> {
				// System.err.println("Recursive expansion to: " + x);
				b.append(x);
				expandPatterns(s, cont, len, b, out);
				b.setLength(l);
			});
			return;
		}
		throw new OptimizerException("Unhandled character " + cur + " at " + s.substring(Math.max(0, i - 5), Math.min(s.length(), i + 5)));
	}

	private static boolean isSimple(char cur) {
		return cur == ' ' || cur == '\'' || cur == '&' || cur == '-' || cur == ',' || Character.isAlphabetic(cur) || Character.isDigit(cur);
	}

	private static final Comparator<String> upperLowerChar = new Comparator<String>() {
		public int compare(String o1, String o2) {
			int l1 = o1.length(), l2 = o2.length();
			int l = l1 < l2 ? l1 : l2;
			for (int i = 0; i < l; i++) {
				char c1 = o1.charAt(i), c2 = o2.charAt(i);
				if (c1 != c2) {
					char d1 = Character.toLowerCase(c1), d2 = Character.toLowerCase(c2);
					return (d1 == d2) ? Character.compare(c1, c2) : Character.compare(d1, d2);
				}
			}
			return l1 < l2 ? -1 : l1 == l2 ? 0 : +1;
		}
	};

	public static String combinePatterns(Collection<String> patterns) throws OptimizerException {
		String[] ps = patterns.toArray(new String[patterns.size()]);
		Arrays.sort(ps, upperLowerChar);
		// Remove duplicates:
		int l = 0;
		String last = null;
		for (int i = 0; i < ps.length; i++) {
			String cand = ps[i];
			if (cand.equals(last)) {
				continue;
			}
			ps[l++] = cand;
			last = cand;
		}
		if (l < ps.length)
			LOG.trace("Removed {} duplicate strings.", ps.length - l);
		if (l == 0)
			return "";
		ArrayList<String> toplevel = new ArrayList<>();
		build(ps, 0, l, 0, x -> toplevel.add(x.toString()));
		StringBuilder buf = new StringBuilder();
		buildGroup(toplevel.toArray(new String[toplevel.size()]), 0, toplevel.size(), 0, 0, x -> {
			assert (buf.length() == 0);
			buf.append(x);
		}, new StringBuilder(), new StringBuilder());
		return buf.toString();
	}

	private static void build(String[] ps, int start, int end, int knownl, Consumer out) throws OptimizerException {
		String k = ps[start];
		// assert (k.length() > knownl) : "Duplicates not removed?";
		// Only one string remaining:
		if (start + 1 == end) {
			if (knownl == k.length()) {
				out.accept("");
				return;
			}
			char next = k.charAt(knownl);
			if (next == '*' || next == '?' || next == '+') {
				throw new OptimizerException("Bad split: " + k.substring(0, knownl) + "<<>>" + k.substring(knownl));
			}
			out.accept(k.substring(knownl));
			return;
		}
		int l = knownl < k.length() ? nextLength(k, knownl) : knownl;
		// System.err.println("Next length: " + l + " in " + k);
		StringBuilder buf1 = new StringBuilder(), buf2 = new StringBuilder();
		int begin = start, pos = start;
		while (pos < end) {
			String cand = ps[pos];
			if (k.regionMatches(0, cand, 0, l)) {
				++pos;
				continue;
			}
			buildGroup(ps, begin, pos, knownl, l, out, buf1, buf2);
			k = cand;
			begin = pos;
			l = nextLength(k, knownl);
			// System.err.println("Next length: " + l + " in " + k);
		}
		if (begin < pos) {
			buildGroup(ps, begin, pos, knownl, l, out, buf1, buf2);
		}
	}

	private static void buildGroup(String[] ps, int begin, int end, int subbegin, int subend, Consumer out, StringBuilder buf, StringBuilder tmp) throws OptimizerException {
		String key = ps[begin];
		// One element "group":
		if (begin + 1 == end) {
			buf.setLength(0);
			buf.append(key, subbegin, key.length());
			out.accept(buf);
			return;
		}
		// Two element "group":
		if (begin + 2 == end) {
			int p = prefixLength(ps, begin, end, subend);
			String other = ps[begin + 1];
			assert (!other.equals(key)) : "Duplicates not removed?";
			buf.setLength(0);
			buf.append(key, subbegin, p);
			if (p == key.length()) {
				if (p + 1 == other.length()) {
					buf.append(other.charAt(p)).append('?');
				} else {
					buf.append("(?:").append(other, p, other.length()).append(")?");
				}
			} else {
				buf.append("(?:").append(key, p, key.length()).append('|');
				buf.append(other, p, other.length()).append(')');
			}
			LOG.trace("buildGroup two-element case: {}", buf);
			out.accept(buf);
			return;
		}
		// So we have at least three strings now.
		// The basic pattern we build is: <prefix>(<midfix>(<alt1>|<alt2>)<?><postfix>)<?>
		// This should probably be handled by clever recursion eventually...
		// Challenges arise because of pattern optimizations, such as character groups.
		// Skip a prefix if shared by all strings:
		assert (subend <= key.length()) : key + " " + subbegin + "-" + subend;
		// p is the first position where they differ.
		int prefixend = prefixLength(ps, begin, end, subend), midfixend = prefixend;
		// Prefix alone is a valid pattern:
		final boolean prefixOnly;
		if (prefixOnly = (key.length() == prefixend)) {
			++begin;
			// Find midfix:
			midfixend = prefixLength(ps, begin, end, prefixend);
			key = ps[begin];
		}
		// All remaining patterns will begin with midfix now.
		// Expand all patterns, starting at the midfix position:
		ArrayList<String> cs = new ArrayList<>();
		build(ps, begin, end, midfixend, x -> cs.add(x.toString()));
		// Find a common postfix:
		String postfix = findPostfix(cs);
		// Check if we have an entry "<prefix><midfix><postfix>":
		boolean innerGroupOptional = cs.remove(postfix);
		if (cs.isEmpty()) {
			assert (prefixOnly && innerGroupOptional);
			// Simply <prefix>?
			buf.setLength(0);
			buf.append(key, subbegin, prefixend); // Add prefix
			buf.append("(?:");
			buf.append(key, prefixend, midfixend); // Midfix
			assert (postfix.charAt(postfix.length() - 1) == '?'); // Must be optional
			buf.append(postfix);
			buf.append(")?"); // prefixOnly
			LOG.trace("buildGroup degenerate case: {}", buf);
			out.accept(buf);
			return;
		}
		// Special case: the remaining difference is a single character:
		if (sameLength(cs, 1 + postfix.length())) {
			// Build the inner group in tmp as: <midfix>[a-z]<?><postfix>
			tmp.setLength(0);
			tmp.append(key, prefixend, midfixend); // Midfix
			if (cs.size() == 1) {
				// Single (optional) character:
				tmp.append(cs.get(0).charAt(0));
			} else {
				// Build character range (if more than one character):
				tmp.append('[');
				for (int i = 0; i < cs.size(); i++) {
					tmp.append(cs.get(i).charAt(0));
				}
				mergeCharRanges(tmp, 1); // Ignoring "["
				tmp.append(']');
			}
			if (innerGroupOptional) {
				tmp.append('?'); // May be optional
			}
			tmp.append(postfix); // We simply add the postfix, too.
			// tmp now is: <midfix>[a-z]<?><postfix>
			// Basic pattern to build: <prefix>(<tmp>)?
			buf.setLength(0);
			buf.append(key, subbegin, prefixend); // Add prefix
			// We only need the group for prefixOnly AND (midfix or postfix).
			if (prefixOnly && (prefixend != midfixend || !postfix.isEmpty())) {
				// Pattern: <prefix>(?:<mid><tmp?><postfix>)?
				buf.append("(?:");
				buf.append(tmp); // Char range
				buf.append(")?"); // close prefixOnly=true group
			} else {
				// Pattern: <prefix><[tmp]>?
				buf.append(tmp); // Char range (cannot have a '?')
				if (prefixOnly) {
					assert (!innerGroupOptional);
					buf.append('?'); // prefixOnly = true!
				}
			}
			LOG.trace("buildGroup sameLength case: {}", buf);
			out.accept(buf);
			return;
		}
		// At this point, at least one case has length 2!
		// We may need groups because:
		// 1. prefixOnly == true
		// 2. innerGroupOptional == true
		// Pattern: <prefix>(?:<midfix>(?:<alt1>|<alt2>)<?><postfix>)<?>
		// If midfix and postfix are empty, then we only need the inner group, even if prefixOnly
		final boolean outerParentheses = prefixOnly && (midfixend != prefixend || !postfix.isEmpty());
		buf.setLength(0);
		buf.append(key, subbegin, prefixend);
		if (outerParentheses) {
			buf.append("(?:");
		}
		buf.append(key, prefixend, midfixend);
		buf.append("(?:"); // inner

		// Merge subsequent alternatives with a common postfix except the first char into char ranges:
		for (int i = 0; i < cs.size(); i++) {
			String wi = cs.get(i);
			if (wi == null) {
				continue;
			}
			assert (wi.length() > 0);
			// Collect the letters if the postfix matches:
			tmp.setLength(0);
			tmp.append(wi.charAt(0));
			for (int j = i + 1; j < cs.size(); j++) {
				String wj = cs.get(j);
				if (wj == null || wi.length() != wj.length()) {
					continue;
				}
				char cj = wj.charAt(0);
				if (isSimple(cj) && wi.regionMatches(1, wj, 1, wi.length() - 1)) {
					tmp.append(cj);
					cs.set(j, null);
				}
			}
			// Separate alternatives
			if (i > 0) {
				buf.append('|');
			}
			if (tmp.length() > 1) {
				mergeCharRanges(tmp, 0);
				buf.append('[').append(tmp).append(']');
				buf.append(wi, 1, wi.length() - postfix.length());
			} else {
				buf.append(wi, 0, wi.length() - postfix.length());
			}
		}
		buf.append(')'); // Close inner group.
		if (innerGroupOptional) {
			buf.append('?');
		}
		buf.append(postfix);
		if (outerParentheses) {
			buf.append(')');
		}
		if (prefixOnly) {
			assert (outerParentheses || !innerGroupOptional);
			assert (buf.charAt(buf.length() - 1) == ')');
			buf.append('?');
		}
		LOG.trace("buildGroup base case: {}", buf);
		out.accept(buf);
	}

	/**
	 * Check if all strings have the same length.
	 *
	 * @param cs
	 *                Collection
	 * @param len
	 *                Required length
	 * @return {@code true} if all have the same length.
	 */
	private static boolean sameLength(Collection<String> cs, int len) {
		for (String s : cs) {
			if (s.length() != len) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Merge subsequent character ranges, if more than 2.
	 * 
	 * E.g. convert "01234" -> "0-4"
	 * 
	 * @param chars
	 *                Character buffer
	 * @param start
	 *                Starting position (set to 1 if you already have '[' in the buffer)
	 */
	private static void mergeCharRanges(StringBuilder chars, int start) {
		// Build ranges:
		for (int i = start; i < chars.length();) {
			char c = chars.charAt(i);
			int j = i + 1;
			while (j < chars.length() && chars.charAt(j) == ++c && isSimple(chars.charAt(j))) {
				++j;
			}
			if (j - i >= 3) {
				chars.replace(i, j, chars.charAt(i) + "-" + chars.charAt(j - 1));
				i += 2;
			} else {
				i = j;
			}
		}
	}

	private static String findPostfix(ArrayList<String> cs) {
		final String first = cs.get(0);
		final int num = cs.size();
		int l = 1, p = first.length() - 1, good = 0, level = 0;
		outer: while (p >= 0) {
			char c = first.charAt(p);
			char prev = p > 0 ? first.charAt(p - 1) : 'X';
			for (int i = 1; i < num; i++) {
				String cand = cs.get(i);
				if (cand.length() < l || cand.charAt(cand.length() - l) != c) {
					break outer;
				}
			}
			if (prev != '\\' && (c == '[' || c == '(')) {
				--level;
			}
			good = (level != 0 || !isSimple(c) || prev == '\\') ? good : l;
			if (prev != '\\' && (c == ']' || c == ')')) {
				++level;
			}
			if (prev == '\\' && c == '\\') {
				break; // Too complex, there could be more.
			}
			++l;
			--p;
		}
		return good > 0 ? first.substring(first.length() - good) : "";
	}

	private static int nextLength(String k, int p) throws OptimizerException {
		int l = p;
		assert (l < k.length()) : "Trying to access char " + l + " of: " + k;
		char next = k.charAt(l);
		if (next == '\\') {
			if (k.length() == l) {
				throw new OptimizerException("Trailing backslash? " + k);
			}
			++l;
		}
		++l;
		while (l < k.length()) {
			char next2 = k.charAt(l);
			if (next2 == '?' || next2 == '*' || next2 == '+') {
				++l;
			} else {
				break;
			}
		}
		return l;
	}

	/**
	 * Find the length of a shared prefix.
	 * 
	 * @param ps
	 *                Data array
	 * @param start
	 *                Subset begin
	 * @param end
	 *                Subset end
	 * @param p
	 *                Known prefix length
	 * @return New prefix length
	 */
	private static int prefixLength(String[] ps, int start, int end, int p) {
		final String k = ps[start];
		if (p == k.length()) {
			return p;
		}
		int good = p;
		int inset = 0;
		char prev = p > 0 ? k.charAt(p - 1) : 'X';
		char next = k.charAt(p);
		common: while (p < k.length()) {
			for (int i = start + 1; i < end; i++) {
				String cand = ps[i];
				if (cand.length() < p || cand.charAt(p) != next) {
					break common;
				}
			}
			if (prev == '\\') {
				prev = 'X';
			} else {
				if (next == '[') {
					++inset;
				} else if (next == ']') {
					--inset;
				}
				prev = next;
			}
			++p;
			next = p < k.length() ? k.charAt(p) : 'X';
			good = (inset > 0 || prev == '\\' || next == '?' || next == '*' || next == '+') ? good : p;
		}
		return good;
	}

	public static void main(String[] args) {
		try {
			String[] test = { //
					  // "1(?:st|\\.)? Advent", "first Advent", //
					  // "2(?:nd|\\.)? Advent", "second Advent", //
					  // "3(?:rd|\\.)? Advent", "third Advent", //
					  // "4(?:th|\\.)? Advent", "fourth Advent", //
					  // "Christmas(?: [Ee]ve| [Dd]ay)?", "Calennig", //
					  // "X-?(?:mas|MAS)", //
					  // "[0-9][0-9]?[0-9]?[0-9]?", // produces duplicates!
					  // "[Ss]ix", "[Ss]ixty", "[Ss]ixteen", //
					  // "[Ss]ixty[ -]?(?:one|two|three|four|five|six|seven|eight|nine)", //
					  // "1[0-9]", "1[0-9]th", //
					//"[Hh]eilig(?:en?|) [Dd]rei König(?:en?|)", "[Hh]eilig(?:en|) Abend",//
					"(?:[Aa](?:pril(?:is)?|ugusti?)|[Dd]e(?:cemb(?:er|r(?:\\.|is)?)|zember)|[Ff]ebruar(?:ii|y)?|[Hh]ornung|[Jj](?:anuar(?:ii|y)?|u(?:[ln](?:ii?|y))|änner)|[Mm](?:a(?:erz|ii?|r(?:ch|t(?:ii)?)|y)|[eä]rz)|[Nn]ovemb(?:er|r(?:\\.|is)?)|[Oo](?:ctob(?:er|r(?:\\.|is)?)|ktober)|[Ss]eptemb(?:er|r(?:\\.|is)?))", //
					"(?:[Aa](?:pr(?:\\.)?|ug(?:\\.)?)|[Dd]e(?:[cz](?:\\.)?)|[Ff]eb(?:\\.)?|[Jj](?:an(?:\\.)?|u(?:[ln](?:\\.)?))|[Mm](?:a(?:[iry])|är(?:\\.)?)|[Nn]ov(?:\\.)?|[Oo][ck]t(?:\\.)?|[Ss]ep(?:\\.|t(?:\\.)?)?)", //
					"(?:0[1-9]|1[0-2]?|[2-9])", //
			};

			ArrayList<String> expanded = new ArrayList<>();
			for (String s : test) {
				expandPatterns(s, x -> expanded.add(x.toString()));
			}
			// Note: this may still contain duplicates!
			Collections.sort(expanded, upperLowerChar);
			for (String s : expanded.subList(0, Math.min(expanded.size(), 100))) {
				System.out.println(s);
			}
			if (expanded.size() >= 100) {
				System.out.println("... and " + (expanded.size() - 100) + " more.");
			}
			System.out.println("---> converted to --->");
			String combined = combinePatterns(expanded);
			System.out.println(combined);
		} catch (OptimizerException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}