package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * This class contains methods that work with the dependence of a subject with its surrounding data; namely via the jcas element or a subset list.
 * 
 * @author jannik stroetgen
 */
public class ContextAnalyzer {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ContextAnalyzer.class);

	public static enum Tense {
		PRESENTFUTURE, PAST, FUTURE
	}

	private static final Pattern BC_TWO_DIGITS = Pattern.compile("^(?:BC)?[0-9][0-9]");
	private static final Pattern BC_THREE_DIGITS = Pattern.compile("^(?:BC)?[0-9][0-9][0-9]");
	private static final Pattern BC_FOUR_DIGITS = Pattern.compile("^(?:BC)?[0-9][0-9][0-9][0-9]");
	private static final Pattern BC_YEAR_MON = Pattern.compile("^(?:BC)?[0-9][0-9][0-9][0-9]-[0-9][0-9]");
	private static final Pattern YEAR_MON = Pattern.compile("^[0-9][0-9][0-9][0-9]-[0-9][0-9]");
	private static final Pattern YEAR_MON_WK = Pattern.compile("^[0-9][0-9][0-9][0-9]-W[0-9][0-9]");
	private static final Pattern YEAR_MON_DAY = Pattern.compile("^([0-9][0-9][0-9][0-9])-([0-9][0-9])-([0-9][0-9])");
	private static final Pattern YEAR_QUARTER = Pattern.compile("^[0-9][0-9][0-9][0-9]-Q[1234]");
	private static final Pattern YEAR_SEASON = Pattern.compile("^[0-9][0-9][0-9][0-9]-(SP|SU|FA|WI)");

	private static final Pattern PREVUE_ENVISAGEE = Pattern.compile("^(?:prévue?s?|envisagée?s?)$");

	/**
	 * The value of the x of the last mentioned Timex is calculated.
	 * 
	 * Within the same sentence, prefer a longer timex (e.g. a day vs. a year).
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
		final int t_i_begin = t_i.getBegin();

		String bestrep = null, bestin = null;
		int bestsen = -1, besti = -1;
		for (int j = i - 1; j >= 0; --j) {
			Timex3 timex = linearDates.get(j);
			if (j < besti - 5 || bestrep != null && timex.getSentId() < bestsen - 1)
				break; // Don't go further back.
			// check that the two timexes to compare do not have the same offset:
			if (t_i_begin == timex.getBegin())
				continue;
			String value = timex.getTimexValue();
			if (bestrep != null && value.length() <= bestin.length())
				continue; // Only try to find more precise dates.
			if (value.contains("funcDate"))
				continue;
			String rep = func.apply(value);
			if (rep != null) {
				bestrep = rep;
				bestin = value;
				bestsen = timex.getSentId();
				// We don't care beyond month resolution, or we don't have sentences.
				if (value.length() >= 6 || bestsen == 0)
					break;
			}
		}
		// If we did not find in the same sentence, try also looking forward a little bit
		final int curSen = t_i.getSentId();
		if (besti != curSen && curSen > 0) {
			final int t_i_end = t_i.getEnd();
			int end = Math.min(i + 2, linearDates.size());
			for (int j = i + 1; j < end; j++) {
				Timex3 timex = linearDates.get(j);
				if (bestrep != null && timex.getSentId() > curSen)
					break; // Don't go further forward.
				// check that the two timexes to compare do not have the same offset:
				if (t_i_end > timex.getBegin())
					continue;
				String value = timex.getTimexValue();
				if (bestrep != null && value.length() <= bestin.length())
					continue; // Only try to find more precise dates.
				if (value.contains("funcDate"))
					continue;
				String rep = func.apply(value);
				if (rep != null) {
					bestrep = rep;
					bestin = value;
					bestsen = timex.getSentId();
					// We don't care beyond month resolution, or we don't have sentences.
					if (value.length() >= 6 || bestsen == 0)
						break;
				}
			}
		}
		return bestrep != null ? bestrep : "";
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
		Matcher m = BC_TWO_DIGITS.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? m.group(0) : null);
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
		Matcher m = BC_THREE_DIGITS.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? m.group(0) : null);
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
		Matcher m = BC_FOUR_DIGITS.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? m.group(0) : null);
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
		final Matcher m = BC_FOUR_DIGITS.matcher("");
		// TODO: return group instead of value?
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? value : null);
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
		Matcher m = BC_YEAR_MON.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? m.group(0) : null);
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
		Matcher m = YEAR_MON.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? m.group(0) : null);
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
		final Matcher m = YEAR_MON_DAY.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? value.substring(0, 10) : null);
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
		Matcher m1 = YEAR_MON_DAY.matcher("");
		Matcher m2 = YEAR_MON_WK.matcher("");
		return getLastMentionedX(linearDates, i, value -> //
		m1.reset(value).find() ? (m1.group(1) + "-W" + DateCalculator.getWeekOfDate(m1.group(0))) : //
				m2.reset(value).find() ? value /* group? */ : null //
		);
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
		final Matcher m1 = YEAR_MON.matcher("");
		final Matcher m2 = YEAR_QUARTER.matcher("");
		return getLastMentionedX(linearDates, i, value -> {
			if (m1.reset(value).find()) {
				NormalizationManager nm = NormalizationManager.getInstance(language, true);
				String month = value.substring(5, 7);
				String quarter = nm.getFromNormMonthInQuarter(month);
				if (quarter == null)
					quarter = "1";
				return value.substring(0, 4) + "-Q" + quarter;
			}
			return m2.reset(value).find() ? value.substring(0, 7) : null;
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
		final Matcher m = YEAR_QUARTER.matcher("");
		return getLastMentionedX(linearDates, i, value -> m.reset(value).find() ? value.substring(0, 7) : null);
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
		final Matcher m1 = YEAR_MON.matcher("");
		final Matcher m2 = YEAR_SEASON.matcher("");
		return getLastMentionedX(linearDates, i, value -> {
			if (m1.reset(value).find()) {
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
			if (m2.reset(value).find())
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

		ArrayList<Token> tmToken = getCloseTokens(timex, jcas);

		// Get the last VERB token
		for (Token token : tmToken) {
			tokenCounter++;
			if (token.getEnd() < timex.getBegin()) {
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
		for (Token token : tmToken) {
			tokenCounter++;
			if (nextTense == null) {
				if (token.getEnd() > timex.getEnd()) {
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
		ArrayList<Token> tmToken = getCloseTokens(timex, jcas);

		// Get the last VERB token
		for (Token token : tmToken) {
			if (token.getEnd() < timex.getBegin()) {
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
				} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
					lastTense = Tense.PAST;
				} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
					if (tenseWord4Future.reset(coveredText).matches()) {
						lastTense = Tense.FUTURE;
					}
				}
				if (coveredText.equals("since") || coveredText.equals("depuis")) {
					lastTense = Tense.PAST;
				}
			}
			if (lastTense == null && token.getEnd() > timex.getEnd()) {
				String pos = token.getPos();

				if (LOG.isTraceEnabled()) {
					LOG.trace("GET NEXT TENSE: string:" + token.getCoveredText() + " pos:" + pos);
					LOG.trace("hmAllRePattern.containsKey(tensePos4PresentFuture):" + tensePos4PresentFuture.pattern().pattern());
					LOG.trace("hmAllRePattern.containsKey(tensePos4Future):" + tensePos4Future.pattern().pattern());
					LOG.trace("hmAllRePattern.containsKey(tensePos4Past):" + tensePos4Past.pattern().pattern());
					LOG.trace("CHECK TOKEN:" + pos);
				}

				if (pos != null) {
					if (tensePos4PresentFuture != null && tensePos4PresentFuture.reset(pos).matches()) {
						lastTense = Tense.PRESENTFUTURE;
					} else if (tensePos4Past != null && tensePos4Past.reset(pos).matches()) {
						lastTense = Tense.PAST;
					} else if (tensePos4Future != null && tensePos4Future.reset(pos).matches()) {
						if (tenseWord4Future.reset(token.getCoveredText()).matches())
							lastTense = Tense.FUTURE;
					}
				}
			}
			if (lastTense != null)
				LOG.trace("this tense: {} {}", token.getCoveredText(), lastTense);
		}
		// check for double POS Constraints (not included in the rule language, yet) TODO
		// VHZ VNN and VHZ VNN and VHP VNN and VBP VVN
		String prevPos = "";
		Tense longTense = null;
		if (lastTense == Tense.PRESENTFUTURE) {
			for (Token token : tmToken) {
				if (token.getEnd() < timex.getBegin()) {
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
				if (longTense == null && token.getEnd() > timex.getEnd()) {
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
			for (Token token : tmToken) {
				if (token.getEnd() < timex.getBegin()) {
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
					if (token.getEnd() > timex.getEnd()) {
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

	/**
	 * Get the tokens close to the given timex (i.e. the same sentence).
	 *
	 * @param timex
	 *                Timex
	 * @param jcas
	 *                Cas
	 * @return Tokens, sorted by end.
	 */
	private static ArrayList<Token> getCloseTokens(Timex3 timex, JCas jcas) {
		// Get the sentence
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		Sentence s = null;
		for (FSIterator<Sentence> iterSentence = sentences.iterator(); iterSentence.hasNext();) {
			s = iterSentence.next();
			if (s.getBegin() <= timex.getBegin() && s.getEnd() >= timex.getEnd())
				break;
		}

		// Get the tokens
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		FSIterator<Token> iter = (s != null) ? tokens.subiterator(s) : tokens.iterator();
		ArrayList<Token> tmToken = new ArrayList<Token>();
		while (iter.hasNext())
			tmToken.add(iter.next());
		tmToken.sort(SORT_TOKENS);
		return tmToken;
	}

	/**
	 * Sort tokens by the token end.
	 */
	private static final Comparator<Token> SORT_TOKENS = new Comparator<Token>() {
		public int compare(Token o1, Token o2) {
			return Integer.compare(o1.getEnd(), o2.getEnd());
		}
	};
}
