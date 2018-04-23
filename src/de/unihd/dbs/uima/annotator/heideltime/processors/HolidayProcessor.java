package de.unihd.dbs.uima.annotator.heideltime.processors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.types.heideltime.Timex3;

/**
 * Addition to HeidelTime to recognize several (mostly, but not entirely christian) holidays.
 * 
 * @author Hans-Peter Pfeiffer
 *
 */
public class HolidayProcessor extends GenericProcessor {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(HolidayProcessor.class);

	/**
	 * Constructor just calls the parent constructor here.
	 */
	public HolidayProcessor() {
		super();
	}

	/**
	 * not needed here
	 */
	public void initialize(UimaContext aContext) {
		return;
	}

	/**
	 * all the functionality was put into evaluateCalculationFunctions().
	 */
	public void process(JCas jcas) {
		evaluateCalculationFunctions(jcas);
	}

	Pattern cmd_p = Pattern.compile("((\\w\\w\\w\\w)-(\\w\\w)-(\\w\\w))\\s+funcDateCalc\\((\\w+)\\((.+)\\)\\)");
	Pattern year_p = Pattern.compile("(\\d\\d\\d\\d)");
	Pattern date_p = Pattern.compile("(\\d\\d\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])");

	/**
	 * This function replaces function calls from the resource files with their TIMEX value.
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param jcas
	 */
	public void evaluateCalculationFunctions(JCas jcas) {
		// compile regex pattern for validating commands/arguments
		Matcher cmd_m = cmd_p.matcher("");

		AnnotationIndex<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);
		// Avoid concurrent modification exceptions
		ArrayList<Timex3> copy = new ArrayList<Timex3>(timexes.size());
		for (Timex3 timex : timexes)
			if (timex.getTimexType().equals("DATE") || timex.getTimexType().equals("TIME"))
				copy.add(timex);

