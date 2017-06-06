package de.unihd.dbs.uima.annotator.heideltime;

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
import de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer;
import de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer.Tense;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Season;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Timex3;

class ResolveAmbiguousValues {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ResolveAmbiguousValues.class);

	private static final Pattern UNDEF_PATTERN = Pattern.compile("^(UNDEF-(this|REFUNIT|REF)-(.*?)-(MINUS|PLUS)-([0-9]+)).*");

	private static final Pattern UNDEF_MONTH = Pattern
			.compile("(UNDEF-(last|this|next)-(january|february|march|april|may|june|july|august|september|october|november|december)(-([0-9][0-9]))?).*");

	private static final Pattern UNDEF_SEASON = Pattern.compile("^(UNDEF-(last|this|next)-(SP|SU|FA|WI)).*");

	private static final Pattern UNDEF_WEEKDAY = Pattern.compile("^(UNDEF-(last|this|next|day)-(monday|tuesday|wednesday|thursday|friday|saturday|sunday)).*");

	private static final Pattern EIGHT_DIGITS = Pattern.compile("^\\d\\d\\d\\d\\d\\d\\d\\d$");

	private static final Pattern TWO_DIGITS = Pattern.compile("^\\d\\d$");

	// Document creation time
	public static class ParsedDct {
		String dctValue = "";
		int dctCentury = 0;
		int dctYear = 0;
		int dctDecade = 0;
		int dctMonth = 0;
		int dctDay = 0;
		Season dctSeason = null;
		String dctQuarter = "";
		String dctHalf = "";
		int dctWeekday = 0;
		int dctWeek = 0;

		public boolean read(JCas jcas) {
			AnnotationIndex<Dct> dcts = jcas.getAnnotationIndex(Dct.type);
			FSIterator<Dct> dctIter = dcts.iterator();
			if (!dctIter.hasNext()) {
				LOG.debug("No DCT available...");
				return false;
			}
			dctValue = dctIter.next().getValue();
			// year, month, day as mentioned in the DCT
			if (EIGHT_DIGITS.matcher(dctValue).matches()) {
				dctCentury = Integer.parseInt(dctValue.substring(0, 2));
				dctYear = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth = Integer.parseInt(dctValue.substring(4, 6));
				dctDay = Integer.parseInt(dctValue.substring(6, 8));
			} else {
				dctCentury = Integer.parseInt(dctValue.substring(0, 2));
				dctYear = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth = Integer.parseInt(dctValue.substring(5, 7));
				dctDay = Integer.parseInt(dctValue.substring(8, 10));
			}
			dctQuarter = DateCalculator.getQuarterOfMonth(dctMonth);
			dctHalf = DateCalculator.getHalfYearOfMonth(dctMonth);

			// season, week, weekday, have to be calculated
			dctSeason = DateCalculator.getSeasonOfMonth(dctMonth);
			dctWeekday = DateCalculator.getWeekdayOfDate(dctYear, dctMonth, dctDay);
			dctWeek = DateCalculator.getWeekOfDate(dctYear, dctMonth, dctDay);

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
			return true;
		}
	}

	NormalizationManager norm;

	Language language;

	private DocumentType documentType;

	private static final Pattern VALID_DCT = Pattern.compile("\\d{4}.\\d{2}.\\d{2}|\\d{8}");

	public void init(Language language, boolean find_temponyms, DocumentType typeToProcess) {
		if (this.language != language) {
			this.language = language;
			norm = NormalizationManager.getInstance(language, find_temponyms);
		}
		this.documentType = typeToProcess;
	}

	public String specifyAmbiguousValuesString(String ambigString, Timex3 t_i, int i, List<Timex3> linearDates, JCas jcas) {
		// ////////////////////////////////////////////
		// INFORMATION ABOUT DOCUMENT CREATION TIME //
		// ////////////////////////////////////////////
		ResolveAmbiguousValues.ParsedDct dct = new ParsedDct();
		boolean dctAvailable = dct.read(jcas);

		// check if value_i has month, day, season, week (otherwise no UNDEF-year
		// is possible)
		int viThisMonth = -1, viThisDay = -1, viThisWeek = -1;
		Season viThisSeason = null;
		String viThisQuarter = null, viThisHalf = null;
		String[] valueParts = ambigString.split("-");
		// check if UNDEF-year or UNDEF-century
		if (ambigString.startsWith("UNDEF-year") || ambigString.startsWith("UNDEF-century")) {
			if (valueParts.length > 2) {
				final String part2 = valueParts[2];
				// get vi month
				if (TWO_DIGITS.matcher(part2).matches()) {
					// FIXME: check range?
					viThisMonth = Integer.parseInt(part2);
				}
				// get vi season
				else if ((viThisSeason = Season.of(part2)) != null) {
					// Season is assigned in if statement.
				}
				// get vi quarter
				else if (part2.charAt(0) == 'Q' && (part2.equals("Q1") || part2.equals("Q2") || part2.equals("Q3") || part2.equals("Q4"))) {
					viThisQuarter = part2;
				}
				// get vi half
				else if (part2.charAt(0) == 'Q' && (part2.equals("H1") || part2.equals("H2"))) {
					viThisHalf = part2;
				}
				// get vi Week
				else if (part2.charAt(0) == 'W') {
					viThisWeek = Integer.parseInt(part2.substring(1));
				}
				// get vi day
				if (valueParts.length > 3 && TWO_DIGITS.matcher(valueParts[3]).matches()) {
					// FIXME: check range?
					viThisDay = Integer.parseInt(valueParts[3]);
				}
			}
		} else {
			if (valueParts.length > 1) {
				final String part1 = valueParts[1];
				// get vi month
				if (TWO_DIGITS.matcher(part1).matches()) {
					// FIXME: check range?
					viThisMonth = Integer.parseInt(part1);
				}
				// get vi season
				else if ((viThisSeason = Season.of(part1)) != null) {
					// Season is assigned in if statement.
				}
				// get v1 quarter
				else if (part1.charAt(0) == 'Q' && (part1.equals("Q1") || part1.equals("Q2") || part1.equals("Q3") || part1.equals("Q4"))) {
					viThisQuarter = part1;
				}
				// get v1 half
				else if (part1.charAt(0) == 'H' && (part1.equals("H1") || part1.equals("H2"))) {
					viThisHalf = part1;
				}
				// get vi Week
				else if (part1.charAt(0) == 'W') {
					viThisWeek = Integer.parseInt(part1.substring(1));
				}
				// get vi day
				if (valueParts.length > 2 && TWO_DIGITS.matcher(valueParts[2]).matches()) {
					// FIXME: check range?
					viThisDay = Integer.parseInt(valueParts[2]);
				}
			}
		}
		// get the last tense (depending on the part of speech tags used in front
		// or behind the expression)
		Tense last_used_tense = ContextAnalyzer.getLastTense(t_i, jcas, language);

		//////////////////////////
		// DISAMBIGUATION PHASE //
		//////////////////////////
		final boolean useDct = dctAvailable && (documentType != DocumentType.NARRATIVE);
		////////////////////////////////////////////////////
		// IF YEAR IS COMPLETELY UNSPECIFIED (UNDEF-year) //
		////////////////////////////////////////////////////
		String valueNew = ambigString;
		if (ambigString.startsWith("UNDEF-year")) {
			String newYearValue = useDct ? Integer.toString(dct.dctYear) : "";
			// vi has month (ignore day)
			if (viThisMonth > 0 && viThisSeason == null) {
				// WITH DOCUMENT CREATION TIME
				if (useDct) {
					// Tense is FUTURE
					if (last_used_tense == Tense.FUTURE || last_used_tense == Tense.PRESENTFUTURE) {
						// if dct-month is larger than vi-month, then add 1 to dct-year
						if (dct.dctMonth > viThisMonth)
							newYearValue = Integer.toString(dct.dctYear + 1);
					}
					// Tense is PAST
					if (last_used_tense == Tense.PAST) {
						// if dct-month is smaller than vi month, then subtract 1 from dct-year
						if (dct.dctMonth < viThisMonth)
							newYearValue = Integer.toString(dct.dctYear - 1);
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}
			// vi has quaurter
			if (viThisQuarter != null) {
				// WITH DOCUMENT CREATION TIME
				if (useDct) {
					// Tense is FUTURE
					if (last_used_tense == Tense.FUTURE || last_used_tense == Tense.PRESENTFUTURE) {
						if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1)))
							newYearValue = Integer.toString(dct.dctYear + 1);
					}
					// Tense is PAST
					if (last_used_tense == Tense.PAST) {
						if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1)))
							newYearValue = Integer.toString(dct.dctYear - 1);
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense == null) {
						if (documentType == DocumentType.COLLOQUIAL) {
							// IN COLLOQUIAL: future temporal expressions
							if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1)))
								newYearValue = Integer.toString(dct.dctYear + 1);
						} else {
							// IN NEWS: past temporal expressions
							if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1)))
								newYearValue = Integer.toString(dct.dctYear - 1);
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}
			// vi has half
			if (viThisHalf != null) {
				// WITH DOCUMENT CREATION TIME
				if (useDct) {
					// Tense is FUTURE
					if (last_used_tense == Tense.FUTURE || last_used_tense == Tense.PRESENTFUTURE) {
						if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1)))
							newYearValue = Integer.toString(dct.dctYear + 1);
					}
					// Tense is PAST
					if (last_used_tense == Tense.PAST) {
						if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1)))
							newYearValue = Integer.toString(dct.dctYear - 1);
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense == null) {
						if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1)))
							// IN COLLOQUIAL: future temporal expressions
							// IN NEWS: past temporal expressions
							newYearValue = Integer.toString(dct.dctYear + (documentType == DocumentType.COLLOQUIAL ? 1 : -1));
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}

			// TODO: the logic of this part is messy.
			// vi has season
			if (viThisMonth <= 0 && viThisDay <= 0 && viThisSeason == null)
				// TODO check tenses?
				newYearValue = useDct ? Integer.toString(dct.dctYear) : ContextAnalyzer.getLastMentionedYear(linearDates, i);
			// vi has week
			if (viThisWeek > -1)
				newYearValue = useDct ? Integer.toString(dct.dctYear) : ContextAnalyzer.getLastMentionedYear(linearDates, i);

			// REPLACE THE UNDEF-YEAR WITH THE NEWLY CALCULATED YEAR AND ADD
			// TIMEX TO INDEXES
			valueNew = ambigString.replaceFirst("UNDEF-year", newYearValue.isEmpty() ? "XXXX" : newYearValue);
		}

		///////////////////////////////////////////////////
		// just century is unspecified (UNDEF-century86) //
		///////////////////////////////////////////////////
		else if (ambigString.startsWith("UNDEF-century")) {
			String newCenturyValue = useDct ? Integer.toString(dct.dctCentury) : "";

			// NEWS and COLLOQUIAL DOCUMENTS
			if (useDct && !ambigString.equals("UNDEF-century")) {
				int viThisDecade = Integer.parseInt(ambigString.substring(13, 14));

				if (LOG.isDebugEnabled())
					LOG.debug("dctCentury {}", dct.dctCentury);

				newCenturyValue = Integer.toString(dct.dctCentury);

				// Tense is FUTURE
				if (last_used_tense == Tense.FUTURE || last_used_tense == Tense.PRESENTFUTURE) {
					if (viThisDecade < dct.dctDecade) {
						newCenturyValue = Integer.toString(dct.dctCentury + 1);
					} else {
						newCenturyValue = Integer.toString(dct.dctCentury);
					}
				}
				// Tense is PAST
				if (last_used_tense == Tense.PAST) {
					if (dct.dctDecade < viThisDecade) {
						newCenturyValue = Integer.toString(dct.dctCentury - 1);
					} else {
						newCenturyValue = Integer.toString(dct.dctCentury);
					}
				}
			}
			// NARRATIVE DOCUMENTS
			else {
				newCenturyValue = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
				if (!newCenturyValue.startsWith("BC")) {
					if (newCenturyValue.matches("^\\d\\d.*") && Integer.parseInt(newCenturyValue.substring(0, 2)) < 10) {
						newCenturyValue = "00";
					}
				} else {
					newCenturyValue = "00";
				}
			}
			valueNew = ambigString.replaceFirst("UNDEF-century", //
					!newCenturyValue.isEmpty() ? newCenturyValue : //
					// LREC change: assume in narrative-style documents that
					// if no other century was mentioned before, 1st century
					// Otherwise, assume that sixties, twenties, and so on
					// are 19XX if no century found (LREC change)
							documentType == DocumentType.NARRATIVE ? "00" : "19");
			// always assume that sixties, twenties, and so on are 19XX -- if
			// not narrative document (LREC change)
			if (valueNew.matches("\\d\\d\\d") && documentType != DocumentType.NARRATIVE)
				valueNew = "19" + valueNew.substring(2);
		}

		////////////////////////////////////////////////////
		// CHECK IMPLICIT EXPRESSIONS STARTING WITH UNDEF //
		////////////////////////////////////////////////////
		else if (ambigString.startsWith("UNDEF")) {
			Matcher m;
			valueNew = ambigString;
			if (ambigString.equals("UNDEF-REFDATE")) {
				valueNew = i > 0 ? linearDates.get(i - 1).getTimexValue() : "XXXX-XX-XX";

				//////////////////
				// TO CALCULATE //
				//////////////////
				// year to calculate
			} else if ((m = UNDEF_PATTERN.matcher(ambigString)).find()) {
				String checkUndef = m.group(1);
				String ltn = m.group(2);
				String unit = m.group(3);
				boolean positive = ambigString.regionMatches(m.start(4), "PLUS", 0, 4); // May only be PLUS or MINUS.
				int diff = 0;
				try {
					diff = Integer.parseInt(m.group(5));
				} catch (Exception e) {
					LOG.error("Expression difficult to normalize: {}", ambigString);
					LOG.error("{} probably too long for parsing as integer.", m.group(5));
					valueNew = positive ? "FUTURE_REF" : "PAST_REF";
					LOG.error("Set normalized value as PAST_REF / FUTURE_REF: {}", valueNew);
					return valueNew;
				}
				int sdiff = positive ? diff : -diff; // Signed diff

				// do the processing for SCIENTIFIC documents (TPZ
				// identification could be improved)
				if (documentType == DocumentType.SCIENTIFIC) {
					char opSymbol = positive ? '+' : '-';
					if (unit.equals("year")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c%04d", opSymbol, diff);
					} else if (unit.equals("month")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-%02d", opSymbol, diff);
					} else if (unit.equals("week")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-W%02d", opSymbol, diff);
					} else if (unit.equals("day")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-00-%02d", opSymbol, diff);
					} else if (unit.equals("hour")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-00-00T%02d", opSymbol, diff);
					} else if (unit.equals("minute")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-00-00T00:%02d", opSymbol, diff);
					} else if (unit.equals("second")) {
						valueNew = String.format(Locale.ROOT, "TPZ%c0000-00-00T00:00:%02d", opSymbol, diff);
					}
				} else {
					// check for REFUNIT (only allowed for "year")
					if (ltn.equals("REFUNIT") && unit.equals("year")) {
						String dateWithYear = ContextAnalyzer.getLastMentionedDateYear(linearDates, i);
						if (dateWithYear.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							String year = dateWithYear.substring(0, dateWithYear.startsWith("BC") ? 6 : 4);
							String yearNew = DateCalculator.getXNextYear(year, sdiff);
							// FIXME: Why could we have "rest" (= tail)?
							String rest = dateWithYear.substring(dateWithYear.startsWith("BC") ? 6 : 4);
							valueNew = valueNew.replace(checkUndef, yearNew + rest);
						}
					}

					// REF and this are handled here
					if (unit.equals("century")) {
						if (useDct && ltn.equals("this")) {
							valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctCentury + sdiff));
						} else {
							String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmCentury.isEmpty() ? "XX" : DateCalculator.getXNextCentury(lmCentury, sdiff));
						}
					} else if (unit.equals("decade")) {
						if (useDct && ltn.equals("this")) {
							int decade = dct.dctCentury * 10 + dct.dctDecade + sdiff;
							valueNew = valueNew.replace(checkUndef, decade + "X");
						} else {
							String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmDecade.isEmpty() ? "XXX" : DateCalculator.getXNextDecade(lmDecade, sdiff));
						}
					} else if (unit.equals("year")) {
						if (useDct && ltn.equals("this")) {
							valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear + sdiff));
						} else {
							String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmYear.isEmpty() ? "XXXX" : DateCalculator.getXNextYear(lmYear, sdiff));
						}
					} else if (unit.equals("quarter")) {
						// TODO BC years
						if (useDct && ltn.equals("this")) {
							// Use quarters, 0 to 3, for computation.
							int quarters = (dct.dctYear << 2) + Integer.parseInt(dct.dctQuarter.substring(1)) - 1 + diff;
							valueNew = valueNew.replace(checkUndef, (quarters >> 2) + "-Q" + ((quarters & 0x3) + 1));
						} else {
							String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
							if (lmQuarter.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX-XX");
							} else {
								// Use quarters, 0 to 3, for computation.
								int quarters = (Integer.parseInt(lmQuarter.substring(0, 4)) << 2) + Integer.parseInt(lmQuarter.substring(6)) - 1 + diff;
								valueNew = valueNew.replace(checkUndef, (quarters >> 2) + "-Q" + ((quarters & 0x3) + 1));
							}
						}
					} else if (unit.equals("month")) {
						if (useDct && ltn.equals("this")) {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), sdiff));
						} else {
							String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmMonth.isEmpty() ? "XXXX-XX" : DateCalculator.getXNextMonth(lmMonth, sdiff));
						}
					} else if (unit.equals("week")) {
						if (useDct && ltn.equals("this")) {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), sdiff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmDay.isEmpty() ? "XXXX-XX-XX" : DateCalculator.getXNextDay(lmDay, sdiff * 7));
						}
					} else if (unit.equals("day")) {
						if (useDct && ltn.equals("this")) {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, sdiff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
							valueNew = valueNew.replace(checkUndef, lmDay.isEmpty() ? "XXXX-XX-XX" : DateCalculator.getXNextDay(lmDay, sdiff));
						}
					}
				}
			}

			// century
			else if (ambigString.startsWith("UNDEF-last-century")) {
				String checkUndef = "UNDEF-last-century";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury - 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmCentury.isEmpty() ? "XX" : DateCalculator.getXNextCentury(lmCentury, -1));
				}
			} else if (ambigString.startsWith("UNDEF-this-century")) {
				String checkUndef = "UNDEF-this-century";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmCentury.isEmpty() ? "XX" : lmCentury);
				}
			} else if (ambigString.startsWith("UNDEF-next-century")) {
				String checkUndef = "UNDEF-next-century";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury + 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmCentury.isEmpty() ? "XX" : DateCalculator.getXNextCentury(lmCentury, +1));
				}
			}

			// decade
			else if (ambigString.startsWith("UNDEF-last-decade")) {
				String checkUndef = "UNDEF-last-decade";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear - 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmDecade.isEmpty() ? "XXXX" : DateCalculator.getXNextDecade(lmDecade, -1));
				}
			} else if (ambigString.startsWith("UNDEF-this-decade")) {
				String checkUndef = "UNDEF-this-decade";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmDecade.isEmpty() ? "XXXX" : lmDecade);
				}
			} else if (ambigString.startsWith("UNDEF-next-decade")) {
				String checkUndef = "UNDEF-next-decade";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear + 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmDecade.isEmpty() ? "XXXX" : DateCalculator.getXNextDecade(lmDecade, 1));
				}
			}

			// year
			else if (ambigString.startsWith("UNDEF-last-year")) {
				String checkUndef = "UNDEF-last-year";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear - 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmYear.isEmpty() ? "XXXX" : DateCalculator.getXNextYear(lmYear, -1));
				}
				if (valueNew.endsWith("-FY"))
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
			} else if (ambigString.startsWith("UNDEF-this-year")) {
				String checkUndef = "UNDEF-this-year";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmYear.isEmpty() ? "XXXX" : lmYear);
				}
				if (valueNew.endsWith("-FY"))
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
			} else if (ambigString.startsWith("UNDEF-next-year")) {
				String checkUndef = "UNDEF-next-year";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear + 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmYear.isEmpty() ? "XXXX" : DateCalculator.getXNextYear(lmYear, 1));
				}
				if (valueNew.endsWith("-FY"))
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
			}

			// month
			else if (ambigString.startsWith("UNDEF-last-month")) {
				String checkUndef = "UNDEF-last-month";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), -1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmMonth.isEmpty() ? "XXXX-XX" : DateCalculator.getXNextMonth(lmMonth, -1));
				}
			} else if (ambigString.startsWith("UNDEF-this-month")) {
				String checkUndef = "UNDEF-this-month";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + norm.normNumber(dct.dctMonth));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmMonth.isEmpty() ? "XXXX-XX" : lmMonth);
				}
			} else if (ambigString.startsWith("UNDEF-next-month")) {
				String checkUndef = "UNDEF-next-month";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), 1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmMonth.isEmpty() ? "XXXX-XX" : DateCalculator.getXNextMonth(lmMonth, 1));
				}
			}

			// day
			else if (ambigString.startsWith("UNDEF-last-day")) {
				String checkUndef = "UNDEF-last-day";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, -1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmDay.isEmpty() ? "XXXX-XX-XX" : DateCalculator.getXNextDay(lmDay, -1));
				}
			} else if (ambigString.startsWith("UNDEF-this-day")) {
				String checkUndef = "UNDEF-this-day";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + norm.normNumber(dct.dctMonth) + "-" + norm.normNumber(dct.dctDay));
				} else {
					if (ambigString.equals("UNDEF-this-day")) {
						valueNew = "PRESENT_REF";
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						valueNew = valueNew.replace(checkUndef, lmDay.isEmpty() ? "XXXX-XX-XX" : lmDay);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-day")) {
				String checkUndef = "UNDEF-next-day";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, 1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmDay.isEmpty() ? "XXXX-XX-XX" : DateCalculator.getXNextDay(lmDay, 1));
				}
			}

			// week
			else if (ambigString.startsWith("UNDEF-last-week")) {
				String checkUndef = "UNDEF-last-week";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), -1));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmWeek.isEmpty() ? "XXXX-WXX" : DateCalculator.getXNextWeek(lmWeek, -1));
				}
			} else if (ambigString.startsWith("UNDEF-this-week")) {
				String checkUndef = "UNDEF-this-week";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-W" + norm.normNumber(dct.dctWeek));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmWeek.isEmpty() ? "XXXX-WXX" : lmWeek);
				}
			} else if (ambigString.startsWith("UNDEF-next-week")) {
				String checkUndef = "UNDEF-next-week";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), 1));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					valueNew = valueNew.replace(checkUndef, lmWeek.isEmpty() ? "XXXX-WXX" : DateCalculator.getXNextWeek(lmWeek, 1));
				}
			}

			// quarter
			else if (ambigString.startsWith("UNDEF-last-quarter")) {
				String checkUndef = "UNDEF-last-quarter";
				if (useDct) {
					if (dct.dctQuarter.equals("Q1")) {
						valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-Q4");
					} else {
						int newQuarter = Integer.parseInt(dct.dctQuarter.substring(1, 2)) - 1;
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-Q" + newQuarter);
					}
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
					if (lmQuarter.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-QX");
					} else {
						int lmQuarterOnly = Integer.parseInt(lmQuarter.substring(6, 7));
						int lmYearOnly = Integer.parseInt(lmQuarter.substring(0, 4));
						if (lmQuarterOnly == 1) {
							valueNew = valueNew.replace(checkUndef, lmYearOnly - 1 + "-Q4");
						} else {
							int newQuarter = lmQuarterOnly - 1;
							valueNew = valueNew.replace(checkUndef, lmYearOnly + "-Q" + newQuarter);
						}
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-quarter")) {
				String checkUndef = "UNDEF-this-quarter";
				if (useDct) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + dct.dctQuarter);
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
					valueNew = valueNew.replace(checkUndef, lmQuarter.isEmpty() ? "XXXX-QX" : lmQuarter);
				}
			} else if (ambigString.startsWith("UNDEF-next-quarter")) {
				String checkUndef = "UNDEF-next-quarter";
				if (useDct) {
					if (dct.dctQuarter.equals("Q4")) {
						valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-Q1");
					} else {
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-Q" + (Integer.parseInt(dct.dctQuarter.substring(1, 2)) + 1));
					}
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
					if (lmQuarter.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-QX");
					} else {
						int lmQuarterOnly = Integer.parseInt(lmQuarter.substring(6, 7));
						int lmYearOnly = Integer.parseInt(lmQuarter.substring(0, 4));
						if (lmQuarterOnly == 4) {
							valueNew = valueNew.replace(checkUndef, lmYearOnly + 1 + "-Q1");
						} else {
							valueNew = valueNew.replace(checkUndef, lmYearOnly + "-Q" + (lmQuarterOnly + 1));
						}
					}
				}
			}

			// MONTH NAMES
			else if ((m = UNDEF_MONTH.matcher(ambigString)).matches()) {
				String checkUndef = m.group(1);
				String ltn = m.group(2);
				String newMonth = norm.getFromNormMonthName((m.group(3)));
				int newMonthInt = Integer.parseInt(newMonth);
				String daystr = m.group(5);
				int day = (daystr != null && daystr.length() > 0) ? Integer.parseInt(daystr) : 0;
				if (ltn.equals("last")) {
					if (useDct) {
						// check day if dct-month and newMonth are
						// equal
						if ((dct.dctMonth == newMonthInt) && (!(day == 0))) {
							if (dct.dctDay > day) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newMonth);
							}
						} else if (dct.dctMonth <= newMonthInt) {
							valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newMonth);
						} else {
							valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
						}
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedMonthDetails(linearDates, i);
						if (lmMonth.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							int lmMonthInt = Integer.parseInt(lmMonth.substring(5, 7));
							//
							int lmDayInt = 0;
							if ((lmMonth.length() > 9) && TWO_DIGITS.matcher(lmMonth.subSequence(8, 10)).matches()) {
								lmDayInt = Integer.parseInt(lmMonth.subSequence(8, 10).toString());
							}
							if ((lmMonthInt == newMonthInt) && (!(lmDayInt == 0)) && (!(day == 0))) {
								if (lmDayInt > day) {
									valueNew = valueNew.replace(checkUndef, lmMonth.substring(0, 4) + "-" + newMonth);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmMonth.substring(0, 4)) - 1 + "-" + newMonth);
								}
							}
							if (lmMonthInt <= newMonthInt) {
								valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmMonth.substring(0, 4)) - 1 + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, lmMonth.substring(0, 4) + "-" + newMonth);
							}
						}
					}
				} else if (ltn.equals("this")) {
					if (useDct) {
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedMonthDetails(linearDates, i);
						valueNew = valueNew.replace(checkUndef, lmMonth.isEmpty() ? "XXXX-XX" : lmMonth.substring(0, 4) + "-" + newMonth);
					}
				} else if (ltn.equals("next")) {
					if (useDct) {
						// check day if dct-month and newMonth are
						// equal
						if ((dct.dctMonth == newMonthInt) && (!(day == 0))) {
							if (dct.dctDay < day) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newMonth);
							}
						} else if (dct.dctMonth >= newMonthInt) {
							valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newMonth);
						} else {
							valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
						}
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedMonthDetails(linearDates, i);
						if (lmMonth.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							int lmMonthInt = Integer.parseInt(lmMonth.substring(5, 7));
							if (lmMonthInt >= newMonthInt) {
								valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmMonth.substring(0, 4)) + 1 + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, lmMonth.substring(0, 4) + "-" + newMonth);
							}
						}
					}
				}
			}

			// SEASONS NAMES
			else if ((m = UNDEF_SEASON.matcher(ambigString)).matches()) {
				String checkUndef = m.group(1);
				String ltn = m.group(2);
				Season newSeason = Season.of(ambigString, m.start(3));
				if (ltn.equals("last")) {
					if (useDct) {
						int newYear = dct.dctYear - (newSeason.ord() < dct.dctSeason.ord() //
								|| (dct.dctSeason == Season.WINTER && dct.dctMonth < 12) //
										? 1 : 0);
						valueNew = valueNew.replace(checkUndef, newYear + "-" + newSeason);
					} else { // NARRATVIE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						if (lmSeason == null || lmSeason.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							Season se = Season.of(lmSeason, 5);
							int newYear = Integer.parseInt(lmSeason.substring(0, 4)) //
									- (newSeason.ord() < se.ord() ? 1 : 0);
							valueNew = valueNew.replace(checkUndef, newYear + "-" + newSeason);
						}
					}
				} else if (ltn.equals("this")) {
					if (useDct) {
						// TODO use tense of sentence?
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
					} else {
						// TODO use tense of sentence?
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						valueNew = valueNew.replace(checkUndef, lmSeason.isEmpty() ? "XXXX-XX" : (lmSeason.substring(0, 4) + "-" + newSeason));
					}
				} else if (ltn.equals("next")) {
					if (useDct) {
						int newYear = dct.dctYear + (newSeason.ord() <= dct.dctSeason.ord() ? 1 : 0);
						valueNew = valueNew.replace(checkUndef, newYear + "-" + newSeason);
					} else { // NARRATIVE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						if (lmSeason.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							Season se = Season.of(lmSeason, 5);
							int newYear = Integer.parseInt(lmSeason.substring(0, 4)) //
									+ (newSeason.ord() <= se.ord() ? 1 : 0);
							valueNew = valueNew.replace(checkUndef, newYear + "-" + newSeason);
						}
					}
				}
			}

			// WEEKDAY NAMES
			// TODO the calculation is strange, but works
			// TODO tense should be included?!
			else if ((m = UNDEF_WEEKDAY.matcher(ambigString)).matches()) {
				String checkUndef = m.group(1);
				String ltnd = m.group(2);
				String newWeekday = m.group(3);
				int newWeekdayInt = Integer.parseInt(norm.getFromNormDayInWeek(newWeekday));
				if (ltnd.equals("last")) {
					if (useDct) {
						int diff = -(dct.dctWeekday - newWeekdayInt);
						diff = (diff >= 0) ? diff - 7 : diff;
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = -(lmWeekdayInt - newWeekdayInt);
							diff = (diff >= 0) ? diff - 7 : diff;
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("this")) {
					if (useDct) {
						// TODO tense should be included?!
						int diff = -(dct.dctWeekday - newWeekdayInt);
						diff = (diff > 0) ? diff - 7 : diff;
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						// TODO tense should be included?!
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = (-1) * (lmWeekdayInt - newWeekdayInt);
							diff = (diff > 0) ? diff - 7 : diff;
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("next")) {
					if (useDct) {
						int diff = newWeekdayInt - dct.dctWeekday;
						diff = (diff <= 0) ? diff + 7 : diff;
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = newWeekdayInt - lmWeekdayInt;
							diff = (diff <= 0) ? diff + 7 : diff;
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("day")) {
					if (useDct) {
						// TODO tense should be included?!
						int diff = -(dct.dctWeekday - newWeekdayInt);
						diff = (diff > 0) ? diff - 7 : diff;
						// Tense is FUTURE
						if ((last_used_tense == Tense.FUTURE) && diff != 0) {
							diff += 7;
						}
						// Tense is PAST
						if ((last_used_tense == Tense.PAST)) {

						}
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						// TODO tense should be included?!
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = -(lmWeekdayInt - newWeekdayInt);
							diff = (diff > 0) ? diff - 7 : diff;
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				}
			} else {
				LOG.debug("ATTENTION: UNDEF value for: {} is not handled in disambiguation phase!", valueNew);
			}
		}

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
			if (LOG.isDebugEnabled())
				LOG.debug("{} DISAMBIGUATION PHASE: foundBy: {} text: {} value: {} NEW value: {} ", //
						t_i.getTimexId(), t_i.getFoundByRule(), t_i.getCoveredText(), t_i.getTimexValue(), valueNew);

			t_i.setTimexValue(valueNew);
			t_i.addToIndexes();
			linearDates.set(i, t_i);
		}
	}

	/**
	 * Check whether or not a jcas object has a correct DCT value. If there is no DCT present, we canonically return true since fallback calculation takes care of that scenario.
	 * 
	 * @param jcas
	 * @return Whether or not the given jcas contains a valid DCT
	 */
	public static boolean isValidDCT(JCas jcas) {
		AnnotationIndex<Dct> dcts = jcas.getAnnotationIndex(Dct.type);
		FSIterator<Dct> dctIter = dcts.iterator();

		if (!dctIter.hasNext())
			return true;
		String dctVal = dctIter.next().getValue();
		if (dctVal == null)
			return false;
		// Something like 20041224 or 2004-12-24
		return VALID_DCT.matcher(dctVal).matches();
	}
}