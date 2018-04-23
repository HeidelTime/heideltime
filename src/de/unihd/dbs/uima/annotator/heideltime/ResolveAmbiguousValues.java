package de.unihd.dbs.uima.annotator.heideltime;

import static de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer.*;
import static de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator.*;
import static de.unihd.dbs.uima.annotator.heideltime.utilities.ParseInteger.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer.Tense;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Season;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Timex3;

class ResolveAmbiguousValues {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ResolveAmbiguousValues.class);

	private static final Pattern UNDEF_PATTERN = Pattern.compile("^UNDEF-(this|REFUNIT|REF)-(.*?)-(MINUS|PLUS)-([0-9]+)");

	private static final Pattern UNDEF_UNIT = Pattern.compile("^UNDEF-(last|this|next)-(century|decade|year|quarter|month|week|day)");

	private static final Pattern UNDEF_MONTH = Pattern.compile("^UNDEF-(last|this|next)-(january|february|march|april|may|june|july|august|september|october|november|december)(?:-([0-9][0-9]))?");

	private static final Pattern UNDEF_SEASON = Pattern.compile("^UNDEF-(last|this|next)-(SP|SU|FA|WI)");

	private static final Pattern UNDEF_WEEKDAY = Pattern.compile("^UNDEF-(last|this|next|day)-(monday|tuesday|wednesday|thursday|friday|saturday|sunday)");

	private static final Pattern TWO_DIGITS = Pattern.compile("^\\d\\d$");

	private static final Pattern THREE_DIGITS = Pattern.compile("^\\d\\d\\d$");

	// Document creation time
	public static class ParsedDct {
		String dctValue = "";
		int dctCentury = 0, dctYear = 0, dctDecade = 0, dctMonth = 0, dctDay = 0;
		Season dctSeason = null;
		String dctQuarter = "";
		String dctHalf = "";
		int dctWeekday = 0, dctWeek = 0;

		private ParsedDct(String dctValue) {
			// year, month, day as mentioned in the DCT
			dctYear = parseInt(dctValue, 0, 4);
			dctCentury = dctYear / 100;
			dctDecade = parseInt(dctValue, 2, 3);
			// Could be separated by slashes, or not.
			if (Character.isDigit(dctValue.charAt(4))) {
				dctMonth = parseInt(dctValue, 4, 6);
				dctDay = parseInt(dctValue, 6, 8);
			} else {
				dctMonth = parseInt(dctValue, 5, 7);
				dctDay = parseInt(dctValue, 8, 10);
			}
			dctQuarter = getQuarterOfMonth(dctMonth);
			dctHalf = getHalfYearOfMonth(dctMonth);

			// season, week, weekday, have to be calculated
			dctSeason = getSeasonOfMonth(dctMonth);
			dctWeekday = getWeekdayOfDate(dctYear, dctMonth, dctDay);
			dctWeek = getWeekOfDate(dctYear, dctMonth, dctDay);

			if (LOG.isDebugEnabled()) {
				LOG.debug("dctCentury: {}", dctCentury);
				LOG.debug("dctYear: {}", dctYear);
				LOG.debug("dctDecade: {}", dctDecade);
				LOG.debug("dctMonth: {}", dctMonth);
				LOG.debug("dctDay: {}", dctDay);
				LOG.debug("dctQuarter: {}", dctQuarter);
				LOG.debug("dctSeason: {}", dctSeason);
				LOG.debug("dctWeekday: {}", dctWeekday);
				LOG.debug("dctWeek: {}", dctWeek);
			}
		}

		public static ParsedDct read(JCas jcas) {
			String dctString = getDct(jcas);
			return dctString != null ? new ParsedDct(dctString) : null;
		}

		public static String getDct(JCas jcas) {
			AnnotationIndex<Dct> dcts = jcas.getAnnotationIndex(Dct.type);
			FSIterator<Dct> dctIter = dcts.iterator();
			return dctIter.hasNext() ? dctIter.next().getValue() : null;
		}

		private static final Pattern VALID_DCT = Pattern.compile("^\\d{4}[.-]?\\d{2}[.-]?\\d{2}");

		/**
		 * Check whether or not a jcas object has a correct DCT value. If there is no DCT present, we canonically return true since fallback calculation takes care of that scenario.
		 * 
		 * @param jcas
		 * @return Whether or not the given jcas contains a valid DCT
		 */
		public static boolean isValidDCT(JCas jcas) {
			String dctString = getDct(jcas);
			// Something like 20041224 or 2004-12-24
			return dctString == null || VALID_DCT.matcher(dctString).find();
		}
	}

	NormalizationManager norm;

	Language language;

	private DocumentType documentType;

	public void init(Language language, boolean find_temponyms, DocumentType typeToProcess) {
		if (this.language != language) {
			this.language = language;
			norm = NormalizationManager.getInstance(language, find_temponyms);
		}
		this.documentType = typeToProcess;
	}

	public String specifyAmbiguousValuesString(String ambigString, Timex3 t_i, int i, List<Timex3> linearDates, JCas jcas) {
		if (!ambigString.startsWith("UNDEF"))
			return ambigString;
		// If available, parse document creation time:
		ParsedDct dct = ParsedDct.read(jcas); // was: (documentType != DocumentType.NARRATIVE) ? ParsedDct.read(jcas) : null;

		// get the last tense (depending on the part of speech tags used in front or behind the expression)
		Tense last_used_tense = getLastTense(t_i, jcas, language);

		// DISAMBIGUATION PHASE:
		if (ambigString.equals("UNDEF-REFDATE"))
			return i > 0 ? linearDates.get(i - 1).getTimexValue() : "XXXX-XX-XX";
		// Different patterns:
		String repl = handleUndefYear(ambigString, linearDates, i, dct, last_used_tense);
		repl = repl != null ? repl : handleUndefCentury(ambigString, linearDates, i, dct, last_used_tense);
		repl = repl != null ? repl : handleUndefPlusMinus(ambigString, linearDates, i, dct);
		repl = repl != null ? repl : handleUndefNextPrevThis(ambigString, linearDates, i, dct);
		repl = repl != null ? repl : handleUndefMonth(ambigString, linearDates, i, dct);
		repl = repl != null ? repl : handleUndefSeason(ambigString, linearDates, i, dct);
		repl = repl != null ? repl : handleUndefWeekday(ambigString, linearDates, i, dct, last_used_tense);
		if (repl == null) {
			LOG.warn("Unhandled UNDEF value: {}", ambigString);
			return ambigString;
		}
		return repl;
	}

	private String handleUndefPlusMinus(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct) {
		Matcher m = UNDEF_PATTERN.matcher(ambigString);
		if (!m.find())
			return null;
		boolean fuzz = !ambigString.regionMatches(m.start(1), "REFUNIT", 0, 7);
		String unit = m.group(2);
		boolean positive = ambigString.regionMatches(m.start(3), "PLUS", 0, 4); // May only be PLUS or MINUS.
		try {
			int diff = parseInt(ambigString, m.start(4), m.end(4));
			diff = positive ? diff : -diff; // Signed diff
			String rep = adjustByUnit(linearDates, i, dct, unit, diff, fuzz);
			if (rep == null)
				return ambigString;
			StringBuilder valueNew = join(rep, ambigString, m.end());
			if ("year".equals(unit))
				handleFiscalYear(valueNew);
			return valueNew.toString();
		} catch (NumberFormatException e) {
			LOG.error("Invalid integer {} in {}", m.group(4), ambigString);
			return positive ? "FUTURE_REF" : "PAST_REF";
		}
	}

	private String handleUndefNextPrevThis(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct) {
		Matcher m = UNDEF_UNIT.matcher(ambigString);
		if (!m.find())
			return null;
		String rel = m.group(1), unit = m.group(2);
		int sdiff = 0;
		switch (rel) {
		case "this":
			break;
		case "last":
			sdiff = -1;
			break;
		case "next":
			sdiff = +1;
			break;
		default:
			LOG.warn("Unknown relationship {} in {}", rel, ambigString);
			return null;
		}
		String rep = adjustByUnit(linearDates, i, dct, unit, sdiff, true);
		if (rep == null)
			return ambigString;
		StringBuilder valueNew = join(rep, ambigString, m.end());
		if ("year".equals(unit))
			handleFiscalYear(valueNew);
		return valueNew.toString();
	}

	/**
	 * Adjust a date.
	 *
	 * @param linearDates
	 *                Date mentions
	 * @param i
	 *                Position
	 * @param dct
	 *                Document creation time
	 * @param unit
	 *                Unit
	 * @param sdiff
	 *                Difference
	 * @param fuzz
	 *                Fuzzing factor
	 * @return Adjusted date, or null.
	 */
	private String adjustByUnit(List<Timex3> linearDates, int i, ParsedDct dct, String unit, int sdiff, boolean fuzz) {
		// do the processing for SCIENTIFIC documents (TPZ identification could be improved)
		if (documentType == DocumentType.SCIENTIFIC)
			return formatScientific(unit, sdiff);
		// TODO: BC dates are likely not handled correctly everywhere, although some cases may never occur, because we won't have day information BC.
		switch (unit) {
		case "century":
			if (dct != null)
				return norm.normNumber(dct.dctCentury + sdiff);
			String lmCentury = getLastMentionedCentury(linearDates, i);
			return lmCentury.isEmpty() ? "XX" : getXNextCentury(lmCentury, sdiff);
		case "decade":
			if (dct != null)
				return (Integer.toString(dct.dctYear + sdiff * 10)).substring(0, 3);
			String lmDecade = getLastMentionedDecade(linearDates, i);
			return lmDecade.isEmpty() ? "XXXX" : getXNextDecade(lmDecade, sdiff);
		case "year":
			if (fuzz) { // Use year precision
				if (dct != null)
					return Integer.toString(dct.dctYear + sdiff);
				String lmYear = getLastMentionedYear(linearDates, i);
				return lmYear.isEmpty() ? "XXXX" : getXNextYear(lmYear, sdiff);
			}
			// Use day precision, if possible
			// FIXME: Use dct?
			String dateWithYear = getLastMentionedDateYear(linearDates, i);
			if (dateWithYear.length() == 0)
				return "XXXX";
			// FIXME: clean up BC handling!
			final int p = dateWithYear.startsWith("BC") ? 6 : 4;
			String year = dateWithYear.substring(0, p);
			String rest = dateWithYear.substring(p);
			String yearNew = getXNextYear(year, sdiff);
			return yearNew + rest;
		case "quarter":
			// TODO: assert not BC?
			if (dct != null) {
				// Use quarters, 0 to 3, for computation.
				int quarters = (dct.dctYear << 2) + parseIntAt(dct.dctQuarter, 1) - 1 + sdiff;
				return (quarters >> 2) + "-Q" + ((quarters & 0x3) + 1);
			}
			String lmQuarter = getLastMentionedQuarter(linearDates, i, language);
			if (lmQuarter.isEmpty())
				return "XXXX-XX";
			// Use quarters, 0 to 3, for computation.
			int quarters = (parseInt(lmQuarter, 0, 4) << 2) + parseIntAt(lmQuarter, 6) - 1 + sdiff;
			return (quarters >> 2) + "-Q" + ((quarters & 0x3) + 1);
		case "month":
			// TODO: assert not BC?
			if (dct != null)
				return getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), sdiff);
			String lmMonth = getLastMentionedMonth(linearDates, i);
			return lmMonth.isEmpty() ? "XXXX-XX" : getXNextMonth(lmMonth, sdiff);
		case "week":
			// TODO: assert not BC?
			if (fuzz /* && (sdiff > 1 || sdiff < -1) */) { // Use week precision
				if (dct != null)
					return getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), sdiff);
				String lmWeek = getLastMentionedWeek(linearDates, i);
				return lmWeek.isEmpty() ? "XXXX-WXX" : getXNextWeek(lmWeek, sdiff);
			}
			// Use day precision, if possible
			if (dct != null)
				return getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, sdiff * 7);
			String lmDayW = getLastMentionedDay(linearDates, i);
			return lmDayW.isEmpty() ? "XXXX-WXX" : getXNextDay(lmDayW, sdiff * 7);
		case "day":
			if (dct != null)
				return getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, sdiff);
			String lmDay = getLastMentionedDay(linearDates, i);
			return lmDay.isEmpty() ? "XXXX-XX-XX" : getXNextDay(lmDay, sdiff);
		case "minute":
		case "second":
		case "hour":
			// FIXME: support these, too?
			return null;
		case "week-WE":
			// TODO: assert not BC?
			if (fuzz /* && (sdiff > 1 || sdiff < -1) */) { // Use week precision
				if (dct != null)
					return getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), sdiff);
				String lmWeek = getLastMentionedWeek(linearDates, i);
				return lmWeek.isEmpty() ? "XXXX-WXX-WE" : getXNextWeek(lmWeek, sdiff);
			}
			// Use day precision, if possible
			if (dct != null)
				return getXNextWeek(dct.dctYear, dct.dctMonth, dct.dctDay, sdiff) + "-WE";
			String lmWeek = getLastMentionedWeek(linearDates, i);
			return lmWeek.isEmpty() ? "XXXX-WXX-WE" : getXNextWeek(lmWeek, sdiff) + "-WE";
		default:
			LOG.warn("Unknown unit {}", unit);
			return null;
		}
	}

	private String formatScientific(String unit, int sdiff) {
		final String fmt;
		switch (unit) {
		case "year":
			fmt = "TPZ%c%04d";
			break;
		case "month":
			fmt = "TPZ%c0000-%02d";
			break;
		case "week":
			fmt = "TPZ%c0000-W%02d";
			break;
		case "day":
			fmt = "TPZ%c0000-00-%02d";
			break;
		case "hour":
			fmt = "TPZ%c0000-00-00T%02d";
			break;
		case "minute":
			fmt = "TPZ%c0000-00-00T00:%02d";
			break;
		case "second":
			fmt = "TPZ%c0000-00-00T00:00:%02d";
			break;
		default:
			LOG.error("no scientific format for unit type {}", unit);
			return null;
		}
		return String.format(Locale.ROOT, fmt, sdiff >= 0 ? '+' : '-', Math.abs(sdiff));
	}

	private String handleUndefYear(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct, Tense last_used_tense) {
		if (!ambigString.startsWith("UNDEF-year"))
			return null;
		last_used_tense = last_used_tense != null ? last_used_tense //
				// In COLLOQUIAL, default to present/future, otherwise assume past (if undefined).
				: (documentType == DocumentType.COLLOQUIAL ? Tense.PRESENTFUTURE : Tense.PAST);
		String[] valueParts = ambigString.split("-");
		String repl;
		if (dct != null && valueParts.length > 2) {
			int newYear = dct.dctYear;
			String part2 = valueParts[2];
			Season viThisSeason;
			// get vi month
			if (TWO_DIGITS.matcher(part2).matches()) {
				// FIXME: check range of month and day?
				int viThisMonth = parseInt(part2);
				// Get day in vi
				int viThisDay = (valueParts.length > 3 && TWO_DIGITS.matcher(valueParts[3]).matches()) //
						? parseInt(valueParts[3]) : -1;
				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE) { // || last_used_tense == Tense.PRESENTFUTURE) {
					// if dct-month is larger than vi-month, then add 1 to dct-year
					if (dct.dctMonth > viThisMonth || //
							(dct.dctMonth == viThisMonth && viThisDay > 0 && dct.dctDay > viThisDay))
						++newYear;
				}
				// Tense is PAST
				else if (last_used_tense == Tense.PAST) {
					// if dct-month is smaller than vi month, then subtract 1 from dct-year
					if (dct.dctMonth < viThisMonth || //
							(dct.dctMonth == viThisMonth && viThisDay > 0 && dct.dctDay < viThisDay))
						--newYear;
				}
			}
			// get vi season
			else if ((viThisSeason = Season.of(part2)) != null) {
				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE) { // || last_used_tense == Tense.PRESENTFUTURE) {
					// if dct-month is larger than vi-month, then add 1 to dct-year
					if (dct.dctSeason.ord() > viThisSeason.ord())
						++newYear;
				}
				// Tense is PAST
				else if (last_used_tense == Tense.PAST) {
					// if dct-month is smaller than vi month, then subtract 1 from dct-year
					if (dct.dctSeason.ord() < viThisSeason.ord())
						--newYear;
				}
			}
			// get vi quarter
			else if (part2.charAt(0) == 'Q' && part2.charAt(1) >= '1' && part2.charAt(1) <= '4') {
				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE) { // || last_used_tense == Tense.PRESENTFUTURE) {
					if (parseIntAt(dct.dctQuarter, 1) > parseIntAt(part2, 1))
						++newYear;
				}
				// Tense is PAST
				if (last_used_tense == Tense.PAST) {
					if (parseIntAt(dct.dctQuarter, 1) < parseIntAt(part2, 1))
						--newYear;
				}
			}
			// get vi half
			else if (part2.charAt(0) == 'H' && (part2.equals("H1") || part2.equals("H2"))) {
				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE) { // || last_used_tense == Tense.PRESENTFUTURE) {
					if (parseIntAt(dct.dctHalf, 1) > parseIntAt(part2, 1))
						++newYear;
				}
				// Tense is PAST
				if (last_used_tense == Tense.PAST) {
					if (parseIntAt(dct.dctHalf, 1) < parseIntAt(part2, 1))
						--newYear;
				}
			}
			// get vi Week
			else if (part2.charAt(0) == 'W') {
				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE) { // || last_used_tense == Tense.PRESENTFUTURE) {
					if (dct.dctWeek > parseIntAt(part2, 1))
						++newYear;
				}
				// Tense is PAST
				if (last_used_tense == Tense.PAST) {
					if (dct.dctWeek < parseIntAt(part2, 1))
						--newYear;
				}
			}
			repl = Integer.toString(newYear);
		} else {
			repl = getLastMentionedYear(linearDates, i);
			if (repl.isEmpty())
				repl = "XXXX";
		}
		// REPLACE THE UNDEF-YEAR WITH THE NEWLY CALCULATED YEAR
		return join(repl, ambigString, "UNDEF-year".length()).toString();
	}

	private String handleUndefCentury(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct, Tense last_used_tense) {
		if (!ambigString.startsWith("UNDEF-century"))
			return null;
		String repl = dct != null ? Integer.toString(dct.dctCentury) : "";

		// FIXME: supposed to be NEWS and COLLOQUIAL DOCUMENTS
		if (dct != null) {
			int viThisDecade = parseInt(ambigString, 13, 14);
			// Tense is FUTURE
			if (last_used_tense == Tense.FUTURE || last_used_tense == Tense.PRESENTFUTURE)
				repl = Integer.toString(dct.dctCentury + (viThisDecade < dct.dctDecade ? 1 : 0));
			// Tense is PAST
			else if (last_used_tense == Tense.PAST)
				repl = Integer.toString(dct.dctCentury - (dct.dctDecade < viThisDecade ? 1 : 0));
		}
		// NARRATIVE DOCUMENTS
		else {
			repl = getLastMentionedCentury(linearDates, i);
			if (!repl.startsWith("BC")) {
				if (repl.matches("^\\d\\d.*") && parseInt(repl, 0, 2) < 10)
					repl = "00";
			} else {
				repl = "00";
			}
		}
		// LREC change: assume in narrative-style documents that
		// if no other century was mentioned before, 1st century
		// Otherwise, assume that sixties, twenties, and so on
		// are 19XX if no century found (LREC change)
		if (repl.isEmpty())
			repl = (documentType == DocumentType.NARRATIVE ? "00" : "19");
		StringBuilder valueNew = join(repl, ambigString, "UNDEF-century".length());
		// always assume that sixties, twenties, and so on are 19XX -- if
		// not narrative document (LREC change)
		if (documentType != DocumentType.NARRATIVE && THREE_DIGITS.matcher(valueNew).matches())
			valueNew.replace(0, 2, "19");
		return valueNew.toString();
	}

	private String handleUndefMonth(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct) {
		Matcher m = UNDEF_MONTH.matcher(ambigString);
		if (!m.find())
			return null;
		String ltn = m.group(1), newMonth = norm.getFromNormMonthName(m.group(2)), daystr = m.group(3);
		String repl = "XXXX-XX";
		if (ltn.equals("last")) {
			if (dct != null) {
				int newYear = dct.dctYear;
				int newMonthInt = parseInt(newMonth);
				int day = (daystr != null && daystr.length() > 0) ? parseInt(daystr) : 0;
				// check day if dct-month and newMonth are equal
				if (dct.dctMonth == newMonthInt) {
					if (day != 0 && dct.dctDay <= day)
						--newYear;
				} else if (dct.dctMonth <= newMonthInt)
					--newYear;
				// TODO: 'format' year? could be < 1000.
				repl = newYear + "-" + newMonth;
			} else {
				String lmMonth = getLastMentionedMonthDetails(linearDates, i);
				if (!lmMonth.isEmpty()) {
					int lmMonthInt = parseInt(lmMonth, 5, 7);
					int lmDayInt = 0;
					if (lmMonth.length() > 9 && TWO_DIGITS.matcher(lmMonth.subSequence(8, 10)).matches())
						lmDayInt = parseInt(lmMonth, 8, 10);
					int newYear = parseInt(lmMonth, 0, 4);
					int newMonthInt = parseInt(newMonth);
					int day = (daystr != null && daystr.length() > 0) ? parseInt(daystr) : 0;
					if (lmMonthInt == newMonthInt) {
						if (lmDayInt != 0 && day != 0 && lmDayInt <= day)
							--newYear;
					} else if (lmMonthInt <= newMonthInt)
						--newYear;
					// TODO: 'format' year? could be < 1000.
					repl = newYear + "-" + newMonth;
				}
			}
		} else if (ltn.equals("this")) {
			if (dct != null) {
				// TODO: 'format' year? could be < 1000.
				repl = dct.dctYear + "-" + newMonth;
			} else {
				String lmMonth = getLastMentionedMonthDetails(linearDates, i);
				if (!lmMonth.isEmpty())
					repl = lmMonth.substring(0, 4) + "-" + newMonth;
			}
		} else if (ltn.equals("next")) {
			if (dct != null) {
				int newYear = dct.dctYear;
				int newMonthInt = parseInt(newMonth);
				int day = (daystr != null && daystr.length() > 0) ? parseInt(daystr) : 0;
				// check day if dct-month and newMonth are equal
				if (dct.dctMonth == newMonthInt) {
					if (day != 0 && dct.dctDay >= day)
						++newYear;
				} else if (dct.dctMonth >= newMonthInt)
					++newYear;
				// TODO: 'format' year? could be < 1000.
				repl = newYear + "-" + newMonth;
			} else {
				String lmMonth = getLastMentionedMonthDetails(linearDates, i);
				if (!lmMonth.isEmpty()) {
					int newYear = parseInt(lmMonth, 0, 4), lmMonthInt = parseInt(lmMonth, 5, 7);
					int newMonthInt = parseInt(newMonth);
					if (lmMonthInt >= newMonthInt)
						++newYear;
					// TODO: 'format' year? could be < 1000.
					repl = newYear + "-" + newMonth;
				}
			}
		} else {
			LOG.warn("Unhandled undef-month: {}", ltn);
		}
		return join(repl, ambigString, m.end()).toString();
	}

	private String handleUndefSeason(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct) {
		Matcher m = UNDEF_SEASON.matcher(ambigString);
		if (!m.find())
			return null;
		String ltn = m.group(1);
		Season newSeason = Season.of(ambigString, m.start(2));
		String repl = "XXXX-XX";
		if (ltn.equals("last")) {
			if (dct != null) {
				int newYear = dct.dctYear - (newSeason.ord() < dct.dctSeason.ord() //
						|| (dct.dctSeason == Season.WINTER && dct.dctMonth < 12) //
								? 1 : 0);
				// TODO: 'format' year? could be < 1000.
				repl = newYear + "-" + newSeason;
			} else { // NARRATVIE DOCUMENT
				String lmSeason = getLastMentionedSeason(linearDates, i, language);
				if (lmSeason != null && !lmSeason.isEmpty()) {
					Season se = Season.of(lmSeason, 5);
					int newYear = parseInt(lmSeason, 0, 4) - (newSeason.ord() < se.ord() ? 1 : 0);
					// TODO: 'format' year? could be < 1000.
					repl = newYear + "-" + newSeason;
				}
			}
		} else if (ltn.equals("this")) {
			// TODO use tense of sentence?
			if (dct != null) {
				// TODO: 'format' year? could be < 1000.
				repl = dct.dctYear + "-" + newSeason;
			} else {
				String lmSeason = getLastMentionedSeason(linearDates, i, language);
				if (lmSeason != null && !lmSeason.isEmpty())
					repl = lmSeason.substring(0, 4) + "-" + newSeason;
			}
		} else if (ltn.equals("next")) {
			if (dct != null) {
				int newYear = dct.dctYear + (newSeason.ord() <= dct.dctSeason.ord() ? 1 : 0);
				// TODO: 'format' year? could be < 1000.
				repl = newYear + "-" + newSeason;
			} else { // NARRATIVE DOCUMENT
				String lmSeason = getLastMentionedSeason(linearDates, i, language);
				if (lmSeason != null && !lmSeason.isEmpty()) {
					Season se = Season.of(lmSeason, 5);
					int newYear = parseInt(lmSeason, 0, 4) + (newSeason.ord() <= se.ord() ? 1 : 0);
					// TODO: 'format' year? could be < 1000.
					repl = newYear + "-" + newSeason;
				}
			}
		} else {
			LOG.warn("Unhandled undef-season: {}", ltn);
		}
		return join(repl, ambigString, m.end()).toString();
	}

	private String handleUndefWeekday(String ambigString, List<Timex3> linearDates, int i, ParsedDct dct, Tense last_used_tense) {
		Matcher m = UNDEF_WEEKDAY.matcher(ambigString);
		if (!m.find())
			return null;
		// TODO (before refactoring:) the calculation is strange, but works
		// But we improved this during refactoring, is it less strange now?
		// TODO tense should be included?!
		String ltnd = m.group(1), newWeekday = m.group(2);
		int newWeekdayInt = parseInt(norm.getFromNormDayInWeek(newWeekday));
		String repl = "XXXX-XX-XX";
		if (ltnd.equals("last")) {
			if (dct != null) {
				int diff = -(dct.dctWeekday - newWeekdayInt);
				diff = (diff >= 0) ? diff - 7 : diff;
				repl = getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, diff);
			} else {
				String lmDay = getLastMentionedDay(linearDates, i);
				if (!lmDay.isEmpty()) {
					int lmWeekdayInt = getWeekdayOfDate(lmDay);
					int diff = -(lmWeekdayInt - newWeekdayInt);
					diff = (diff >= 0) ? diff - 7 : diff;
					repl = getXNextDay(lmDay, diff);
				}
			}
		} else if (ltnd.equals("this")) {
			if (dct != null) {
				// TODO tense should be included?!
				int diff = -(dct.dctWeekday - newWeekdayInt);
				diff = (diff > 0) ? diff - 7 : diff;
				repl = getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, diff);
			} else {
				// TODO tense should be included?!
				String lmDay = getLastMentionedDay(linearDates, i);
				if (!lmDay.isEmpty()) {
					int lmWeekdayInt = getWeekdayOfDate(lmDay);
					int diff = -(lmWeekdayInt - newWeekdayInt);
					diff = (diff > 0) ? diff - 7 : diff;
					repl = getXNextDay(lmDay, diff);
				}
			}
		} else if (ltnd.equals("next")) {
			if (dct != null) {
				int diff = newWeekdayInt - dct.dctWeekday;
				diff = (diff <= 0) ? diff + 7 : diff;
				repl = getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, diff);
			} else {
				String lmDay = getLastMentionedDay(linearDates, i);
				if (!lmDay.isEmpty()) {
					int lmWeekdayInt = getWeekdayOfDate(lmDay);
					int diff = newWeekdayInt - lmWeekdayInt;
					diff = (diff <= 0) ? diff + 7 : diff;
					repl = getXNextDay(lmDay, diff);
				}
			}
		} else if (ltnd.equals("day")) {
			if (dct != null) {
				// TODO tense should be included?!
				int diff = -(dct.dctWeekday - newWeekdayInt);
				diff = (diff > 0) ? diff - 7 : diff;
				// Tense is FUTURE
				if ((last_used_tense == Tense.FUTURE) && diff != 0)
					diff += 7;
				// Tense is PAST
				// if ((last_used_tense == Tense.PAST)) ?
				repl = getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, diff);
			} else {
				// TODO tense should be included?!
				String lmDay = getLastMentionedDay(linearDates, i);
				if (!lmDay.isEmpty()) {
					int lmWeekdayInt = getWeekdayOfDate(lmDay);
					int diff = -(lmWeekdayInt - newWeekdayInt);
					diff = (diff > 0) ? diff - 7 : diff;
					repl = getXNextDay(lmDay, diff);
				}
			}
		} else {
			LOG.warn("Unhandled undef-weekday: {}", ltnd);
		}
		return join(repl, ambigString, m.end()).toString();
	}

	/**
	 * Join pre-string + post-string beginning at offsetPost, effectively replacing the first offsetPost characters with the pre string.
	 * 
	 * @param pre
	 *                Prefix
	 * @param post
	 *                Postfix
	 * @param offsetPost
	 *                Number of chars in postfix to skip.
	 * @return String builder, for futher modification
	 */
	private static StringBuilder join(String pre, String post, final int offsetPost) {
		StringBuilder valueNew = new StringBuilder(pre.length() + post.length() - offsetPost);
		valueNew.append(pre);
		valueNew.append(post, offsetPost, post.length());
		return valueNew;
	}

	/**
	 * Under-specified values are disambiguated here. Only Timexes of types "date" and "time" can be under-specified.
	 * 
	 * @param jcas
	 */
	public void specifyAmbiguousValues(JCas jcas) {
		// build up a list with all found TIMEX expressions
		List<Timex3> linearDates = new ArrayList<Timex3>();
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);

		// Create List of all Timexes of types "date" and "time"
		for (Timex3 timex : timexes) {
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME"))
				linearDates.add(timex);

			if (timex.getTimexType().equals("DURATION") && timex.getEmptyValue().length() > 0)
				linearDates.add(timex);
		}

		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		for (int i = 0; i < linearDates.size(); i++) {
			Timex3 t_i = linearDates.get(i);
			String value_i = t_i.getTimexValue();

			String valueNew = value_i;
			// handle the value attribute only if we have a TIME or DATE
			if (t_i.getTimexType().equals("TIME") || t_i.getTimexType().equals("DATE"))
				valueNew = specifyAmbiguousValuesString(value_i, t_i, i, linearDates, jcas);

			// handle the emptyValue attribute for any type
			if (t_i.getEmptyValue() != null && t_i.getEmptyValue().length() > 0)
				t_i.setEmptyValue(specifyAmbiguousValuesString(t_i.getEmptyValue(), t_i, i, linearDates, jcas));

			t_i.removeFromIndexes();
			if (LOG.isDebugEnabled() && !valueNew.equals(t_i.getTimexValue()))
				LOG.debug("{} {} DISAMBIGUATION: foundBy: {} text: {} value: {} NEW value: {} ", //
						t_i.getSentId(), t_i.getTimexId(), t_i.getFoundByRule(), t_i.getCoveredText(), t_i.getTimexValue(), valueNew);

			t_i.setTimexValue(valueNew);
			t_i.addToIndexes();
			linearDates.set(i, t_i);
		}
	}

	/**
	 * Convert a -FY postfix to a FY prefix.
	 * 
	 * @param buf
	 *                Buffer to operate on
	 */
	private static void handleFiscalYear(StringBuilder buf) {
		if (buf.length() < 4)
			return;
		// Unfortunately, StringBuilder does not have and "endsWith".
		int p = buf.length() - 3;
		if (buf.charAt(p) == '-' && buf.charAt(++p) == 'F' && buf.charAt(++p) == 'Y') {
			// Keep at most the year:
			buf.setLength(Math.min(p, 4));
			buf.insert(0, "FY");
		}
	}
}