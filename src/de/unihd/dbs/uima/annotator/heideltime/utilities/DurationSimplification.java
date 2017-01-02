package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationSimplification {
	private static final Pattern SIMPLIFY_DURATION = Pattern.compile("(PT?)(\\d+)([HM])");

	/**
	 * Durations of a finer granularity are mapped to a coarser one if possible, e.g., "PT24H" -> "P1D". One may add several further corrections.
	 * 
	 * @param value
	 * @return
	 */
	public static String simplify(String value) {
		Matcher m = SIMPLIFY_DURATION.matcher(value);
		if (m.matches()) {
			int ival = Integer.parseInt(m.group(2));
			String g1 = m.group(1), g3 = m.group(3);
			if (g1.equals("PT")) {
				// x*24 hours to x days
				if (g3.equals("H") && (ival % 24 == 0))
					return "P" + (ival / 24) + "D";
				// x*60 minutes to x days
				if (g3.equals("M") && (ival % 60 == 0)) {
					// x*60*24 minutes to x days
					if (ival % 1440 == 0)
						return "P" + (ival / 1440) + "D";
					return "PT" + (ival / 60) + "H";
				}
			} else if (g1.equals("P")) {
				// x*12 months to years
				if (g3.equals("M") && (ival % 12 == 0))
					return "P" + (ival / 12) + "Y";

			}
		}
		return value;
	}
}