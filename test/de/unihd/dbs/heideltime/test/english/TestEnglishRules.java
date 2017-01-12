package de.unihd.dbs.heideltime.test.english;

import static org.junit.Assert.fail;

import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Timex3;

public class TestEnglishRules {
	String[][] CASES = { // Rule name, sample text, [expected covered text]
			{ "date_historic_1a-BCADhint", "190 BC" }, // 1- to 4-digit year
			{ "date_historic_1b-BCADhint", "BC 190" }, // 1- to 4-digit year
			{ "date_historic_1c-BCADhint", "190 or 180 BC" }, // find "190 BC"; 1- to 4-digit year
			{ "date_historic_2a-BCADhint", "March 190 BC" }, // 1- to 4-digit year
			{ "date_historic_2b", "March 190" }, // 3-digit year
			{ "date_historic_2c", "in March 90", "March 90" }, // 2-digit year
			{ "date_historic_2d", "March of 90", "March of 90" }, // 2-digit year
			{ "date_historic_3a-BCADhint", "March 29, 190 BC" }, // 1- to 4-digit year
			{ "date_historic_3b-BCADhint", "29 March 190 BC" }, // 1- to 4-digit year
			{ "date_historic_3c-BCADhint", "29th of March 190 BC" }, // 1- to 4-digit year
			{ "date_historic_3d-BCADhint", "March 29, 190" }, // 3-digit year
			{ "date_historic_3e-BCADhint", "March 29, 90" }, // 2-digit year
			{ "date_historic_4a-BCADhint", "summer of 190 BC" }, // 1- to 4-digit year
			{ "date_historic_5a-BCADhint", "the 2nd century BC" }, //
			{ "date_historic_5b-BCADhint", "beginning of the 2nd century BC" }, //
			{ "date_historic_5c-BCADhint", "2nd or 3rd century BC" }, // find "2nd century BC"
			{ "date_historic_5d-BCADhint", "beginning of the 2nd or 3rd century BC" }, // find "beginning 2nd century BC"
			{ "date_historic_6a-BCADhint", "1990s BC" }, //
			{ "date_historic_6b-BCADhint", "190s BC" }, //
			{ "date_historic_6c-BCADhint", "90s BC" }, //
			{ "date_historic_7a", "in 190", "190" }, // 3-digit year
			{ "date_historic_7b", "in 190,", "190" }, // 3-digit year
			{ "date_historic_7c", "\n190\n", "190" }, // (2- to 4-digit year
			{ "date_historic_7d", "year of 90" }, // 2-digit year
			{ "date_historic_7e", "year of 190" }, // 3-digit year
			{ "date_historic_8ab", "in 90,", "90" }, // 2-digit year
			{ "date_historic_8ab", "in 90", "90" }, // 2-digit year
			{ "date_historic_0ab", "in 90 cases", "" }, // 2- to 4-digit year
			{ "date_historic_0ab", "in 90 nice cases", "" }, // 2- to 4-digit year
			{ "date_historic_0ab", "in 90 nice law cases", "" }, // 2- to 4-digit year
			{ "date_historic_0d", "in 90 percent", "" }, // 2- to 4-digit year
			{ "date_r0a", "2010-01-29" }, //
			{ "date_r0b", "10-29-99" }, //
			{ "date_r0c", "09/26/1999" }, //
			{ "date_r0d", "09/26/99" }, //
			{ "date_r0e", "7-14 (AP)", "7-14" }, // find 7-14
			{ "date_r0g", "1.3.99" }, //
			{ "date_r0h", "1.3.1999" }, //
			{ "date_r1a", "February 25, 2009" }, //
			{ "date_r1a", "Feb. 25, 2009" }, //
			{ "date_r1a", "Feb. 25, 2009, Monday" }, //
			{ "date_r1b", "25 February 2009" }, //
			{ "date_r1c", "25 of February 2009" }, //
			{ "date_r2a", "November 19" }, //
			{ "date_r2a", "Nov 19" }, //
			{ "date_r2a", "January 19th" }, //
			{ "date_r2a", "January nineteenth" }, //
			{ "date_r2b", "November 19-20" }, // find November 20
			{ "date_r2c", "19 November" }, //
			{ "date_r2c", "19 Nov" }, //
			{ "date_r2c", "19th of November" }, //
			{ "date_r2d", "3 to 6 May" }, // find May 3
			{ "date_r2e", "3 to 6 May 2004" }, // find May 3, 2004
			{ "date_r2a2", "January 19th of that year" }, //
			{ "date_r2b2", "19th of January of the same year" }, //
			{ "date_r3a", "Friday October 13" }, //
			{ "date_r3a", "Monday, Oct 12" }, //
			{ "date_r3b", "Friday October 13 2009" }, //
			{ "date_r3b", "Monday, October 12th 2009" }, //
			{ "date_r4a", "September 14 and 18, 2010" }, // find September 14 2010
			{ "date_r4b", "September 14 and 18, 2010" }, // find September 18 2010
			{ "date_r5a", "tomorrow" }, //
			{ "date_r5b", "earlier yesterday" }, //
			{ "date_r5c", "Monday" }, //
			{ "date_r5d", "earlier Monday" }, //
			{ "date_r6a", "the weekend" }, //
			{ "date_r7a", "November 2001" }, //
			{ "date_r7a", "Nov. 2001" }, //
			{ "date_r7a", "February of 1999" }, //
			{ "date_r7b", "May and June 2011" }, // find May 2001 AND June 2011
			{ "date_r8a", "November next year" }, //
			{ "date_r8a", "May last year" }, //
			{ "date_r9a", "summer" }, //
			{ "date_r9b", "winter 2001" }, //
			{ "date_r9b", "winter of 2001" }, //
			{ "date_r9c", "summer of 69" }, //
			{ "date_r10a", "the third quarter of 2001" }, //
			{ "date_r10b", "the second half" }, //
			{ "date_r10c", "the 2001 third quarter" }, //
			{ "date_r11a", "this year's third quarter" }, //
			{ "date_r11a", "next year's first quarter" }, //
			{ "date_r11b", "the year-earlier first half" }, //
			{ "date_r11c", "the second half of this year" }, //
			{ "date_r12a", "2009" }, //
			{ "date_r12b", "1850-58" }, // find: 1858
			{ "date_r12c", "nineteen ninety-one" }, //
			{ "date_r12d", "two-thousand ten" }, //
			{ "date_r13a", "the 1990s" }, //
			{ "date_r13b", "the 90s" }, //
			{ "date_r13c", "the seventies" }, //
			{ "date_r13d", "the nineteen seventies" }, //
			{ "date_r14a", "the early 1990s" }, //
			{ "date_r14b", "the mid-90s" }, //
			{ "date_r14c", "the late seventies" }, //
			{ "date_r14d", "the early nineteen seventies" }, //
			{ "date_r15a", "the 19th century" }, //
			{ "date_r15a", "the seventh century" }, //
			{ "date_r16a", "March" }, //
			{ "date_r16b", "Early 2001" }, //
			{ "date_r16c", "the beginning of November 1999" }, //
			{ "date_r16d", "the middle of September" }, //
			{ "date_r17a", "this year" }, //
			{ "date_r17b", "this November" }, //
			{ "date_r17c", "this November 24" }, //
			{ "date_r17d", "this Monday" }, //
			{ "date_r17e", "this summer" }, //
			{ "date_r17f", "this day" }, // using UNDEF-REF normalization
			{ "date_r18a", "the beginning of this year" }, //
			{ "date_r18b", "the beginning of this November" }, //
			{ "date_r18c", "the beginning of this November 24" }, //
			{ "date_r18d", "the beginning of this Monday" }, //
			{ "date_r18e", "the beginning of this summer" }, //
			{ "date_r19a", "at least several years ago" }, //
			{ "date_r19b", "about twenty years ago" }, //
			{ "date_r19c", "about 20 years ago" }, //
			{ "date_r19d", "a month ago" }, //
			{ "date_r20a", "some days later" }, //
			{ "date_r20b", "about twenty days later" }, //
			{ "date_r20c", "about 20  days later" }, //
			{ "date_r20d", "a week later" }, //
			{ "date_r21a", "twenty days earlier" }, //
			{ "date_r21b", "about 20 days earlier" }, //
			{ "date_r21c", "a week earlier" }, //
			{ "date_r22a", "a year ago" }, //
			{ "date_r22b", "a year later" }, //
			{ "date_r23a", "the year-earlier first quarter" }, //
			{ "date_r23b", "the year-earlier quarter" }, //
			{ "date_r23c", "the quarter" }, //
			{ "date_r24a", "Christmas" }, //
			{ "date_r24b", "Christmas 2010" }, //
			{ "date_r24cd", "Christmas 87" }, //
			{ "date_r24cd", "Christmas '87" }, //
			{ "date_r25a", "Easter Sunday" }, //
			{ "date_r25b", "Easter Sunday 2010" }, //
			{ "date_r25cd", "Easter Sunday 87" }, //
			{ "date_r25cd", "Easter Sunday '87" }, //
			{ "date_r1a_negative", "as soon as" }, // do not match soon if it is in "as soon as"
			{ "date_r2a_negative", "they march the way" }, // if it is a verb
			{ "date_r2b_negative", "they march the way" }, // if it is a verb
			{ "date_r2c_negative", "may" }, // if it is a verb
			{ "date_r2d_negative", "may" }, // or march, fall -- if it is lower case and without any further temporal stuff around it...
			{ "date_r3a_negative", "2000 soldiers" }, // four digit number followed by a plural noun
			{ "date_r3b_negative", "2000 dead soldiers" }, // four digit number followed by an adjective and a plural noun
			{ "date_r3c_negative", "2000 kilometer" }, // four digit number followed a non-temporal unit
			{ "date_r4a_negative", "W2000.1920" }, //
			{ "x_date_r11a_negative", "in his 20s" }, //
			{ "duration_r1ad", "less than sixty days" }, //
			{ "duration_r1e12", "less than 60 days" }, //
			{ "duration_r1cf", "several days" }, //
			{ "duration_r1ad", "less than sixty minutes" }, //
			{ "duration_r1e12", "less than 60 minutes" }, //
			{ "duration_r1cf", "several minutes" }, //
			{ "duration_r2ad", "at least the last twenty years" }, //
			{ "duration_r2be", "at least the last 20 years" }, //
			{ "duration_r2cf", "at least the last several years" }, //
			{ "duration_r2ad", "at least the last twenty minutes" }, //
			{ "duration_r2be", "at least the last 20 minutes" }, //
			{ "duration_r2cf", "at least the last several minutes" }, //
			{ "duration_r3ac", "a three-year period" }, //
			{ "duration_r3bd", "a 300 year period" }, //
			{ "duration_r3ac", "a three-hour period" }, //
			{ "duration_r3bd", "a 300 hour period" }, //
			{ "duration_r5_a", "two and six days", "two" },
			{ "duration_r1a_negative", "about 200 years older" }, //
			{ "duration_r1b_negative", "several days old" }, //
			{ "duration_r1c_negative", "59-year-old" }, //
			/* */
			/*
			{ "interval_interval_01", "from 1999 to 2012" }, //
			{ "interval_interval_02", "between March and May" }, //
			{ "interval_interval_03", "20.3.2003 - 1.5.2003" }, //
			{ "interval_interval_04", "20.3.2003 to 1.5.2003" }, //
			{ "interval_interval_05", "on 20.3.2003 the war began and it lastet until 1.5.2003" }, //
			{ "interval_interval_06", "for December after leaving in February" }, //
			{ "interval_interval_07", "began on March 20 in 2003 and ended on May 1" }, //
			{ "interval_interval_08", "in 1999/2000" }, //
			{ "interval_interval_09", "War ended in May, after fighting from March on" }, //
			{ "interval_interval_10", "March, April and May" }, //
			{ "interval_interval_11", "Monday, Thuesday, Wednesday and Thursday" }, //
			{ "set_r1a", "each day" }, //
			{ "set_r1b", "every Monday" }, //
			{ "set_r1c", "each September" }, //
			{ "set_r1d", "every summer" }, //
			{ "set_r2a", "once a week" }, //
			{ "set_r2b", "twice a month" }, //
			{ "set_r2c", "three times a month" }, //
			{ "set_r2d", "40 times per month" }, //
			{ "set_r2e", "a month" }, //
			{ "set_r2f", "a minute" }, //
			{ "set_r3a", "every 5 years" }, //
			{ "set_r3b", "every two days" }, //
			{ "set_r4a", "2 days each week" }, //
			{ "set_r5a", "annually" }, //
			{ "set_r6a", "Monday afternoons" }, //
			{ "set_r6b", "Monday and Tuesday nights" }, // find: Monday nights
			*/
			{ "time_r1a", "2009-12-19T17:00:00" }, //
			{ "time_r1a", "2009-12-19 17:00:00" }, //
			{ "time_r1b", "2009-12-19T17:00" }, //
			{ "time_r1c", "12/29/2000 20:29" }, //
			{ "time_r1d", "12/29/2000 20:29:29" }, //
			{ "time_r1e", "12/29/2000 20:29:29.79" }, //
			{ "time_r2a", "09-24-99 1145EST" }, // TimeStamp style with timezone information
			{ "time_r2b", "November 24, 2011 1535 GMT" }, //
			{ "time_r2c", "Wed, 29 Dec 2004 00:28:16 +0000" }, //
			{ "time_r2d", "Sat, 29 Jan 2005 17:21:13 -0600" }, //
			{ "time_r2d", "1 Feb 2005 16:13:33 +1300" }, //
			{ "time_r3a", "midnight Monday" }, //
			{ "time_r3b", "Monday night" }, //
			{ "time_r3b2", "early Friday morning" }, //
			{ "time_r3c", "midnight today" }, //
			{ "time_r3d", "yesterday morning" }, //
			{ "time_r3d2", "late yesterday evening" }, //
			{ "time_r3e", "last Friday morning" }, //
			{ "time_r4a", "earlier this afternoon" }, //
			{ "time_r4a", "later last night" }, //
			{ "time_r4b", "tonight" }, //
			{ "time_r5a", "circa 9 a.m." }, //
			{ "time_r5b", "11 PM" }, //
			{ "time_r5c", "11:30 a.m." }, //
			{ "time_r5d", "9:30 p.m." }, //
			{ "time_r5e", "10:30:34 a.m." }, //
			{ "time_r5e", "10:30:34 p.m." }, //
			{ "time_r6a", "9 am Wednesday" }, //
			{ "time_r6b", "9 pm Wednesday" }, //
			{ "time_r6c", "9:30 a.m. Wednesday" }, //
			{ "time_r6d", "9:30 p.m. Wednesday" }, //
			{ "time_r8a", "the morning of April 18, 1775" }, //
			{ "time_r8c", "the morning of April 18" }, //
			/* */
	};

