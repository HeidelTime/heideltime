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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.unihd.dbs.uima.annotator.heideltime.ProcessorManager.Priority;
import de.unihd.dbs.uima.annotator.heideltime.resources.GenericResourceManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RuleManager;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
import de.unihd.dbs.uima.annotator.heideltime.utilities.ContextAnalyzer;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;


/**
 * HeidelTime finds temporal expressions and normalizes them according to the TIMEX3 
 * TimeML annotation standard.
 * 
 * @author jannik stroetgen
 * 
 */
public class HeidelTime extends JCasAnnotator_ImplBase {

	// TOOL NAME (may be used as componentId)
	private Class<?> component = this.getClass();
	
	// PROCESSOR MANAGER
	ProcessorManager procMan = ProcessorManager.getInstance();

	// COUNTER (how many timexes added to CAS? (finally)
	public int timex_counter        = 0;
	public int timex_counter_global = 0;
	
	// COUNTER FOR TIMEX IDS
	private int timexID = 0;
	
	// INPUT PARAMETER HANDLING WITH UIMA
	private String PARAM_LANGUAGE         = "Language_english_german";
	// supported languages (2012-05-19): english, german, dutch, englishcoll, englishsci
	private String PARAM_TYPE_TO_PROCESS  = "Type_news_narratives";
	// supported types (2012-05-19): news (english, german, dutch), narrative (english, german, dutch), colloquial
	private Language language       = Language.ENGLISH;
	private String typeToProcess  = "news";
	
	// INPUT PARAMETER HANDLING WITH UIMA (which types shall be extracted)
	private String PARAM_DATE      = "Date";
	private String PARAM_TIME      = "Time";
	private String PARAM_DURATION  = "Duration";
	private String PARAM_SET       = "Set";
	private Boolean find_dates     = true;
	private Boolean find_times     = true;
	private Boolean find_durations = true;
	private Boolean find_sets      = true;
	
	// FOR DEBUGGING PURPOSES (IF FALSE)
	private Boolean deleteOverlapped = true;


