package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * 
 * This class contains methods that work with the dependence of a subject with its surrounding data; namely via the jcas element or a subset list.
 * 
 * @author jannik stroetgen
 *
 */
public class ContextAnalyzer {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ContextAnalyzer.class);

	private static final Pattern TWO_DIGITS = Pattern.compile("^[0-9][0-9]");
	private static final Pattern BC_TWO_DIGITS = Pattern.compile("^BC[0-9][0-9]");
	private static final Pattern THREE_DIGITS = Pattern.compile("^[0-9][0-9][0-9]");
	private static final Pattern BC_THREE_DIGITS = Pattern.compile("^BC[0-9][0-9][0-9]");
	private static final Pattern FOUR_DIGITS = Pattern.compile("^[0-9][0-9][0-9][0-9]");
	private static final Pattern BC_FOUR_DIGITS = Pattern.compile("^BC[0-9][0-9][0-9][0-9]");
	private static final Pattern YEAR_MON = Pattern.compile("^[0-9][0-9][0-9][0-9]-[0-9][0-9]");
	private static final Pattern BC_YEAR_MON = Pattern.compile("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9]");
	private static final Pattern YEAR_MON_WK = Pattern.compile("^[0-9][0-9][0-9][0-9]-W[0-9][0-9]");
	private static final Pattern YEAR_MON_DAY = Pattern.compile("^([0-9][0-9][0-9][0-9])-([0-9][0-9])-([0-9][0-9])");
	private static final Pattern YEAR_QUARTER = Pattern.compile("^[0-9][0-9][0-9][0-9]-Q[1234]");
	private static final Pattern YEAR_SEASON = Pattern.compile("^[0-9][0-9][0-9][0-9]-(SP|SU|FA|WI)");

	private static final Pattern PREVUE_ENVISAGEE = Pattern.compile("^(?:prévue?s?|envisagée?s?)$");

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @param x
	 *                type to search for
	 * @return last mentioned entry
	 */
	public static String getLastMentionedX(List<Timex3> linearDates, int i, Function<String, String> func) {
		// Timex for which to get the last mentioned x (i.e., Timex i)
		Timex3 t_i = linearDates.get(i);

		for (int j = i - 1; j >= 0; --j) {
			Timex3 timex = linearDates.get(j);
			// check that the two timexes to compare do not have the same offset:
			if (t_i.getBegin() == timex.getBegin())
				continue;
			String value = timex.getTimexValue();
			if (value.contains("funcDate"))
				continue;
			String rep = func.apply(value);
			if (rep != null)
				return rep;
		}
		return "";
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned century
	 */
	public static String getLastMentionedCentury(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (TWO_DIGITS.matcher(value).find())
				return value.substring(0, 2);
			if (BC_TWO_DIGITS.matcher(value).find())
				return value.substring(0, 4);
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned decade
	 */
	public static String getLastMentionedDecade(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (THREE_DIGITS.matcher(value).find())
				return value.substring(0, 3);
			if (BC_THREE_DIGITS.matcher(value).find())
				return value.substring(0, 5);
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned year
	 */
	public static String getLastMentionedYear(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (FOUR_DIGITS.matcher(value).find())
				return value.substring(0, 4);
			if (BC_FOUR_DIGITS.matcher(value).find())
				return value.substring(0, 6);
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned date year
	 */
	public static String getLastMentionedDateYear(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (FOUR_DIGITS.matcher(value).find())
				return value; // .substring?
			if (BC_FOUR_DIGITS.matcher(value).find())
				return value; // .substring?
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned month
	 */
	public static String getLastMentionedMonth(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_MON.matcher(value).find())
				return value.substring(0, 7);
			if (BC_YEAR_MON.matcher(value).find())
				return value.substring(0, 9);
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned month with details
	 */
	public static String getLastMentionedMonthDetails(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_MON.matcher(value).find())
				return value; // .substring?
			// else if (value.matches(YEAR_MON_BC.matcher(value).find())
			// return value;
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned day
	 */
	public static String getLastMentionedDay(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_MON_DAY.matcher(value).find())
				return value.substring(0, 10);
			// else if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].*"))
			// return value.substring(0,12);
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned weeh
	 */
	public static String getLastMentionedWeek(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			Matcher matcher = YEAR_MON_DAY.matcher(value);
			if (matcher.find())
				return matcher.group(1) + "-W" + DateCalculator.getWeekOfDate(matcher.group(0));
			if (YEAR_MON_WK.matcher(value).find())
				return value; // .substring?
			// TODO check what to do for BC times
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @param language
	 *                Language
	 * @return last mentioned quarter
	 */
	public static String getLastMentionedQuarter(List<Timex3> linearDates, int i, Language language) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_MON.matcher(value).find()) {
				NormalizationManager nm = NormalizationManager.getInstance(language, true);
				String month = value.substring(5, 7);
				String quarter = nm.getFromNormMonthInQuarter(month);
				if (quarter == null)
					quarter = "1";
				return value.substring(0, 4) + "-Q" + quarter;
			}
			if (YEAR_QUARTER.matcher(value).find())
				return value.substring(0, 7);
			// TODO check what to do for BC times
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned century
	 */
	public static String getLastMentionedDateQuarter(List<Timex3> linearDates, int i) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_QUARTER.matcher(value).find())
				return value.substring(0, 7);
			// TODO check what to do for BC times
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @return last mentioned season
	 */
	public static String getLastMentionedSeason(List<Timex3> linearDates, int i, Language language) {
		return getLastMentionedX(linearDates, i, value -> {
			if (YEAR_MON.matcher(value).find()) {
				NormalizationManager nm = NormalizationManager.getInstance(language, true);
				String month = value.substring(5, 7);
				String season = nm.getFromNormMonthInSeason(month);
				return value.substring(0, 4) + "-" + season;
			}
			// if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
			// String month = value.substring(7,9);
			// String season = nm.getFromNormMonthInSeason(month);
			// return value.substring(0,6)+"-"+season;
			// }
			if (YEAR_SEASON.matcher(value).find())
				return value.substring(0, 7);
			// else if (value.matches("^BC[0-9][0-9][0-9][0-9]-(SP|SU|FA|WI).*")) {
			// return value.substring(0,9);
			// }
			return null;
		});
	}

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * @param linearDates
	 *                list of previous linear dates
	 * @param i
	 *                index for the previous date entry
	 * @param x
	 *                type to search for
	 * @return last mentioned entry
	 */
	public static String getLastMentionedX(List<Timex3> linearDates, int i, String x, Language language) {
		if (x.equals("century")) {
			return getLastMentionedCentury(linearDates, i);
		} else if (x.equals("decade")) {
			return getLastMentionedDecade(linearDates, i);
		} else if (x.equals("year")) {
			return getLastMentionedYear(linearDates, i);
		} else if (x.equals("dateYear")) {
			return getLastMentionedDateYear(linearDates, i);
		} else if (x.equals("month")) {
			return getLastMentionedMonth(linearDates, i);
		} else if (x.equals("month-with-details")) {
			return getLastMentionedMonthDetails(linearDates, i);
		} else if (x.equals("day")) {
			return getLastMentionedDay(linearDates, i);
		} else if (x.equals("week")) {
			return getLastMentionedWeek(linearDates, i);
		} else if (x.equals("quarter")) {
			return getLastMentionedQuarter(linearDates, i, language);
		} else if (x.equals("dateQuarter")) {
			return getLastMentionedDateQuarter(linearDates, i);
		} else if (x.equals("season")) {
			return getLastMentionedSeason(linearDates, i, language);
		} else {
			LOG.error("Unknown type {}", x);
			return "";
		}
	}

	/**
	 * Get the last tense used in the sentence
	 * 
	 * @param timex
	 *                timex construct to discover tense data for
	 * @return string that contains the tense
	 */
	public static String getClosestTense(Timex3 timex, JCas jcas, Language language) {
		RePatternManager rpm = RePatternManager.getInstance(language, false);
		Matcher tensePos4PresentFuture = rpm.getCompiled("tensePos4PresentFuture").matcher("");
		Matcher tensePos4Past = rpm.getCompiled("tensePos4Past").matcher("");
		Matcher tensePos4Future = rpm.getCompiled("tensePos4Future").matcher("");
		Matcher tenseWord4Future = rpm.getCompiled("tenseWord4Future").matcher("");

		String lastTense = "";
		String nextTense = "";

		int tokenCounter = 0;
		int lastid = 0;
		int nextid = 0;
		int tid = 0;

		// Get the sentence
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		Sentence s = new Sentence(jcas);
		for (FSIterator<Sentence> iterSentence = sentences.iterator(); iterSentence.hasNext();) {
			s = (Sentence) iterSentence.next();
			if ((s.getBegin() <= timex.getBegin()) && (s.getEnd() >= timex.getEnd())) {
				break;
			}
		}

		// Get the tokens
		TreeMap<Integer, Token> tmToken = new TreeMap<Integer, Token>();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		for (Token token : tokens) {
			tmToken.put(token.getEnd(), token);
		}

		// Get the last VERB token
		for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
			tokenCounter++;
			if (ent.getKey() < timex.getBegin()) {
				Token token = ent.getValue();
				String pos = token.getPos();
				if (pos == null)
					continue; // POS not available?

				if (LOG.isTraceEnabled()) {
					LOG.trace("GET LAST TENSE: string:" + token.getCoveredText() + " pos:" + pos);
					LOG.trace("tensePos4PresentFuture pattern:" + tensePos4PresentFuture.pattern().pattern());
					LOG.trace("tensePos4Future pattern:" + tensePos4Future.pattern().pattern());
					LOG.trace("tensePos4Past pattern:" + tensePos4Past.pattern().pattern());
					LOG.trace("CHECK TOKEN: " + pos);
				}

				if (tensePos4PresentFuture != null && tensePos4PresentFuture.reset(pos).matches()) {
					lastTense = "PRESENTFUTURE";
					lastid = tokenCounter;
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = "PAST";
					lastid = tokenCounter;
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
						lastTense = "FUTURE";
						lastid = tokenCounter;
					}
				}
			} else {
				if (tid == 0)
					tid = tokenCounter;
			}
		}
		tokenCounter = 0;
		for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
			tokenCounter++;
			if (nextTense.length() == 0) {
				if (ent.getKey() > timex.getEnd()) {
					Token token = ent.getValue();
					String pos = token.getPos();
					if (pos == null)
						continue; // No POS available?

					if (LOG.isTraceEnabled()) {
						LOG.trace("GET NEXT TENSE: string:" + token.getCoveredText() + " pos:" + pos);
						LOG.trace("tensePos4PresentFuture pattern:" + tensePos4PresentFuture.pattern().pattern());
						LOG.trace("tensePos4Future pattern:" + tensePos4Future.pattern().pattern());
						LOG.trace("tensePos4Past pattern:" + tensePos4Past.pattern().pattern());
						LOG.trace("CHECK TOKEN: " + pos);
					}

					if (tensePos4PresentFuture != null && tensePos4PresentFuture.reset(pos).matches()) {
						nextTense = "PRESENTFUTURE";
						nextid = tokenCounter;
					} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
						nextTense = "PAST";
						nextid = tokenCounter;
					} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
						if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
							nextTense = "FUTURE";
							nextid = tokenCounter;
						}
					}
				}
			}
		}
		if (lastTense.length() == 0) {
			LOG.trace("TENSE: {}", nextTense);
			return nextTense;
		} else if (nextTense.length() == 0) {
			LOG.trace("TENSE: {}", lastTense);
			return lastTense;
		} else {
			// If there is tense before and after the timex token,
			// return the closer one:
			if ((tid - lastid) > (nextid - tid)) {
				LOG.trace("TENSE: {}", nextTense);
				return nextTense;
			} else {
				LOG.trace("TENSE: {}", lastTense);
				return lastTense;
			}
		}
	}

	/**
	 * Get the last tense used in the sentence
	 * 
	 * @param timex
	 *                timex construct to discover tense data for
	 * @return string that contains the tense
	 */
	public static String getLastTense(Timex3 timex, JCas jcas, Language language) {
		RePatternManager rpm = RePatternManager.getInstance(language, false);
		Matcher tensePos4Past = rpm.getCompiled("tensePos4Past").matcher("");
		Matcher tensePos4Future = rpm.getCompiled("tensePos4Future").matcher("");
		Matcher tensePos4PresentFuture = rpm.getCompiled("tensePos4PresentFuture").matcher("");
		Matcher tenseWord4Future = rpm.getCompiled("tenseWord4Future").matcher("");

		String lastTense = "";

		// Get the sentence
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		Sentence s = new Sentence(jcas);
		for (FSIterator<Sentence> iterSentence = sentences.iterator(); iterSentence.hasNext();) {
			s = (Sentence) iterSentence.next();
			if ((s.getBegin() <= timex.getBegin()) && (s.getEnd() >= timex.getEnd())) {
				break;
			}
		}

		// Get the tokens
		TreeMap<Integer, Token> tmToken = new TreeMap<Integer, Token>();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		for (Token token : tokens) {
			tmToken.put(token.getEnd(), token);
		}

		// Get the last VERB token
		for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
			if (ent.getKey() < timex.getBegin()) {
				Token token = ent.getValue();
				String coveredText = token.getCoveredText();
				String pos = token.getPos();
				if (pos == null)
					continue; // No POS available?

				if (LOG.isTraceEnabled()) {
					LOG.trace("GET LAST TENSE: string:" + coveredText + " pos: " + pos);
					LOG.trace("tensePos4PresentFuture pattern:" + tensePos4PresentFuture.pattern().pattern());
					LOG.trace("tensePos4Future pattern:" + tensePos4Future.pattern().pattern());
					LOG.trace("tensePos4Past pattern:" + tensePos4Past.pattern().pattern());
					LOG.trace("CHECK TOKEN: " + pos);
				}

				if (tensePos4PresentFuture != null && tensePos4PresentFuture.reset(pos).matches()) {
					lastTense = "PRESENTFUTURE";
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = "PAST";
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(coveredText).matches()) {
						lastTense = "FUTURE";
						LOG.debug("this tense: {}", lastTense);
					}
				}
				if (coveredText.equals("since") || coveredText.equals("depuis")) {
					lastTense = "PAST";
					LOG.debug("this tense: {}", lastTense);
				}
			}
			if (lastTense.length() == 0 && ent.getKey() > timex.getEnd()) {
				Token token = ent.getValue();
				String pos = token.getPos();

				if (LOG.isDebugEnabled()) {
					LOG.debug("GET NEXT TENSE: string:" + token.getCoveredText() + " pos:" + pos);
					LOG.debug("hmAllRePattern.containsKey(tensePos4PresentFuture):" + tensePos4PresentFuture.pattern().pattern());
					LOG.debug("hmAllRePattern.containsKey(tensePos4Future):" + tensePos4Future.pattern().pattern());
					LOG.debug("hmAllRePattern.containsKey(tensePos4Past):" + tensePos4Past.pattern().pattern());
					LOG.debug("CHECK TOKEN:" + pos);
				}

				if (pos == null) {

				} else if (tensePos4PresentFuture != null && tensePos4PresentFuture.reset(pos).matches()) {
					lastTense = "PRESENTFUTURE";
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = "PAST";
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
						lastTense = "FUTURE";
						LOG.debug("this tense: {}", lastTense);
					}
				}
			}
		}
		// check for double POS Constraints (not included in the rule language, yet) TODO
		// VHZ VNN and VHZ VNN and VHP VNN and VBP VVN
		String prevPos = "";
		String longTense = "";
		if (lastTense.equals("PRESENTFUTURE")) {
			for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
				if (ent.getKey() < timex.getBegin()) {
					Token token = ent.getValue();
					String pos = token.getPos();
					if ("VHZ".equals(prevPos) || "VBZ".equals(prevPos) || "VHP".equals(prevPos) || "VBP".equals(prevPos) || prevPos.equals("VER:pres")) {
						if ("VVN".equals(pos) || "VER:pper".equals(pos)) {
							String covered = token.getCoveredText();
							if (!(covered.equals("expected")) && !(covered.equals("scheduled"))) {
								lastTense = longTense = "PAST";
								LOG.debug("this tense: {}", lastTense);
							}
						}
					}
					prevPos = pos;
				}
				if (longTense.length() == 0 && ent.getKey() > timex.getEnd()) {
					Token token = ent.getValue();
					if ("VHZ".equals(prevPos) || "VBZ".equals(prevPos) || "VHP".equals(prevPos) || "VBP".equals(prevPos) || "VER:pres".equals(prevPos)) {
						if ("VVN".equals(token.getPos()) || "VER:pper".equals(token.getPos())) {
							String covered = token.getCoveredText();
							if (!(covered.equals("expected")) && !(covered.equals("scheduled"))) {
								lastTense = longTense = "PAST";
								LOG.debug("this tense: {}", lastTense);
							}
						}
					}
					prevPos = token.getPos();
				}
			}
		}
		// French: VER:pres VER:pper
		if (lastTense.equals("PAST")) {
			for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
				if (ent.getKey() < timex.getBegin()) {
					Token token = ent.getValue();
					String pos = token.getPos();
					if ("VER:pres".equals(prevPos) && "VER:pper".equals(pos)) {
						if (PREVUE_ENVISAGEE.matcher(token.getCoveredText()).matches()) {
							lastTense = longTense = "FUTURE";
							LOG.debug("this tense: {}", lastTense);
						}
					}
					prevPos = pos;
				}
				if (longTense.length() == 0) {
					if (ent.getKey() > timex.getEnd()) {
						Token token = ent.getValue();
						String pos = token.getPos();
						if ("VER:pres".equals(prevPos) && "VER:pper".equals(pos)) {
							if (PREVUE_ENVISAGEE.matcher(token.getCoveredText()).matches()) {
								lastTense = longTense = "FUTURE";
								LOG.debug("this tense: {}", lastTense);
							}
						}
						prevPos = pos;
					}
				}
			}
		}
		LOG.trace("TENSE: {}", lastTense);

		return lastTense;
	}

	/**
	 * Check token boundaries of expressions.
	 * 
	 * @param r
	 *                MatchResult
	 * @param s
	 *                Respective sentence
	 * @return whether or not the MatchResult is a clean one
	 */
	public static boolean checkInfrontBehind(MatchResult r, Sentence s) {
		// get rid of expressions such as "1999" in 53453.1999
		String cov = s.getCoveredText();
		if (r.start() >= 1) {
			char prev = cov.charAt(r.start() - 1);
			if (r.start() >= 2 && prev == '.' && Character.isDigit(cov.charAt(r.start() - 2)))
				return false;

			// get rid of expressions if there is a character or symbol ($+) directly in front of the expression
			if (prev == '(' || prev == '$' || prev == '+' || Character.isAlphabetic(prev))
				return false;
		}

		final int len = cov.length();
		if (r.end() < len) {
			char succ = cov.charAt(r.end());
			if (succ == ')' || succ == '°' || Character.isAlphabetic(succ))
				return false;
			if (r.end() + 1 < len && Character.isDigit(succ)) {
				char succ2 = cov.charAt(r.end() + 1);
				if (succ2 == '.' || succ2 == ',')
					return false;
			}
		}
		return true;
	}

	/**
	 * Check token boundaries using token information
	 *
	 * @param r
	 *                MatchResult
	 * @param s
	 *                respective Sentence
	 * @param jcas
	 *                current CAS object
	 * @return whether or not the MatchResult is a clean one
	 */
	public static boolean checkTokenBoundaries(MatchResult r, Sentence s, JCas jcas) {
		// whole expression is marked as a sentence
		final int offset = s.getBegin();
		if (r.end() - r.start() == s.getEnd() - offset)
			return true;

		// Only check Token boundaries if no white-spaces in front of and behind the match-result
		String cov = s.getCoveredText();
		boolean beginOK = r.start() == 0 || r.start() == s.getBegin() //
				|| (r.start() < cov.length() && cov.charAt(r.start() - 1) == ' ');
		boolean endOK = r.end() == cov.length() || r.end() == s.getEnd() //
				|| (r.end() < cov.length() && cov.charAt(r.end()) == ' ');
		if (beginOK && endOK)
			return true;

		// other token boundaries than white-spaces
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		for (FSIterator<Token> iterToken = tokens.subiterator(s); iterToken.hasNext();) {
			Token t = iterToken.next();

			// Check begin
			if (r.start() + offset == t.getBegin()) {
				beginOK = true;
			}
			// Tokenizer does not split number from some symbols (".", "/", "-", "–"),
			// e.g., "...12 August-24 August..."
			else if (r.start() > 0) {
				char prev = cov.charAt(r.start() - 1);
				if (prev == '.' || prev == '/' || prev == '-' || prev == '–')
					beginOK = true;
			}

			// Check end
			if (r.end() + offset == t.getEnd()) {
				endOK = true;
			}
			// Tokenizer does not split number from some symbols (".", "/", "-", "–"),
			// e.g., "... in 1990. New Sentence ..."
			else if (r.end() < cov.length()) {
				char last = cov.charAt(r.end());
				if (last == '.' || last == '/' || last == '-' || last == '–')
					endOK = true;
			}

			if (beginOK && endOK)
				return true;
		}
		return false;
	}
}
