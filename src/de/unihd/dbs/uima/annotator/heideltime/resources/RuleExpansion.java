package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.utilities.ChineseNumbers;

/**
 * HeidelTime rule expansion logic.
 * 
 * There is some copy and paste involved in the {@code expandX} functions, but this allows the hotspot VM to optimize them independently.
 * 
 * This should probably be integrated into the {@link Rule} class, and only some expansions are necessary.
 * 
 * @author Erich Schubert
 */
public class RuleExpansion {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(RuleExpansion.class);

	static Pattern paNorm = Pattern.compile("%([A-Za-z0-9]+?)\\(group\\(([0-9]+)\\)\\)");
	static Pattern paGroup = Pattern.compile("group\\(([0-9]+)\\)");
	static Pattern paSubstring = Pattern.compile("%SUBSTRING%\\((.*?),([0-9]+),([0-9]+)\\)");
	static Pattern paLowercase = Pattern.compile("%LOWERCASE%\\((.*?)\\)");
	static Pattern paUppercase = Pattern.compile("%UPPERCASE%\\((.*?)\\)");
	static Pattern paSum = Pattern.compile("%SUM%\\((.*?),(.*?)\\)");
	static Pattern paNormNoGroup = Pattern.compile("%([A-Za-z0-9]+?)\\((.*?)\\)");
	static Pattern paChineseNorm = Pattern.compile("%CHINESENUMBERS%\\((.*?)\\)");
	static Pattern WHITESPACE_NORM = Pattern.compile("[\n\\s]+");

	public static String applyRuleFunctions(String rule, String pattern, MatchResult m, NormalizationManager norm, Language language) {
		StringBuilder tonormalize = new StringBuilder(pattern);
		// pattern for normalization functions + group information
		// pattern for group information
		Matcher mr = paNorm.matcher(tonormalize);
		while (tonormalize.indexOf("%") >= 0 || tonormalize.indexOf("group") >= 0) {
			// replace normalization functions
			expandNormalizationGroup(tonormalize, mr, norm, m, rule);
			// replace other groups
			expandGroups(tonormalize, mr, m, rule);
			// apply the substring function
			expandSubstringFunction(tonormalize, mr, m, rule);
			if (language.useLowercase()) {
				expandLowerCaseFunction(tonormalize, mr);
				expandUpperCaseFunction(tonormalize, mr);
			}
			// replace sum, concatenation
			expandSumFunction(tonormalize, mr, m, rule);
			// replace normalization function without group
			expandNormalizationFull(tonormalize, mr, norm, rule);
			// replace Chinese with Arabic numerals
			replaceChineseNumerals(tonormalize, mr);
		}
		return tonormalize.toString();
	}

	private static void expandNormalizationGroup(StringBuilder tonormalize, Matcher mr, NormalizationManager norm, MatchResult m, String rule) {
		mr.usePattern(paNorm).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			String normfunc = mr.group(1);
			int start = mr.start(), end = mr.end();
			int groupid = Integer.parseInt(mr.group(2));
			if (LOG.isTraceEnabled()) {
				LOG.trace("rule:" + rule);
				LOG.trace("tonormalize:" + tonormalize.toString());
				LOG.trace("x.group():" + mr.group());
				LOG.trace("x.group(1):" + normfunc);
				LOG.trace("x.group(2):" + mr.group(2));
				LOG.trace("m.group():" + m.group());
				LOG.trace("m.group(" + groupid + "):" + m.group(groupid));
				LOG.trace("hmR...:" + norm.getFromHmAllNormalization(normfunc).get(m.group(groupid)));
			}

			if (groupid > m.groupCount()) {
				LOG.error("Invalid group reference '{}' in normalization pattern of rule: {}", groupid, rule);
				tonormalize.delete(start, end);
				continue;
			}
			String value = m.group(groupid);
			if (value == null) {
				// This is not unusual to happen
				LOG.debug("Empty part to normalize in {}, rule {}, '{}'", normfunc, rule, m.group());
				tonormalize.delete(start, end);
				continue;
			}
			value = WHITESPACE_NORM.matcher(value).replaceAll(" ");
			RegexHashMap<String> normmap = norm.getFromHmAllNormalization(normfunc);
			String rep = normmap != null ? normmap.get(value) : null;
			if (rep == null) {
				if (normfunc.contains("Temponym")) {
					LOG.debug("Temponym '{}' normalization problem. Value: {} in " + //
							"rule: {} tonormalize: {}", normfunc, value, rule, tonormalize);
					tonormalize.delete(start, end);
					continue;
				}
				LOG.warn("'{}' normalization problem. Value: {} in " + //
						"rule: {} tonormalize: {}", normfunc, value, rule, tonormalize);
				tonormalize.delete(start, end);
				continue;
			}
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}

