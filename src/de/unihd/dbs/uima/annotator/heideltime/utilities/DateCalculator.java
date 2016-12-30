package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
/**
 * 
 * This class contains methods that rely on calendar functions to calculate data.
 * @author jannik stroetgen
 *
 */
public class DateCalculator {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(DateCalculator.class);
	
	// two formatters depending if BC or not
	static final SimpleDateFormat YEARFORMATTER   = new SimpleDateFormat("yyyy");
	static final SimpleDateFormat YEARFORMATTERBC = new SimpleDateFormat("GGyyyy");
	
	static SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	public static String getXNextYear(String date, Integer x){		
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			// read the original date
			if (date.matches("^\\d.*")){
				c.setTime(YEARFORMATTER.parse(date));
			}
			else{
				c.setTime(YEARFORMATTERBC.parse(date));
			}
			// make calucaltion
			c.add(Calendar.YEAR, x);
			c.getTime();
			// check if new date is BC or AD for choosing formatter or formatterBC
			int newEra = c.get(Calendar.ERA);
			if (newEra > 0){
				return YEARFORMATTER.format(c.getTime());
			}
			else{
				return YEARFORMATTERBC.format(c.getTime());
			}
		}
		catch (ParseException e) {
			LOG.error(e.getMessage(), e);
		}
		return "";
	}
	
	public static String getXNextDecade(String date, Integer x) {
		date = date + "0"; // deal with years not with centuries
		
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			// read the original date
			if (date.matches("^\\d.*")){
				c.setTime(YEARFORMATTER.parse(date));
			}
			else{
				c.setTime(YEARFORMATTERBC.parse(date));
			}
			
			// make calucaltion
			c.add(Calendar.YEAR, x*10);
			c.getTime();
			
			// check if new date is BC or AD for choosing formatter or formatterBC
			int newEra = c.get(Calendar.ERA);
			if (newEra > 0){
				return YEARFORMATTER.format(c.getTime()).substring(0, 3);
			}
			else{
				return YEARFORMATTERBC.format(c.getTime()).substring(0, 5);
			}
			
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}
	
	
	public static String getXNextCentury(String date, Integer x) {
		date = date + "00"; // deal with years not with centuries
		int oldEra = 0;     // 0 if BC date, 1 if AD date
				
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			// read the original date
			if (date.matches("^\\d.*")){
				c.setTime(YEARFORMATTER.parse(date));
				oldEra = 1;
			}
			else{
				c.setTime(YEARFORMATTERBC.parse(date));
			}
			
			// make calucaltion
			c.add(Calendar.YEAR, x*100);
			c.getTime();
			
			// check if new date is BC or AD for choosing formatter or formatterBC
			int newEra = c.get(Calendar.ERA);
			if (newEra > 0){
				if (oldEra == 0){
					// -100 if from BC to AD
					c.add(Calendar.YEAR, -100);
					c.getTime();
				}
				return YEARFORMATTER.format(c.getTime()).substring(0, 2);
			}
			else{
				if (oldEra > 0){
					// +100 if from AD to BC
					c.add(Calendar.YEAR, 100);
					c.getTime();
				}
				return YEARFORMATTERBC.format(c.getTime()).substring(0, 4);
			}
			
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}
	
	/**
	 * get the x-next day of date.
	 * 
	 * @param date given date to get new date from
	 * @param x type of temporal event to search for
	 * @return
	 */
	public static String getXNextDay(String date, Integer x) {
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			c.setTime(FORMATTER.parse(date));
			c.add(Calendar.DAY_OF_MONTH, x);
			c.getTime();
			return FORMATTER.format(c.getTime());
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next month of date
	 * 
	 * @param date current date
	 * @param x amount of months to go forward 
	 * @return new month
	 */
	public static String getXNextMonth(String date, Integer x) {
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			// read the original date
			if (date.matches("^\\d.*")){
				c.setTime(YEARFORMATTER.parse(date));
			}
			else{
				c.setTime(YEARFORMATTERBC.parse(date));
			}
			// make calucaltion
			c.add(Calendar.MONTH, x);
			c.getTime();
			
			// check if new date is BC or AD for choosing formatter or formatterBC
			int newEra = c.get(Calendar.ERA);
			if (newEra > 0){
				return YEARFORMATTER.format(c.getTime());
			}
			else{
				return YEARFORMATTERBC.format(c.getTime());
			}
			
		}
		catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}
	
	static final SimpleDateFormat WEEKFORMATTER = new SimpleDateFormat("yyyy-w");

	/**
	 * get the x-next week of date
	 * @param date current date
	 * @param x amount of weeks to go forward
	 * @return new week
	 */
	public static String getXNextWeek(String date, Integer x, Language language) {
		NormalizationManager nm = NormalizationManager.getInstance(language, false);
		String date_no_W = date.replace("W", "");
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			c.setTime(WEEKFORMATTER.parse(date_no_W));
			c.add(Calendar.WEEK_OF_YEAR, x);
			c.getTime();
			String newDate = WEEKFORMATTER.format(c.getTime());
			return newDate.substring(0,4)+"-W"+nm.getFromNormNumber(newDate.substring(5));
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
		}
		return "";
	}

	/**
	 * Get the weekday of date
	 * 
	 * @param date current date
	 * @return day of week
	 */
	public static int getWeekdayOfDate(String date) {
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			c.setTime(FORMATTER.parse(date));
			return c.get(Calendar.DAY_OF_WEEK);
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return 0;
		}
	}

	/**
	 * Get the week of date
	 * 
	 * @param date current date
	 * @return week of year
	 */
	public static int getWeekOfDate(String date) {
		try {
			Calendar c = Calendar.getInstance(GMT, Locale.ROOT);
			c.setTime(FORMATTER.parse(date));
			return c.get(Calendar.WEEK_OF_YEAR);
		} catch (ParseException e) {
			LOG.error(e.getMessage(), e);
			return 0;
		}
	}
	
	/**
	 * takes a desired locale input string, iterates through available locales, returns a locale object
	 * @param locale String to grab a locale for, i.e. en_US, en_GB, de_DE
	 * @return Locale to represent the input String
	 */
	public static Locale getLocaleFromString(String locale) throws LocaleException {
		for(Locale l : Locale.getAvailableLocales())
			if(locale.equalsIgnoreCase(l.toString()))
				return l;
		throw new LocaleException();
	}
}
