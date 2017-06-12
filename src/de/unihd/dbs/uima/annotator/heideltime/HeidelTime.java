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

import static de.unihd.dbs.uima.annotator.heideltime.utilities.ParseInteger.parseInt;
import static de.unihd.dbs.uima.annotator.heideltime.utilities.ParseInteger.parseIntAt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
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
import de.unihd.dbs.uima.annotator.heideltime.resources.Rule;
import de.unihd.dbs.uima.annotator.heideltime.resources.RuleExpansion;
import de.unihd.dbs.uima.annotator.heideltime.resources.RuleManager;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DurationSimplification;
import de.unihd.dbs.uima.annotator.heideltime.utilities.LocaleException;
import de.unihd.dbs.uima.annotator.heideltime.utilities.TokenBoundaryMatcher;
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

	// PROCESSOR MANAGER
	private ProcessorManager procMan = new ProcessorManager();

	// COUNTER (how many timexes added to CAS? (finally)
	public int timex_counter = 0;
	public int timex_counter_global = 0;

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
	private DocumentType typeToProcess = DocumentType.NEWS;

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

	// Whether to generate "allTokIds" strings.
	// Required for TempEval!
	private boolean doAllTokIds = true;

	private ResolveAmbiguousValues resolver;

	/**
	 * @see AnalysisComponent#initialize(UimaContext)
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

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
				Locale locale = getLocaleFromString(requestedLocale);
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

		typeToProcess = DocumentType.of((String) aContext.getConfigParameterValue(PARAM_TYPE_TO_PROCESS));
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
		LOG.debug("Enabled modules:{}{}{}{}{}", //
				find_dates ? " dates" : "", //
				find_times ? " times" : "", //
				find_durations ? " durations" : "", //
				find_sets ? " sets" : "", //
				find_temponyms ? " temponyms" : "");

		if (resolver == null)
			resolver = new ResolveAmbiguousValues();
		resolver.init(language, find_temponyms, typeToProcess);
	}

	/**
	 * @see JCasAnnotator_ImplBase#process(JCas)
	 */
	public void process(JCas jcas) {
		// check whether a given DCT (if any) is of the correct format and if not,skip this call
		if (!ResolveAmbiguousValues.ParsedDct.isValidDCT(jcas)) {
			LOG.error("The reader component of this workflow has set an incorrect DCT.\n" + //
					" HeidelTime expects either \"YYYYMMDD\" or \"YYYY-MM-DD...\", got \"{}\".\n" + //
					"This document was skipped.", ResolveAmbiguousValues.ParsedDct.getDct(jcas));
			return;
		}

		// run preprocessing processors
		procMan.executeProcessors(jcas, Priority.PREPROCESSING);

		RuleManager rulem = RuleManager.getInstance(language, find_temponyms);

		timexID = 1; // reset counter once per document processing

		timex_counter = 0;

		boolean flagHistoricDates = false;

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

		TokenBoundaryMatcher matcher = new TokenBoundaryMatcher();
		for (Sentence s : sentences) {
			try {
				final CharSequence coveredText = TokenBoundaryMatcher.simplifyString(s.getCoveredText());
				if (LOG.isTraceEnabled())
					LOG.trace("Sentence {}: {}", s.getSentenceId(), coveredText);

				// Build a list of "good" token positions to anchor matches:
				matcher.tokenBoundaries(coveredText, s, jcas);

				if (find_dates)
					findTimexes("DATE", rulem.getHmDateRules(), matcher, s, jcas, coveredText);
				if (find_times)
					findTimexes("TIME", rulem.getHmTimeRules(), matcher, s, jcas, coveredText);

				/*
				 * check for historic dates/times starting with BC to check if post-processing step is required
				 */
				if (typeToProcess == DocumentType.NARRATIVE) {
					AnnotationIndex<Timex3> dates = jcas.getAnnotationIndex(Timex3.type);
					for (Timex3 t : dates)
						if (t.getTimexValue().startsWith("BC")) {
							flagHistoricDates = true;
							break;
						}
				}

				if (find_sets)
					findTimexes("SET", rulem.getHmSetRules(), matcher, s, jcas, coveredText);
				if (find_durations)
					findTimexes("DURATION", rulem.getHmDurationRules(), matcher, s, jcas, coveredText);
				if (find_temponyms)
					findTimexes("TEMPONYM", rulem.getHmTemponymRules(), matcher, s, jcas, coveredText);
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
		if (resolver != null)
			resolver.specifyAmbiguousValues(jcas);

		// disambiguate historic dates
		// check dates without explicit hints to AD or BC if they might refer to BC dates
		if (flagHistoricDates)
			try {
				disambiguateHistoricDates(jcas);
			} catch (Exception e) {
				LOG.error("Failed disambiguating historic dates: {}", e.getMessage(), e);
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
		if (doAllTokIds) {
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
		}
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

		if (LOG.isTraceEnabled()) {
			LOG.trace(annotation.getTimexId() + " EXTRACTION PHASE:   " + " found by:" + annotation.getFoundByRule() + " text:" + annotation.getCoveredText());
			LOG.trace(annotation.getTimexId() + " NORMALIZATION PHASE:" + " found by:" + annotation.getFoundByRule() + " text:" + annotation.getCoveredText() + " value:"
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
		AnnotationIndex<Timex3> annotations = jcas.getAnnotationIndex(Timex3.type);

		// Create List of all Timexes of types "date" and "time"
		List<Timex3> linearDates = new ArrayList<Timex3>();
		for (Timex3 timex : annotations)
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME"))
				linearDates.add(timex);

		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		for (int i = 1; i < linearDates.size(); i++) {
			Timex3 t_i = linearDates.get(i);
			if (t_i.getFoundByRule().contains("-BCADhint"))
				continue;
			String value_i = t_i.getTimexValue(), newValue = value_i;
			if (value_i.charAt(0) != '0')
				continue;
			boolean change = false;
			int offset = 1, counter = 1;
			do {
				String txval = linearDates.get(i - offset).getTimexValue();
				if ((i == 1 || (i > 1 && !change)) && txval.startsWith("BC")) {
					if (value_i.length() > 1) {
						if (txval.startsWith("BC" + value_i.substring(0, 2)) //
								|| txval.startsWith(String.format("BC%02d", parseInt(value_i, 0, 2) + 1))) {
							if ((value_i.startsWith("00") && txval.startsWith("BC00")) || (value_i.startsWith("01") && txval.startsWith("BC01"))) {
								if ((value_i.length() > 2) && (txval.length() > 4)) {
									if (parseInt(value_i, 0, 3) <= parseInt(txval, 2, 5)) {
										newValue = "BC" + value_i;
										change = true;
										if (LOG.isDebugEnabled())
											LOG.debug("DisambiguateHistoricDates: " + value_i + " to " + newValue + ". Expression " + t_i.getCoveredText()
													+ " due to " + linearDates.get(i - offset).getCoveredText());
									}
								}
							} else {
								newValue = "BC" + value_i;
								change = true;
								if (LOG.isDebugEnabled())
									LOG.debug("DisambiguateHistoricDates: " + value_i + " to " + newValue + ". Expression " + t_i.getCoveredText() + " due to "
											+ linearDates.get(i - offset).getCoveredText());
							}
						}
					}
				}

				String txtype = linearDates.get(i - offset).getTimexType();
				if ((txtype.equals("TIME") || txtype.equals("DATE")) && txval.matches("^\\d.*")) {
					counter++;
				}
			} while (counter < 5 && ++offset < i);
			if (!newValue.equals(value_i)) {
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
		for (Timex3 timex : timexes)
			if (timex.getTimexValue().equals("REMOVE"))
				hsTimexToRemove.add(timex);

		// remove invalids, finally
		for (Timex3 timex3 : hsTimexToRemove) {
			timex3.removeFromIndexes();
			this.timex_counter--;
			if (LOG.isDebugEnabled())
				LOG.debug("{} REMOVING PHASE: found by: {} text:{} value:{}", timex3.getTimexId(), timex3.getFoundByRule(), timex3.getCoveredText(), timex3.getTimexValue());
		}
	}

	/**
	 * @param jcas
	 */
	private void deleteOverlappingTimexesPreprocessing(JCas jcas) {
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		HashSet<Timex3> hsTimexesToRemove = new HashSet<Timex3>();
		for (Timex3 t1 : timexes) {
			if (hsTimexesToRemove.contains(t1)) {
				continue;
			}
			for (Timex3 t2 : timexes) {
				if (t1 == t2 || hsTimexesToRemove.contains(t2)) {
					continue;
				}
				if ( // t1 starts inside or with t2 and ends before t2 -> remove t1
				((t1.getBegin() >= t2.getBegin()) && (t1.getEnd() < t2.getEnd()))
						// t1 starts inside t2 and ends with or before t2 -> remove t1
						|| ((t1.getBegin() > t2.getBegin()) && (t1.getEnd() <= t2.getEnd()))) {
					logRemove(t1, "overlaps and begins later than", t2);
					hsTimexesToRemove.add(t1);
					continue;
				}
				// t2 starts inside or with t1 and ends before t1 -> remove t2
				if (((t2.getBegin() >= t1.getBegin()) && (t2.getEnd() < t1.getEnd()))
						// t2 starts inside t1 and ends with or before t1 -> remove t2
						|| ((t2.getBegin() > t1.getBegin()) && (t2.getEnd() <= t1.getEnd()))) {
					logRemove(t2, "overlaps and begins later than", t1);
					hsTimexesToRemove.add(t2);
					continue;
				}
				// identical length
				if ((t1.getBegin() == t2.getBegin()) && (t1.getEnd() == t2.getEnd())) {
					if (t1.getTimexValue().startsWith("UNDEF") && !t2.getTimexValue().startsWith("UNDEF")) {
						logRemove(t1, "is UNDEF, compared to", t2);
						hsTimexesToRemove.add(t1);
					} else if (!t1.getTimexValue().startsWith("UNDEF") && t2.getTimexValue().startsWith("UNDEF")) {
						logRemove(t2, "is UNDEF, compared to", t1);
						hsTimexesToRemove.add(t2);
					}
					// t1 is explicit, but t2 is not
					else if (t1.getFoundByRule().endsWith("explicit") && !t2.getFoundByRule().endsWith("explicit")) {
						logRemove(t2, "is not explicit, compared to", t1);
						hsTimexesToRemove.add(t2);
					}
					// remove timexes that are identical, but one has an emptyvalue
					else if (t2.getEmptyValue().isEmpty() && !t1.getEmptyValue().isEmpty()) {
						logRemove(t2, "has emptyvalue, compared to", t1);
						hsTimexesToRemove.add(t2);
					}
					// REMOVE REAL DUPLICATES (the one with the lower timexID)
					else if (parseIntAt(t1.getTimexId(), 1) < parseIntAt(t2.getTimexId(), 1)) {
						logRemove(t1, "has lower id value than", t2);
						hsTimexesToRemove.add(t1);
					}
				}
			}
		}
		// remove, finally
		for (Timex3 t : hsTimexesToRemove) {
			t.removeFromIndexes();
			timex_counter--;
		}
	}

	private void logRemove(Timex3 t1, String reason, Timex3 t2) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("DUPLICATE: {} (id:{} value:{} found by:{}) removed because it {} {} (id:{} value:{} found by:{})", //
					t1.getCoveredText(), t1.getTimexId(), t1.getTimexValue(), t1.getFoundByRule(), //
					reason, //
					t2.getCoveredText(), t2.getTimexId(), t2.getTimexValue(), t2.getFoundByRule());
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
			Timex3 newTimex;

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
				if (LOG.isTraceEnabled())
					LOG.trace("Are these overlapping timexes of same type? => {}", allSameTypes);

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
				if (LOG.isTraceEnabled())
					LOG.trace("Selected {}: {} [{}] as the longest-valued timex.", longestTimex.getTimexId(), longestTimex.getCoveredText(), longestTimex.getTimexValue());

				// check combined beginning/end
				if (combinedBegin > t.getBegin())
					combinedBegin = t.getBegin();
				if (combinedEnd < t.getEnd())
					combinedEnd = t.getEnd();
				if (LOG.isTraceEnabled())
					LOG.trace("Selected combined constraints: {}:{}", combinedBegin, combinedEnd);

				// disassemble and remember the token ids
				if (doAllTokIds) {
					String[] tokenizedTokenIds = t.getAllTokIds().split("<-->");
					for (int i = 1; i < tokenizedTokenIds.length; i++) {
						int tokid = parseInt(tokenizedTokenIds[i]);
						if (!tokenIds.contains(tokid))
							tokenIds.add(tokid);
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
				for (Integer tokenId : tokenIds)
					tokenIdText += "<-->" + tokenId;
				newTimex.setAllTokIds(tokenIdText);
			}

			// remove old overlaps.
			for (Timex3 t : tSet)
				t.removeFromIndexes();
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
	 *                Type to find
	 * @param sortedRules
	 *                sorted rules
	 * @param startpos
	 *                Valid starting positions
	 * @param endpos
	 *                Valid end positions
	 * @param s
	 *                Sentence
	 * @param jcas
	 *                JCas
	 * @param coveredText
	 *                covered text
	 */
	public void findTimexes(String timexType, List<Rule> sortedRules, TokenBoundaryMatcher matcher, Sentence s, JCas jcas, CharSequence coveredText) {
		// Iterator over the rules by sorted by the name of the rules
		// this is important since later, the timexId will be used to
		// decide which of two expressions shall be removed if both
		// have the same offset
		for (Rule rule : sortedRules) {
			String key = rule.getName();
			// validate fast check first, if no fast match, everything else is
			// not required anymore
			Pattern f = rule.getFastCheck();
			if (f != null && matcher.matchNext(0, f.matcher(coveredText), key) < 0)
				continue;

			Matcher m = rule.getPattern().matcher(coveredText);
			for (int tpos = 0; (tpos = matcher.matchNext(tpos, m, key)) >= 0;) {
				// CHECK POS CONSTRAINTS
				String constraint = rule.getPosConstratint();
				if (constraint != null && !checkPosConstraint(key, s, constraint, m, jcas))
					continue;
				// Offset of timex expression (in the checked sentence)
				int timexStart = m.start(), timexEnd = m.end();

				// Any offset parameter?
				String offset = rule.getOffset();
				if (offset != null) {
					Matcher mr = paOffset.matcher(offset);
					if (mr.matches()) {
						timexStart = m.start(parseInt(mr.group(1)));
						timexEnd = m.end(parseInt(mr.group(2)));
					} else {
						LOG.warn("Offset pattern does not match: {}", offset);
					}
				}

				// Normalization Parameter
				if (rule.getNormalization() == null) {
					LOG.warn("No normalization pattern for: {}", key);
					continue;
				}
				String[] attributes = getAttributesForTimexFromFile(key, rule, m, jcas);
				if (attributes != null) {
					addTimexAnnotation(timexType, timexStart + s.getBegin(), timexEnd + s.getBegin(), s, attributes[0], attributes[1], attributes[2], attributes[3], attributes[4],
							"t" + timexID++, key, jcas);
				}
			}
		}
	}

	static Pattern paConstraint = Pattern.compile("group\\(([0-9]+)\\):(.*?):");

	/**
	 * Check whether the part of speech constraint defined in a rule is satisfied.
	 * 
	 * @param rule
	 *                Rule name, for error reporting
	 * @param s
	 * @param posConstraint
	 * @param m
	 * @param jcas
	 * @return
	 */
	public boolean checkPosConstraint(String rule, Sentence s, String posConstraint, MatchResult m, JCas jcas) {
		Matcher mr = paConstraint.matcher(posConstraint);
		while (mr.find()) {
			try {
				int groupNumber = parseInt(mr.group(1));
				int tokenBegin = s.getBegin() + m.start(groupNumber);
				int tokenEnd = s.getBegin() + m.end(groupNumber);
				String pos = mr.group(2);
				String pos_as_is = getPosFromMatchResult(tokenBegin, tokenEnd, s, jcas);
				if (!pos_as_is.matches(pos))
					return false;
				if (LOG.isTraceEnabled())
					LOG.trace("POS CONSTRAINT IS VALID: pos should be {} and is {}", pos, pos_as_is);
			} catch (IndexOutOfBoundsException e) {
				LOG.debug("Bad group number in rule {}", rule);
			}
		}
		return true;
	}

	public String[] getAttributesForTimexFromFile(String key, Rule rule, MatchResult m, JCas jcas) {
		String[] attributes = new String[5];

		// Normalize Value
		String value_normalization_pattern = rule.getNormalization();
		NormalizationManager norm = NormalizationManager.getInstance(language, find_temponyms);
		String value = RuleExpansion.applyRuleFunctions(key, value_normalization_pattern, m, norm, language);
		if (value == null)
			return null;
		// For example "PT24H" -> "P1D"
		if (group_gran)
			value = DurationSimplification.simplify(value);
		attributes[0] = value;

		// get quant
		String quant_normalization_pattern = rule.getQuant();
		attributes[1] = (quant_normalization_pattern != null) ? RuleExpansion.applyRuleFunctions(key, quant_normalization_pattern, m, norm, language) : "";

		// get freq
		String freq_normalization_pattern = rule.getFreq();
		attributes[2] = (freq_normalization_pattern != null) ? RuleExpansion.applyRuleFunctions(key, freq_normalization_pattern, m, norm, language) : "";

		// get mod
		String mod_normalization_pattern = rule.getMod();
		attributes[3] = (mod_normalization_pattern != null) ? RuleExpansion.applyRuleFunctions(key, mod_normalization_pattern, m, norm, language) : "";

		// get emptyValue
		String emptyValue_normalization_pattern = rule.getEmptyValue();
		attributes[4] = (emptyValue_normalization_pattern != null) ? //
				DurationSimplification.simplify(RuleExpansion.applyRuleFunctions(key, emptyValue_normalization_pattern, m, norm, language)) : "";

		return attributes;
	}

	/**
	 * takes a desired locale input string, iterates through available locales, returns a locale object
	 * 
	 * @param locale
	 *                String to grab a locale for, i.e. en_US, en_GB, de_DE
	 * @return Locale to represent the input String
	 */
	public static Locale getLocaleFromString(String locale) throws LocaleException {
		for (Locale l : Locale.getAvailableLocales())
			if (locale.equalsIgnoreCase(l.toString()))
				return l;
		throw new LocaleException();
	}
}
