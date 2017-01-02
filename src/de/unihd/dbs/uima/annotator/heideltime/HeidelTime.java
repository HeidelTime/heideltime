/*
 * HeidelTime.java
 * 
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, Heidelberg University. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU General Public License.
 * 
 * author: Jannik Strötgen
 * email:  stroetgen@uni-hd.de
 * 
 * HeidelTime is a multilingual, cross-domain temporal tagger.
 * For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
 */

package de.unihd.dbs.uima.annotator.heideltime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.ProcessorManager.Priority;
import de.unihd.dbs.uima.annotator.heideltime.processors.TemponymPostprocessing;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RuleManager;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DurationSimplification;
import de.unihd.dbs.uima.annotator.heideltime.utilities.ChineseNumbers;
import de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer;
import de.unihd.dbs.uima.annotator.heideltime.utilities.LocaleException;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * HeidelTime finds temporal expressions and normalizes them according to the TIMEX3 TimeML annotation standard.
 * 
 * @author jannik stroetgen
 * 
 */
public class HeidelTime extends JCasAnnotator_ImplBase {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(HeidelTime.class);

	private static final Pattern UNDEF_PATTERN = Pattern.compile("^(UNDEF-(this|REFUNIT|REF)-(.*?)-(MINUS|PLUS)-([0-9]+)).*");

	private static final Pattern UNDEF_MONTH = Pattern
			.compile("(UNDEF-(last|this|next)-(january|february|march|april|may|june|july|august|september|october|november|december)(-([0-9][0-9]))?).*");

	private static final Pattern UNDEF_SEASON = Pattern.compile("^(UNDEF-(last|this|next)-(SP|SU|FA|WI)).*");

	private static final Pattern UNDEF_WEEKDAY = Pattern.compile("^(UNDEF-(last|this|next|day)-(monday|tuesday|wednesday|thursday|friday|saturday|sunday)).*");

	private static final Pattern EIGHT_DIGITS = Pattern.compile("^\\d\\d\\d\\d\\d\\d\\d\\d$");

	private static final Pattern TWO_DIGITS = Pattern.compile("^\\d\\d$");

	// PROCESSOR MANAGER
	private ProcessorManager procMan = new ProcessorManager();

	// COUNTER (how many timexes added to CAS? (finally)
	public int timex_counter = 0;
	public int timex_counter_global = 0;

	// FLAG (for historic expressions referring to BC)
	public boolean flagHistoricDates = false;

	// COUNTER FOR TIMEX IDS
	private int timexID = 0;

	// INPUT PARAMETER HANDLING WITH UIMA
	private String PARAM_LANGUAGE = "Language";
	// supported languages (2012-05-19): english, german, dutch, englishcoll,
	// englishsci
	private String PARAM_TYPE_TO_PROCESS = "Type";
	// chosen locale parameter name
	private String PARAM_LOCALE = "locale";
	// supported types (2012-05-19): news (english, german, dutch), narrative
	// (english, german, dutch), colloquial
	private Language language = Language.ENGLISH;
	private String typeToProcess = "news";

	// INPUT PARAMETER HANDLING WITH UIMA (which types shall be extracted)
	private String PARAM_DATE = "Date";
	private String PARAM_TIME = "Time";
	private String PARAM_DURATION = "Duration";
	private String PARAM_SET = "Set";
	private String PARAM_TEMPONYMS = "Temponym";
	private String PARAM_GROUP = "ConvertDurations";
	private boolean find_dates = true;
	private boolean find_times = true;
	private boolean find_durations = true;
	private boolean find_sets = true;
	private boolean find_temponyms = false;
	private boolean group_gran = true;
	// FOR DEBUGGING PURPOSES (IF FALSE)
	private boolean deleteOverlapping = true;

	// To profile regular expression matching.
	private static final boolean PROFILE_REGEXP = false;

	private HashMap<String, Long> profileData = PROFILE_REGEXP ? new HashMap<String, Long>() : null;

