package de.unihd.dbs.uima.annotator.intervaltagger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.ResourceMap;
import de.unihd.dbs.uima.annotator.heideltime.resources.ResourceScanner;
import de.unihd.dbs.uima.annotator.heideltime.resources.RuleManager;
import de.unihd.dbs.uima.types.heideltime.IntervalCandidateSentence;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;

/**
 * IntervalTagger is a UIMA annotator that discovers and tags intervals in documents.
 * @author Manuel Dewald, Julian Zell
 *
 */
public class IntervalTagger extends JCasAnnotator_ImplBase {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(IntervalTagger.class);

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	
	// descriptor parameter names
	public static String PARAM_LANGUAGE = "language";
	public static String PARAM_INTERVALS = "annotate_intervals";
	public static String PARAM_INTERVAL_CANDIDATES = "annotate_interval_candidates";
	// descriptor configuration
	private Language language = null;
	private boolean find_intervals = true;
	private boolean find_interval_candidates = true;
	
	private HashMap<Pattern, String> hmIntervalPattern = new HashMap<Pattern, String>();
	private HashMap<String, String> hmIntervalNormalization = new HashMap<String, String>();
	
	/**
	 * initialization: read configuration parameters and resources
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		
		language = Language.getLanguageFromString((String) aContext.getConfigParameterValue(PARAM_LANGUAGE));
		
		find_intervals = (Boolean) aContext.getConfigParameterValue(PARAM_INTERVALS);
		find_interval_candidates = (Boolean) aContext.getConfigParameterValue(PARAM_INTERVAL_CANDIDATES);
		
		ResourceScanner rs = ResourceScanner.getInstance();
		readResources(rs.getRules(language.getName()));
	}
	
	/**
	 * called by the pipeline to process the document
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		if(find_intervals) {
			findIntervals(jcas);
			findSentenceIntervals(jcas);
		}
	}
	
	/**
	 * reads in heideltime's resource files.
	 * @throws ResourceInitializationException
	 */
	private void readResources(ResourceMap hmResourcesRules) throws ResourceInitializationException {
		Matcher maReadRules = Pattern.compile("RULENAME=\"(.*?)\",EXTRACTION=\"(.*?)\",NORM_VALUE=\"(.*?)\"(.*)").matcher("");
		
		// read normalization data
		for (String resource : hmResourcesRules.keySet()) {
			if(!resource.equals("intervalrules"))
				continue;
			try (InputStream is = hmResourcesRules.getInputStream(resource);//
				InputStreamReader isr = new InputStreamReader(is, "UTF-8");//
				BufferedReader br = new BufferedReader(isr)) {
				LOG.debug("Adding rule resource: {}", resource);
				for(String line; (line = br.readLine()) != null; ) {
					if(line.startsWith("//") || line.equals(""))
						continue;
					
					LOG.debug("reading rules... {}", line);
					// check each line for the name, extraction, and normalization part
					for (maReadRules.reset(line); maReadRules.find(); ) {
						String rule_name          = maReadRules.group(1);
						String rule_extraction    = maReadRules.group(2);
						String rule_normalization = maReadRules.group(3);
						
						////////////////////////////////////////////////////////////////////
						// RULE EXTRACTION PARTS ARE TRANSLATED INTO REGULAR EXPRESSSIONS //
						////////////////////////////////////////////////////////////////////
						// create pattern for rule extraction part
						RePatternManager rpm = RePatternManager.getInstance(language, false);
						rule_extraction = RuleManager.expandVariables(rule_name, rule_extraction, rpm);
						rule_extraction = RuleManager.replaceSpaces(rule_extraction);
						Pattern pattern = null;
						try{
							pattern = Pattern.compile(rule_extraction);
						}
						catch (PatternSyntaxException e) {
							LOG.error("Compiling rules resulted in errors.", e);
							LOG.error("Problematic rule is: {}\nCannot compile pattern: {}", rule_name, rule_extraction);
							System.exit(1);
						}
						
						/////////////////////////////////////////////////
						// READ INTERVAL RULES AND MAKE THEM AVAILABLE //
						/////////////////////////////////////////////////
						hmIntervalPattern.put(pattern,rule_name);
						hmIntervalNormalization.put(rule_name, rule_normalization);
					}
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
				throw new ResourceInitializationException();
			}
		}
	}
	
	Pattern pNorm=Pattern.compile("group\\(([1-9]+)\\)-group\\(([1-9]+)\\)");

	/**
	 * Extract Timex3Intervals, delimited by two Timex3Intervals in a sentence.
	 * finsInterval needs to be run with jcas before.
	 * @param jcas
	 * @author Manuel Dewald
	 */
	private void findSentenceIntervals(JCas jcas){
		HashSet<Timex3Interval> timexesToRemove = new HashSet<Timex3Interval>();
		
		AnnotationIndex<Sentence> sentences = jcas.getAnnotationIndex(Sentence.type);
		for(Sentence s : sentences) {
			String sString=s.getCoveredText();
			AnnotationIndex<Timex3Interval> intervals = jcas.getAnnotationIndex(Timex3Interval.type);
			
			int count=0;
			List<Timex3Interval> txes=new ArrayList<Timex3Interval>();

			for(FSIterator<Timex3Interval> iterInter = intervals.subiterator(s); iterInter.hasNext(); ){
				Timex3Interval t=(Timex3Interval)iterInter.next();
				sString=sString.replace(t.getCoveredText(), "<TX3_"+count+">");
				count++;
				txes.add(t);
			}

			if(count == 0)
				continue;

			if (find_interval_candidates){
				IntervalCandidateSentence sI=new IntervalCandidateSentence(jcas);
				sI.setBegin(s.getBegin());
				sI.setEnd(s.getEnd());
				sI.addToIndexes();
			}
			List<Timex3Interval> sentenceTxes=new ArrayList<Timex3Interval>();
			for(Map.Entry<Pattern,String> ent : hmIntervalPattern.entrySet()){
				String name=ent.getValue();
				Matcher m = ent.getKey().matcher(sString);
				while (m.find()) {
					String norm=hmIntervalNormalization.get(name);
							
					Matcher mNorm=pNorm.matcher(norm);
					if(!mNorm.matches()){
						LOG.warn("Problem with the Norm in rule "+name);
						continue;
					}
					Timex3Interval startTx=null,endTx=null;
					try{
						int startId=Integer.parseInt(mNorm.group(1));
						int endId=Integer.parseInt(mNorm.group(2));

						startTx=txes.get(Integer.parseInt(m.group(startId)));
						endTx=txes.get(Integer.parseInt(m.group(endId)));
					}catch(Exception e){
						LOG.error(e.getMessage(), e);
						return;
					}
					Timex3Interval annotation=new Timex3Interval(jcas);
					annotation.setBegin(startTx.getBegin()>endTx.getBegin()?endTx.getBegin():startTx.getBegin());
					annotation.setEnd(startTx.getEnd()>endTx.getEnd()?startTx.getEnd():endTx.getEnd());

					//Does the interval already exist,
					//found by another pattern?
					boolean duplicate=false;
					for(Timex3Interval tx:sentenceTxes)
						if(tx.getBegin()==annotation.getBegin() &&
						tx.getEnd()==annotation.getEnd()){
							duplicate=true;
							break;
						}
					if(duplicate)
						continue;

					annotation.setTimexValueEB(startTx.getTimexValueEB());
					annotation.setTimexValueLB(startTx.getTimexValueLE());
					annotation.setTimexValueEE(endTx.getTimexValueEB());
					annotation.setTimexValueLE(endTx.getTimexValueLE());
					annotation.setTimexType(startTx.getTimexType());
					annotation.setFoundByRule(name);

					// create emptyvalue value
					String emptyValue = createEmptyValue(startTx, endTx, jcas);
					annotation.setEmptyValue(emptyValue);
					annotation.setBeginTimex(startTx.getBeginTimex());
					annotation.setEndTimex(endTx.getEndTimex());

					try {
						sentenceTxes.add(annotation);
					} catch(NumberFormatException e) {
						LOG.error("Couldn't do emptyValue calculation on accont of a faulty normalization in {} or {}", 
								annotation.getTimexValueEB(), annotation.getTimexValueEE());
					}

					// prepare tx3intervals to remove
					timexesToRemove.add(startTx);
					timexesToRemove.add(endTx);

					annotation.addToIndexes();
				}
			}
		}
		
		for(Timex3Interval txi : timexesToRemove)
			txi.removeFromIndexes();
	}
	
	Pattern datep = Pattern.compile("(\\d{1,4})?-?(\\d{2})?-?(\\d{2})?(T)?(\\d{2})?:?(\\d{2})?:?(\\d{2})?");
	//				 1            2          3        4   5          6          7
	
	private String createEmptyValue(Timex3Interval startTx, Timex3Interval endTx, JCas jcas) throws NumberFormatException {
		String dateStr = "", timeStr = "";

		// find granularity for start/end timex values
		Matcher mStart = datep.matcher(startTx.getTimexValue());
		Matcher mEnd = datep.matcher(endTx.getTimexValue());
		
		if(!mStart.find() || !mEnd.find())
			return "";
		
		// find the highest granularity in each timex
		int granularityStart = -1;
		for(int i = 1; i <= mStart.groupCount(); i++)
			if(mStart.group(i) != null)
				granularityStart = i;
		int granularityEnd = -2;
		for(int i = 1; i <= mEnd.groupCount(); i++)
			if(mEnd.group(i) != null)
				granularityEnd = i;
		
		// if granularities aren't the same, we can't do anything here.
		if(granularityEnd != granularityStart)
			return "";

		// otherwise, set maximum granularity
		int granularity = granularityStart;

		// check all the different granularities, starting with seconds, calculate differences, add carries
		int myYears = 0,
				myMonths = 0,
				myDays = 0,
				myHours = 0,
				myMinutes = 0,
				mySeconds = 0;
		
		if(granularity >= 7 && mStart.group(7) != null && mEnd.group(7) != null) {
			mySeconds = Integer.parseInt(mEnd.group(7)) - Integer.parseInt(mStart.group(7));
			if(mySeconds < 0) {
				mySeconds += 60;
				myMinutes -= 1;
			}
		}
		
		if(granularity >= 6 && mStart.group(6) != null && mEnd.group(6) != null) {
			myMinutes += Integer.parseInt(mEnd.group(6)) - Integer.parseInt(mStart.group(6));
			if(myMinutes < 0) {
				myMinutes += 60;
				myHours -= 1;
			}
		}
		
		if(granularity >= 5 && mStart.group(5) != null && mEnd.group(5) != null) {
			myHours += Integer.parseInt(mEnd.group(5)) - Integer.parseInt(mStart.group(5));
			if(myHours < 0) {
				myMinutes += 24;
				myDays -= 1;
			}
		}
		
		if(granularity >= 3 && mStart.group(3) != null && mEnd.group(3) != null) {
			myDays += Integer.parseInt(mEnd.group(3)) - Integer.parseInt(mStart.group(3));
			if(myDays < 0) {
				Calendar cal = Calendar.getInstance(GMT, Locale.ROOT);
				cal.set(Calendar.YEAR, Integer.parseInt(mStart.group(1)));
				cal.set(Calendar.MONTH, Integer.parseInt(mStart.group(2)));

				myMonths = myMonths - 1;
				myDays += cal.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
		}
		
		if(granularity >= 2 && mStart.group(2) != null && mEnd.group(2) != null) {
			myMonths += Integer.parseInt(mEnd.group(2)) - Integer.parseInt(mStart.group(2));
			if(myMonths < 0) {
				myMonths += Integer.parseInt(mStart.group(2));
				myYears -= 1;
			}
		}
		
		String myYearUnit = "";
		if(granularity >= 1 && mStart.group(1) != null && mEnd.group(1) != null) {
			String year1str = mStart.group(1), year2str = mEnd.group(1);
			
			// trim year strings to same length (NNNN year, NNN decade, NN century)
			while(year2str.length() > year1str.length())
				year2str = year2str.substring(0, year2str.length()-1);
			while(year1str.length() > year2str.length())
				year1str = year1str.substring(0, year1str.length()-1);
			
			// check for year unit
			switch(year1str.length()) {
				case 2:
					myYearUnit = "CE";
					myYears = Integer.parseInt(year2str) - Integer.parseInt(year1str);
					break;
				case 3:
					myYearUnit = "DE";
					myYears = Integer.parseInt(year2str) - Integer.parseInt(year1str);
					break;
				case 4:
					myYearUnit = "Y";
					myYears += Integer.parseInt(year2str) - Integer.parseInt(year1str);
					break;
				default:
					break;
			}
		}

		// assemble strings
		dateStr += (myYears > 0 ? myYears + myYearUnit : "");
		dateStr += (myMonths > 0 ? myMonths + "M" : "");
		dateStr += (myDays > 0 ? myDays + "D" : "");

		timeStr += (myHours > 0 ? myHours + "H" : "");
		timeStr += (myMinutes > 0 ? myMinutes + "M" : "");
		timeStr += (mySeconds > 0 ? mySeconds + "S" : "");

		// output
		return "P" + dateStr + (timeStr.length() > 0 ? "T" + timeStr : "");
	}

	//DATE Pattern
	Pattern pDate = Pattern.compile("(?:BC)?(\\d\\d\\d\\d)(-(\\d+))?(-(\\d+))?(T(\\d+))?(:(\\d+))?(:(\\d+))?");
	Pattern pCentury = Pattern.compile("(\\d\\d)");
	Pattern pDecade = Pattern.compile("(\\d\\d\\d)");
	Pattern pQuarter = Pattern.compile("(\\d+)-Q([1-4])");
	Pattern pHalf = Pattern.compile("(\\d+)-H([1-2])");
	Pattern pSeason = Pattern.compile("(\\d+)-(SP|SU|FA|WI)");
	Pattern pWeek = Pattern.compile("(\\d+)-W(\\d+)");
	Pattern pWeekend = Pattern.compile("(\\d+)-W(\\d+)-WE");
	Pattern pTimeOfDay = Pattern.compile("(\\d+)-(\\d+)-(\\d+)T(AF|DT|MI|MO|EV|NI)");
	
	/**
	 * Build Timex3Interval-Annotations out of Timex3Annotations in jcas.
	 * @author Manuel Dewald
	 * @param jcas
	 */
	private void findIntervals(JCas jcas) {
		ArrayList<Timex3Interval> newAnnotations = new ArrayList<Timex3Interval>();
		
		Matcher mDate   = pDate.matcher("");
		Matcher mCentury= pCentury.matcher("");
		Matcher mDecade = pDecade.matcher("");
		Matcher mQuarter= pQuarter.matcher("");
		Matcher mHalf   = pHalf.matcher("");
		Matcher mSeason = pSeason.matcher("");
		Matcher mWeek   = pWeek.matcher("");
		Matcher mWeekend= pWeekend.matcher("");
		Matcher mTimeOfDay= pTimeOfDay.matcher("");
		
		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		for (Timex3 timex3 : timexes) {
			Timex3Interval annotation=new Timex3Interval(jcas);
			String timexValue = timex3.getTimexValue();
			
			String beginYear="UNDEF", endYear="UNDEF";
			String beginMonth="01", endMonth="12";
			String beginDay="01", endDay="31";
			String beginHour="00", endHour="23";
			String beginMinute="00", endMinute="59";
			String beginSecond="00", endSecond="59";
			
			if(mDate.reset(timexValue).matches()){
				//Get Year(1)
				beginYear=endYear=mDate.group(1);
				
				//Get Month(3)
				if(mDate.group(3)!=null){
					beginMonth=endMonth=mDate.group(3);
					
					//Get Day(5)
					if(mDate.group(5)==null){
						Calendar c=Calendar.getInstance(GMT, Locale.ROOT);
						c.set(Integer.parseInt(beginYear), Integer.parseInt(beginMonth)-1, 1);
						endDay=Integer.toString(+c.getActualMaximum(Calendar.DAY_OF_MONTH));
						beginDay="01";
					}else{
						beginDay=endDay=mDate.group(5);
						
						//Get Hour(7)
						if(mDate.group(7)!=null){
							beginHour=endHour=mDate.group(7);

							//Get Minute(9)
							if(mDate.group(9)!=null){
								beginMinute=endMinute=mDate.group(9);

								//Get Second(11)
								if(mDate.group(11)!=null){
									beginSecond=endSecond=mDate.group(11);
								}
							}
						}
						
					}
				}
				
			}else if(mCentury.reset(timexValue).matches()){
				beginYear=mCentury.group(1)+"00";
				endYear=mCentury.group(1)+"99";
			}else if(mDecade.reset(timexValue).matches()){
				beginYear=mDecade.group(1)+"0";
				endYear=mDecade.group(1)+"9";
			}else if(mQuarter.reset(timexValue).matches()){
				beginYear=endYear=mQuarter.group(1);
				int beginMonthI=3*(Integer.parseInt(mQuarter.group(2))-1)+1;
				beginMonth=Integer.toString(beginMonthI);
				endMonth=Integer.toString(beginMonthI+2);
				Calendar c=Calendar.getInstance(GMT, Locale.ROOT);
				c.set(Integer.parseInt(beginYear), Integer.parseInt(endMonth)-1, 1);
				endDay=Integer.toString(+c.getActualMaximum(Calendar.DAY_OF_MONTH));
			}else if(mHalf.reset(timexValue).matches()){
				beginYear=endYear=mHalf.group(1);
				int beginMonthI=6*(Integer.parseInt(mHalf.group(2))-1)+1;
				beginMonth=Integer.toString(beginMonthI);
				endMonth=Integer.toString(beginMonthI+5);
				Calendar c=Calendar.getInstance(GMT, Locale.ROOT);
				c.set(Integer.parseInt(beginYear), Integer.parseInt(endMonth)-1, 1);
				endDay=Integer.toString(+c.getActualMaximum(Calendar.DAY_OF_MONTH));
			}else if(mSeason.reset(timexValue).matches()){
				beginYear=mSeason.group(1);
				endYear=beginYear;
				if(mSeason.group(2).equals("SP")){
					beginMonth="03";
					beginDay="21";
					endMonth="06";
					endDay="20";
				}else if(mSeason.group(2).equals("SU")){
					beginMonth="06";
					beginDay="21";
					endMonth="09";
					endDay="22";
				}else if(mSeason.group(2).equals("FA")){
					beginMonth="09";
					beginDay="23";
					endMonth="12";
					endDay="21";
				}else if(mSeason.group(2).equals("WI")){
					endYear=Integer.toString(Integer.parseInt(beginYear)+1);
					beginMonth="12";
					beginDay="22";
					endMonth="03";
					endDay="20";
				}
			}else if(mWeek.reset(timexValue).matches()){
				beginYear=endYear=mWeek.group(1);
				Calendar c=Calendar.getInstance(GMT, Locale.ROOT);
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.set(Calendar.YEAR,Integer.parseInt(beginYear));
				c.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(mWeek.group(2)));
				c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				beginDay=Integer.toString(+c.get(Calendar.DAY_OF_MONTH));
				beginMonth=Integer.toString(c.get(Calendar.MONTH)+1);
				c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				endDay=Integer.toString(c.get(Calendar.DAY_OF_MONTH));
				endMonth=Integer.toString(c.get(Calendar.MONTH)+1);
			}else if(mWeekend.reset(timexValue).matches()){
				beginYear=endYear=mWeekend.group(1);
				Calendar c=Calendar.getInstance(GMT, Locale.ROOT);
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.set(Calendar.YEAR,Integer.parseInt(beginYear));
				c.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(mWeekend.group(2)));
				c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				beginDay=Integer.toString(+c.get(Calendar.DAY_OF_MONTH));
				beginMonth=Integer.toString(c.get(Calendar.MONTH)+1);
				c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				endDay=Integer.toString(c.get(Calendar.DAY_OF_MONTH));
				endMonth=Integer.toString(c.get(Calendar.MONTH)+1);
			}else if(mTimeOfDay.reset(timexValue).matches()){
				beginYear=endYear=mTimeOfDay.group(1);
				beginMonth=endMonth=mTimeOfDay.group(2);
				beginDay=endDay=mTimeOfDay.group(3);
			}
			
			// correct month and days < 10
			if (Integer.parseInt(beginDay) < 10){
				beginDay = "0" + Integer.parseInt(beginDay);
			}
			if (Integer.parseInt(beginMonth) < 10){
				beginMonth = "0" + Integer.parseInt(beginMonth);
			}
			if (Integer.parseInt(endDay) < 10){
				endDay = "0" + Integer.parseInt(endDay);
			}
			if (Integer.parseInt(endMonth) < 10){
				endMonth = "0" + Integer.parseInt(endMonth);
			}
			
			if(!beginYear.equals("UNDEF") && !endYear.equals("UNDEF")){
				annotation.setTimexValueEB(beginYear+"-"+beginMonth+"-"+beginDay+"T"+beginHour+":"+beginMinute+":"+beginSecond);
//				annotation.setTimexValueLB(beginYear+"-"+beginMonth+"-"+beginDay+"T"+endHour+":"+endMinute+":"+endSecond);
//				annotation.setTimexValueEE(endYear+"-"+endMonth+"-"+endDay+"T"+beginHour+":"+beginMinute+":"+beginSecond);
				annotation.setTimexValueLE(endYear+"-"+endMonth+"-"+endDay+"T"+endHour+":"+endMinute+":"+endSecond);
				annotation.setTimexValueLB(endYear+"-"+endMonth+"-"+endDay+"T"+endHour+":"+endMinute+":"+endSecond);
				annotation.setTimexValueEE(beginYear+"-"+beginMonth+"-"+beginDay+"T"+beginHour+":"+beginMinute+":"+beginSecond);
				
				//Copy Values from the Timex3 Annotation
				annotation.setTimexFreq(timex3.getTimexFreq());
				annotation.setTimexId(timex3.getTimexId());
				annotation.setTimexInstance(timex3.getTimexInstance());
				annotation.setTimexMod(timex3.getTimexMod());
				annotation.setTimexQuant(timex3.getTimexMod());
				annotation.setTimexType(timex3.getTimexType());
				annotation.setTimexValue(timexValue);
				annotation.setSentId(timex3.getSentId());
				annotation.setBegin(timex3.getBegin());
				annotation.setFoundByRule(timex3.getFoundByRule());
				annotation.setEnd(timex3.getEnd());
				annotation.setAllTokIds(timex3.getAllTokIds());
				annotation.setFilename(timex3.getFilename());
				annotation.setBeginTimex(timex3.getTimexId());
				annotation.setEndTimex(timex3.getTimexId());
				
				// remember this one for addition to indexes later
				newAnnotations.add(annotation);
			}
		}
		// add to indexes
		for(Timex3Interval t3i : newAnnotations)
			t3i.addToIndexes();
	}

}