	private static void expandGroups(StringBuilder tonormalize, Matcher mr, MatchResult m, String rule) {
		mr.usePattern(paGroup).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			int groupid = Integer.parseInt(mr.group(1));
			int start = mr.start(), end = mr.end();
			if (groupid > m.groupCount()) {
				LOG.error("Invalid group reference '{}' in normalization pattern of rule: {}", groupid, rule);
				tonormalize.delete(start, end);
				continue;
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("tonormalize:" + tonormalize);
				LOG.trace("x.group():" + mr.group());
				LOG.trace("x.group(1):" + mr.group(1));
				LOG.trace("m.group():" + mr.group());
				LOG.trace("m.group(" + mr.group(1) + "):" + m.group(groupid));
			}
			String rep = m.group(groupid);
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}

	private static void expandNormalizationFull(StringBuilder tonormalize, Matcher mr, NormalizationManager norm, String rule) {
		mr.usePattern(paNormNoGroup).reset(tonormalize);
		int pos = 0;
		while (mr.find(pos)) {
			String normfunc = mr.group(1);
			String value = mr.group(2);
			String rep = norm.getFromHmAllNormalization(normfunc).get(value);
			if (rep == null) {
				LOG.warn("'{}' normalization problem. Value: {} in " + //
						"rule: {} tonormalize: {}", normfunc, value, rule, tonormalize);
				rep = "";
			}
			int start = mr.start(), end = mr.end();
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}

	private static void expandSubstringFunction(StringBuilder tonormalize, Matcher mr, MatchResult m, String rule) {
		mr.usePattern(paSubstring).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			int start = mr.start(), end = mr.end();
			try {
				String rep = mr.group(1).substring(Integer.parseInt(mr.group(2)), Integer.parseInt(mr.group(3)));
				tonormalize.replace(start, end, rep);
				pos = start + rep.length();
			} catch (StringIndexOutOfBoundsException e) {
				LOG.error("Substring out of bounds: '{}' for '{}' with rule '{}'", mr.group(), m.group(), rule, e);
				tonormalize.delete(start, end);
			}
		}
	}

	private static void expandLowerCaseFunction(StringBuilder tonormalize, Matcher mr) {
		mr.usePattern(paLowercase).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			String rep = mr.group(1).toLowerCase();
			int start = mr.start(), end = mr.end();
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}

	private static void expandUpperCaseFunction(StringBuilder tonormalize, Matcher mr) {
		mr.usePattern(paUppercase).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			String rep = mr.group(1).toUpperCase();
			int start = mr.start(), end = mr.end();
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}

	private static void expandSumFunction(StringBuilder tonormalize, Matcher mr, MatchResult m, String rule) {
		mr.usePattern(paSum).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			int start = mr.start(), end = mr.end();
			try {
				String rep = Integer.toString(Integer.parseInt(mr.group(1)) + Integer.parseInt(mr.group(2)));
				tonormalize.replace(start, end, rep);
				pos = start + rep.length();
			} catch (NumberFormatException e) {
				LOG.error("Failed to expand sum: '{}' for '{}' with rule '{}'", mr.group(), m.group(), rule, e);
				tonormalize.delete(start, end);
			}
		}
	}

	private static void replaceChineseNumerals(StringBuilder tonormalize, Matcher mr) {
		mr.usePattern(paChineseNorm).reset(tonormalize);
		for (int pos = 0; mr.find(pos);) {
			String rep = ChineseNumbers.normalize(mr.group(1));
			if (rep == null) // TODO: Add a warning
				continue;
			int start = mr.start(), end = mr.end();
			tonormalize.replace(start, end, rep);
			pos = start + rep.length();
		}
	}
}