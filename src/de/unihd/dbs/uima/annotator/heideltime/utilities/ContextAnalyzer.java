package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
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

	public static enum Tense {
		PRESENTFUTURE, PAST, FUTURE
	}

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
	 * Get the last tense used in the sentence
	 * 
	 * @param timex
	 *                timex construct to discover tense data for
	 * @return string that contains the tense
	 */
	public static Tense getClosestTense(Timex3 timex, JCas jcas, Language language) {
		RePatternManager rpm = RePatternManager.getInstance(language, false);
		Matcher tensePos4PresentFuture = rpm.getCompiled("tensePos4PresentFuture").matcher("");
		Matcher tensePos4Past = rpm.getCompiled("tensePos4Past").matcher("");
		Matcher tensePos4Future = rpm.getCompiled("tensePos4Future").matcher("");
		Matcher tenseWord4Future = rpm.getCompiled("tenseWord4Future").matcher("");

		Tense lastTense = null, nextTense = null;

		int tokenCounter = 0;
		int lastid = 0, nextid = 0;
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
					lastTense = Tense.PRESENTFUTURE;
					lastid = tokenCounter;
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = Tense.PAST;
					lastid = tokenCounter;
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
						lastTense = Tense.FUTURE;
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
			if (nextTense == null) {
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
						nextTense = Tense.PRESENTFUTURE;
						nextid = tokenCounter;
					} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
						nextTense = Tense.PAST;
						nextid = tokenCounter;
					} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
						if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
							nextTense = Tense.FUTURE;
							nextid = tokenCounter;
						}
					}
				}
			}
		}
		if (lastTense == null) {
			LOG.trace("TENSE: {}", nextTense);
			return nextTense;
		} else if (nextTense == null) {
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
	public static Tense getLastTense(Timex3 timex, JCas jcas, Language language) {
		RePatternManager rpm = RePatternManager.getInstance(language, false);
		Matcher tensePos4Past = rpm.getCompiled("tensePos4Past").matcher("");
		Matcher tensePos4Future = rpm.getCompiled("tensePos4Future").matcher("");
		Matcher tensePos4PresentFuture = rpm.getCompiled("tensePos4PresentFuture").matcher("");
		Matcher tenseWord4Future = rpm.getCompiled("tenseWord4Future").matcher("");

		Tense lastTense = null;

		// Get the sentence
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		Sentence s = new Sentence(jcas);
		for (FSIterator<Sentence> iterSentence = sentences.iterator(); iterSentence.hasNext();) {
			s = iterSentence.next();
			if (s.getBegin() <= timex.getBegin() && s.getEnd() >= timex.getEnd())
				break;
		}

		// Get the tokens
		TreeMap<Integer, Token> tmToken = new TreeMap<Integer, Token>();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		for (Token token : tokens)
			tmToken.put(token.getEnd(), token);

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
					lastTense = Tense.PRESENTFUTURE;
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = Tense.PAST;
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(coveredText).matches()) {
						lastTense = Tense.FUTURE;
						LOG.debug("this tense: {}", lastTense);
					}
				}
				if (coveredText.equals("since") || coveredText.equals("depuis")) {
					lastTense = Tense.PAST;
					LOG.debug("this tense: {}", lastTense);
				}
			}
			if (lastTense == null && ent.getKey() > timex.getEnd()) {
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
					lastTense = Tense.PRESENTFUTURE;
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = Tense.PAST;
					LOG.debug("this tense: {}", lastTense);
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(token.getCoveredText()).matches()) {
						lastTense = Tense.FUTURE;
						LOG.debug("this tense: {}", lastTense);
					}
				}
			}
		}
		// check for double POS Constraints (not included in the rule language, yet) TODO
		// VHZ VNN and VHZ VNN and VHP VNN and VBP VVN
		String prevPos = "";
		Tense longTense = null;
		if (lastTense == Tense.PRESENTFUTURE) {
			for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
				if (ent.getKey() < timex.getBegin()) {
					Token token = ent.getValue();
					String pos = token.getPos();
					if ("VHZ".equals(prevPos) || "VBZ".equals(prevPos) || "VHP".equals(prevPos) || "VBP".equals(prevPos) || prevPos.equals("VER:pres")) {
						if ("VVN".equals(pos) || "VER:pper".equals(pos)) {
							String covered = token.getCoveredText();
							if (!(covered.equals("expected")) && !(covered.equals("scheduled"))) {
								lastTense = longTense = Tense.PAST;
								LOG.debug("this tense: {}", lastTense);
							}
						}
					}
					prevPos = pos;
				}
				if (longTense == null && ent.getKey() > timex.getEnd()) {
					Token token = ent.getValue();
					if ("VHZ".equals(prevPos) || "VBZ".equals(prevPos) || "VHP".equals(prevPos) || "VBP".equals(prevPos) || "VER:pres".equals(prevPos)) {
						if ("VVN".equals(token.getPos()) || "VER:pper".equals(token.getPos())) {
							String covered = token.getCoveredText();
							if (!(covered.equals("expected")) && !(covered.equals("scheduled"))) {
								lastTense = longTense = Tense.PAST;
								LOG.debug("this tense: {}", lastTense);
							}
						}
					}
					prevPos = token.getPos();
				}
			}
		}
		// French: VER:pres VER:pper
		if (lastTense == Tense.PAST) {
			for (Map.Entry<Integer, Token> ent : tmToken.entrySet()) {
				if (ent.getKey() < timex.getBegin()) {
					Token token = ent.getValue();
					String pos = token.getPos();
					if ("VER:pres".equals(prevPos) && "VER:pper".equals(pos)) {
						if (PREVUE_ENVISAGEE.matcher(token.getCoveredText()).matches()) {
							lastTense = longTense = Tense.FUTURE;
							LOG.debug("this tense: {}", lastTense);
						}
					}
					prevPos = pos;
				}
				if (longTense == null) {
					if (ent.getKey() > timex.getEnd()) {
						Token token = ent.getValue();
						String pos = token.getPos();
						if ("VER:pres".equals(prevPos) && "VER:pper".equals(pos)) {
							if (PREVUE_ENVISAGEE.matcher(token.getCoveredText()).matches()) {
								lastTense = longTense = Tense.FUTURE;
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
}