	private HeidelTimeStandalone standalone;

	@Before
	public void init() {
		standalone = new HeidelTimeStandalone(Language.ENGLISH, DocumentType.NARRATIVES, //
				OutputType.XMI, "test/test.props", POSTagger.NO);
	}

	@Test
	public void testEnglishRules() {
		for (String[] set : CASES) {
			testSingleCase(set[0], set[1], set.length >= 3 ? set[2] : set[1]);
		}
	}

	ResultFormatter formatter = new TestResultFormatter();

	private static class TestResultFormatter implements ResultFormatter {
		@Override
		public String format(JCas jcas) throws Exception {
			StringBuilder buf = new StringBuilder();
			String text = jcas.getDocumentText();
			AnnotationIndex<Timex3> times = jcas.getAnnotationIndex(Timex3.type);
			for (Timex3 timex3 : times) {
				buf.append(timex3.getFoundByRule());
				buf.append('\t');
				buf.append(text.substring(timex3.getBegin(), timex3.getEnd()));
				buf.append('\n');
			}
			return buf.toString();
		}
	}

	// NOT a @Test, only a part.
	private void testSingleCase(String rule, String fragment, String expectf) {
		String expected = rule + "\t" + expectf;
		if (expected.contains("negative"))
			expected = "";
		try {
			String result = standalone.process(fragment, null, formatter);
			String[] parts = result.split("\n");
			for (String part : parts) {
				if (expected.equals(part.replaceAll("-(relative|explicit)", "")))
					continue;
				System.err.println(rule + "\t" + fragment + " -> " + part);
			}
		} catch (DocumentCreationTimeMissingException e) {
			fail(e.getMessage());
		}
	}
}
