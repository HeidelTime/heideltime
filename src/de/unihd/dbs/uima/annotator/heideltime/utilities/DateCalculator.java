package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class contains methods that rely on calendar functions to calculate data.
 * 
 * @author jannik stroetgen
 *
 */
public class DateCalculator {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(DateCalculator.class);

	// two formatters depending if BC or not
	// "u" allows 0, "y" does not (BC0001, then AD0001)
	static final DateTimeFormatter YEARFORMATTER = DateTimeFormatter.ofPattern("uuuu", Locale.ROOT);
	static final DateTimeFormatter YEARFORMATTERBC = DateTimeFormatter.ofPattern("GGyyyy", Locale.ROOT);

	static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT);
	static final DateTimeFormatter FORMATTERBC = DateTimeFormatter.ofPattern("GGyyyy-MM-dd", Locale.ROOT);

	static final DateTimeFormatter MONTHFORMATTER = DateTimeFormatter.ofPattern("uuuu-MM", Locale.ROOT);
	static final DateTimeFormatter MONTHFORMATTERBC = DateTimeFormatter.ofPattern("GGyyyy-MM", Locale.ROOT);

	static final DateTimeFormatter WEEKFORMATTER = new DateTimeFormatterBuilder().appendPattern("YYYY-['W']w").parseDefaulting(WeekFields.ISO.dayOfWeek(), 1).toFormatter(Locale.ROOT);
	static final DateTimeFormatter WEEKFORMATTER_WIDE = new DateTimeFormatterBuilder().appendPattern("YYYY-['W']ww").parseDefaulting(WeekFields.ISO.dayOfWeek(), 1).toFormatter(Locale.ROOT);

	private static Year parseBC(String date) throws DateTimeParseException {
		if (date.length() == 0)
			throw new DateTimeParseException("Empty date string.", date, 0);
		if ("BC0000".equals(date))
			return Year.of(-1);
		return Year.from(((Character.isDigit(date.charAt(0)) ? YEARFORMATTER : YEARFORMATTERBC)//
				.parse(date, new ParsePosition(0))));
	}

	public static String getXNextYear(String date, int x) {
		try {
			Year d = parseBC(date).plusYears(x);
			d.get(ChronoField.ERA);
			return d.format((d.get(ChronoField.ERA) == 1) ? YEARFORMATTER : YEARFORMATTERBC);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	public static String getXNextDecade(String date, int x) {
		try {
			date = date + "0"; // deal with years not with centuries
			Year d = parseBC(date).plusYears(10 * x);
			return d.format((d.get(ChronoField.ERA) == 1) ? YEARFORMATTER : YEARFORMATTERBC);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	public static String getXNextCentury(String date, int x) {
		try {
			date = date + "00"; // deal with years not with centuries
			Year d = parseBC(date);
			int oldEra = d.get(ChronoField.ERA);
			d = d.plusYears(x * 100);

			// check if new date is BC or AD for choosing formatter or formatterBC
			int newEra = d.get(ChronoField.ERA);
			if (newEra == 1) {
				if (oldEra == 0) {
					// -100 if from BC to AD
					d = d.minusYears(100);
				}
				return d.format(YEARFORMATTER).substring(0, 2);
			} else {
				if (oldEra == 1) {
					// +100 if from AD to BC
					d = d.plusYears(100);
				}
				return d.format(YEARFORMATTERBC).substring(0, 4);
			}

		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next day of date.
	 * 
	 * @param date
	 *                given date to get new date from
	 * @param x
	 *                type of temporal event to search for
	 * @return
	 */
	public static String getXNextDay(String date, int x) {
		try {
			return LocalDate.from(FORMATTER.parse(date, new ParsePosition(0))).plusDays(x).format(FORMATTER);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next day of date.
	 * 
	 * @param date
	 *                given date to get new date from
	 * @param x
	 *                type of temporal event to search for
	 * @return
	 */
	public static String getXNextDay(int year, int month, int days, int x) {
		try {
			return LocalDate.of(year, month, days).plusDays(x).format(FORMATTER);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next month of date
	 * 
	 * @param date
	 *                current date
	 * @param x
	 *                amount of months to go forward
	 * @return new month
	 */
	public static String getXNextMonth(String date, int x) {
		try {
			YearMonth d = YearMonth.from((Character.isDigit(date.charAt(0)) ? MONTHFORMATTER : MONTHFORMATTERBC).parse(date, new ParsePosition(0))).plusMonths(x);
			return d.format((d.get(ChronoField.ERA) == 1) ? MONTHFORMATTER : MONTHFORMATTERBC);

		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next week of date
	 * 
	 * @param date
	 *                current date
	 * @param x
	 *                amount of weeks to go forward
	 * @return new week
	 */
	public static String getXNextWeek(String date, int x) {
		try {
			LocalDate d = LocalDate.from(WEEKFORMATTER.parse(date, new ParsePosition(0))).plusWeeks(x);
			return d.format(WEEKFORMATTER_WIDE);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * get the x-next week of date
	 * 
	 * @param date
	 *                current date
	 * @param x
	 *                amount of weeks to go forward
	 * @return new week
	 */
	public static String getXNextWeek(int year, int month, int day, int x) {
		try {
			LocalDate d = LocalDate.of(year, month, day).plusWeeks(x);
			return d.format(WEEKFORMATTER_WIDE);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * Get the weekday of date
	 * 
	 * Important: with the switch to Java 8, sunday became 7 rather than 1!
	 * 
	 * @param date
	 *                current date
	 * @return day of week
	 */
	public static int getWeekdayOfDate(String date) {
		try {
			return LocalDate.from(FORMATTER.parse(date, new ParsePosition(0))).getDayOfWeek().getValue();
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return 0;
		}
	}

	/**
	 * Get the weekday of date
	 * 
	 * Important: with the switch to Java 8, sunday became 7 rather than 1!
	 * 
	 * @param year
	 *                Year
	 * @param month
	 *                Month
	 * @param day
	 *                Day of Month
	 * @return day of week
	 */
	public static int getWeekdayOfDate(int year, int month, int day) {
		return LocalDate.of(year, month, day).getDayOfWeek().getValue();
	}

	/**
	 * Get the week of date
	 * 
	 * @param date
	 *                current date
	 * @return week of year
	 */
	public static int getWeekOfDate(String date) {
		try {
			return LocalDate.from(FORMATTER.parse(date, new ParsePosition(0))).get(WeekFields.ISO.weekOfWeekBasedYear());
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return 0;
		}
	}

	/**
	 * Get the week of date
	 * 
	 * @param year
	 *                Year
	 * @param month
	 *                Month
	 * @param day
	 *                Day of Month
	 * @return week of year
	 */
	public static int getWeekOfDate(int year, int month, int day) {
		return LocalDate.of(year, month, day).get(WeekFields.ISO.weekOfWeekBasedYear());
	}

	/**
	 * Get the quarter of the year, as string.
	 * 
	 * @param dctMonth
	 *                Month
	 * @return Quarter
	 */
	public static String getQuarterOfMonth(int dctMonth) {
		return dctMonth <= 3 ? "Q1" : dctMonth <= 6 ? "Q2" : dctMonth <= 9 ? "Q3" : "Q4";
	}

	/**
	 * Get the half year, as string
	 * 
	 * @param dctMonth
	 *                Month
	 * @return Half year
	 */
	public static String getHalfYearOfMonth(int dctMonth) {
		return (dctMonth <= 6) ? "H1" : "H2";
	}

	/**
	 * Get the season of a month, as string
	 * 
	 * @param dctMonth
	 *                Month
	 * @return Season
	 */
	public static Season getSeasonOfMonth(int dctMonth) {
		return dctMonth <= 2 ? Season.WINTER : dctMonth <= 5 ? Season.SPRING : dctMonth <= 8 ? Season.SUMMER : dctMonth <= 11 ? Season.FALL : Season.WINTER;
	}
}