	/**
	 * @see AnalysisComponent#initialize(UimaContext)
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/////////////////////////////////
		// DEBUGGING PARAMETER SETTING //
		/////////////////////////////////
		this.deleteOverlapping = true;

		/////////////////////////////////
		// HANDLE LOCALE //
		/////////////////////////////////
		String requestedLocale = (String) aContext.getConfigParameterValue(PARAM_LOCALE);
		if (requestedLocale == null || requestedLocale.length() == 0) {
			// if the PARAM_LOCALE setting was left empty,
			Locale.setDefault(Locale.UK);
			// use the ISO8601-adhering UK locale (equivalent to "en_GB")
		} else { // otherwise, check if the desired locale exists in the JVM's
			 // available locale repertoire
			try {
				Locale locale = DateCalculator.getLocaleFromString(requestedLocale);
				Locale.setDefault(locale); // sets it for the entire JVM
							   // session
			} catch (LocaleException e) {
				StringBuilder localesString = new StringBuilder();
				localesString.append("Supplied locale parameter couldn't be resolved to a working locale. Try one of these:");
				for (Locale l : Locale.getAvailableLocales()) {
					// list all available locales
					localesString.append(l.toString()).append(' ');
				}
				LOG.error(localesString.toString());
				System.exit(1);
			}
		}

		//////////////////////////////////
		// GET CONFIGURATION PARAMETERS //
		//////////////////////////////////
		language = Language.getLanguageFromString((String) aContext.getConfigParameterValue(PARAM_LANGUAGE));

		typeToProcess = (String) aContext.getConfigParameterValue(PARAM_TYPE_TO_PROCESS);
		find_dates = (Boolean) aContext.getConfigParameterValue(PARAM_DATE);
		find_times = (Boolean) aContext.getConfigParameterValue(PARAM_TIME);
		find_durations = (Boolean) aContext.getConfigParameterValue(PARAM_DURATION);
		find_sets = (Boolean) aContext.getConfigParameterValue(PARAM_SET);
		find_temponyms = (Boolean) aContext.getConfigParameterValue(PARAM_TEMPONYMS);
		group_gran = (Boolean) aContext.getConfigParameterValue(PARAM_GROUP);
		////////////////////////////////////////////////////////////
		// READ NORMALIZATION RESOURCES FROM FILES AND STORE THEM //
		////////////////////////////////////////////////////////////
		NormalizationManager.getInstance(language, find_temponyms);

		//////////////////////////////////////////////////////
		// READ PATTERN RESOURCES FROM FILES AND STORE THEM //
		//////////////////////////////////////////////////////
		RePatternManager.getInstance(language, find_temponyms);

		///////////////////////////////////////////////////
		// READ RULE RESOURCES FROM FILES AND STORE THEM //
		///////////////////////////////////////////////////
		RuleManager.getInstance(language, find_temponyms);

		///////////////////////////////////////////////////////////////////
		// SUBPROCESSOR CONFIGURATION. REGISTER YOUR OWN PROCESSORS HERE //
		///////////////////////////////////////////////////////////////////
		procMan.registerProcessor(de.unihd.dbs.uima.annotator.heideltime.processors.HolidayProcessor.class.getName());
		procMan.registerProcessor(de.unihd.dbs.uima.annotator.heideltime.processors.DecadeProcessor.class.getName());
		procMan.initializeAllProcessors(aContext);

		/////////////////////////////
		// PRINT WHAT WILL BE DONE //
		/////////////////////////////
		if (find_dates)
			LOG.debug("Getting Dates...");
		if (find_times)
			LOG.debug("Getting Times...");
		if (find_durations)
			LOG.debug("Getting Durations...");
		if (find_sets)
			LOG.debug("Getting Sets...");
		if (find_temponyms)
			LOG.debug("Getting Temponyms...");
	}

	/**
	 * @see JCasAnnotator_ImplBase#process(JCas)
	 */
	public void process(JCas jcas) {
		// check whether a given DCT (if any) is of the correct format and if not,
		// skip this call
		if (!isValidDCT(jcas)) {
			LOG.error("The reader component of this workflow has set an incorrect DCT." + " HeidelTime expects either \"YYYYMMDD\" or \"YYYY-MM-DD...\". This document was skipped.");
			return;
		}

		// run preprocessing processors
		procMan.executeProcessors(jcas, Priority.PREPROCESSING);

		RuleManager rulem = RuleManager.getInstance(language, find_temponyms);

		timexID = 1; // reset counter once per document processing

		timex_counter = 0;

		flagHistoricDates = false;

		boolean documentTypeNarrative = typeToProcess.equals("narrative") || typeToProcess.equals("narratives");

		////////////////////////////////////////////
		// CHECK SENTENCE BY SENTENCE FOR TIMEXES //
		////////////////////////////////////////////
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		/*
		 * check if the pipeline has annotated any sentences. if not, heideltime can't do any work, will return from process() with a warning message.
		 */
		if (sentences.size() == 0) {
			LOG.error("HeidelTime has not found any sentence tokens in this document. " + "HeidelTime needs sentence tokens tagged by a preprocessing UIMA analysis engine to "
					+ "do its work. Please check your UIMA workflow and add an analysis engine that creates " + "these sentence tokens.");
		}

		for (Sentence s : sentences) {
			try {
				if (find_dates)
					findTimexes("DATE", rulem.getHmDatePattern(), rulem.getHmDateOffset(), rulem.getHmDateNormalization(), s, jcas);
				if (find_times)
					findTimexes("TIME", rulem.getHmTimePattern(), rulem.getHmTimeOffset(), rulem.getHmTimeNormalization(), s, jcas);

				/*
				 * check for historic dates/times starting with BC to check if post-processing step is required
				 */
				if (documentTypeNarrative) {
					AnnotationIndex<Timex3> dates = jcas.getAnnotationIndex(Timex3.type);
					for (Timex3 t : dates) {
						if (t.getTimexValue().startsWith("BC")) {
							flagHistoricDates = true;
							break;
						}
					}
				}

				if (find_sets)
					findTimexes("SET", rulem.getHmSetPattern(), rulem.getHmSetOffset(), rulem.getHmSetNormalization(), s, jcas);
				if (find_durations)
					findTimexes("DURATION", rulem.getHmDurationPattern(), rulem.getHmDurationOffset(), rulem.getHmDurationNormalization(), s, jcas);
				if (find_temponyms)
					findTimexes("TEMPONYM", rulem.getHmTemponymPattern(), rulem.getHmTemponymOffset(), rulem.getHmTemponymNormalization(), s, jcas);
			} catch (NullPointerException npe) {
				LOG.error("HeidelTime's execution has been interrupted by an exception that " + "is likely rooted in faulty normalization resource files. "
						+ "Please consider opening an issue report containing the following "
						+ "information at our GitHub project issue tracker (if possible, also enable debug logging): "
						+ "https://github.com/HeidelTime/heideltime/issues - Thanks!", npe);
				LOG.error("Sentence [" + s.getBegin() + "-" + s.getEnd() + "]: " + s.getCoveredText());
				LOG.error("Language: " + language);
				// LOG.error("Re-running this sentence with DEBUGGING
				// enabled...");
				// TODO: add a flag to force-log debugging information?
			}
		}

		/*
		 * kick out some overlapping expressions
		 */
		if (deleteOverlapping)
			deleteOverlappingTimexesPreprocessing(jcas);

		/*
		 * specify ambiguous values, e.g.: specific year for date values of format UNDEF-year-01-01; specific month for values of format UNDEF-last-month
		 */
		specifyAmbiguousValues(jcas);

		// disambiguate historic dates
		// check dates without explicit hints to AD or BC if they might refer to
		// BC dates
		if (flagHistoricDates)
			try {
				disambiguateHistoricDates(jcas);
			} catch (Exception e) {
				LOG.error("Something went wrong disambiguating historic dates.", e);
			}

		if (find_temponyms)
			TemponymPostprocessing.handleIntervals(jcas);

		/*
		 * kick out the rest of the overlapping expressions
		 */
		if (deleteOverlapping)
			deleteOverlappedTimexesPostprocessing(jcas);

		// run arbitrary processors
		procMan.executeProcessors(jcas, Priority.ARBITRARY);

		// remove invalid timexes
		removeInvalids(jcas);

		// run postprocessing processors
		procMan.executeProcessors(jcas, Priority.POSTPROCESSING);

		timex_counter_global += timex_counter;
		LOG.info("Number of Timexes added to CAS: {} (global: {})", timex_counter, timex_counter_global);
	}

	/**
	 * Add timex annotation to CAS object.
	 * 
	 * @param timexType
	 * @param begin
	 * @param end
	 * @param timexValue
	 * @param timexId
	 * @param foundByRule
	 * @param jcas
	 */
	public void addTimexAnnotation(String timexType, int begin, int end, Sentence sentence, String timexValue, String timexQuant, String timexFreq, String timexMod, String emptyValue,
			String timexId, String foundByRule, JCas jcas) {

		Timex3 annotation = new Timex3(jcas);
		annotation.setBegin(begin);
		annotation.setEnd(end);

		annotation.setFilename(sentence.getFilename());
		annotation.setSentId(sentence.getSentenceId());

		annotation.setEmptyValue(emptyValue);

		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		StringBuilder allTokIds = new StringBuilder();
		for (FSIterator<Token> iterToken = tokens.subiterator(sentence); iterToken.hasNext();) {
			Token tok = iterToken.next();
			if (tok.getBegin() <= begin && tok.getEnd() > begin) {
				annotation.setFirstTokId(tok.getTokenId());
				allTokIds.setLength(0);
				allTokIds.append("BEGIN<-->").append(tok.getTokenId());
			}
			if ((tok.getBegin() > begin) && (tok.getEnd() <= end))
				allTokIds.append("<-->").append(tok.getTokenId());
		}
		annotation.setAllTokIds(allTokIds.toString());
		annotation.setTimexType(timexType);
		annotation.setTimexValue(timexValue);
		annotation.setTimexId(timexId);
		annotation.setFoundByRule(foundByRule);
		if (timexType.equals("DATE") || timexType.equals("TIME")) {
			if (timexValue.startsWith("X") || timexValue.startsWith("UNDEF")) {
				annotation.setFoundByRule(foundByRule + "-relative");
			} else {
				annotation.setFoundByRule(foundByRule + "-explicit");
			}
		}
		if (timexQuant != null)
			annotation.setTimexQuant(timexQuant);
		if (timexFreq != null)
			annotation.setTimexFreq(timexFreq);
		if (timexMod != null)
			annotation.setTimexMod(timexMod);
		annotation.addToIndexes();
		this.timex_counter++;

		if (LOG.isDebugEnabled()) {
			LOG.debug(annotation.getTimexId() + "EXTRACTION PHASE:   " + " found by:" + annotation.getFoundByRule() + " text:" + annotation.getCoveredText());
			LOG.debug(annotation.getTimexId() + "NORMALIZATION PHASE:" + " found by:" + annotation.getFoundByRule() + " text:" + annotation.getCoveredText() + " value:"
					+ annotation.getTimexValue());
		}
	}

