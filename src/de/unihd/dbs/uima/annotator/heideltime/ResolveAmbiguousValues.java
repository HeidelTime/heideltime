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
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
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
		String dctSeason = "";
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

	boolean documentTypeNews, documentTypeNarrative, documentTypeColloquial, documentTypeScientific;

	NormalizationManager norm;

	Language language;

	private static final Pattern VALID_DCT = Pattern.compile("\\d{4}.\\d{2}.\\d{2}|\\d{8}");

	public void init(Language language, boolean find_temponyms, String typeToProcess) {
		if (this.language != language) {
			this.language = language;
			norm = NormalizationManager.getInstance(language, find_temponyms);
		}

		// ////////////////////////////
		// DOCUMENT TYPE TO PROCESS //
		// //////////////////////////
		documentTypeNews = typeToProcess.equals("news");
		documentTypeNarrative = typeToProcess.equals("narrative") || typeToProcess.equals("narratives");
		documentTypeColloquial = typeToProcess.equals("colloquial");
		documentTypeScientific = typeToProcess.equals("scientific");
	}

	public String specifyAmbiguousValuesString(String ambigString, Timex3 t_i, int i, List<Timex3> linearDates, JCas jcas) {
		// ////////////////////////////////////////////
		// INFORMATION ABOUT DOCUMENT CREATION TIME //
		// ////////////////////////////////////////////
		ResolveAmbiguousValues.ParsedDct dct = new ParsedDct();
		boolean dctAvailable = dct.read(jcas);

		// check if value_i has month, day, season, week (otherwise no UNDEF-year
		// is possible)
		boolean viHasMonth = false;
		boolean viHasDay = false;
		boolean viHasSeason = false;
		boolean viHasWeek = false;
		boolean viHasQuarter = false;
		boolean viHasHalf = false;
		int viThisMonth = 0;
		int viThisDay = 0;
		String viThisSeason = "";
		String viThisQuarter = "";
		String viThisHalf = "";
		String[] valueParts = ambigString.split("-");
		// check if UNDEF-year or UNDEF-century
		if (ambigString.startsWith("UNDEF-year") || ambigString.startsWith("UNDEF-century")) {
			if (valueParts.length > 2) {
				// get vi month
				if (TWO_DIGITS.matcher(valueParts[2]).matches()) {
					viHasMonth = true;
					viThisMonth = Integer.parseInt(valueParts[2]);
				}
				// get vi season
				else if (valueParts[2].equals("SP") || valueParts[2].equals("SU") || valueParts[2].equals("FA") || valueParts[2].equals("WI")) {
					viHasSeason = true;
					viThisSeason = valueParts[2];
				}
				// get v1 quarter
				else if (valueParts[2].equals("Q1") || valueParts[2].equals("Q2") || valueParts[2].equals("Q3") || valueParts[2].equals("Q4")) {
					viHasQuarter = true;
					viThisQuarter = valueParts[2];
				}
				// get v1 half
				else if (valueParts[2].equals("H1") || valueParts[2].equals("H2")) {
					viHasHalf = true;
					viThisHalf = valueParts[2];
				}
				// get vi day
				if (valueParts.length > 3 && TWO_DIGITS.matcher(valueParts[3]).matches()) {
					viHasDay = true;
					viThisDay = Integer.parseInt(valueParts[3]);
				}
			}
		} else {
			if (valueParts.length > 1) {
				// get vi month
				if (TWO_DIGITS.matcher(valueParts[1]).matches()) {
					viHasMonth = true;
					viThisMonth = Integer.parseInt(valueParts[1]);
				}
				// get vi season
				else if (valueParts[1].equals("SP") || valueParts[1].equals("SU") || valueParts[1].equals("FA") || valueParts[1].equals("WI")) {
					viHasSeason = true;
					viThisSeason = valueParts[1];
				}
				// get v1 quarter
				else if (valueParts[1].equals("Q1") || valueParts[1].equals("Q2") || valueParts[1].equals("Q3") || valueParts[1].equals("Q4")) {
					viHasQuarter = true;
					viThisQuarter = valueParts[1];
				}
				// get v1 half
				else if (valueParts[1].equals("H1") || valueParts[1].equals("H2")) {
					viHasHalf = true;
					viThisHalf = valueParts[1];
				}
				// get vi day
				if (valueParts.length > 2 && TWO_DIGITS.matcher(valueParts[2]).matches()) {
					viHasDay = true;
					viThisDay = Integer.parseInt(valueParts[2]);
				}
			}
		}
		// get the last tense (depending on the part of speech tags used in front
		// or behind the expression)
		String last_used_tense = ContextAnalyzer.getLastTense(t_i, jcas, language);

		//////////////////////////
		// DISAMBIGUATION PHASE //
		//////////////////////////

		////////////////////////////////////////////////////
		// IF YEAR IS COMPLETELY UNSPECIFIED (UNDEF-year) //
		////////////////////////////////////////////////////
		String valueNew = ambigString;
		if (ambigString.startsWith("UNDEF-year")) {
			String newYearValue = Integer.toString(dct.dctYear);
			// vi has month (ignore day)
			if (viHasMonth && !viHasSeason) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
						// if dct-month is larger than vi-month,
						// than add 1 to dct-year
						if (dct.dctMonth > viThisMonth) {
							int intNewYear = dct.dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						// if dct-month is smaller than vi month,
						// than substrate 1 from dct-year
						if (dct.dctMonth < viThisMonth) {
							int intNewYear = dct.dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}
			// vi has quaurter
			if (viHasQuarter) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
						if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
							int intNewYear = dct.dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
							int intNewYear = dct.dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense.length() == 0) {
						if (documentTypeColloquial) {
							// IN COLLOQUIAL: future temporal
							// expressions
							if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dct.dctYear + 1;
								newYearValue = Integer.toString(intNewYear);
							}
						} else {
							// IN NEWS: past temporal
							// expressions
							if (Integer.parseInt(dct.dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dct.dctYear - 1;
								newYearValue = Integer.toString(intNewYear);
							}
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}
			// vi has half
			if (viHasHalf) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
						if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
							int intNewYear = dct.dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
							int intNewYear = dct.dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense.length() == 0) {
						if (documentTypeColloquial) {
							// IN COLLOQUIAL: future temporal
							// expressions
							if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dct.dctYear + 1;
								newYearValue = Integer.toString(intNewYear);
							}
						} else {
							// IN NEWS: past temporal
							// expressions
							if (Integer.parseInt(dct.dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dct.dctYear - 1;
								newYearValue = Integer.toString(intNewYear);
							}
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}

			// vi has season
			if (!viHasMonth && !viHasDay && viHasSeason) {
				// TODO check tenses?
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					newYearValue = Integer.toString(dct.dctYear);
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}
			// vi has week
			if (viHasWeek) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					newYearValue = Integer.toString(dct.dctYear);
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedYear(linearDates, i);
				}
			}

			// REPLACE THE UNDEF-YEAR WITH THE NEWLY CALCULATED YEAR AND ADD
			// TIMEX TO INDEXES
			if (newYearValue.length() == 0) {
				valueNew = ambigString.replaceFirst("UNDEF-year", "XXXX");
			} else {
				valueNew = ambigString.replaceFirst("UNDEF-year", newYearValue);
			}
		}

		///////////////////////////////////////////////////
		// just century is unspecified (UNDEF-century86) //
		///////////////////////////////////////////////////
		else if (ambigString.startsWith("UNDEF-century")) {
			String newCenturyValue = Integer.toString(dct.dctCentury);

			// NEWS and COLLOQUIAL DOCUMENTS
			if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && !ambigString.equals("UNDEF-century")) {
				int viThisDecade = Integer.parseInt(ambigString.substring(13, 14));

				if (LOG.isDebugEnabled())
					LOG.debug("dctCentury" + dct.dctCentury);

				newCenturyValue = Integer.toString(dct.dctCentury);

				// Tense is FUTURE
				if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
					if (viThisDecade < dct.dctDecade) {
						newCenturyValue = Integer.toString(dct.dctCentury + 1);
					} else {
						newCenturyValue = Integer.toString(dct.dctCentury);
					}
				}
				// Tense is PAST
				if (last_used_tense.equals("PAST")) {
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
			if (newCenturyValue.length() == 0) {
				if (!documentTypeNarrative) {
					// always assume that sixties, twenties, and so on
					// are 19XX if no century found (LREC change)
					valueNew = ambigString.replaceFirst("UNDEF-century", "19");
				}
				// LREC change: assume in narrative-style documents that
				// if no other century was mentioned before, 1st century
				else {
					valueNew = ambigString.replaceFirst("UNDEF-century", "00");
				}
			} else {
				valueNew = ambigString.replaceFirst("UNDEF-century", newCenturyValue);
			}
			// always assume that sixties, twenties, and so on are 19XX -- if
			// not narrative document (LREC change)
			if ((valueNew.matches("\\d\\d\\d")) && !documentTypeNarrative) {
				valueNew = "19" + valueNew.substring(2);
			}
		}

		////////////////////////////////////////////////////
		// CHECK IMPLICIT EXPRESSIONS STARTING WITH UNDEF //
		////////////////////////////////////////////////////
		else if (ambigString.startsWith("UNDEF")) {
			Matcher m;
			valueNew = ambigString;
			if (ambigString.equals("UNDEF-REFDATE")) {
				if (i > 0) {
					Timex3 anyDate = linearDates.get(i - 1);
					String lmDate = anyDate.getTimexValue();
					valueNew = lmDate;
				} else {
					valueNew = "XXXX-XX-XX";
				}

				//////////////////
				// TO CALCULATE //
				//////////////////
				// year to calculate
			} else if ((m = UNDEF_PATTERN.matcher(ambigString)).find()) {
				String checkUndef = m.group(1);
				String ltn = m.group(2);
				String unit = m.group(3);
				String op = m.group(4);
				String sDiff = m.group(5);
				int diff = 0;
				try {
					diff = Integer.parseInt(sDiff);
				} catch (Exception e) {
					LOG.error("Expression difficult to normalize: ");
					LOG.error(ambigString);
					LOG.error("{} probably too long for parsing as integer.", sDiff);
					LOG.error("set normalized value as PAST_REF / FUTURE_REF:");
					valueNew = op.equals("PLUS") ? "FUTURE_REF" : "PAST_REF";
					return valueNew;
				}

				// do the processing for SCIENTIFIC documents (TPZ
				// identification could be improved)
				if (documentTypeScientific) {
					char opSymbol = op.equals("PLUS") ? '+' : '-';
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
						String year = dateWithYear;
						if (dateWithYear.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							if (dateWithYear.startsWith("BC")) {
								year = dateWithYear.substring(0, 6);
							} else {
								year = dateWithYear.substring(0, 4);
							}
							if (op.equals("MINUS")) {
								diff = diff * (-1);
							}
							String yearNew = DateCalculator.getXNextYear(dateWithYear, diff);
							String rest = dateWithYear.substring(4);
							valueNew = valueNew.replace(checkUndef, yearNew + rest);
						}
					}

					// REF and this are handled here
					if (unit.equals("century")) {
						if ((documentTypeNews | documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							int century = dct.dctCentury;
							if (op.equals("MINUS")) {
								century = dct.dctCentury - diff;
							} else if (op.equals("PLUS")) {
								century = dct.dctCentury + diff;
							}
							valueNew = valueNew.replace(checkUndef, Integer.toString(century));
						} else {
							String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
							if (lmCentury.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XX");
							} else {
								if (op.equals("MINUS")) {
									diff = (-1) * diff;
								}
								lmCentury = DateCalculator.getXNextCentury(lmCentury, diff);
								valueNew = valueNew.replace(checkUndef, lmCentury);
							}
						}
					} else if (unit.equals("decade")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							int dctDecadeLong = Integer.parseInt(dct.dctCentury + "" + dct.dctDecade);
							int decade = dctDecadeLong;
							if (op.equals("MINUS")) {
								decade = dctDecadeLong - diff;
							} else if (op.equals("PLUS")) {
								decade = dctDecadeLong + diff;
							}
							valueNew = valueNew.replace(checkUndef, decade + "X");
						} else {
							String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
							if (lmDecade.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXX");
							} else {
								if (op.equals("MINUS")) {
									diff = (-1) * diff;
								}
								lmDecade = DateCalculator.getXNextDecade(lmDecade, diff);
								valueNew = valueNew.replace(checkUndef, lmDecade);
							}
						}
					} else if (unit.equals("year")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							int intValue = dct.dctYear;
							if (op.equals("MINUS")) {
								intValue = dct.dctYear - diff;
							} else if (op.equals("PLUS")) {
								intValue = dct.dctYear + diff;
							}
							valueNew = valueNew.replace(checkUndef, Integer.toString(intValue));
						} else {
							String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
							if (lmYear.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX");
							} else {
								if (op.equals("MINUS")) {
									diff = (-1) * diff;
								}
								lmYear = DateCalculator.getXNextYear(lmYear, diff);
								valueNew = valueNew.replace(checkUndef, lmYear);
							}
						}
						// TODO BC years
					} else if (unit.equals("quarter")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							int intYear = dct.dctYear;
							int intQuarter = Integer.parseInt(dct.dctQuarter.substring(1));
							int diffQuarters = diff % 4;
							diff = diff - diffQuarters;
							int diffYears = diff / 4;
							if (op.equals("MINUS")) {
								diffQuarters = diffQuarters * (-1);
								diffYears = diffYears * (-1);
							}
							intYear = intYear + diffYears;
							intQuarter = intQuarter + diffQuarters;
							valueNew = valueNew.replace(checkUndef, intYear + "-Q" + intQuarter);
						} else {
							String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
							if (lmQuarter.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX-XX");
							} else {
								int intYear = Integer.parseInt(lmQuarter.substring(0, 4));
								int intQuarter = Integer.parseInt(lmQuarter.substring(6));
								int diffQuarters = diff % 4;
								diff = diff - diffQuarters;
								int diffYears = diff / 4;
								if (op.equals("MINUS")) {
									diffQuarters = diffQuarters * (-1);
									diffYears = diffYears * (-1);
								}
								intYear = intYear + diffYears;
								intQuarter = intQuarter + diffQuarters;
								valueNew = valueNew.replace(checkUndef, intYear + "-Q" + intQuarter);
							}
						}
					} else if (unit.equals("month")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							if (op.equals("MINUS")) {
								diff = diff * (-1);
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), diff));
						} else {
							String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
							if (lmMonth.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX-XX");
							} else {
								if (op.equals("MINUS")) {
									diff = diff * (-1);
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, diff));
							}
						}
					} else if (unit.equals("week")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							if (op.equals("MINUS")) {
								diff = diff * (-1);
							} else if (op.equals("PLUS")) {
								// diff = diff * 7;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), diff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
							if (lmDay.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
							} else {
								if (op.equals("MINUS")) {
									diff = diff * 7 * (-1);
								} else if (op.equals("PLUS")) {
									diff = diff * 7;
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
							}
						}
					} else if (unit.equals("day")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && ltn.equals("this")) {
							if (op.equals("MINUS")) {
								diff = diff * (-1);
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, diff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
							if (lmDay.length() == 0) {
								valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
							} else {
								if (op.equals("MINUS")) {
									diff = diff * (-1);
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
							}
						}
					}
				}
			}

			// century
			else if (ambigString.startsWith("UNDEF-last-century")) {
				String checkUndef = "UNDEF-last-century";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury - 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					if (lmCentury.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XX");
					} else {
						lmCentury = DateCalculator.getXNextCentury(lmCentury, -1);
						valueNew = valueNew.replace(checkUndef, lmCentury);
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-century")) {
				String checkUndef = "UNDEF-this-century";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					if (lmCentury.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmCentury);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-century")) {
				String checkUndef = "UNDEF-next-century";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dct.dctCentury + 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedCentury(linearDates, i);
					if (lmCentury.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XX");
					} else {
						lmCentury = DateCalculator.getXNextCentury(lmCentury, +1);
						valueNew = valueNew.replace(checkUndef, lmCentury);
					}
				}
			}

			// decade
			else if (ambigString.startsWith("UNDEF-last-decade")) {
				String checkUndef = "UNDEF-last-decade";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear - 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					if (lmDecade.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						lmDecade = DateCalculator.getXNextDecade(lmDecade, -1);
						valueNew = valueNew.replace(checkUndef, lmDecade);
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-decade")) {
				String checkUndef = "UNDEF-this-decade";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					if (lmDecade.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmDecade);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-decade")) {
				String checkUndef = "UNDEF-next-decade";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dct.dctYear + 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedDecade(linearDates, i);
					if (lmDecade.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						lmDecade = DateCalculator.getXNextDecade(lmDecade, 1);
						valueNew = valueNew.replace(checkUndef, lmDecade);
					}
				}
			}

			// year
			else if (ambigString.startsWith("UNDEF-last-year")) {
				String checkUndef = "UNDEF-last-year";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear - 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					if (lmYear.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						lmYear = DateCalculator.getXNextYear(lmYear, -1);
						valueNew = valueNew.replace(checkUndef, lmYear);
					}
				}
				if (valueNew.endsWith("-FY")) {
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
				}
			} else if (ambigString.startsWith("UNDEF-this-year")) {
				String checkUndef = "UNDEF-this-year";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					if (lmYear.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmYear);
					}
				}
				if (valueNew.endsWith("-FY")) {
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
				}
			} else if (ambigString.startsWith("UNDEF-next-year")) {
				String checkUndef = "UNDEF-next-year";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, Integer.toString(dct.dctYear + 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedYear(linearDates, i);
					if (lmYear.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						lmYear = DateCalculator.getXNextYear(lmYear, 1);
						valueNew = valueNew.replace(checkUndef, lmYear);
					}
				}
				if (valueNew.endsWith("-FY")) {
					valueNew = "FY" + valueNew.substring(0, Math.min(valueNew.length(), 4));
				}
			}

			// month
			else if (ambigString.startsWith("UNDEF-last-month")) {
				String checkUndef = "UNDEF-last-month";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), -1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					if (lmMonth.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, -1));
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-month")) {
				String checkUndef = "UNDEF-this-month";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + norm.normNumber(dct.dctMonth));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					if (lmMonth.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmMonth);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-month")) {
				String checkUndef = "UNDEF-next-month";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dct.dctYear + "-" + norm.normNumber(dct.dctMonth), 1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedMonth(linearDates, i);
					if (lmMonth.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, 1));
					}
				}
			}

			// day
			else if (ambigString.startsWith("UNDEF-last-day")) {
				String checkUndef = "UNDEF-last-day";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, -1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
					if (lmDay.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, -1));
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-day")) {
				String checkUndef = "UNDEF-this-day";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + norm.normNumber(dct.dctMonth) + "-" + norm.normNumber(dct.dctDay));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
					if (lmDay.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmDay);
					}
					if (ambigString.equals("UNDEF-this-day")) {
						valueNew = "PRESENT_REF";
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-day")) {
				String checkUndef = "UNDEF-next-day";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear, dct.dctMonth, dct.dctDay, 1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
					if (lmDay.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, 1));
					}
				}
			}

			// week
			else if (ambigString.startsWith("UNDEF-last-week")) {
				String checkUndef = "UNDEF-last-week";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), -1));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					if (lmWeek.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						String nextWeek = DateCalculator.getXNextWeek(lmWeek, -1);
						if (nextWeek.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
						} else {
							valueNew = valueNew.replace(checkUndef, nextWeek);
						}
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-week")) {
				String checkUndef = "UNDEF-this-week";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-W" + norm.normNumber(dct.dctWeek));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					if (lmWeek.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmWeek);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-week")) {
				String checkUndef = "UNDEF-next-week";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dct.dctYear + "-W" + norm.normNumber(dct.dctWeek), 1));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedWeek(linearDates, i);
					if (lmWeek.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(lmWeek, 1));
					}
				}
			}

			// quarter
			else if (ambigString.startsWith("UNDEF-last-quarter")) {
				String checkUndef = "UNDEF-last-quarter";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
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
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + dct.dctQuarter);
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedQuarter(linearDates, i, language);
					if (lmQuarter.length() == 0) {
						valueNew = valueNew.replace(checkUndef, "XXXX-QX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmQuarter);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-quarter")) {
				String checkUndef = "UNDEF-next-quarter";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					if (dct.dctQuarter.equals("Q4")) {
						valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-Q1");
					} else {
						int newQuarter = Integer.parseInt(dct.dctQuarter.substring(1, 2)) + 1;
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-Q" + newQuarter);
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
							int newQuarter = lmQuarterOnly + 1;
							valueNew = valueNew.replace(checkUndef, lmYearOnly + "-Q" + newQuarter);
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
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
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
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newMonth);
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedMonthDetails(linearDates, i);
						if (lmMonth.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmMonth.substring(0, 4) + "-" + newMonth);
						}
					}
				} else if (ltn.equals("next")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
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
				String newSeason = m.group(3);
				if (ltn.equals("last")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						if (dct.dctSeason.equals("SP")) {
							valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newSeason);
						} else if (dct.dctSeason.equals("SU")) {
							if (newSeason.equals("SP")) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newSeason);
							}
						} else if (dct.dctSeason.equals("FA")) {
							if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newSeason);
							}
						} else if (dct.dctSeason.equals("WI")) {
							if (newSeason.equals("WI")) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newSeason);
							} else {
								if (dct.dctMonth < 12) {
									valueNew = valueNew.replace(checkUndef, dct.dctYear - 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
								}
							}
						}
					} else { // NARRATVIE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						if (lmSeason.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							String se = lmSeason.substring(5, 7);
							if (se.equals("SP")) {
								valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) - 1 + "-" + newSeason);
							} else if (se.equals("SU")) {
								if (se.equals("SP")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) - 1 + "-" + newSeason);
								}
							} else if (se.equals("FA")) {
								if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) - 1 + "-" + newSeason);
								}
							} else if (se.equals("WI")) {
								if (newSeason.equals("WI")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) - 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								}
							}
						}
					}
				} else if (ltn.equals("this")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						// TODO include tense of sentence?
						valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
					} else {
						// TODO include tense of sentence?
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						if (lmSeason.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmSeason.substring(0, 4) + "-" + newSeason);
						}
					}
				} else if (ltn.equals("next")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						if (dct.dctSeason.equals("SP")) {
							if (newSeason.equals("SP")) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
							}
						} else if (dct.dctSeason.equals("SU")) {
							if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
							}
						} else if (dct.dctSeason.equals("FA")) {
							if (newSeason.equals("WI")) {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newSeason);
							}
						} else if (dct.dctSeason.equals("WI")) {
							valueNew = valueNew.replace(checkUndef, dct.dctYear + 1 + "-" + newSeason);
						}
					} else { // NARRATIVE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedSeason(linearDates, i, language);
						if (lmSeason.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							if (lmSeason.regionMatches(5, "SP", 0, 2)) {
								if (newSeason.equals("SP")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								}
							} else if (lmSeason.regionMatches(5, "SU", 0, 2)) {
								if (newSeason.equals("SP") || newSeason.equals("SU")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								}
							} else if (lmSeason.regionMatches(5, "FA", 0, 2)) {
								if (newSeason.equals("WI")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								}
							} else if (lmSeason.regionMatches(5, "WI", 0, 2)) {
								valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
							}
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
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						int diff = (-1) * (dct.dctWeekday - newWeekdayInt);
						if (diff >= 0) {
							diff = diff - 7;
						}
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = (-1) * (lmWeekdayInt - newWeekdayInt);
							if (diff >= 0) {
								diff = diff - 7;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("this")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						// TODO tense should be included?!
						int diff = (-1) * (dct.dctWeekday - newWeekdayInt);
						if (diff >= 0) {
							diff = diff - 7;
						}
						if (diff == -7) {
							diff = 0;
						}

						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						// TODO tense should be included?!
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = (-1) * (lmWeekdayInt - newWeekdayInt);
							if (diff >= 0) {
								diff = diff - 7;
							}
							if (diff == -7) {
								diff = 0;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("next")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						int diff = newWeekdayInt - dct.dctWeekday;
						if (diff <= 0) {
							diff = diff + 7;
						}
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = newWeekdayInt - lmWeekdayInt;
							if (diff <= 0) {
								diff = diff + 7;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, diff));
						}
					}
				} else if (ltnd.equals("day")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						// TODO tense should be included?!
						int diff = (-1) * (dct.dctWeekday - newWeekdayInt);
						if (diff >= 0) {
							diff = diff - 7;
						}
						if (diff == -7) {
							diff = 0;
						}
						// Tense is FUTURE
						if ((last_used_tense.equals("FUTURE")) && diff != 0) {
							diff = diff + 7;
						}
						// Tense is PAST
						if ((last_used_tense.equals("PAST"))) {

						}
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dct.dctYear + "-" + dct.dctMonth + "-" + dct.dctDay, diff));
					} else {
						// TODO tense should be included?!
						String lmDay = ContextAnalyzer.getLastMentionedDay(linearDates, i);
						if (lmDay.length() == 0) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							int lmWeekdayInt = DateCalculator.getWeekdayOfDate(lmDay);
							int diff = (-1) * (lmWeekdayInt - newWeekdayInt);
							if (diff >= 0) {
								diff = diff - 7;
							}
							if (diff == -7) {
								diff = 0;
							}
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
			if (t_i.getEmptyValue() != null && t_i.getEmptyValue().length() > 0) {
				String emptyValueNew = specifyAmbiguousValuesString(t_i.getEmptyValue(), t_i, i, linearDates, jcas);
				t_i.setEmptyValue(emptyValueNew);
			}

			t_i.removeFromIndexes();
			if (LOG.isDebugEnabled())
				LOG.debug("{} DISAMBIGUATION PHASE: foundBy: {} text: {} value: {} NEW value: {} ", t_i.getTimexId(), t_i.getFoundByRule(), t_i.getCoveredText(),
						t_i.getTimexValue(), valueNew);

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