	/**
	 * @see AnalysisComponent#initialize(UimaContext)
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/////////////////////////////////
		// DEBUGGING PARAMETER SETTING //
		/////////////////////////////////
		this.deleteOverlapped = true;
		Logger.setPrintDetails(false);
		
		//////////////////////////////////
		// GET CONFIGURATION PARAMETERS //
		//////////////////////////////////
		try {
			language = Language.getLanguageFromString((String) aContext.getConfigParameterValue(PARAM_LANGUAGE));
		} catch (HeidelTimeException e) {
			Logger.printError("Supplied language parameter was not recognized.");
			System.exit(-1);
		}
		
		typeToProcess  = (String)  aContext.getConfigParameterValue(PARAM_TYPE_TO_PROCESS);
		find_dates     = (Boolean) aContext.getConfigParameterValue(PARAM_DATE);
		find_times     = (Boolean) aContext.getConfigParameterValue(PARAM_TIME);
		find_durations = (Boolean) aContext.getConfigParameterValue(PARAM_DURATION);
		find_sets      = (Boolean) aContext.getConfigParameterValue(PARAM_SET);

		//////////////////////////////////////////
		// SET LANGUAGE FOR RESOURCE PROCESSING //
		//////////////////////////////////////////
		GenericResourceManager.LANGUAGE = language.getResourceFolder();

		////////////////////////////////////////////////////////////
		// READ NORMALIZATION RESOURCES FROM FILES AND STORE THEM //
		////////////////////////////////////////////////////////////
		NormalizationManager.getInstance();
		
		//////////////////////////////////////////////////////
		// READ PATTERN RESOURCES FROM FILES AND STORE THEM //
		//////////////////////////////////////////////////////
		RePatternManager.getInstance();
	
		///////////////////////////////////////////////////
		// READ RULE RESOURCES FROM FILES AND STORE THEM //
		///////////////////////////////////////////////////
		RuleManager.getInstance();
		
		/////////////////////////////////////////////////////////////////////////////////
		// SUBPROCESSOR CONFIGURATION. REGISTER YOUR OWN PROCESSORS HERE FOR EXECUTION //
		/////////////////////////////////////////////////////////////////////////////////
		procMan.registerProcessor("de.unihd.dbs.uima.annotator.heideltime.processors.HolidayProcessor");
		procMan.initializeAllProcessors(aContext);
		
		/////////////////////////////
		// PRINT WHAT WILL BE DONE //
		/////////////////////////////
		if (find_dates) Logger.printDetail("Getting Dates...");	
		if (find_times) Logger.printDetail("Getting Times...");	
		if (find_durations) Logger.printDetail("Getting Durations...");	
		if (find_sets) Logger.printDetail("Getting Sets...");
	}

	
	/**
	 * @see JCasAnnotator_ImplBase#process(JCas)
	 */
	public void process(JCas jcas) {
		// run preprocessing processors
		procMan.executeProcessors(jcas, Priority.PREPROCESSING);
		
		RuleManager rulem = RuleManager.getInstance();

		timex_counter = 0;
		
		////////////////////////////////////////////
		// CHECK SENTENCE BY SENTENCE FOR TIMEXES //
		////////////////////////////////////////////
		FSIterator sentIter = jcas.getAnnotationIndex(Sentence.type).iterator();
		while (sentIter.hasNext()) {
			Sentence s = (Sentence) sentIter.next();
			if (find_dates) {
				findTimexes("DATE", rulem.getHmDatePattern(), rulem.getHmDateOffset(), rulem.getHmDateNormalization(), rulem.getHmDateQuant(), s, jcas);
			}
			if (find_times) {
				findTimexes("TIME", rulem.getHmTimePattern(), rulem.getHmTimeOffset(), rulem.getHmTimeNormalization(), rulem.getHmTimeQuant(), s, jcas);
			}
			if (find_durations) {
				findTimexes("DURATION", rulem.getHmDurationPattern(), rulem.getHmDurationOffset(), rulem.getHmDurationNormalization(), rulem.getHmDurationQuant(), s, jcas);
			}
			if (find_sets) {
				findTimexes("SET", rulem.getHmSetPattern(), rulem.getHmSetOffset(), rulem.getHmSetNormalization(), rulem.getHmSetQuant(), s, jcas);
			}
		}

		/*
		 * get longest Timex expressions only (if needed)
		 */
		if (deleteOverlapped == true)
			// could be modified to: get longest TIMEX expressions of one type, only ???
			deleteOverlappedTimexes(jcas);

		/*
		 * specify ambiguous values, e.g.: specific year for date values of
		 * format UNDEF-year-01-01; specific month for values of format UNDEF-last-month
		 */
		specifyAmbiguousValues(jcas);
		
		// run arbitrary processors
		procMan.executeProcessors(jcas, Priority.ARBITRARY);
		
		// remove invalid timexes
		removeInvalids(jcas);
		
		// run postprocessing processors
		procMan.executeProcessors(jcas, Priority.POSTPROCESSING);

		timex_counter_global = timex_counter_global + timex_counter;
		Logger.printDetail(component, "Number of Timexes added to CAS: "+timex_counter + "(global: "+timex_counter_global+")");
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
	public void addTimexAnnotation(String timexType, int begin, int end, Sentence sentence, String timexValue, String timexQuant,
			String timexFreq, String timexMod, String timexId, String foundByRule, JCas jcas) {
		
		Timex3 annotation = new Timex3(jcas);
		annotation.setBegin(begin);
		annotation.setEnd(end);

		annotation.setFilename(sentence.getFilename());
		annotation.setSentId(sentence.getSentenceId());

		FSIterator iterToken = jcas.getAnnotationIndex(Token.type).subiterator(sentence);
		String allTokIds = "";
		while (iterToken.hasNext()) {
			Token tok = (Token) iterToken.next();
			if (tok.getBegin() == begin) {
				annotation.setFirstTokId(tok.getTokenId());
				allTokIds = "BEGIN<-->" + tok.getTokenId();
			}
			if ((tok.getBegin() > begin) && (tok.getEnd() <= end)) {
				allTokIds = allTokIds + "<-->" + tok.getTokenId();
			}
		}
		annotation.setAllTokIds(allTokIds);
		annotation.setTimexType(timexType);
		annotation.setTimexValue(timexValue);
		annotation.setTimexId(timexId);
		annotation.setFoundByRule(foundByRule);
		if ((timexType.equals("DATE")) || (timexType.equals("TIME"))) {
			if ((timexValue.startsWith("X")) || (timexValue.startsWith("UNDEF"))) {
				annotation.setFoundByRule(foundByRule+"-relative");
			} else {
				annotation.setFoundByRule(foundByRule+"-explicit");
			}
		}
		if (!(timexQuant == null)) {
			annotation.setTimexQuant(timexQuant);
		}
		if (!(timexFreq == null)) {
			annotation.setTimexFreq(timexFreq);
		}
		if (!(timexMod == null)) {
			annotation.setTimexMod(timexMod);
		}
		annotation.addToIndexes();
		this.timex_counter++;
		
		Logger.printDetail(annotation.getTimexId()+"EXTRACTION PHASE:   "+" found by:"+annotation.getFoundByRule()+" text:"+annotation.getCoveredText());
		Logger.printDetail(annotation.getTimexId()+"NORMALIZATION PHASE:"+" found by:"+annotation.getFoundByRule()+" text:"+annotation.getCoveredText()+" value:"+annotation.getTimexValue());
		
	}

	
	/**
	 * Postprocessing: Remove invalid timex expressions. These are already
	 * marked as invalid: timexValue().equals("REMOVE")
	 * 
	 * @param jcas
	 */
	public void removeInvalids(JCas jcas) {

		/*
		 * Iterate over timexes and add invalids to HashSet 
		 * (invalids cannot be removed directly since iterator is used)
		 */
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		HashSet<Timex3> hsTimexToRemove = new HashSet<Timex3>();
		while (iterTimex.hasNext()) {
			Timex3 timex = (Timex3) iterTimex.next();
			if (timex.getTimexValue().equals("REMOVE")) {
				hsTimexToRemove.add(timex);
			}
		}

		// remove invalids, finally
		for (Timex3 timex3 : hsTimexToRemove) {
			timex3.removeFromIndexes();
			this.timex_counter--;
			Logger.printDetail(timex3.getTimexId()+" REMOVING PHASE: "+"found by:"+timex3.getFoundByRule()+" text:"+timex3.getCoveredText()+" value:"+timex3.getTimexValue());
		}
	}

	
	/**
	 * Under-specified values are disambiguated here. Only Timexes of types "date" and "time" can be under-specified.
	 * @param jcas
	 */
	@SuppressWarnings("unused")
	public void specifyAmbiguousValues(JCas jcas) {
		NormalizationManager norm = NormalizationManager.getInstance();

		// build up a list with all found TIMEX expressions
		List<Timex3> linearDates = new ArrayList<Timex3>();
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();

		// Create List of all Timexes of types "date" and "time"
		while (iterTimex.hasNext()) {
			Timex3 timex = (Timex3) iterTimex.next();
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME")) {
				linearDates.add(timex);
			}
		}
		
		////////////////////////////////////////
		// IS THERE A DOCUMENT CREATION TIME? //
		////////////////////////////////////////
		boolean dctAvailable = false;
		
		//////////////////////////////
		// DOCUMENT TYPE TO PROCESS //
		////////////////////////////
		boolean documentTypeNews       = false;
		boolean documentTypeNarrative  = false;
		boolean documentTypeColloquial = false;
		boolean documentTypeScientific = false;
		if (typeToProcess.equals("news")) {
			documentTypeNews = true;
		}
		if (typeToProcess.equals("narrative")) {
			documentTypeNarrative = true;
		}
		if (typeToProcess.equals("colloquial")) {
			documentTypeColloquial = true;
		}
		if (typeToProcess.equals("scientific")) {
			documentTypeScientific = true;
		}
		
		// get the dct information
		String dctValue   = "";
		int dctCentury    = 0;
		int dctYear       = 0;
		int dctDecade     = 0;
		int dctMonth      = 0;
		int dctDay        = 0;
		String dctSeason  = "";
		String dctQuarter = "";
		String dctHalf    = "";
		int dctWeekday    = 0;
		int dctWeek       = 0;
		
		//////////////////////////////////////////////
		// INFORMATION ABOUT DOCUMENT CREATION TIME //
		//////////////////////////////////////////////
		FSIterator dctIter = jcas.getAnnotationIndex(Dct.type).iterator();
		if (dctIter.hasNext()) {
			dctAvailable = true;
			Dct dct = (Dct) dctIter.next();
			dctValue = dct.getValue();
			// year, month, day as mentioned in the DCT
			if (dctValue.matches("\\d\\d\\d\\d\\d\\d\\d\\d")) {
				dctCentury   = Integer.parseInt(dctValue.substring(0, 2));
				dctYear      = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade    = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth     = Integer.parseInt(dctValue.substring(4, 6));
				dctDay       = Integer.parseInt(dctValue.substring(6, 8));
				
				Logger.printDetail("dctCentury:"+dctCentury);
				Logger.printDetail("dctYear:"+dctYear);
				Logger.printDetail("dctDecade:"+dctDecade);
				Logger.printDetail("dctMonth:"+dctMonth);
				Logger.printDetail("dctDay:"+dctDay);
			} else {
				dctCentury   = Integer.parseInt(dctValue.substring(0, 2));
				dctYear      = Integer.parseInt(dctValue.substring(0, 4));
				dctDecade    = Integer.parseInt(dctValue.substring(2, 3));
				dctMonth     = Integer.parseInt(dctValue.substring(5, 7));
				dctDay       = Integer.parseInt(dctValue.substring(8, 10));
				
				Logger.printDetail("dctCentury:"+dctCentury);
				Logger.printDetail("dctYear:"+dctYear);
				Logger.printDetail("dctDecade:"+dctDecade);
				Logger.printDetail("dctMonth:"+dctMonth);
				Logger.printDetail("dctDay:"+dctDay);
			}
			dctQuarter = "Q"+norm.getFromNormMonthInQuarter(norm.getFromNormNumber(dctMonth+""));
			dctHalf = "H1";
			if (dctMonth > 6) {
				dctHalf = "H2";
			}
			
			// season, week, weekday, have to be calculated
			dctSeason    = norm.getFromNormMonthInSeason(norm.getFromNormNumber(dctMonth+"")+"");
			dctWeekday   = DateCalculator.getWeekdayOfDate(dctYear+"-"+norm.getFromNormNumber(dctMonth+"")+"-"+norm.getFromNormNumber(dctDay+""));
			dctWeek      = DateCalculator.getWeekOfDate(dctYear+"-"+norm.getFromNormNumber(dctMonth+"") +"-"+norm.getFromNormNumber(dctDay+""));
			
			Logger.printDetail("dctQuarter:"+dctQuarter);
			Logger.printDetail("dctSeason:"+dctSeason);
			Logger.printDetail("dctWeekday:"+dctWeekday);
			Logger.printDetail("dctWeek:"+dctWeek);
		} else {
			Logger.printDetail("No DCT available...");
		}
		
		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		for (int i = 0; i < linearDates.size(); i++) {
			Timex3 t_i = (Timex3) linearDates.get(i);
			String value_i = t_i.getTimexValue();

			// check if value_i has month, day, season, week (otherwise no UNDEF-year is possible)
			Boolean viHasMonth   = false;
			Boolean viHasDay     = false;
			Boolean viHasSeason  = false;
			Boolean viHasWeek    = false;
			Boolean viHasQuarter = false;
			Boolean viHasHalf    = false;
			int viThisMonth      = 0;
			int viThisDay        = 0;
			String viThisSeason  = "";
			String viThisQuarter = "";
			String viThisHalf    = "";
			String[] valueParts  = value_i.split("-");
			// check if UNDEF-year or UNDEF-century
			if ((value_i.startsWith("UNDEF-year")) || (value_i.startsWith("UNDEF-century"))) {
				if (valueParts.length > 2) {
					// get vi month
					if (valueParts[2].matches("\\d\\d")) {
						viHasMonth  = true;
						viThisMonth = Integer.parseInt(valueParts[2]);
					}
					// get vi season
					else if ((valueParts[2].equals("SP")) || (valueParts[2].equals("SU")) || (valueParts[2].equals("FA")) || (valueParts[2].equals("WI"))) {
						viHasSeason  = true;
						viThisSeason = valueParts[2]; 
					}
					// get v1 quarter
					else if ((valueParts[2].equals("Q1")) || (valueParts[2].equals("Q2")) || (valueParts[2].equals("Q3")) || (valueParts[2].equals("Q4"))) {
						viHasQuarter  = true;
						viThisQuarter = valueParts[2]; 
					}
					else if ((valueParts[2].equals("H1")) || (valueParts[2].equals("H2"))) {
						viHasHalf  = true;
						viThisHalf = valueParts[2];
					}
					// get vi day
					if ((valueParts.length > 3) && (valueParts[3].matches("\\d\\d"))) {
						viHasDay = true;
						viThisDay = Integer.parseInt(valueParts[3]);
					}
				}
			}
			else {
				if (valueParts.length > 1) {
					// get vi month
					if (valueParts[1].matches("\\d\\d")) {
						viHasMonth  = true;
						viThisMonth = Integer.parseInt(valueParts[1]);
					}
					// get vi season
					else if ((valueParts[1].equals("SP")) || (valueParts[1].equals("SU")) || (valueParts[1].equals("FA")) || (valueParts[1].equals("WI"))) {
						viHasSeason  = true;
						viThisSeason = valueParts[1]; 
					}
					// get vi day
					if ((valueParts.length > 2) && (valueParts[2].matches("\\d\\d"))) {
						viHasDay = true;
						viThisDay = Integer.parseInt(valueParts[2]);
					}
				}
			}
			// get the last tense (depending on the part of speech tags used in front or behind the expression)
			String last_used_tense = ContextAnalyzer.getLastTense(t_i, jcas);

			//////////////////////////
			// DISAMBIGUATION PHASE //
			//////////////////////////
			
			////////////////////////////////////////////////////
			// IF YEAR IS COMPLETELY UNSPECIFIED (UNDEF-year) // 
			////////////////////////////////////////////////////
			String valueNew = value_i;
			if (value_i.startsWith("UNDEF-year")) {
				String newYearValue = dctYear+"";
				// vi has month (ignore day)
				if (viHasMonth == true && (viHasSeason == false)) {
					// WITH DOCUMENT CREATION TIME
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						//  Tense is FUTURE
						if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
							// if dct-month is larger than vi-month, than add 1 to dct-year
							if (dctMonth > viThisMonth) {
								int intNewYear = dctYear + 1;
								newYearValue = intNewYear + "";
							}
						}
						// Tense is PAST
						if ((last_used_tense.equals("PAST"))) {
							// if dct-month is smaller than vi month, than substrate 1 from dct-year						
							if (dctMonth < viThisMonth) {
								int intNewYear = dctYear - 1;
								newYearValue = intNewYear + "";
							}
						}
					}
					// WITHOUT DOCUMENT CREATION TIME
					else {
						newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
					}
				}
				// vi has quaurter
				if (viHasQuarter == true) {
					// WITH DOCUMENT CREATION TIME
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						//  Tense is FUTURE
						if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
							if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dctYear + 1;
								newYearValue = intNewYear + "";
							}
						}
						// Tense is PAST
						if ((last_used_tense.equals("PAST"))) {
							if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))) {
								int intNewYear = dctYear - 1;
								newYearValue = intNewYear + "";
							}
						}
						// IF NO TENSE IS FOUND
						if (last_used_tense.equals("")){
							if (documentTypeColloquial){
								// IN COLLOQUIAL: future temporal expressions
								if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))){
									int intNewYear = dctYear + 1;
									newYearValue = intNewYear + "";
								}
							}
							else{
								// IN NEWS: past temporal expressions
								if (Integer.parseInt(dctQuarter.substring(1)) < Integer.parseInt(viThisQuarter.substring(1))){
									int intNewYear = dctYear - 1;
									newYearValue = intNewYear + "";
								}
							}
						}
					}
					// WITHOUT DOCUMENT CREATION TIME
					else {
						newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
					}
				}
				// vi has half
				if (viHasHalf == true) {
					// WITH DOCUMENT CREATION TIME
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						//  Tense is FUTURE
						if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
							if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dctYear + 1;
								newYearValue = intNewYear + "";
							}
						}
						// Tense is PAST
						if ((last_used_tense.equals("PAST"))) {
							if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))) {
								int intNewYear = dctYear - 1;
								newYearValue = intNewYear + "";
							}
						}
						// IF NO TENSE IS FOUND
						if (last_used_tense.equals("")){
							if (documentTypeColloquial){
								// IN COLLOQUIAL: future temporal expressions
								if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))){
									int intNewYear = dctYear + 1;
									newYearValue = intNewYear + "";
								}
							}
							else{
								// IN NEWS: past temporal expressions
								if (Integer.parseInt(dctHalf.substring(1)) < Integer.parseInt(viThisHalf.substring(1))){
									int intNewYear = dctYear - 1;
									newYearValue = intNewYear + "";
								}
							}
						}
					}
					// WITHOUT DOCUMENT CREATION TIME
					else {
						newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
					}
				}
				
				// vi has season
				if ((viHasMonth == false) && (viHasDay == false) && (viHasSeason == true)) {
					// TODO check tenses?
					// WITH DOCUMENT CREATION TIME
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						newYearValue = dctYear+"";
					}
					// WITHOUT DOCUMENT CREATION TIME
					else {
						newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
					}
				}
				// vi has week
				if (viHasWeek) {
					// WITH DOCUMENT CREATION TIME
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						newYearValue = dctYear+"";
					}
					// WITHOUT DOCUMENT CREATION TIME
					else {
						newYearValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
					}
				}

				// REPLACE THE UNDEF-YEAR WITH THE NEWLY CALCULATED YEAR AND ADD TIMEX TO INDEXES
				if (newYearValue.equals("")) {
					valueNew = value_i.replaceFirst("UNDEF-year", "XXXX");
				}
				else {
					valueNew = value_i.replaceFirst("UNDEF-year", newYearValue);
				}
			}

			///////////////////////////////////////////////////
			// just century is unspecified (UNDEF-century86) //
			///////////////////////////////////////////////////
			else if ((value_i.startsWith("UNDEF-century"))) {
				String newCenturyValue = dctCentury+"";
				int viThisDecade = Integer.parseInt(value_i.substring(13, 14));
				// NEWS and COLLOQUIAL DOCUMENTS
				if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
					Logger.printDetail("dctCentury"+dctCentury);
					
					newCenturyValue = dctCentury+"";
					Logger.printDetail("dctCentury"+dctCentury);
					
					//  Tense is FUTURE
					if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
						if (viThisDecade < dctDecade) {
							newCenturyValue = dctCentury + 1+"";
						} else {
							newCenturyValue = dctCentury+"";
						}
					}
					// Tense is PAST
					if ((last_used_tense.equals("PAST"))) {
						if (dctDecade <= viThisDecade) {
							newCenturyValue = dctCentury - 1+"";
						} else {
							newCenturyValue = dctCentury+"";
						}
					}
