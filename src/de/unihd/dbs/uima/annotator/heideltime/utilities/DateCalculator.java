package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
/**
 * 
 * This class contains methods that rely on calendar functions to calculate data.
 * @author jannik stroetgen
 *
 */
public class DateCalculator {
	
	/**
	 * get the x-next day of date.
	 * 
	 * @param date given date to get new date from
	 * @param x type of temporal event to search for
	 * @return
	 */
	public static String getXNextDay(String date, Integer x) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String newDate = "";
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(formatter.parse(date));
			c.add(Calendar.DAY_OF_MONTH, x);
			c.getTime();
			newDate = formatter.format(c.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newDate;
	}

	/**
	 * get the x-next month of date
	 * 
	 * @param date current date
	 * @param x amount of months to go forward 
	 * @return new month
	 */
	public static String getXNextMonth(String date, Integer x) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
		String newDate = "";
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(formatter.parse(date));
			c.add(Calendar.MONTH, x);
			c.getTime();
			newDate = formatter.format(c.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newDate;
	}
	
	/**
	 * get the x-next week of date
	 * @param date current date
	 * @param x amount of weeks to go forward
	 * @return new week
	 */
	public static String getXNextWeek(String date, Integer x, Language language) {
		NormalizationManager nm = NormalizationManager.getInstance(language);
		String date_no_W = date.replace("W", "");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-w");
		String newDate = "";
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(formatter.parse(date_no_W));
			c.add(Calendar.WEEK_OF_YEAR, x);
			c.getTime();
			newDate = formatter.format(c.getTime());
			newDate = newDate.substring(0,4)+"-W"+nm.getFromNormNumber(newDate.substring(5));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newDate;
	}

	/**
	 * Get the weekday of date
	 * 
	 * @param date current date
	 * @return day of week
	 */
	public static int getWeekdayOfDate(String date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		int weekday = 0;
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(formatter.parse(date));
			weekday = c.get(Calendar.DAY_OF_WEEK);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return weekday;
	}

	/**
	 * Get the week of date
	 * 
	 * @param date current date
	 * @return week of year
	 */
	public static int getWeekOfDate(String date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		int week = 0;
		;
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(formatter.parse(date));
			week = c.get(Calendar.WEEK_OF_YEAR);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return week;
	}
	
	/**
	 * takes a desired locale input string, iterates through available locales, returns a locale object
	 * @param locale String to grab a locale for, i.e. en_US, en_GB, de_DE
	 * @return Locale to represent the input String
	 */
	public static Locale getLocaleFromString(String locale) throws LocaleException {
		for(Locale l : Locale.getAvailableLocales()) {
			if(locale.toLowerCase().equals(l.toString().toLowerCase())) {
				return l;
			}
		}
		throw new LocaleException();
	}
}
