package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
/**
 * 
 * This class contains methods that rely on calendar functions to calculate data.
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
	public static String getXNextWeek(String date, Integer x) {
		NormalizationManager nm = NormalizationManager.getInstance();
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
}