		for (Timex3 timex : copy) {
			String value_i = timex.getTimexValue();

			if (cmd_m.reset(value_i).matches()) {
				String date = cmd_m.group(1);
				String year = cmd_m.group(2);
				String month = cmd_m.group(3);
				String day = cmd_m.group(4);
				String function = cmd_m.group(5);
				String args[] = cmd_m.group(6).split("\\s*,\\s*");

				// replace keywords in function with actual values
				for (int j = 0; j < args.length; j++) {
					args[j] = args[j].replace("DATE", date);
					args[j] = args[j].replace("YEAR", year);
					args[j] = args[j].replace("MONTH", month);
					args[j] = args[j].replace("DAY", day);
				}

				String valueNew = value_i;
				if (function.equals("EasterSunday")) {
					Matcher year_m = year_p.matcher(args[0]);
					// check if args[0] is a valid YEAR value
					if (year_m.matches()) {
						valueNew = getEasterSunday(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
					} else {
						LOG.info("Could not compute EasterSunday for {} {}", args[0], args[1]);
						valueNew = "XXXX-XX-XX";
					}
				} else if (function.equals("WeekdayRelativeTo")) {
					Matcher date_m = date_p.matcher(args[0]);
					// check if args[0] is a valid DATE value
					if (date_m.matches()) {
						valueNew = getWeekdayRelativeTo(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Boolean.parseBoolean(args[3]));
					} else {
						LOG.info("Could not compute WeekdayRelativeTo for {} {} {} {}", args[0], args[1], args[2], args[3]);
						valueNew = "XXXX-XX-XX";
					}
				} else if (function.equals("EasterSundayOrthodox")) {
					Matcher year_m = year_p.matcher(args[0]);
					// check if args[0] is a valid YEAR value
					if (year_m.matches()) {
						valueNew = getEasterSundayOrthodox(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
					} else {
						LOG.info("Could not compute EasterSundayOrthodox for {} {}", args[0], args[1]);
						valueNew = "XXXX-XX-XX";
					}
				} else if (function.equals("ShroveTideOrthodox")) {
					Matcher year_m = year_p.matcher(args[0]);
					// check if args[0] is a valid YEAR value
					if (year_m.matches()) {
						valueNew = getShroveTideWeekOrthodox(Integer.parseInt(args[0]));
					} else {
						LOG.info("Could not compute ShroveTideOrthodox for {}", args[0]);
						valueNew = "XXXX-XX-XX";
					}
				} else {
					// if function call doesn't match any supported function
					LOG.error("command not found {}", function);
					valueNew = "XXXX-XX-XX";
				}

				timex.removeFromIndexes();
				timex.setTimexValue(valueNew);
				timex.addToIndexes();
			}
		}
	}

	static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/**
	 * Get the date of a day relative to Easter Sunday in a given year. Algorithm used is from the "Physikalisch-Technische Bundesanstalt Braunschweig" PTB.
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param year
	 * @param days
	 * @return date
	 */
	public String getEasterSunday(int year, int days) {
		int K = year / 100;
		int M = 15 + ((3 * K + 3) / 4) - ((8 * K + 13) / 25);
		int S = 2 - ((3 * K + 3) / 4);
		int A = year % 19;
		int D = (19 * A + M) % 30;
		int R = (D / 29) + ((D / 28) - (D / 29) * (A / 11));
		int OG = 21 + D - R;
		int SZ = 7 - (year + (year / 4) + S) % 7;
		int OE = 7 - (OG - SZ) % 7;
		int OS = OG + OE;

		LocalDate d;
		if (OS <= 31)
			d = LocalDate.of(year, 3 /* 1-based */, OS).plusDays(days);
		else
			d = LocalDate.of(year, 4 /* 1-based */, OS - 31).plusDays(days);
		return d.format(FORMATTER);
	}

	/**
	 * Get the date of Eastersunday in a given year
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param year
	 * @return date
	 */
	public String getEasterSunday(int year) {
		return getEasterSunday(year, 0);
	}

	/**
	 * Get the date of a day relative to Easter Sunday in a given year. Algorithm used is from the http://en.wikipedia.org/wiki/Computus#cite_note-otheralgs-47.
	 *
	 * @author Elena Klyachko
	 * @param year
	 * @param days
	 * @return date
	 */
	public String getEasterSundayOrthodox(int year, int days) {
		int A = year % 4;
		int B = year % 7;
		int C = year % 19;
		int D = (19 * C + 15) % 30;
		int E = ((2 * A + 4 * B - D + 34)) % 7;
		int Month = (int) (Math.floor((D + E + 114) / 31));
		int Day = ((D + E + 114) % 31) + 1;

		/*
		 * int K = year / 100; int M = 15 + ( ( 3 * K + 3 ) / 4 ) - ( ( 8 * K + 13 ) / 25 ); int S = 2 - ( (3 * K + 3) / 4 ); int A = year % 19; int D = ( 19 * A + M ) % 30; int R = ( D / 29)
		 * + ( ( D / 28 ) - ( D / 29 ) * ( A / 11 ) ); int OG = 21 + D - R; int SZ = 7 - ( year + ( year / 4 ) + S ) % 7; int OE = 7 - ( OG - SZ ) % 7; int OS = OG + OE;
		 */

		return LocalDate.of(year, Month, Day)//
				.plusDays(days + getJulianDifference(year))//
				.format(FORMATTER);
	}

	/**
	 * Get the date of Eastersunday in a given year
	 *
	 * @author Elena Klyachko
	 * @param year
	 * @return date
	 */
	public String getEasterSundayOrthodox(int year) {
		return getEasterSundayOrthodox(year, 0);
	}

	/**
	 * Get the date of the Shrove-Tide week in a given year
	 *
	 * @author Elena Klyachko
	 * @param year
	 * @return date
	 */
	public String getShroveTideWeekOrthodox(int year) {
		int A = year % 4;
		int B = year % 7;
		int C = year % 19;
		int D = (19 * C + 15) % 30;
		int E = ((2 * A + 4 * B - D + 34)) % 7;
		int Month = (int) (Math.floor((D + E + 114) / 31));
		int Day = ((D + E + 114) % 31) + 1;

		int shroveTideWeek = LocalDate.of(year, Month, Day)//
				.plusDays(getJulianDifference(year))//
				.plusDays(-49)//
				.get(WeekFields.ISO.weekOfWeekBasedYear());
		if (shroveTideWeek < 10) {
			return year + "-W0" + shroveTideWeek;
		}
		return year + "-W" + shroveTideWeek;
	}

	/**
	 * Get the date of a weekday relative to a date, e.g. first Wednesday before 11-23
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param date
	 * @param weekday
	 * @param number
	 * @param count_itself
	 * @return
	 */
	public String getWeekdayRelativeTo(String date, int weekday, int number, boolean count_itself) {
		if (number == 0)
			return date;

		if (number < 0)
			number += 1;

		try {
			LocalDate d = LocalDate.parse(date, FORMATTER);
			int day = d.getDayOfWeek().getValue(); // 1 = Monday.
			int add;
			if ((count_itself && number > 0) || (!count_itself && number <= 0)) {
				add = (day <= weekday) ? weekday - day : weekday - day + 7;
			} else {
				add = (day < weekday) ? weekday - day : weekday - day + 7;
			}
			add += ((number - 1) * 7);
			return d.plusDays(add).format(FORMATTER);
		} catch (DateTimeParseException e) {
			LOG.error(e.getMessage(), e);
			return "";
		}
	}

	/**
	 * Get the date of a the first, second, third etc. weekday in a month
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param number
	 * @param weekday
	 * @param month
	 * @param year
	 * @return date
	 */
	public String getWeekdayOfMonth(int number, int weekday, int month, int year) {
		return getWeekdayRelativeTo(String.format("%04d-%02d-01", year, month), weekday, number, true);
	}

	private int getJulianDifference(int year) {
		// FIXME: this is not entirely correct!
		int century = year / 100 + 1;
		if (century < 18)
			return 10;
		if (century == 18)
			return 11;
		if (century == 19)
			return 12;
		if (century == 20 || century == 21)
			return 13;
		if (century == 22)
			return 14;
		return 15;
	}
}