	/**
	 * Postprocessing: Check dates starting with "0" which were extracted without explicit "AD" hints if it is likely that they refer to the respective date BC
	 * 
	 * @param jcas
	 */
	public void disambiguateHistoricDates(JCas jcas) {

		// build up a list with all found TIMEX expressions
		List<Timex3> linearDates = new ArrayList<Timex3>();
		AnnotationIndex<Timex3> annotations = jcas.getAnnotationIndex(Timex3.type);
		FSIterator<Timex3> iterTimex = annotations.iterator();

		// Create List of all Timexes of types "date" and "time"
		while (iterTimex.hasNext()) {
			Timex3 timex = iterTimex.next();
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME")) {
				linearDates.add(timex);
			}
		}

		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		for (int i = 1; i < linearDates.size(); i++) {
			Timex3 t_i = (Timex3) linearDates.get(i);
			String value_i = t_i.getTimexValue();
			String newValue = value_i;
			boolean change = false;
			if (!(t_i.getFoundByRule().contains("-BCADhint"))) {
				if (value_i.startsWith("0")) {
					int offset = 1, counter = 1;
					do {
						if ((i == 1 || (i > 1 && !change)) && linearDates.get(i - offset).getTimexValue().startsWith("BC")) {
							if (value_i.length() > 1) {
								if ((linearDates.get(i - offset).getTimexValue().startsWith("BC" + value_i.substring(0, 2))) || (linearDates.get(i - offset)
										.getTimexValue().startsWith("BC" + String.format("%02d", (Integer.parseInt(value_i.substring(0, 2)) + 1))))) {
									if (((value_i.startsWith("00")) && (linearDates.get(i - offset).getTimexValue().startsWith("BC00")))
											|| ((value_i.startsWith("01")) && (linearDates.get(i - offset).getTimexValue().startsWith("BC01")))) {
										if ((value_i.length() > 2) && (linearDates.get(i - offset).getTimexValue().length() > 4)) {
											if (Integer.parseInt(value_i.substring(0, 3)) <= Integer
													.parseInt(linearDates.get(i - offset).getTimexValue().substring(2, 5))) {
												newValue = "BC" + value_i;
												change = true;
												if (LOG.isDebugEnabled())
													LOG.debug("DisambiguateHistoricDates: " + value_i + " to " + newValue + ". Expression "
															+ t_i.getCoveredText() + " due to "
															+ linearDates.get(i - offset).getCoveredText());
											}
										}
									} else {
										newValue = "BC" + value_i;
										change = true;
										if (LOG.isDebugEnabled())
											LOG.debug("DisambiguateHistoricDates: " + value_i + " to " + newValue + ". Expression " + t_i.getCoveredText()
													+ " due to " + linearDates.get(i - offset).getCoveredText());
									}
								}
							}
						}

						if ((linearDates.get(i - offset).getTimexType().equals("TIME") || linearDates.get(i - offset).getTimexType().equals("DATE"))
								&& (linearDates.get(i - offset).getTimexValue().matches("^\\d.*"))) {
							counter++;
						}
					} while (counter < 5 && ++offset < i);
				}
			}
			if (!(newValue.equals(value_i))) {
				t_i.removeFromIndexes();
				LOG.debug("DisambiguateHistoricDates: value changed to BC");

				t_i.setTimexValue(newValue);
				t_i.addToIndexes();
				linearDates.set(i, t_i);
			}
		}
	}

	/**
	 * Postprocessing: Remove invalid timex expressions. These are already marked as invalid: timexValue().equals("REMOVE")
	 * 
	 * @param jcas
	 */
	public void removeInvalids(JCas jcas) {

		/*
		 * Iterate over timexes and add invalids to HashSet (invalids cannot be removed directly since iterator is used)
		 */
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		HashSet<Timex3> hsTimexToRemove = new HashSet<Timex3>();
		for (Timex3 timex : timexes) {
			if (timex.getTimexValue().equals("REMOVE")) {
				hsTimexToRemove.add(timex);
			}
		}

		// remove invalids, finally
		for (Timex3 timex3 : hsTimexToRemove) {
			timex3.removeFromIndexes();
			this.timex_counter--;
			if (LOG.isDebugEnabled())
				LOG.debug(timex3.getTimexId() + " REMOVING PHASE: " + "found by:" + timex3.getFoundByRule() + " text:" + timex3.getCoveredText() + " value:" + timex3.getTimexValue());
		}
	}

	@SuppressWarnings("unused")
	public String specifyAmbiguousValuesString(String ambigString, Timex3 t_i, int i, List<Timex3> linearDates, JCas jcas) {
		NormalizationManager norm = NormalizationManager.getInstance(language, find_temponyms);

		// //////////////////////////////////////
		// IS THERE A DOCUMENT CREATION TIME? //
		// //////////////////////////////////////
		boolean dctAvailable = false;

		// ////////////////////////////
		// DOCUMENT TYPE TO PROCESS //
		// //////////////////////////
		boolean documentTypeNews = typeToProcess.equals("news");
		boolean documentTypeNarrative = typeToProcess.equals("narrative") || typeToProcess.equals("narratives");
		boolean documentTypeColloquial = typeToProcess.equals("colloquial");
		boolean documentTypeScientific = typeToProcess.equals("scientific");

		// get the dct information
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

		// ////////////////////////////////////////////
		// INFORMATION ABOUT DOCUMENT CREATION TIME //
		// ////////////////////////////////////////////
		AnnotationIndex<Dct> dcts = jcas.getAnnotationIndex(Dct.type);
		FSIterator<Dct> dctIter = dcts.iterator();
		if (dctIter.hasNext()) {
			dctAvailable = true;
			dctValue = dctIter.next().getValue();
			// year, month, day as mentioned in the DCT
			if (EIGHT_DIGITS.matcher(dctValue).matches()) {
				dctCentury = Integer.parseInt(dctValue.substring(0, 2));
				dctYear = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth = Integer.parseInt(dctValue.substring(4, 6));
				dctDay = Integer.parseInt(dctValue.substring(6, 8));

				if (LOG.isDebugEnabled()) {
					LOG.debug("dctCentury:" + dctCentury);
					LOG.debug("dctYear:" + dctYear);
					LOG.debug("dctDecade:" + dctDecade);
					LOG.debug("dctMonth:" + dctMonth);
					LOG.debug("dctDay:" + dctDay);
				}
			} else {
				dctCentury = Integer.parseInt(dctValue.substring(0, 2));
				dctYear = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth = Integer.parseInt(dctValue.substring(5, 7));
				dctDay = Integer.parseInt(dctValue.substring(8, 10));

				if (LOG.isDebugEnabled()) {
					LOG.debug("dctCentury:" + dctCentury);
					LOG.debug("dctYear:" + dctYear);
					LOG.debug("dctDecade:" + dctDecade);
					LOG.debug("dctMonth:" + dctMonth);
					LOG.debug("dctDay:" + dctDay);
				}
			}
			dctQuarter = "Q" + norm.getFromNormMonthInQuarter(norm.normNumber(dctMonth));
			dctHalf = (dctMonth <= 6) ? "H1" : "H2";

			// season, week, weekday, have to be calculated
			dctSeason = norm.getFromNormMonthInSeason(norm.normNumber(dctMonth));
			dctWeekday = DateCalculator.getWeekdayOfDate(dctYear + "-" + norm.normNumber(dctMonth) + "-" + norm.normNumber(dctDay));
			dctWeek = DateCalculator.getWeekOfDate(dctYear + "-" + norm.normNumber(dctMonth) + "-" + norm.normNumber(dctDay));

			if (LOG.isDebugEnabled()) {
				LOG.debug("dctQuarter:" + dctQuarter);
				LOG.debug("dctSeason:" + dctSeason);
				LOG.debug("dctWeekday:" + dctWeekday);
				LOG.debug("dctWeek:" + dctWeek);
			}
		} else {
			LOG.debug("No DCT available...");
		}

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
			String newYearValue = Integer.toString(dctYear);
			// vi has month (ignore day)
			if (viHasMonth && !viHasSeason) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
						// if dct-month is larger than vi-month,
						// than add 1 to dct-year
						if (dctMonth > viThisMonth) {
							int intNewYear = dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						// if dct-month is smaller than vi month,
						// than substrate 1 from dct-year
						if (dctMonth < viThisMonth) {
							int intNewYear = dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
				}
			}
			// vi has quaurter
			if (viHasQuarter) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
						if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
							int intNewYear = dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
							int intNewYear = dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense.equals("")) {
						if (documentTypeColloquial) {
							// IN COLLOQUIAL: future temporal
							// expressions
							if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dctYear + 1;
								newYearValue = Integer.toString(intNewYear);
							}
						} else {
							// IN NEWS: past temporal
							// expressions
							if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dctYear - 1;
								newYearValue = Integer.toString(intNewYear);
							}
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
				}
			}
			// vi has half
			if (viHasHalf) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					// Tense is FUTURE
					if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
						if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
							int intNewYear = dctYear + 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// Tense is PAST
					if (last_used_tense.equals("PAST")) {
						if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
							int intNewYear = dctYear - 1;
							newYearValue = Integer.toString(intNewYear);
						}
					}
					// IF NO TENSE IS FOUND
					if (last_used_tense.equals("")) {
						if (documentTypeColloquial) {
							// IN COLLOQUIAL: future temporal
							// expressions
							if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dctYear + 1;
								newYearValue = Integer.toString(intNewYear);
							}
						} else {
							// IN NEWS: past temporal
							// expressions
							if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dctYear - 1;
								newYearValue = Integer.toString(intNewYear);
							}
						}
					}
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
				}
			}

			// vi has season
			if (!viHasMonth && !viHasDay && viHasSeason) {
				// TODO check tenses?
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					newYearValue = Integer.toString(dctYear);
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
				}
			}
			// vi has week
			if (viHasWeek) {
				// WITH DOCUMENT CREATION TIME
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					newYearValue = Integer.toString(dctYear);
				}
				// WITHOUT DOCUMENT CREATION TIME
				else {
					newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
				}
			}

			// REPLACE THE UNDEF-YEAR WITH THE NEWLY CALCULATED YEAR AND ADD
			// TIMEX TO INDEXES
			if (newYearValue.equals("")) {
				valueNew = ambigString.replaceFirst("UNDEF-year", "XXXX");
			} else {
				valueNew = ambigString.replaceFirst("UNDEF-year", newYearValue);
			}
		}

		///////////////////////////////////////////////////
		// just century is unspecified (UNDEF-century86) //
		///////////////////////////////////////////////////
		else if (ambigString.startsWith("UNDEF-century")) {
			String newCenturyValue = Integer.toString(dctCentury);

			// NEWS and COLLOQUIAL DOCUMENTS
			if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable && !ambigString.equals("UNDEF-century")) {
				int viThisDecade = Integer.parseInt(ambigString.substring(13, 14));

				if (LOG.isDebugEnabled())
					LOG.debug("dctCentury" + dctCentury);

				newCenturyValue = Integer.toString(dctCentury);

				// Tense is FUTURE
				if (last_used_tense.equals("FUTURE") || last_used_tense.equals("PRESENTFUTURE")) {
					if (viThisDecade < dctDecade) {
						newCenturyValue = Integer.toString(dctCentury + 1);
					} else {
						newCenturyValue = Integer.toString(dctCentury);
					}
				}
				// Tense is PAST
				if (last_used_tense.equals("PAST")) {
					if (dctDecade < viThisDecade) {
						newCenturyValue = Integer.toString(dctCentury - 1);
					} else {
						newCenturyValue = Integer.toString(dctCentury);
					}
				}
			}
			// NARRATIVE DOCUMENTS
			else {
				newCenturyValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "century", language);
				if (!newCenturyValue.startsWith("BC")) {
					if (newCenturyValue.matches("^\\d\\d.*") && Integer.parseInt(newCenturyValue.substring(0, 2)) < 10) {
						newCenturyValue = "00";
					}
				} else {
					newCenturyValue = "00";
				}
			}
			if (newCenturyValue.equals("")) {
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
				if ((documentTypeScientific)) {
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
						String dateWithYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "dateYear", language);
						String year = dateWithYear;
						if (dateWithYear.equals("")) {
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
							int century = dctCentury;
							if (op.equals("MINUS")) {
								century = dctCentury - diff;
							} else if (op.equals("PLUS")) {
								century = dctCentury + diff;
							}
							valueNew = valueNew.replace(checkUndef, Integer.toString(century));
						} else {
							String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates, i, "century", language);
							if (lmCentury.equals("")) {
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
							int dctDecadeLong = Integer.parseInt(dctCentury + "" + dctDecade);
							int decade = dctDecadeLong;
							if (op.equals("MINUS")) {
								decade = dctDecadeLong - diff;
							} else if (op.equals("PLUS")) {
								decade = dctDecadeLong + diff;
							}
							valueNew = valueNew.replace(checkUndef, decade + "X");
						} else {
							String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates, i, "decade", language);
							if (lmDecade.equals("")) {
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
							int intValue = dctYear;
							if (op.equals("MINUS")) {
								intValue = dctYear - diff;
							} else if (op.equals("PLUS")) {
								intValue = dctYear + diff;
							}
							valueNew = valueNew.replace(checkUndef, Integer.toString(intValue));
						} else {
							String lmYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
							if (lmYear.equals("")) {
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
							int intYear = dctYear;
							int intQuarter = Integer.parseInt(dctQuarter.substring(1));
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
							String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter", language);
							if (lmQuarter.equals("")) {
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
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.normNumber(dctMonth), diff));
						} else {
							String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month", language);
							if (lmMonth.equals("")) {
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
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dctYear + "-W" + norm.normNumber(dctWeek), diff, language));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.normNumber(dctMonth) + "-" + dctDay, diff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dctCentury - 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates, i, "century", language);
					if (lmCentury.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XX");
					} else {
						lmCentury = DateCalculator.getXNextCentury(lmCentury, -1);
						valueNew = valueNew.replace(checkUndef, lmCentury);
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-century")) {
				String checkUndef = "UNDEF-this-century";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dctCentury));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates, i, "century", language);
					if (lmCentury.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmCentury);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-century")) {
				String checkUndef = "UNDEF-next-century";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, norm.normNumber(dctCentury + 1));
				} else {
					String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates, i, "century", language);
					if (lmCentury.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dctYear - 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates, i, "decade", language);
					if (lmDecade.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						lmDecade = DateCalculator.getXNextDecade(lmDecade, -1);
						valueNew = valueNew.replace(checkUndef, lmDecade);
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-decade")) {
				String checkUndef = "UNDEF-this-decade";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dctYear)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates, i, "decade", language);
					if (lmDecade.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmDecade);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-decade")) {
				String checkUndef = "UNDEF-next-decade";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, (Integer.toString(dctYear + 10)).substring(0, 3));
				} else {
					String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates, i, "decade", language);
					if (lmDecade.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, Integer.toString(dctYear - 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
					if (lmYear.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, Integer.toString(dctYear));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
					if (lmYear.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, Integer.toString(dctYear + 1));
				} else {
					String lmYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "year", language);
					if (lmYear.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.normNumber(dctMonth), -1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month", language);
					if (lmMonth.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, -1));
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-month")) {
				String checkUndef = "UNDEF-this-month";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dctYear + "-" + norm.normNumber(dctMonth));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month", language);
					if (lmMonth.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmMonth);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-month")) {
				String checkUndef = "UNDEF-next-month";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.normNumber(dctMonth), 1));
				} else {
					String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month", language);
					if (lmMonth.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.normNumber(dctMonth) + "-" + dctDay, -1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
					if (lmDay.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay, -1));
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-day")) {
				String checkUndef = "UNDEF-this-day";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dctYear + "-" + norm.normNumber(dctMonth) + "-" + norm.normNumber(dctDay));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
					if (lmDay.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.normNumber(dctMonth) + "-" + dctDay, 1));
				} else {
					String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
					if (lmDay.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dctYear + "-W" + norm.normNumber(dctWeek), -1, language));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates, i, "week", language);
					if (lmWeek.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(lmWeek, -1, language));
					}
				}
			} else if (ambigString.startsWith("UNDEF-this-week")) {
				String checkUndef = "UNDEF-this-week";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, dctYear + "-W" + norm.normNumber(dctWeek));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates, i, "week", language);
					if (lmWeek.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmWeek);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-week")) {
				String checkUndef = "UNDEF-next-week";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dctYear + "-W" + norm.normNumber(dctWeek), 1, language));
				} else {
					String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates, i, "week", language);
					if (lmWeek.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
					} else {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(lmWeek, 1, language));
					}
				}
			}

			// quarter
			else if (ambigString.startsWith("UNDEF-last-quarter")) {
				String checkUndef = "UNDEF-last-quarter";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					if (dctQuarter.equals("Q1")) {
						valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-Q4");
					} else {
						int newQuarter = Integer.parseInt(dctQuarter.substring(1, 2)) - 1;
						valueNew = valueNew.replace(checkUndef, dctYear + "-Q" + newQuarter);
					}
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter", language);
					if (lmQuarter.equals("")) {
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
					valueNew = valueNew.replace(checkUndef, dctYear + "-" + dctQuarter);
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter", language);
					if (lmQuarter.equals("")) {
						valueNew = valueNew.replace(checkUndef, "XXXX-QX");
					} else {
						valueNew = valueNew.replace(checkUndef, lmQuarter);
					}
				}
			} else if (ambigString.startsWith("UNDEF-next-quarter")) {
				String checkUndef = "UNDEF-next-quarter";
				if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
					if (dctQuarter.equals("Q4")) {
						valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-Q1");
					} else {
						int newQuarter = Integer.parseInt(dctQuarter.substring(1, 2)) + 1;
						valueNew = valueNew.replace(checkUndef, dctYear + "-Q" + newQuarter);
					}
				} else {
					String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter", language);
					if (lmQuarter.equals("")) {
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
						if ((dctMonth == newMonthInt) && (!(day == 0))) {
							if (dctDay > day) {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newMonth);
							}
						} else if (dctMonth <= newMonthInt) {
							valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newMonth);
						} else {
							valueNew = valueNew.replace(checkUndef, dctYear + "-" + newMonth);
						}
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month-with-details", language);
						if (lmMonth.equals("")) {
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
						valueNew = valueNew.replace(checkUndef, dctYear + "-" + newMonth);
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month-with-details", language);
						if (lmMonth.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmMonth.substring(0, 4) + "-" + newMonth);
						}
					}
				} else if (ltn.equals("next")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						// check day if dct-month and newMonth are
						// equal
						if ((dctMonth == newMonthInt) && (!(day == 0))) {
							if (dctDay < day) {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newMonth);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newMonth);
							}
						} else if (dctMonth >= newMonthInt) {
							valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newMonth);
						} else {
							valueNew = valueNew.replace(checkUndef, dctYear + "-" + newMonth);
						}
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month-with-details", language);
						if (lmMonth.equals("")) {
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
						if (dctSeason.equals("SP")) {
							valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newSeason);
						} else if (dctSeason.equals("SU")) {
							if (newSeason.equals("SP")) {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newSeason);
							}
						} else if (dctSeason.equals("FA")) {
							if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newSeason);
							}
						} else if (dctSeason.equals("WI")) {
							if (newSeason.equals("WI")) {
								valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newSeason);
							} else {
								if (dctMonth < 12) {
									valueNew = valueNew.replace(checkUndef, dctYear - 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
								}
							}
						}
					} else { // NARRATVIE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season", language);
						if (lmSeason.equals("")) {
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
						valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
					} else {
						// TODO include tense of sentence?
						String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season", language);
						if (lmSeason.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmSeason.substring(0, 4) + "-" + newSeason);
						}
					}
				} else if (ltn.equals("next")) {
					if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
						if (dctSeason.equals("SP")) {
							if (newSeason.equals("SP")) {
								valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
							}
						} else if (dctSeason.equals("SU")) {
							if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
								valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
							}
						} else if (dctSeason.equals("FA")) {
							if (newSeason.equals("WI")) {
								valueNew = valueNew.replace(checkUndef, dctYear + "-" + newSeason);
							} else {
								valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newSeason);
							}
						} else if (dctSeason.equals("WI")) {
							valueNew = valueNew.replace(checkUndef, dctYear + 1 + "-" + newSeason);
						}
					} else { // NARRATIVE DOCUMENT
						String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season", language);
						if (lmSeason.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							if (lmSeason.substring(5, 7).equals("SP")) {
								if (newSeason.equals("SP")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								}
							} else if (lmSeason.substring(5, 7).equals("SU")) {
								if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								}
							} else if (lmSeason.substring(5, 7).equals("FA")) {
								if (newSeason.equals("WI")) {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + "-" + newSeason);
								} else {
									valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
								}
							} else if (lmSeason.substring(5, 7).equals("WI")) {
								valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0, 4)) + 1 + "-" + newSeason);
							}
						}
					}
				}
			}

			// WEEKDAY NAMES
			// TODO the calculation is strange, but works
			// TODO tense should be included?!
			else if (UNDEF_WEEKDAY.matcher(ambigString).matches()) {
				for (MatchResult mr : Toolbox.findMatches(UNDEF_WEEKDAY, ambigString)) {
					String checkUndef = mr.group(1);
					String ltnd = mr.group(2);
					String newWeekday = mr.group(3);
					int newWeekdayInt = Integer.parseInt(norm.getFromNormDayInWeek(newWeekday));
					if (ltnd.equals("last")) {
						if ((documentTypeNews || documentTypeColloquial || documentTypeScientific) && dctAvailable) {
							int diff = (-1) * (dctWeekday - newWeekdayInt);
							if (diff >= 0) {
								diff = diff - 7;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-" + dctDay, diff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
							int diff = (-1) * (dctWeekday - newWeekdayInt);
							if (diff >= 0) {
								diff = diff - 7;
							}
							if (diff == -7) {
								diff = 0;
							}

							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-" + dctDay, diff));
						} else {
							// TODO tense should be included?!
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
							int diff = newWeekdayInt - dctWeekday;
							if (diff <= 0) {
								diff = diff + 7;
							}
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-" + dctDay, diff));
						} else {
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
							int diff = (-1) * (dctWeekday - newWeekdayInt);
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
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-" + dctDay, diff));
						} else {
							// TODO tense should be included?!
							String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day", language);
							if (lmDay.equals("")) {
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
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME")) {
				linearDates.add(timex);
			}

			if (timex.getTimexType().equals("DURATION") && !timex.getEmptyValue().equals("")) {
				linearDates.add(timex);
			}
		}

		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		for (int i = 0; i < linearDates.size(); i++) {
			Timex3 t_i = (Timex3) linearDates.get(i);
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
				LOG.debug(t_i.getTimexId() + " DISAMBIGUATION PHASE: foundBy:" + t_i.getFoundByRule() + " text:" + t_i.getCoveredText() + " value:" + t_i.getTimexValue()
						+ " NEW value:" + valueNew);

			t_i.setTimexValue(valueNew);
			t_i.addToIndexes();
			linearDates.set(i, t_i);
		}
	}

	/**
	 * @param jcas
	 */
	private void deleteOverlappingTimexesPreprocessing(JCas jcas) {
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		HashSet<Timex3> hsTimexesToRemove = new HashSet<Timex3>();
		for (Timex3 t1 : timexes) {
			for (Timex3 t2 : timexes) {
				if ( // t1 starts inside or with t2 and ends before t2 -> remove t1
				((t1.getBegin() >= t2.getBegin()) && (t1.getEnd() < t2.getEnd())) ||
				// t1 starts inside t2 and ends with or before t2 -> remove t1
						((t1.getBegin() > t2.getBegin()) && (t1.getEnd() <= t2.getEnd()))) {
					hsTimexesToRemove.add(t1);
					// t2 starts inside or with t1 and ends before t1 -> remove t2
				} else if (((t2.getBegin() >= t1.getBegin()) && (t2.getEnd() < t1.getEnd())) ||
				// t2 starts inside t1 and ends with or before t1 -> remove t2
						((t2.getBegin() > t1.getBegin()) && (t2.getEnd() <= t1.getEnd()))) {
					hsTimexesToRemove.add(t2);
				}
				// identical length
				if (!t1.equals(t2) && (t1.getBegin() == t2.getBegin()) && (t1.getEnd() == t2.getEnd())) {
					if ((t1.getTimexValue().startsWith("UNDEF")) && (!(t2.getTimexValue().startsWith("UNDEF")))) {
						hsTimexesToRemove.add(t1);
					} else if ((!(t1.getTimexValue().startsWith("UNDEF"))) && (t2.getTimexValue().startsWith("UNDEF"))) {
						hsTimexesToRemove.add(t2);
					}
					// t1 is explicit, but t2 is not
					else if ((t1.getFoundByRule().endsWith("explicit")) && (!(t2.getFoundByRule().endsWith("explicit")))) {
						hsTimexesToRemove.add(t2);
					}
					// remove timexes that are identical, but one has an emptyvalue
					else if (t2.getEmptyValue().equals("") && !t1.getEmptyValue().equals("")) {
						hsTimexesToRemove.add(t2);
					}
					// REMOVE REAL DUPLICATES (the one with the lower timexID)
					else if ((Integer.parseInt(t1.getTimexId().substring(1)) < Integer.parseInt(t2.getTimexId().substring(1)))) {
						hsTimexesToRemove.add(t1);
					}
				}
			}
		}
		// remove, finally
		for (Timex3 t : hsTimexesToRemove) {
			if (LOG.isDebugEnabled())
				LOG.debug("REMOVE DUPLICATE: " + t.getCoveredText() + "(id:" + t.getTimexId() + " value:" + t.getTimexValue() + " found by:" + t.getFoundByRule() + ")");

			t.removeFromIndexes();
			timex_counter--;
		}
	}

	private void deleteOverlappedTimexesPostprocessing(JCas jcas) {
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		HashSet<ArrayList<Timex3>> effectivelyToInspect = new HashSet<ArrayList<Timex3>>();
		ArrayList<Timex3> allTimexesToInspect = new ArrayList<Timex3>();
		for (Timex3 myTimex : timexes) {
			ArrayList<Timex3> timexSet = new ArrayList<Timex3>();
			if (!myTimex.getTimexType().equals("TEMPONYM")) {
				timexSet.add(myTimex);
			}

			// compare this timex to all other timexes and mark those that
			// have an overlap
			for (Timex3 myInnerTimex : timexes) {
				if (!myInnerTimex.getTimexType().equals("TEMPONYM")) {
					if (// timex1 starts, timex2 is partial overlap
					(myTimex.getBegin() <= myInnerTimex.getBegin() && myTimex.getEnd() > myInnerTimex.getBegin()) ||
					// same as above, but in reverse
							(myInnerTimex.getBegin() <= myTimex.getBegin() && myInnerTimex.getEnd() > myTimex.getBegin()) ||
							// timex 1 is contained within or identical to timex2
							(myInnerTimex.getBegin() <= myTimex.getBegin() && myTimex.getEnd() <= myInnerTimex.getEnd()) ||
							// same as above, but in reverse
							(myTimex.getBegin() <= myInnerTimex.getBegin() && myInnerTimex.getEnd() <= myTimex.getEnd())) {

						// increase the set
						timexSet.add(myInnerTimex);
						// note that these timexes are being looked at
						allTimexesToInspect.add(myTimex);
						allTimexesToInspect.add(myInnerTimex);
					}
				}
			}

			// if overlaps with myTimex were detected, memorize them
			if (timexSet.size() > 1)
				effectivelyToInspect.add(timexSet);
		}

		/*
		 * prune those sets of overlapping timexes that are subsets of others (i.e. leave only the largest union of overlapping timexes)
		 */
		HashSet<ArrayList<Timex3>> newEffectivelyToInspect = new HashSet<ArrayList<Timex3>>();
		for (Timex3 t : allTimexesToInspect) {
			ArrayList<Timex3> setToKeep = new ArrayList<Timex3>();

			// determine the largest set that contains this timex
			for (ArrayList<Timex3> tSet : effectivelyToInspect) {
				if (tSet.contains(t) && tSet.size() > setToKeep.size())
					setToKeep = tSet;
			}

			newEffectivelyToInspect.add(setToKeep);
		}
		// overwrite previous list of sets
		effectivelyToInspect = newEffectivelyToInspect;

		// iterate over the selected sets and merge information, remove old timexes
		for (ArrayList<Timex3> tSet : effectivelyToInspect) {
			Timex3 newTimex = new Timex3(jcas);

			// if a timex has the timex value REMOVE, remove it from consideration
			@SuppressWarnings("unchecked")
			ArrayList<Timex3> newTSet = (ArrayList<Timex3>) tSet.clone();
			for (Timex3 t : tSet) {
				// remove timexes with value "REMOVE"
				if (t.getTimexValue().equals("REMOVE")) {
					newTSet.remove(t);
				}
			}
			tSet = newTSet;

			// iteration is done if all the timexes have been removed, i.e.
			// the set is empty
			if (tSet.size() == 0)
				continue;

			/*
			 * check - whether all timexes of this set have the same timex type attribute, - which one in the set has the longest value attribute string length, - what the combined extents
			 * are
			 */
			boolean allSameTypes = true;
			String timexType = null;
			Timex3 longestTimex = null;
			int combinedBegin = Integer.MAX_VALUE, combinedEnd = Integer.MIN_VALUE;
			ArrayList<Integer> tokenIds = new ArrayList<Integer>();
			for (Timex3 t : tSet) {
				// check whether the types are identical and either all
				// DATE or TIME
				if (timexType == null) {
					timexType = t.getTimexType();
				} else {
					if (allSameTypes && !timexType.equals(t.getTimexType()) || !(timexType.equals("DATE") || timexType.equals("TIME"))) {
						allSameTypes = false;
					}
				}
				if (LOG.isDebugEnabled())
					LOG.debug("Are these overlapping timexes of same type? => " + allSameTypes);

				// check timex value attribute string length
				if (longestTimex == null) {
					longestTimex = t;
				} else if (allSameTypes && t.getFoundByRule().indexOf("-BCADhint") != -1) {
					longestTimex = t;
				} else if (allSameTypes && t.getFoundByRule().indexOf("relative") == -1 && longestTimex.getFoundByRule().indexOf("relative") != -1) {
					longestTimex = t;
				} else if (longestTimex.getTimexValue().length() == t.getTimexValue().length()) {
					if (t.getBegin() < longestTimex.getBegin())
						longestTimex = t;
				} else if (longestTimex.getTimexValue().length() < t.getTimexValue().length()) {
					longestTimex = t;
				}
				if (LOG.isDebugEnabled())
					LOG.debug("Selected " + longestTimex.getTimexId() + ": " + longestTimex.getCoveredText() + "[" + longestTimex.getTimexValue()
							+ "] as the longest-valued timex.");

				// check combined beginning/end
				if (combinedBegin > t.getBegin())
					combinedBegin = t.getBegin();
				if (combinedEnd < t.getEnd())
					combinedEnd = t.getEnd();
				if (LOG.isDebugEnabled())
					LOG.debug("Selected combined constraints: " + combinedBegin + ":" + combinedEnd);

				// disassemble and remember the token ids
				String[] tokenizedTokenIds = t.getAllTokIds().split("<-->");
				for (int i = 1; i < tokenizedTokenIds.length; i++) {
					if (!tokenIds.contains(Integer.parseInt(tokenizedTokenIds[i]))) {
						tokenIds.add(Integer.parseInt(tokenizedTokenIds[i]));
					}
				}
			}

			/*
			 * types are equal => merge constraints, use the longer, "more granular" value. if types are not equal, just take the longest value.
			 */
			Collections.sort(tokenIds);
			newTimex = longestTimex;
			if (allSameTypes) {
				newTimex.setBegin(combinedBegin);
				newTimex.setEnd(combinedEnd);
				if (tokenIds.size() > 0)
					newTimex.setFirstTokId(tokenIds.get(0));
				String tokenIdText = "BEGIN";
				for (Integer tokenId : tokenIds) {
					tokenIdText += "<-->" + tokenId;
				}
				newTimex.setAllTokIds(tokenIdText);
			}

			// remove old overlaps.
			for (Timex3 t : tSet) {
				t.removeFromIndexes();
			}
			// add the single constructed/chosen timex to the indexes.
			newTimex.addToIndexes();
		}
	}

	/**
	 * Identify the part of speech (POS) of a MarchResult.
	 * 
	 * @param tokBegin
	 * @param tokEnd
	 * @param s
	 * @param jcas
	 * @return
	 */
	public String getPosFromMatchResult(int tokBegin, int tokEnd, Sentence s, JCas jcas) {
		// get all tokens in sentence
		HashMap<Integer, Token> hmTokens = new HashMap<Integer, Token>();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		for (FSIterator<Token> iterTok = tokens.subiterator(s); iterTok.hasNext();) {
			Token token = iterTok.next();
			hmTokens.put(token.getBegin(), token);
		}
		// get correct token
		String pos = "";
		if (hmTokens.containsKey(tokBegin)) {
			Token tokenToCheck = hmTokens.get(tokBegin);
			pos = tokenToCheck.getPos();
		}
		return pos;
	}

	// pattern for offset information
	Pattern paOffset = Pattern.compile("group\\(([0-9]+)\\)-group\\(([0-9]+)\\)");

	/**
	 * Apply the extraction rules, normalization rules
	 * 
	 * @param timexType
	 * @param hmPattern
	 * @param hmOffset
	 * @param hmNormalization
	 * @param s
	 * @param jcas
	 */
	public void findTimexes(String timexType, HashMap<Pattern, String> hmPattern, HashMap<String, String> hmOffset, HashMap<String, String> hmNormalization, Sentence s, JCas jcas) {
		final String coveredText = s.getCoveredText();
		Map<String, String> constraints = Collections.emptyMap();
		Map<String, Pattern> fastCheck = Collections.emptyMap();
		RuleManager rm = RuleManager.getInstance(language, find_temponyms);
		if (timexType.equals("DATE")) {
			fastCheck = rm.getHmDateFastCheck();
			constraints = rm.getHmDatePosConstraint();
		} else if (timexType.equals("TIME")) {
			fastCheck = rm.getHmTimeFastCheck();
			constraints = rm.getHmTimePosConstraint();
		} else if (timexType.equals("DURATION")) {
			fastCheck = rm.getHmDurationFastCheck();
			constraints = rm.getHmDurationPosConstraint();
		} else if (timexType.equals("SET")) {
			fastCheck = rm.getHmSetFastCheck();
			constraints = rm.getHmSetPosConstraint();
		} else if (timexType.equals("TEMPONYM")) {
			fastCheck = rm.getHmTemponymFastCheck();
			constraints = rm.getHmTemponymPosConstraint();
		} else {
			LOG.warn("Unknown timex type {}", timexType);
		}

		// Iterator over the rules by sorted by the name of the rules
		// this is important since later, the timexId will be used to
		// decide which of two expressions shall be removed if both
		// have the same offset
		for (Map.Entry<Pattern, String> e : Toolbox.sortByValue(hmPattern)) {
			String key = e.getValue();
			// validate fast check first, if no fast match, everything else is
			// not required anymore
			Pattern f = fastCheck.get(key);
			if (f != null && !f.matcher(coveredText).find())
				continue;

			long dur = 0;
			Matcher m = e.getKey().matcher(coveredText);
			while (true) {
				boolean found;
				if (PROFILE_REGEXP) {
					long begin = PROFILE_REGEXP ? System.nanoTime() : 0;
					found = m.find();
					dur += System.nanoTime() - begin;
				} else
					found = m.find();
				if (!found)
					break;
				// improved token boundary checking
				// FIXME: these seem to be flawed
				boolean infrontBehindOK = ContextAnalyzer.checkTokenBoundaries(m, s, jcas) && ContextAnalyzer.checkInfrontBehind(m, s);

				// CHECK POS CONSTRAINTS
				String constraint = constraints.get(key);
				boolean posConstraintOK = (constraint == null) || checkPosConstraint(s, constraint, m, jcas);

				if (infrontBehindOK && posConstraintOK) {
					// Offset of timex expression (in the checked sentence)
					int timexStart = m.start(), timexEnd = m.end();

					// Any offset parameter?
					if (hmOffset.containsKey(key)) {
						String offset = hmOffset.get(key);

						for (MatchResult mr : Toolbox.findMatches(paOffset, offset)) {
							timexStart = m.start(Integer.parseInt(mr.group(1)));
							timexEnd = m.end(Integer.parseInt(mr.group(2)));
						}
					}

					// Normalization Parameter
					if (hmNormalization.containsKey(key)) {
						String[] attributes = new String[5];
						if (timexType.equals("DATE")) {
							attributes = getAttributesForTimexFromFile(key, rm.getHmDateNormalization(), rm.getHmDateQuant(), rm.getHmDateFreq(), rm.getHmDateMod(),
									rm.getHmDateEmptyValue(), m, jcas);
						} else if (timexType.equals("DURATION")) {
							attributes = getAttributesForTimexFromFile(key, rm.getHmDurationNormalization(), rm.getHmDurationQuant(), rm.getHmDurationFreq(),
									rm.getHmDurationMod(), rm.getHmDurationEmptyValue(), m, jcas);
						} else if (timexType.equals("TIME")) {
							attributes = getAttributesForTimexFromFile(key, rm.getHmTimeNormalization(), rm.getHmTimeQuant(), rm.getHmTimeFreq(), rm.getHmTimeMod(),
									rm.getHmTimeEmptyValue(), m, jcas);
						} else if (timexType.equals("SET")) {
							attributes = getAttributesForTimexFromFile(key, rm.getHmSetNormalization(), rm.getHmSetQuant(), rm.getHmSetFreq(), rm.getHmSetMod(),
									rm.getHmSetEmptyValue(), m, jcas);
						} else if (timexType.equals("TEMPONYM")) {
							attributes = getAttributesForTimexFromFile(key, rm.getHmTemponymNormalization(), rm.getHmTemponymQuant(), rm.getHmTemponymFreq(),
									rm.getHmTemponymMod(), rm.getHmTemponymEmptyValue(), m, jcas);
						}
						if (!(attributes == null)) {
							addTimexAnnotation(timexType, timexStart + s.getBegin(), timexEnd + s.getBegin(), s, attributes[0], attributes[1], attributes[2], attributes[3],
									attributes[4], "t" + timexID++, key, jcas);
						}
					} else {
						LOG.debug("SOMETHING REALLY WRONG HERE: {}", key);
					}
				}
			}
			if (PROFILE_REGEXP) {
				// Do not enable this by default, because this wastes memory and CPU:
				Long old = profileData.get(e.getValue());
				profileData.put(e.getValue(), dur + (old != null ? (long) old : 0L));
			}
		}
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

	Pattern paConstraint = Pattern.compile("group\\(([0-9]+)\\):(.*?):");

	/**
	 * Check whether the part of speech constraint defined in a rule is satisfied.
	 * 
	 * @param s
	 * @param posConstraint
	 * @param m
	 * @param jcas
	 * @return
	 */
	public boolean checkPosConstraint(Sentence s, String posConstraint, MatchResult m, JCas jcas) {
		Matcher mr = paConstraint.matcher(posConstraint);
		while(mr.find()) {
			int groupNumber = Integer.parseInt(mr.group(1));
			int tokenBegin = s.getBegin() + m.start(groupNumber);
			int tokenEnd = s.getBegin() + m.end(groupNumber);
			String pos = mr.group(2);
			String pos_as_is = getPosFromMatchResult(tokenBegin, tokenEnd, s, jcas);
			if (!pos_as_is.matches(pos))
				return false;
			LOG.debug("POS CONSTRAINT IS VALID: pos should be {} and is {}", pos, pos_as_is);
		}
		return true;
	}

	static Pattern paNorm = Pattern.compile("%([A-Za-z0-9]+?)\\(group\\(([0-9]+)\\)\\)");
	static Pattern paGroup = Pattern.compile("group\\(([0-9]+)\\)");
	static Pattern paSubstring = Pattern.compile("%SUBSTRING%\\((.*?),([0-9]+),([0-9]+)\\)");
	static Pattern paLowercase = Pattern.compile("%LOWERCASE%\\((.*?)\\)");
	static Pattern paUppercase = Pattern.compile("%UPPERCASE%\\((.*?)\\)");
	static Pattern paSum = Pattern.compile("%SUM%\\((.*?),(.*?)\\)");
	static Pattern paNormNoGroup = Pattern.compile("%([A-Za-z0-9]+?)\\((.*?)\\)");
	static Pattern paChineseNorm = Pattern.compile("%CHINESENUMBERS%\\((.*?)\\)");

	static Pattern WHITESPACE_NORM = Pattern.compile("[\n\\s]+");

	public String applyRuleFunctions(String rule, String pattern, MatchResult m) {
		NormalizationManager norm = NormalizationManager.getInstance(language, find_temponyms);
		return applyRuleFunctions(rule, pattern, m, norm, language);
	}

	public static String applyRuleFunctions(String rule, String pattern, MatchResult m,
			NormalizationManager norm, Language language) {
		StringBuilder tonormalize = new StringBuilder(pattern);
		// pattern for normalization functions + group information
		// pattern for group information
		Matcher mr = paNorm.matcher(tonormalize);
		while (tonormalize.indexOf("%") >= 0 || tonormalize.indexOf("group") >= 0) {
			// replace normalization functions
			while (mr.find(0)) {
				String normfunc = mr.group(1);
				int groupid = Integer.parseInt(mr.group(2));
				if (LOG.isDebugEnabled()) {
					LOG.debug("rule:" + rule);
					LOG.debug("tonormalize:" + tonormalize.toString());
					LOG.debug("mr.group():" + mr.group());
					LOG.debug("mr.group(1):" + normfunc);
					LOG.debug("mr.group(2):" + mr.group(2));
					LOG.debug("m.group():" + m.group());
					LOG.debug("m.group(" + groupid + "):" + m.group(groupid));
					LOG.debug("hmR...:" + norm.getFromHmAllNormalization(normfunc).get(m.group(groupid)));
				}

				try {
					String value = m.group(groupid);
					if (value != null) {
						value = WHITESPACE_NORM.matcher(value).replaceAll(" ");
						if (!norm.getFromHmAllNormalization(normfunc).containsKey(value)) {
							LOG.debug("Maybe problem with normalization of the resource: {}\n" + //
									"Maybe problem with part to replace? {}", normfunc, value);
							if (normfunc.contains("Temponym")) {
								LOG.debug("Should be ok, as it's a temponym.");
								return null;
							}
						} else {
							value = norm.getFromHmAllNormalization(normfunc).get(value);
							tonormalize.replace(mr.start(), mr.end(), value);
						}
					} else {
						// This is not unusual to happen
						LOG.debug("Empty part to normalize in {}, rule {}", normfunc, rule);
						tonormalize.delete(mr.start(), mr.end());
					}
				} catch (IndexOutOfBoundsException e) {
					LOG.error("Invalid group reference '{}' in normalization pattern of rule: {}", groupid, rule);
					tonormalize.delete(mr.start(), mr.end());
				}
			}
			// replace other groups
			mr.usePattern(paGroup).reset(tonormalize);
			while (mr.find(0)) {
				int groupid = Integer.parseInt(mr.group(1));
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug("tonormalize:" + tonormalize);
						LOG.debug("mr.group():" + mr.group());
						LOG.debug("mr.group(1):" + mr.group(1));
						LOG.debug("m.group():" + m.group());
						LOG.debug("m.group(" + Integer.parseInt(mr.group(1)) + "):" + m.group(groupid));
					}

					tonormalize.replace(mr.start(), mr.end(), m.group(groupid));
				} catch (IndexOutOfBoundsException e) {
					LOG.error("Invalid group reference '{}' in normalization pattern of rule: {}", groupid, rule);
					tonormalize.delete(mr.start(), mr.end());
				}
			}
			// replace substrings
			mr.usePattern(paSubstring).reset(tonormalize);
			while (mr.find(0)) {
				String substring = mr.group(1).substring(Integer.parseInt(mr.group(2)), Integer.parseInt(mr.group(3)));
				tonormalize.replace(mr.start(), mr.end(), substring);
			}
			if (language.useLowercase()) {
				// replace lowercase
				mr.usePattern(paLowercase).reset(tonormalize);
				while (mr.find(0)) {
					String substring = mr.group(1).toLowerCase();
					tonormalize.replace(mr.start(), mr.end(), substring);
				}

				// replace uppercase
				mr.usePattern(paUppercase).reset(tonormalize);
				while (mr.find(0)) {
					String substring = mr.group(1).toUpperCase();
					tonormalize.replace(mr.start(), mr.end(), substring);
				}
			}
			// replace sum, concatenation
			mr.usePattern(paSum).reset(tonormalize);
			while (mr.find(0)) {
				int newValue = Integer.parseInt(mr.group(1)) + Integer.parseInt(mr.group(2));
				tonormalize.replace(mr.start(), mr.end(), Integer.toString(newValue));
			}
			// replace normalization function without group
			mr.usePattern(paNormNoGroup).reset(tonormalize);
			while (mr.find(0)) {
				tonormalize.replace(mr.start(), mr.end(), norm.getFromHmAllNormalization(mr.group(1)).get(mr.group(2)));
			}
			// replace Chinese with Arabic numerals
			mr.usePattern(paChineseNorm).reset(tonormalize);
			while (mr.find(0)) {
				String outString = ChineseNumbers.normalize(mr.group(1));
				tonormalize.replace(mr.start(), mr.end(), outString);
			}
		}
		return tonormalize.toString();
	}
	
	public String[] getAttributesForTimexFromFile(String rule, HashMap<String, String> hmNormalization, HashMap<String, String> hmQuant, HashMap<String, String> hmFreq,
			HashMap<String, String> hmMod, HashMap<String, String> hmEmptyValue, MatchResult m, JCas jcas) {
		String[] attributes = new String[5];

		// Normalize Value
		String value_normalization_pattern = hmNormalization.get(rule);
		String value = applyRuleFunctions(rule, value_normalization_pattern, m);
		if (value == null)
			return null;

		// get quant
		String quant_normalization_pattern = hmQuant.get(rule);
		String quant = (quant_normalization_pattern != null) ? applyRuleFunctions(rule, quant_normalization_pattern, m) : "";

		// get freq
		String freq_normalization_pattern = hmFreq.get(rule);
		String freq = (freq_normalization_pattern != null) ? applyRuleFunctions(rule, freq_normalization_pattern, m) : "";

		// get mod
		String mod_normalization_pattern = hmMod.get(rule);
		String mod = (mod_normalization_pattern != null) ? applyRuleFunctions(rule, mod_normalization_pattern, m) : "";

		// get emptyValue
		String emptyValue_normalization_pattern = hmEmptyValue.get(rule);
		String emptyValue = (emptyValue_normalization_pattern != null) ? //
				DurationSimplification.simplify(applyRuleFunctions(rule, emptyValue_normalization_pattern, m)) : "";
		// For example "PT24H" -> "P1D"
		if (group_gran)
			value = DurationSimplification.simplify(value);

		attributes[0] = value;
		attributes[1] = quant;
		attributes[2] = freq;
		attributes[3] = mod;
		attributes[4] = emptyValue;

		return attributes;
	}
	
	private static final Pattern VALID_DCT = Pattern.compile("\\d{4}.\\d{2}.\\d{2}|\\d{8}");

	/**
	 * Check whether or not a jcas object has a correct DCT value. If there is no DCT present, we canonically return true since fallback calculation takes care of that scenario.
	 * 
	 * @param jcas
	 * @return Whether or not the given jcas contains a valid DCT
	 */
	private static boolean isValidDCT(JCas jcas) {
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