//					// IF NO TENSE IS FOUND
//					if (last_used_tense.equals("")){
//						if (documentTypeColloquial){
//							// IN COLLOQUIAL: future temporal expressions
//							if (viThisDecade < dctDecade){
//								newCenturyValue = dctCentury + 1+"";
//							}
//							else{
//								newCenturyValue = dctCentury+"";
//							}
//						}
//						else{
//							// IN NEWS: past temporal expressions
//							if (dctDecade <= viThisDecade){
//								newCenturyValue = dctCentury - 1+"";
//							}
//							else{
//								newCenturyValue = dctCentury+"";
//							}
//						}
//					}
				}
				// NARRATIVE DOCUMENTS
				else {
					newCenturyValue = ContextAnalyzer.getLastMentionedX(linearDates, i, "century");
				}
				if (newCenturyValue.equals("")) {
					// always assume that sixties, twenties, and so on are 19XX (changed 2011-09-08)
					valueNew = value_i.replaceFirst("UNDEF-century", "19");
				}
				else {
					valueNew = value_i.replaceFirst("UNDEF-century", newCenturyValue+"");
				}
				// always assume that sixties, twenties, and so on are 19XX (changed 2011-09-08)
				if (valueNew.matches("\\d\\d\\dX")) {
					valueNew = "19" + valueNew.substring(2);
				}
			}
			
			////////////////////////////////////////////////////
			// CHECK IMPLICIT EXPRESSIONS STARTING WITH UNDEF //
			////////////////////////////////////////////////////
			else if (value_i.startsWith("UNDEF")) {
				valueNew = value_i;
				
				//////////////////
				// TO CALCULATE //
				//////////////////
				// year to calculate
				if (value_i.matches("^UNDEF-(this|REFUNIT|REF)-(.*?)-(MINUS|PLUS)-([0-9]+).*")) {
					for (MatchResult mr : Toolbox.findMatches(Pattern.compile("^(UNDEF-(this|REFUNIT|REF)-(.*?)-(MINUS|PLUS)-([0-9]+)).*"), value_i)) {
						String checkUndef = mr.group(1);
						String ltn  = mr.group(2);
						String unit = mr.group(3);
						String op   = mr.group(4);
						int diff    = Integer.parseInt(mr.group(5));
						
						// do the processing for SCIENTIFIC documents (TPZ identification could be improved)
						if ((documentTypeScientific)){
							String opSymbol = "-";
							if (op.equals("PLUS")){
								opSymbol = "+";
							}
							if (unit.equals("year")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "000"+diff;
								}
								else if (diff < 100){
									diffString = "00"+diff;
								}
								else if (diff < 1000){
									diffString = "0"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
							else if (unit.equals("month")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-0"+diff;
								}
								else {
									diffString = "0000-"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
							else if (unit.equals("week")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-W0"+diff;
								}
								else {
									diffString = "0000-W"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
							else if (unit.equals("day")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-00-0"+diff;
								}
								else {
									diffString = "0000-00-"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
							else if (unit.equals("hour")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-00-00T0"+diff;
								}
								else {
									diffString = "0000-00-00T"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
							else if (unit.equals("minute")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-00-00T00:0"+diff;
								}
								else {
									diffString = "0000-00-00T00:"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;								
							}							
							else if (unit.equals("second")){
								String diffString = diff+"";
								if (diff < 10){
									diffString = "0000-00-00T00:00:0"+diff;
								}
								else {
									diffString = "0000-00-00T00:00:"+diff;
								}
								valueNew = "TPZ"+opSymbol+diffString;
							}
						}
						else{	
							
							
							// check for REFUNIT (only allowed for "year")
							if ((ltn.equals("REFUNIT")) && (unit.equals("year"))) {
								String dateWithYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "dateYear");
								if (dateWithYear.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX");
								} else {
									if (op.equals("MINUS")) {
										diff = diff * (-1);
									}
									int yearNew = Integer.parseInt(dateWithYear.substring(0,4)) + diff;
									String rest = dateWithYear.substring(4);
									valueNew = valueNew.replace(checkUndef, yearNew+rest);
								}
							}
							
							
							// REF and this are handled here
							if (unit.equals("century")) {
								if ((documentTypeNews|documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									int century = dctCentury;
									if (op.equals("MINUS")) {
										century = dctCentury - diff;
									} else if (op.equals("PLUS")) {
										century = dctCentury + diff;
									}
									valueNew = valueNew.replace(checkUndef, century+"XX");
								} else {
									String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates, i, "century");
									if (lmCentury.equals("")) {
										valueNew = valueNew.replace(checkUndef, "XX");
									} else {
										if (op.equals("MINUS")) {
											lmCentury = Integer.parseInt(lmCentury) - diff + "XX";
										} else if (op.equals("PLUS")) {
											lmCentury = Integer.parseInt(lmCentury) + diff + "XX";	
										}
										valueNew = valueNew.replace(checkUndef, lmCentury);
									}
								}
							} else if (unit.equals("decade")) {
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									int decade = dctDecade;
									if (op.equals("MINUS")) {
										decade = dctDecade - diff;
									} else if (op.equals("PLUS")) {
										decade = dctDecade + diff;
									}
									valueNew = valueNew.replace(checkUndef, decade+"X");
								} else {
									String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates, i, "decade");
									if (lmDecade.equals("")) {
										valueNew = valueNew.replace(checkUndef, "XXX");
									} else {
										if (op.equals("MINUS")) {
											lmDecade = Integer.parseInt(lmDecade) - diff + "X";
										} else if (op.equals("PLUS")) {
											lmDecade = Integer.parseInt(lmDecade) + diff + "X";
										}
										valueNew = valueNew.replace(checkUndef, lmDecade);
									}
								}
							} else if (unit.equals("year")) {
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									int intValue = dctYear;
									if (op.equals("MINUS")) {
										intValue = dctYear - diff;
									} else if (op.equals("PLUS")) {
										intValue = dctYear + diff;
									}
									valueNew = valueNew.replace(checkUndef, intValue + "");
								} else {
									String lmYear = ContextAnalyzer.getLastMentionedX(linearDates, i, "year");
									if (lmYear.equals("")) {
										valueNew = valueNew.replace(checkUndef, "XXXX");
									} else {
										int intValue = Integer.parseInt(lmYear);
										if (op.equals("MINUS")) {
											intValue = Integer.parseInt(lmYear) - diff;	
										} else if (op.equals("PLUS")) {
											intValue = Integer.parseInt(lmYear) + diff;
										}
										valueNew = valueNew.replace(checkUndef, intValue+"");
									}
								}
							} else if (unit.equals("quarter")) {
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									int intYear    = dctYear;
									int intQuarter = Integer.parseInt(dctQuarter.substring(1));
									int diffQuarters = diff % 4;
									diff = diff - diffQuarters;
									int diffYears    = diff / 4;
									if (op.equals("MINUS")) {
										diffQuarters = diffQuarters * (-1);
										diffYears    = diffYears    * (-1);
									}
									intYear    = intYear + diffYears;
									intQuarter = intQuarter + diffQuarters; 
									valueNew = valueNew.replace(checkUndef, intYear+"-Q"+intQuarter);
								} else {
									String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter");
									if (lmQuarter.equals("")) {
										valueNew = valueNew.replace(checkUndef, "XXXX-XX");
									} else {
										int intYear    = Integer.parseInt(lmQuarter.substring(0, 4));
										int intQuarter = Integer.parseInt(lmQuarter.substring(6)); 
										int diffQuarters = diff % 4;
										diff = diff - diffQuarters;
										int diffYears    = diff / 4;
										if (op.equals("MINUS")) {
											diffQuarters = diffQuarters * (-1);
											diffYears    = diffYears    * (-1);
										}
										intYear    = intYear + diffYears;
										intQuarter = intQuarter + diffQuarters; 
										valueNew = valueNew.replace(checkUndef, intYear+"-Q"+intQuarter);
									}
								}
							} else if (unit.equals("month")) {
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									if (op.equals("MINUS")) {
										diff = diff * (-1);
									}
									valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.getFromNormNumber(dctMonth+""), diff));
								} else {
									String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month");
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
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									if (op.equals("MINUS")) {
										diff = diff * 7 * (-1);
									} else if (op.equals("PLUS")) {
										diff = diff * 7;
									}
									valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.getFromNormNumber(dctMonth+"") + "-"	+ dctDay, diff));
								} else {
									String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
								if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable) && (ltn.equals("this"))) {
									if (op.equals("MINUS")) {
										diff = diff * (-1);
									}
									valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.getFromNormNumber(dctMonth+"") + "-"	+ dctDay, diff));
								} else {
									String lmDay = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
				}
			
				// century
				else if (value_i.startsWith("UNDEF-last-century")) {
					String checkUndef = "UNDEF-last-century";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(dctCentury - 1 +"") + "XX");
					} else {
						String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates,i,"century");
						if (lmCentury.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(Integer.parseInt(lmCentury) - 1 +"") + "XX");
						}
					}
				} else if (value_i.startsWith("UNDEF-this-century")) {
					String checkUndef = "UNDEF-this-century";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(dctCentury+"") + "XX");
					} else {
						String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates,i,"century");
						if (lmCentury.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(Integer.parseInt(lmCentury)+"") + "XX");
						}
					}
				} else if (value_i.startsWith("UNDEF-next-century")) {
					String checkUndef = "UNDEF-next-century";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(dctCentury + 1+"") + "XX");
					} else {
						String lmCentury = ContextAnalyzer.getLastMentionedX(linearDates,i,"century");
						if (lmCentury.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, norm.getFromNormNumber(Integer.parseInt(lmCentury) + 1+"") + "XX");
						}
					}
				}

				// decade
				else if (value_i.startsWith("UNDEF-last-decade")) {
					String checkUndef = "UNDEF-last-decade";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, (dctYear - 10+"").substring(0,3)+"X");
					} else {
						String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates,i,"decade");
						if (lmDecade.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmDecade)-1+"X");
						}
					}
				} else if (value_i.startsWith("UNDEF-this-decade")) {
					String checkUndef = "UNDEF-this-decade";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, (dctYear+"").substring(0,3)+"X");
					} else {
						String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates,i,"decade");
						if (lmDecade.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmDecade+"X");						
						}
					}
				} else if (value_i.startsWith("UNDEF-next-decade")) {
					String checkUndef = "UNDEF-next-decade";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, (dctYear + 10+"").substring(0,3)+"X");
					} else {
						String lmDecade = ContextAnalyzer.getLastMentionedX(linearDates,i,"decade");
						if (lmDecade.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmDecade)+1+"X");
						}
					}
				}
				
				// year
				else if (value_i.startsWith("UNDEF-last-year")) {
					String checkUndef = "UNDEF-last-year";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear -1 +"");
					} else {
						String lmYear = ContextAnalyzer.getLastMentionedX(linearDates,i,"year");
						if (lmYear.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmYear)-1+"");
						}
					}
				} else if (value_i.startsWith("UNDEF-this-year")) {
					String checkUndef = "UNDEF-this-year";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear +"");
					} else {
						String lmYear = ContextAnalyzer.getLastMentionedX(linearDates,i,"year");
						if (lmYear.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmYear);
						}
					}
				} else if (value_i.startsWith("UNDEF-next-year")) {
					String checkUndef = "UNDEF-next-year";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear +1 +"");	
					} else {
						String lmYear = ContextAnalyzer.getLastMentionedX(linearDates,i,"year");
						if (lmYear.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX");
						} else {
							valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmYear)+1+"");						
						}
					}
				}
				
				// month
				else if (value_i.startsWith("UNDEF-last-month")) {
					String checkUndef = "UNDEF-last-month";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.getFromNormNumber(dctMonth+""), -1));
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates,i,"month");
						if (lmMonth.equals("")) {
							valueNew =  valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, -1));
						}
					}
				} else if (value_i.startsWith("UNDEF-this-month")) {
					String checkUndef = "UNDEF-this-month";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear + "-" + norm.getFromNormNumber(dctMonth+""));
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates,i,"month");
						if (lmMonth.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else { 
							valueNew = valueNew.replace(checkUndef, lmMonth);
						}
					}
				}
				else if (value_i.startsWith("UNDEF-next-month")) {
					String checkUndef = "UNDEF-next-month";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(dctYear + "-" + norm.getFromNormNumber(dctMonth+""), 1));
					} else {
						String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates,i,"month");
						if (lmMonth.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextMonth(lmMonth, 1));
						}
					}
				}
				
				// day
				else if (value_i.startsWith("UNDEF-last-day")) {
					String checkUndef = "UNDEF-last-day";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.getFromNormNumber(dctMonth+"") + "-"+ dctDay, -1));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedX(linearDates,i,"day");
						if (lmDay.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay,-1));
						}
					}
				} else if (value_i.startsWith("UNDEF-this-day")) {
					String checkUndef = "UNDEF-this-day";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear + "-" + norm.getFromNormNumber(dctMonth+"") + "-"+ norm.getFromNormNumber(dctDay+""));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedX(linearDates,i,"day");
						if (lmDay.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmDay);
						}
						if (value_i.equals("UNDEF-this-day")) {
							valueNew = "PRESENT_REF"; 
						}
					}					
				}
				else if (value_i.startsWith("UNDEF-next-day")) {
					String checkUndef = "UNDEF-next-day";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + norm.getFromNormNumber(dctMonth+"") + "-"+ dctDay, 1));
					} else {
						String lmDay = ContextAnalyzer.getLastMentionedX(linearDates,i,"day");
						if (lmDay.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-XX-XX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(lmDay,1));
						}
					}	
				}

				// week
				else if (value_i.startsWith("UNDEF-last-week")) {
					String checkUndef = "UNDEF-last-week";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dctYear+"-W"+norm.getFromNormNumber(dctWeek+""),-1));
					} else {
						String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates,i,"week");
						if (lmWeek.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(lmWeek,-1));
						}
					}
				} else if (value_i.startsWith("UNDEF-this-week")) {
					String checkUndef = "UNDEF-this-week";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef,dctYear+"-W"+norm.getFromNormNumber(dctWeek+""));
					} else {
						String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates,i,"week");
						if (lmWeek.equals("")) {
							valueNew = valueNew.replace(checkUndef,"XXXX-WXX");
						} else {
							valueNew = valueNew.replace(checkUndef,lmWeek);
						}
					}					
				} else if (value_i.startsWith("UNDEF-next-week")) {
					String checkUndef = "UNDEF-next-week";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(dctYear+"-W"+norm.getFromNormNumber(dctWeek+""),1));
					} else {
						String lmWeek = ContextAnalyzer.getLastMentionedX(linearDates,i,"week");
						if (lmWeek.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-WXX");
						} else {
							valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextWeek(lmWeek,1));
						}
					}
				}
				
				// quarter
				else if (value_i.startsWith("UNDEF-last-quarter")) {
					String checkUndef = "UNDEF-last-quarter";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						if (dctQuarter.equals("Q1")) {
							valueNew = valueNew.replace(checkUndef, dctYear-1+"-Q4");
						} else {
							int newQuarter = Integer.parseInt(dctQuarter.substring(1,2))-1;
							valueNew = valueNew.replace(checkUndef, dctYear+"-Q"+newQuarter);
						}
					} else {
						String lmQuarter  = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter");
						if (lmQuarter.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-QX");
						} else {
							int lmQuarterOnly = Integer.parseInt(lmQuarter.substring(6,7));
							int lmYearOnly    = Integer.parseInt(lmQuarter.substring(0,4));
							if (lmQuarterOnly == 1) {
								valueNew = valueNew.replace(checkUndef, lmYearOnly-1+"-Q4");
							} else {
								int newQuarter = lmQuarterOnly-1;
								valueNew = valueNew.replace(checkUndef, dctYear+"-Q"+newQuarter);
							}
						}
					}
				} else if (value_i.startsWith("UNDEF-this-quarter")) {
					String checkUndef = "UNDEF-this-quarter";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						valueNew = valueNew.replace(checkUndef, dctYear+"-"+dctQuarter);
					} else {
						String lmQuarter = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter");
						if (lmQuarter.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-QX");
						} else {
							valueNew = valueNew.replace(checkUndef, lmQuarter);
						}
					}					
				} else if (value_i.startsWith("UNDEF-next-quarter")) {
					String checkUndef = "UNDEF-next-quarter";
					if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
						if (dctQuarter.equals("Q4")) {
							valueNew = valueNew.replace(checkUndef, dctYear+1+"-Q1");
						} else {
							int newQuarter = Integer.parseInt(dctQuarter.substring(1,2))+1;
							valueNew = valueNew.replace(checkUndef, dctYear+"-Q"+newQuarter);
						}						
					} else {
						String lmQuarter  = ContextAnalyzer.getLastMentionedX(linearDates, i, "quarter");
						if (lmQuarter.equals("")) {
							valueNew = valueNew.replace(checkUndef, "XXXX-QX");
						} else {
							int lmQuarterOnly = Integer.parseInt(lmQuarter.substring(6,7));
							int lmYearOnly    = Integer.parseInt(lmQuarter.substring(0,4));
							if (lmQuarterOnly == 4) {
								valueNew = valueNew.replace(checkUndef, lmYearOnly+1+"-Q1");
							} else {
								int newQuarter = lmQuarterOnly+1;
								valueNew = valueNew.replace(checkUndef, dctYear+"-Q"+newQuarter);
							}
						}
					}
				}
				
				// MONTH NAMES
				else if (value_i.matches("UNDEF-(last|this|next)-(january|february|march|april|may|june|july|august|september|october|november|december).*")) {
					for (MatchResult mr : Toolbox.findMatches(Pattern.compile("(UNDEF-(last|this|next)-(january|february|march|april|may|june|july|august|september|october|november|december)).*"),value_i)) {
						String checkUndef = mr.group(1);
						String ltn      = mr.group(2);
						String newMonth = norm.getFromNormMonthName((mr.group(3)));
						int newMonthInt = Integer.parseInt(newMonth);
						if (ltn.equals("last")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								if (dctMonth <= newMonthInt) {
									valueNew = valueNew.replace(checkUndef, dctYear-1+"-"+newMonth);
								} else {
									valueNew = valueNew.replace(checkUndef, dctYear+"-"+newMonth);
								}
							} else {
								String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month");
								if (lmMonth.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									int lmMonthInt = Integer.parseInt(lmMonth.substring(5,7));
									if (lmMonthInt <= newMonthInt) {
										valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmMonth.substring(0,4))-1+"-"+newMonth);
									} else {
										valueNew = valueNew.replace(checkUndef, lmMonth.substring(0,4)+"-"+newMonth);
									}
								}
							}
						} else if (ltn.equals("this")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								valueNew = valueNew.replace(checkUndef, dctYear+"-"+newMonth);
							} else {
								String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month");
								if (lmMonth.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									valueNew = valueNew.replace(checkUndef, lmMonth.substring(0,4)+"-"+newMonth);
								}
							}
						} else if (ltn.equals("next")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								if (dctMonth >= newMonthInt) {
									valueNew = valueNew.replace(checkUndef, dctYear+1+"-"+newMonth);
								} else {
									valueNew = valueNew.replace(checkUndef, dctYear+"-"+newMonth);
								}
							} else {
								String lmMonth = ContextAnalyzer.getLastMentionedX(linearDates, i, "month");
								if (lmMonth.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									int lmMonthInt = Integer.parseInt(lmMonth.substring(5,7));
									if (lmMonthInt >= newMonthInt) {
										valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmMonth.substring(0,4))+1+"-"+newMonth);
									} else {
										valueNew = valueNew.replace(checkUndef, lmMonth.substring(0,4)+"-"+newMonth);
									}
								}	
							}
						}
					}
				}
				
				// SEASONS NAMES
				else if (value_i.matches("^UNDEF-(last|this|next)-(SP|SU|FA|WI).*")) {
					for (MatchResult mr : Toolbox.findMatches(Pattern.compile("(UNDEF-(last|this|next)-(SP|SU|FA|WI)).*"),value_i)) {
						String checkUndef = mr.group(1);
						String ltn       = mr.group(2);
						String newSeason = mr.group(3);
						if (ltn.equals("last")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								if (dctSeason.equals("SP")) {
									valueNew = valueNew.replace(checkUndef, dctYear-1+"-"+newSeason);
								} else if (dctSeason.equals("SU")) {
									if (newSeason.equals("SP")) {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear-1+"-"+newSeason);
									}
								} else if (dctSeason.equals("FA")) {
									if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear-1+"-"+newSeason);
									}
								} else if (dctSeason.equals("WI")) {
									if (newSeason.equals("WI")) {
										valueNew = valueNew.replace(checkUndef, dctYear-1+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									}
								}
							} else { // NARRATVIE DOCUMENT
								String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season");
								if (lmSeason.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									if (lmSeason.substring(5,7).equals("SP")) {
										valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))-1+"-"+newSeason);
									} else if (lmSeason.substring(5,7).equals("SU")) {
										if (lmSeason.substring(5,7).equals("SP")) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))-1+"-"+newSeason);
										}
									} else if (lmSeason.substring(5,7).equals("FA")) {
										if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))-1+"-"+newSeason);
										}
									} else if (lmSeason.substring(5,7).equals("WI")) {
										if (newSeason.equals("WI")) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))-1+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										}
									}
								}
							}
						} else if (ltn.equals("this")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								// TODO include tense of sentence?
								valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
							} else {
								// TODO include tense of sentence?
								String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season");
								if (lmSeason.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									valueNew = valueNew.replace(checkUndef, lmSeason.substring(0,4)+"-"+newSeason);
								}
							}
						} else if (ltn.equals("next")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								if (dctSeason.equals("SP")) {
									if (newSeason.equals("SP")) {
										valueNew = valueNew.replace(checkUndef, dctYear+1+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									}
								} else if (dctSeason.equals("SU")) {
									if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
										valueNew = valueNew.replace(checkUndef, dctYear+1+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									}
								} else if (dctSeason.equals("FA")) {
									if (newSeason.equals("WI")) {
										valueNew = valueNew.replace(checkUndef, dctYear+"-"+newSeason);
									} else {
										valueNew = valueNew.replace(checkUndef, dctYear+1+"-"+newSeason);
									}
								} else if (dctSeason.equals("WI")) {
									valueNew = valueNew.replace(checkUndef, dctYear+1+"-"+newSeason);
								}
							} else { // NARRATIVE DOCUMENT
								String lmSeason = ContextAnalyzer.getLastMentionedX(linearDates, i, "season");
								if (lmSeason.equals("")) {
									valueNew = valueNew.replace(checkUndef, "XXXX-XX");
								} else {
									if (lmSeason.substring(5,7).equals("SP")) {
										if (newSeason.equals("SP")) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+1+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										}
									} else if (lmSeason.substring(5,7).equals("SU")) {
										if ((newSeason.equals("SP")) || (newSeason.equals("SU"))) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+1+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										}
									} else if (lmSeason.substring(5,7).equals("FA")) {
										if (newSeason.equals("WI")) {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+"-"+newSeason);
										} else {
											valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+1+"-"+newSeason);
										}
									} else if (lmSeason.substring(5,7).equals("WI")) {
										valueNew = valueNew.replace(checkUndef, Integer.parseInt(lmSeason.substring(0,4))+1+"-"+newSeason);
									}
								}
							}
						}
					}
				}
				
				// WEEKDAY NAMES
				// TODO the calculation is strange, but works
				// TODO tense should be included?!
				else if (value_i.matches("^UNDEF-(last|this|next|day)-(monday|tuesday|wednesday|thursday|friday|saturday|sunday).*")) {
					for (MatchResult mr : Toolbox.findMatches(Pattern.compile("(UNDEF-(last|this|next|day)-(monday|tuesday|wednesday|thursday|friday|saturday|sunday)).*"),value_i)) {
						String checkUndef = mr.group(1);
						String ltnd       = mr.group(2);
						String newWeekday = mr.group(3);
						int newWeekdayInt = Integer.parseInt(norm.getFromNormDayInWeek(newWeekday));
						if (ltnd.equals("last")) {
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								int diff = (-1) * (dctWeekday - newWeekdayInt);
								if (diff >= 0) {
									diff = diff - 7;
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-" + dctDay, diff));
							} else {
								String lmDay     = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								// TODO tense should be included?!		
								int diff = (-1) * (dctWeekday - newWeekdayInt);
								if (diff >= 0) {
									diff = diff - 7;
								}
								if (diff == -7) {
									diff = 0;
								}
								
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-"+ dctDay, diff));
							} else {
								// TODO tense should be included?!
								String lmDay     = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								int diff = newWeekdayInt - dctWeekday;
								if (diff <= 0) {
									diff = diff + 7;
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-"+ dctDay, diff));
							} else {
								String lmDay     = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
							if ((documentTypeNews||documentTypeColloquial||documentTypeScientific) && (dctAvailable)) {
								// TODO tense should be included?!
								int diff = (-1) * (dctWeekday - newWeekdayInt);
								if (diff >= 0) {
									diff = diff - 7;
								}
								if (diff == -7) {
									diff = 0;
								}
								//  Tense is FUTURE
								if ((last_used_tense.equals("FUTURE")) || (last_used_tense.equals("PRESENTFUTURE"))) {
									diff = diff + 7;
								}
								// Tense is PAST
								if ((last_used_tense.equals("PAST"))) {
								
								}
								valueNew = valueNew.replace(checkUndef, DateCalculator.getXNextDay(dctYear + "-" + dctMonth + "-"+ dctDay, diff));
							} else {
								// TODO tense should be included?!
								String lmDay     = ContextAnalyzer.getLastMentionedX(linearDates, i, "day");
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
					Logger.printDetail(component, "ATTENTION: UNDEF value for: " + valueNew+" is not handled in disambiguation phase!");
				}
			}
			t_i.removeFromIndexes();
			Logger.printDetail(t_i.getTimexId()+" DISAMBIGUATION PHASE: foundBy:"+t_i.getFoundByRule()+" text:"+t_i.getCoveredText()+" value:"+t_i.getTimexValue()+" NEW value:"+valueNew);
			
			t_i.setTimexValue(valueNew);
			t_i.addToIndexes();
			linearDates.set(i, t_i);
		}
	}
	
	
	/**
	 * @param jcas
	 */
	public void deleteOverlappedTimexes(JCas jcas) {
		FSIterator timexIter1 = jcas.getAnnotationIndex(Timex3.type).iterator();
		HashSet<Timex3> hsTimexesToRemove = new HashSet<Timex3>();

		
		while (timexIter1.hasNext()) {
			Timex3 t1 = (Timex3) timexIter1.next();
			FSIterator timexIter2 = jcas.getAnnotationIndex(Timex3.type)
					.iterator();

			while (timexIter2.hasNext()) {
				Timex3 t2 = (Timex3) timexIter2.next();
				if (((t1.getBegin() >= t2.getBegin()) && (t1.getEnd() < t2.getEnd())) ||     // t1 starts inside or with t2 and ends before t2 -> remove t1
						((t1.getBegin() > t2.getBegin()) && (t1.getEnd() <= t2.getEnd()))) { // t1 starts inside t2 and ends with or before t2 -> remove t1
					hsTimexesToRemove.add(t1);
				} 
				else if (((t2.getBegin() >= t1.getBegin()) && (t2.getEnd() < t1.getEnd())) || // t2 starts inside or with t1 and ends before t1 -> remove t2
						((t2.getBegin() > t1.getBegin()) && (t2.getEnd() <= t1.getEnd()))) {    // t2 starts inside t1 and ends with or before t1 -> remove t2
					hsTimexesToRemove.add(t2);
				}
				// identical length
				if ((t1.getBegin() == t2.getBegin()) && (t1.getEnd() == t2.getEnd())) {
					if ((t1.getTimexType().equals("SET")) || (t2.getTimexType().equals("SET"))) {
						// REMOVE REAL DUPLICATES (the one with the lower timexID)
						if ((Integer.parseInt(t1.getTimexId().substring(1)) < Integer.parseInt(t2.getTimexId().substring(1)))) {
							hsTimexesToRemove.add(t1);
						}
					} else {
						if (!(t1.equals(t2))) {
							if ((t1.getTimexValue().startsWith("UNDEF")) && (!(t2.getTimexValue().startsWith("UNDEF")))) {
								hsTimexesToRemove.add(t1);
							} 
							else if ((!(t1.getTimexValue().startsWith("UNDEF"))) && (t2.getTimexValue().startsWith("UNDEF"))) {
								hsTimexesToRemove.add(t2);
							}
							// t1 is explicit, but t2 is not
							else if ((t1.getFoundByRule().endsWith("explicit")) && (!(t2.getFoundByRule().endsWith("explicit")))) {
								hsTimexesToRemove.add(t2);
							}
							// REMOVE REAL DUPLICATES (the one with the lower timexID)
							else if ((Integer.parseInt(t1.getTimexId().substring(1)) < Integer.parseInt(t2.getTimexId().substring(1)))) {
								hsTimexesToRemove.add(t1);
							}
						}
					}
				}
			}
		}
		// remove, finally
		for (Timex3 t : hsTimexesToRemove) {
			Logger.printDetail(t.getTimexId()+"REMOVE DUPLICATE: " + t.getCoveredText()+"(id:"+t.getTimexId()+" value:"+t.getTimexValue()+" found by:"+t.getFoundByRule()+")");
			
			t.removeFromIndexes();
			timex_counter--;
		}
	}
	
	
	/**
	 * Identify the part of speech (POS) of a MarchResult.
	 * @param tokBegin
	 * @param tokEnd
	 * @param s
	 * @param jcas
	 * @return
	 */
	public String getPosFromMatchResult(int tokBegin, int tokEnd, Sentence s, JCas jcas) {
		// get all tokens in sentence
		HashMap<Integer, Token> hmTokens = new HashMap<Integer, Token>();
		FSIterator iterTok = jcas.getAnnotationIndex(Token.type).subiterator(s);
		while (iterTok.hasNext()) {
			Token token = (Token) iterTok.next();
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

	
	/**
	 * Apply the extraction rules, normalization rules
	 * @param timexType
	 * @param hmPattern
	 * @param hmOffset
	 * @param hmNormalization
	 * @param hmQuant
	 * @param s
	 * @param jcas
	 */
	public void findTimexes(String timexType, 
							HashMap<Pattern, String> hmPattern,
							HashMap<String, String> hmOffset,
							HashMap<String, String> hmNormalization,
							HashMap<String, String> hmQuant,
							Sentence s,
							JCas jcas) {
		RuleManager rm = RuleManager.getInstance();
		HashMap<String, String> hmDatePosConstraint = rm.getHmDatePosConstraint();
		HashMap<String, String> hmDurationPosConstraint = rm.getHmDurationPosConstraint();
		HashMap<String, String> hmTimePosConstraint = rm.getHmTimePosConstraint();
		HashMap<String, String> hmSetPosConstraint = rm.getHmSetPosConstraint();
		
		// Iterator over the rules by sorted by the name of the rules
		// this is important since later, the timexId will be used to 
		// decide which of two expressions shall be removed if both
		// have the same offset
		for (Iterator<Pattern> i = Toolbox.sortByValue(hmPattern).iterator(); i.hasNext(); ) {
            Pattern p = (Pattern) i.next();

			for (MatchResult r : Toolbox.findMatches(p, s.getCoveredText())) {
				boolean infrontBehindOK = ContextAnalyzer.checkInfrontBehind(r, s);

				boolean posConstraintOK = true;
				// CHECK POS CONSTRAINTS
				if (timexType.equals("DATE")) {
					if (hmDatePosConstraint.containsKey(hmPattern.get(p))) {
						posConstraintOK = checkPosConstraint(s , hmDatePosConstraint.get(hmPattern.get(p)), r, jcas);
					}
				} else if (timexType.equals("DURATION")) {
					if (hmDurationPosConstraint.containsKey(hmPattern.get(p))) {
						posConstraintOK = checkPosConstraint(s , hmDurationPosConstraint.get(hmPattern.get(p)), r, jcas);
					}					
				} else if (timexType.equals("TIME")) {
					if (hmTimePosConstraint.containsKey(hmPattern.get(p))) {
						posConstraintOK = checkPosConstraint(s , hmTimePosConstraint.get(hmPattern.get(p)), r, jcas);
					}
				} else if (timexType.equals("SET")) {
					if (hmSetPosConstraint.containsKey(hmPattern.get(p))) {
						posConstraintOK = checkPosConstraint(s , hmSetPosConstraint.get(hmPattern.get(p)), r, jcas);
					}
				}
				
				if ((infrontBehindOK == true) && (posConstraintOK == true)) {
					
					// Offset of timex expression (in the checked sentence)
					int timexStart = r.start();
					int timexEnd   = r.end();
					
					// Normalization from Files:
					
					// Any offset parameter?
					if (hmOffset.containsKey(hmPattern.get(p))) {
						String offset    = hmOffset.get(hmPattern.get(p));
				
						// pattern for offset information
						Pattern paOffset = Pattern.compile("group\\(([0-9]+)\\)-group\\(([0-9]+)\\)");
						for (MatchResult mr : Toolbox.findMatches(paOffset,offset)) {
							int startOffset = Integer.parseInt(mr.group(1));
							int endOffset   = Integer.parseInt(mr.group(2));
							timexStart = r.start(startOffset);
							timexEnd   = r.end(endOffset); 
						}
					}
					
					// Normalization Parameter
					if (hmNormalization.containsKey(hmPattern.get(p))) {
						String[] attributes = new String[4];
						if (timexType.equals("DATE")) {
							attributes = getAttributesForTimexFromFile(hmPattern.get(p), rm.getHmDateNormalization(), rm.getHmDateQuant(), rm.getHmDateFreq(), rm.getHmDateMod(), r, jcas);
						} else if (timexType.equals("DURATION")) {
							attributes = getAttributesForTimexFromFile(hmPattern.get(p), rm.getHmDurationNormalization(), rm.getHmDurationQuant(), rm.getHmDurationFreq(), rm.getHmDurationMod(), r, jcas);
						} else if (timexType.equals("TIME")) {
							attributes = getAttributesForTimexFromFile(hmPattern.get(p), rm.getHmTimeNormalization(), rm.getHmTimeQuant(), rm.getHmTimeFreq(), rm.getHmTimeMod(), r, jcas);
						} else if (timexType.equals("SET")) {
							attributes = getAttributesForTimexFromFile(hmPattern.get(p), rm.getHmSetNormalization(), rm.getHmSetQuant(), rm.getHmSetFreq(), rm.getHmSetMod(), r, jcas);
						}
						addTimexAnnotation(timexType, timexStart + s.getBegin(), timexEnd + s.getBegin(), s, 
								attributes[0], attributes[1], attributes[2], attributes[3],"t" + timexID++, hmPattern.get(p), jcas);
					}
					else {
						Logger.printError("SOMETHING REALLY WRONG HERE: "+hmPattern.get(p));
					}
				}
			}
		}
	}
	
	
	/**
	 * Check whether the part of speech constraint defined in a rule is satisfied.
	 * @param s
	 * @param posConstraint
	 * @param m
	 * @param jcas
	 * @return
	 */
	public boolean checkPosConstraint(Sentence s, String posConstraint, MatchResult m, JCas jcas) {
		Pattern paConstraint = Pattern.compile("group\\(([0-9]+)\\):(.*?):");
		for (MatchResult mr : Toolbox.findMatches(paConstraint,posConstraint)) {
			int groupNumber = Integer.parseInt(mr.group(1));
			int tokenBegin = s.getBegin() + m.start(groupNumber);
			int tokenEnd   = s.getBegin() + m.end(groupNumber);
			String pos = mr.group(2);
			String pos_as_is = getPosFromMatchResult(tokenBegin, tokenEnd ,s, jcas);
			if (pos.equals(pos_as_is)) {
				Logger.printDetail("POS CONSTRAINT IS VALID: pos should be "+pos+" and is "+pos_as_is);
			} else {
				return false;
			}
		}
		return true;
	}
	
	
	public String applyRuleFunctions(String tonormalize, MatchResult m) {
		NormalizationManager norm = NormalizationManager.getInstance();
		
		String normalized = "";
		// pattern for normalization functions + group information
		// pattern for group information
		Pattern paNorm  = Pattern.compile("%([A-Za-z0-9]+?)\\(group\\(([0-9]+)\\)\\)");
		Pattern paGroup = Pattern.compile("group\\(([0-9]+)\\)");
		while ((tonormalize.contains("%")) || (tonormalize.contains("group"))) {
			// replace normalization functions
			for (MatchResult mr : Toolbox.findMatches(paNorm,tonormalize)) {
				Logger.printDetail("-----------------------------------");
				Logger.printDetail("DEBUGGING: tonormalize:"+tonormalize);
				Logger.printDetail("DEBUGGING: mr.group():"+mr.group());
				Logger.printDetail("DEBUGGING: mr.group(1):"+mr.group(1));
				Logger.printDetail("DEBUGGING: mr.group(2):"+mr.group(2));
				Logger.printDetail("DEBUGGING: m.group():"+m.group());
				Logger.printDetail("DEBUGGING: m.group("+Integer.parseInt(mr.group(2))+"):"+m.group(Integer.parseInt(mr.group(2))));
				Logger.printDetail("DEBUGGING: hmR...:"+norm.getFromHmAllNormalization(mr.group(1)).get(m.group(Integer.parseInt(mr.group(2)))));
				Logger.printDetail("-----------------------------------");
				
				if (! (m.group(Integer.parseInt(mr.group(2))) == null)) {
					String partToReplace = m.group(Integer.parseInt(mr.group(2))).replaceAll("[\n\\s]+", " ");
					if (!(norm.getFromHmAllNormalization(mr.group(1)).containsKey(partToReplace))) {
						Logger.printDetail("Maybe problem with normalization of the resource: "+mr.group(1));
						Logger.printDetail("Maybe problem with part to replace? "+partToReplace);
					}
					tonormalize = tonormalize.replace(mr.group(), norm.getFromHmAllNormalization(mr.group(1)).get(partToReplace));
				} else {
					Logger.printDetail("Empty part to normalize in "+mr.group(1));
					
					tonormalize = tonormalize.replace(mr.group(), "");
				}
			}
			// replace other groups
			for (MatchResult mr : Toolbox.findMatches(paGroup,tonormalize)) {
				Logger.printDetail("-----------------------------------");
				Logger.printDetail("DEBUGGING: tonormalize:"+tonormalize);
				Logger.printDetail("DEBUGGING: mr.group():"+mr.group());
				Logger.printDetail("DEBUGGING: mr.group(1):"+mr.group(1));
				Logger.printDetail("DEBUGGING: m.group():"+m.group());
				Logger.printDetail("DEBUGGING: m.group("+Integer.parseInt(mr.group(1))+"):"+m.group(Integer.parseInt(mr.group(1))));
				Logger.printDetail("-----------------------------------");
				
				tonormalize = tonormalize.replace(mr.group(), m.group(Integer.parseInt(mr.group(1))));
			}	
			// replace substrings
			Pattern paSubstring = Pattern.compile("%SUBSTRING%\\((.*?),([0-9]+),([0-9]+)\\)");
			for (MatchResult mr : Toolbox.findMatches(paSubstring,tonormalize)) {
				String substring = mr.group(1).substring(Integer.parseInt(mr.group(2)), Integer.parseInt(mr.group(3)));
				tonormalize = tonormalize.replace(mr.group(),substring);
			}
			// replace lowercase
			Pattern paLowercase = Pattern.compile("%LOWERCASE%\\((.*?)\\)");
			for (MatchResult mr : Toolbox.findMatches(paLowercase,tonormalize)) {
				String substring = mr.group(1).toLowerCase();
				tonormalize = tonormalize.replace(mr.group(),substring);
			}
			// replace uppercase
			Pattern paUppercase = Pattern.compile("%UPPERCASE%\\((.*?)\\)");
			for (MatchResult mr : Toolbox.findMatches(paUppercase,tonormalize)) {
				String substring = mr.group(1).toUpperCase();
				tonormalize = tonormalize.replace(mr.group(),substring);
			}
			// replace sum, concatenation
			Pattern paSum = Pattern.compile("%SUM%\\((.*?),(.*?)\\)");
			for (MatchResult mr : Toolbox.findMatches(paSum,tonormalize)) {
				int newValue = Integer.parseInt(mr.group(1)) + Integer.parseInt(mr.group(2));
				tonormalize = tonormalize.replace(mr.group(), newValue+"");
			}
			// replace normalization function without group
			Pattern paNormNoGroup = Pattern.compile("%([A-Za-z0-9]+?)\\((.*?)\\)");
			for (MatchResult mr : Toolbox.findMatches(paNormNoGroup, tonormalize)) {
				tonormalize = tonormalize.replace(mr.group(),norm.getFromHmAllNormalization(mr.group(1)).get(mr.group(2)));
			}
		}
		normalized = tonormalize;
		return normalized;
	}
	
	
	public String[] getAttributesForTimexFromFile(String rule,
													HashMap<String, String> hmNormalization,
													HashMap<String, String> hmQuant,
													HashMap<String, String> hmFreq,
													HashMap<String, String> hmMod,
													MatchResult m, 
													JCas jcas) {
		String[] attributes = new String[4];
		String value = "";
		String quant = "";
		String freq = "";
		String mod = "";
		
		// Normalize Value
		String value_normalization_pattern = hmNormalization.get(rule);
		value = applyRuleFunctions(value_normalization_pattern, m);
		
		// get quant
		if (hmQuant.containsKey(rule)) {
			String quant_normalization_pattern = hmQuant.get(rule);
			quant = applyRuleFunctions(quant_normalization_pattern, m);
		}

		// get freq
		if (hmFreq.containsKey(rule)) {
			String freq_normalization_pattern = hmFreq.get(rule);
			freq = applyRuleFunctions(freq_normalization_pattern, m);
		}
		
		// get mod
		if (hmMod.containsKey(rule)) {
			String mod_normalization_pattern = hmMod.get(rule);
			mod = applyRuleFunctions(mod_normalization_pattern, m);
		}
		
		// For example "P24H" -> "P1D"
		value = correctDurationValue(value);
		
		attributes[0] = value;
		attributes[1] = quant;
		attributes[2] = freq;
		attributes[3] = mod;
		
		return attributes;
	}
	

	/**
	 * Durations of a finer granularity are mapped to a coarser one if possible, e.g., "PT24H" -> "P1D".
	 * One may add several further corrections.
	 * @param value 
     * @return
     */
	public static String correctDurationValue(String value) {
		if (value.matches("PT[0-9]+H")){
			for (MatchResult mr : Toolbox.findMatches(Pattern.compile("PT([0-9]+)H"), value)){
				int hours = Integer.parseInt(mr.group(1));
				if ((hours % 24) == 0){
					int days = hours / 24;
					value = "P"+days+"D";
				}
			}
		} else if (value.matches("PT[0-9]+M")){
			for (MatchResult mr : Toolbox.findMatches(Pattern.compile("PT([0-9]+)M"), value)){
				int minutes = Integer.parseInt(mr.group(1));
				if ((minutes % 60) == 0){
					int hours = minutes / 60;
					value = "PT"+hours+"H";
				}
			}
		} else if (value.matches("P[0-9]+M")){
			for (MatchResult mr : Toolbox.findMatches(Pattern.compile("P([0-9]+)M"), value)){
				int months = Integer.parseInt(mr.group(1));
				if ((months % 12) == 0){
					int years = months / 12;
					value = "P"+years+"Y";
				}
			}
		}
		return value;
	}
		
}