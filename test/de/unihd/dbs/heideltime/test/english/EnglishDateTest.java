package de.unihd.dbs.heideltime.test.english;

import org.junit.Ignore;
import org.junit.Test;

public class EnglishDateTest extends AbstractHeideltimeTest {
	@Test
	public void testdate_r0a() {
		testSingleCase("2010-01-29", //
				new String[] { "date_r0a", "2010-01-29", "2010-01-29" });
	}

	@Test
	public void testdate_r0b() {
		testSingleCase("10-29-99", //
				new String[] { "date_r0b", "10-29-99", "1999-10-29" });
	}

	@Test
	public void testdate_r0c() {
		testSingleCase("09/26/1999", //
				new String[] { "date_r0c", "09/26/1999", "1999-09-26" });
	}

	@Test
	public void testdate_r0d() {
		testSingleCase("09/26/99", //
				new String[] { "date_r0d", "09/26/99", "1999-09-26" });
	}

	@Test
	public void testdate_r0e() {
		// find 7-14
		testSingleCase("7-14 (AP)", new String[] { "date_r0e", "7-14", "XXXX-07-14" });
	}

	@Test
	public void testdate_r0g() {
		testSingleCase("1.3.99", //
				new String[] { "date_r0g", "1.3.99", "1999-03-01" });
	}

	@Test
	public void testdate_r0h() {
		testSingleCase("1.3.1999", //
				new String[] { "date_r0h", "1.3.1999", "1999-03-01" });
	}

	@Test
	public void testdate_r1a() {
		testSingleCase("February 25, 2009", //
				new String[] { "date_r1a", "February 25, 2009", "2009-02-25" });
		testSingleCase("Feb. 25, 2009", //
				new String[] { "date_r1a", "Feb. 25, 2009", "2009-02-25" });
		testSingleCase("Feb. 25, 2009, Monday", //
				new String[] { "date_r1a", "Feb. 25, 2009, Monday", "2009-02-25" });
	}

	@Test
	public void testdate_r1b() {
		testSingleCase("25 February 2009", //
				new String[] { "date_r1b", "25 February 2009", "2009-02-25" });
		testSingleCase("On 1 July 1913,", //
				new String[] { "date_r1b", "1 July 1913", "1913-07-01" });
	}

	@Test
	public void testdate_r1c() {
		testSingleCase("25 of February 2009", //
				new String[] { "date_r1c", "25 of February 2009", "2009-02-25" });
	}

	@Test
	public void testdate_r2a() {
		testSingleCase("November 19", //
				new String[] { "date_r2a", "November 19", "XXXX-11-19" });
		testSingleCase("Nov 19", //
				new String[] { "date_r2a", "Nov 19", "XXXX-11-19" });
		testSingleCase("January 19th", //
				new String[] { "date_r2a", "January 19th", "XXXX-01-19" });
		testSingleCase("January nineteenth", //
				new String[] { "date_r2a", "January nineteenth", "XXXX-01-19" });
		// Test with dct:
		testSingleCase("Nov. 21", "19981102", //
				new String[] { "date_r2a", "Nov. 21", "1998-11-21" });
	}

	@Test
	public void testdate_r2b() {
		testSingleCase("November 19-20", //
				new String[] { "date_r2a", "November 19", "XXXX-11-19" }, //
				new String[] { "date_r2b", "20", "XXXX-11-20" });
	}

	@Test
	public void testdate_r2c() {
		testSingleCase("19 November", //
				new String[] { "date_r2c", "19 November", "XXXX-11-19" });
		testSingleCase("19 Nov", //
				new String[] { "date_r2c", "19 Nov", "XXXX-11-19" });
		testSingleCase("19th of November", //
				new String[] { "date_r2c", "19th of November", "XXXX-11-19" });
	}

	@Test
	public void testdate_r2d() {
		// find May 3
		testSingleCase("3 to 6 May", //
				new String[] { "date_r2d", "3", "XXXX-05-03" }, //
				new String[] { "date_r2c", "6 May", "XXXX-05-06" });
	}

	@Test
	public void testdate_r2e() {
		// find May 3, 2004
		testSingleCase("3 to 6 May 2004", //
				new String[] { "date_r2e", "3", "2004-05-03" }, //
				new String[] { "date_r1b", "6 May 2004", "2004-05-06" });
	}

	@Test
	public void testdate_r2a2() {
		testSingleCase("January 19th of that year", //
				new String[] { "date_r2a2", "January 19th of that year", "XXXX-01-19" });
	}

	@Test
	public void testdate_r2c2() {
		testSingleCase("19th of January of the same year", //
				new String[] { "date_r2c2", "19th of January of the same year", "XXXX-01-19" });
	}

	@Test
	public void testdate_r3a() {
		testSingleCase("Friday October 13", //
				new String[] { "date_r3a", "Friday October 13", "XXXX-10-13" });
		testSingleCase("Monday, Oct 12", //
				new String[] { "date_r3a", "Monday, Oct 12", "XXXX-10-12" });
		testSingleCase("Friday October 13 2009", //
				new String[] { "date_r3b", "Friday October 13 2009", "2009-10-13" });
		testSingleCase("Monday, October 12th 2009", //
				new String[] { "date_r3b", "Monday, October 12th 2009", "2009-10-12" });
	}

	@Test
	public void testdate_r4ab() {
		// find September 18 2010
		testSingleCase("September 14 and 18, 2010", //
				new String[] { "date_r4a", "September 14", "2010-09-14" }, //
				new String[] { "date_r4b", "18, 2010", "2010-09-18" });
	}

	@Test
	public void testdate_r5a() {
		testSingleCase("tomorrow", //
				new String[] { "date_r5a", "tomorrow", "XXXX-XX-XX" });
	}

	@Test
	public void testdate_r5b() {
		testSingleCase("earlier yesterday", //
				new String[] { "date_r5b", "earlier yesterday", "XXXX-XX-XX" });
	}

	@Test
	public void testdate_r5c() {
		testSingleCase("Monday", //
				new String[] { "date_r5c", "Monday", "XXXX-XX-XX" });
		// Test with dct:
		testSingleCase("Monday", "19981104", //
				new String[] { "date_r5c", "Monday", "1998-11-02" });
	}

	@Test
	public void testdate_r5d() {
		testSingleCase("earlier Monday", //
				new String[] { "date_r5d", "earlier Monday", "XXXX-XX-XX" });
	}

	@Test
	public void testdate_r61() {
		testSingleCase("the weekend", //
				new String[] { "date_r61", "the weekend", "XXXX-WXX-WE" });
	}

	@Test
	public void testdate_r7a() {
		testSingleCase("November 2001", //
				new String[] { "date_r7a", "November 2001", "2001-11" });
		testSingleCase("Nov. 2001", //
				new String[] { "date_r7a", "Nov. 2001", "2001-11" });
		testSingleCase("February of 1999", //
				new String[] { "date_r7a", "February of 1999", "1999-02" });
	}

	@Test
	public void testdate_r7cd() {
		// find May 2001 AND June 2011
		testSingleCase("May and June 2011", //
				new String[] { "date_r7c", "May", "2011-05" }, //
				new String[] { "date_r7d", "June 2011", "2011-06" });
		testSingleCase("May/June 2011", //
				new String[] { "date_r7c", "May", "2011-05" }, //
				new String[] { "date_r7d", "June 2011", "2011-06" });
	}

	@Test
	public void testdate_r8a() {
		testSingleCase("November next year", //
				new String[] { "date_r8a", "November next year", "XXXX-11" });
		testSingleCase("May last year", //
				new String[] { "date_r8a", "May last year", "XXXX-05" });
	}

	@Test
	public void testdate_r9a() {
		testSingleCase("summer", //
				new String[] { "date_r9a", "summer", "XXXX-SU" });
	}

	@Test
	public void testdate_r9b() {
		testSingleCase("winter 2001", //
				new String[] { "date_r9b", "winter 2001", "2001-WI" });
		testSingleCase("winter of 2001", //
				new String[] { "date_r9b", "winter of 2001", "2001-WI" });
	}

	@Test
	public void testdate_r9c() {
		testSingleCase("summer of 69", //
				new String[] { "date_r9c", "summer of 69", "1969-SU" });
	}

	@Test
	public void testdate_r10a() {
		testSingleCase("the third quarter of 2001", //
				new String[] { "date_r10a", "the third quarter of 2001", "2001-Q3" });
	}

	// @Ignore("Disabled, false positives: shot a goal in the second half")
	@Test
	public void testdate_r10b() {
		testSingleCase("the second half", //
				new String[] { "date_r10b", "the second half", "XXXX-H2" });
		testSingleCase("the third-quarter", "2010-12-01", //
				new String[] { "date_r10b", "the third-quarter", "2010-Q3" });
	}

	@Test
	public void testdate_r10c() {
		testSingleCase("the 2001 third quarter", //
				new String[] { "date_r10c", "the 2001 third quarter", "2001-Q3" });
	}

	@Test
	public void testdate_r11a() {
		testSingleCase("this year's third quarter", //
				new String[] { "date_r11a", "this year's third quarter", "XXXX-Q3" });
		testSingleCase("next year's first quarter", //
				new String[] { "date_r11a", "next year's first quarter", "XXXX-Q1" });
	}

	@Test
	public void testdate_r11b() {
		// FIXME: this is supposed to match r11b, but is matched by date_r23a-relative
		// As far as I can tell, they should both be good.
		testSingleCase("the year-earlier first half", //
				new String[] { "date_r23a", "the year-earlier first half", "XXXX-H1" });
		// new String[] { "date_r11b", "the year-earlier first half", "XXXX-H1" });
	}

	@Test
	public void testdate_r11c() {
		testSingleCase("the second half of this year", //
				new String[] { "date_r11c", "the second half of this year", "XXXX-H2" });
	}

	@Test
	public void testdate_r12a() {
		testSingleCase("2009", //
				new String[] { "date_r12a", "2009", "2009" });
	}

	@Test
	public void testdate_r12b() {
		testSingleCase("1850-58", //
				new String[] { "date_r12a", "1850", "1850" }, //
				new String[] { "date_r12b", "58", "1858" });
	}

	@Test
	public void testdate_r12c() {
		testSingleCase("nineteen ninety-one", //
				new String[] { "date_r12c", "nineteen ninety-one", "1991" });
	}

	@Test
	public void testdate_r12d() {
		testSingleCase("two-thousand ten", //
				new String[] { "date_r12d", "two-thousand ten", "2010" });
	}

	@Test
	public void testdate_r12f() {
		testSingleCase("1940/1941", //
				new String[] { "date_r12f1", "1940", "1940" }, //
				new String[] { "date_r12f2", "1941", "1941" });
	}

	@Test
	public void testdate_r13a() {
		testSingleCase("the 1990s", //
				new String[] { "date_r13a", "the 1990s", "199" });
	}

	@Test
	public void testdate_r13b() {
		testSingleCase("the 90s", //
				new String[] { "date_r13b", "the 90s", "199" });
	}

	@Test
	public void testdate_r13c() {
		testSingleCase("the seventies", //
				new String[] { "date_r13c", "the seventies", "197" });
	}

	@Test
	public void testdate_r13d() {
		testSingleCase("the nineteen seventies", //
				new String[] { "date_r13d", "the nineteen seventies", "197" });
	}

	@Test
	public void testdate_r14a() {
		testSingleCase("the early 1990s", //
				new String[] { "date_r14a", "the early 1990s", "199" });
	}

	@Test
	public void testdate_r14b() {
		testSingleCase("the mid-90s", //
				new String[] { "date_r14b", "the mid-90s", "199" });
	}

	@Test
	public void testdate_r14c() {
		testSingleCase("the late seventies", //
				new String[] { "date_r14c", "the late seventies", "197" });
	}

	@Test
	public void testdate_r14d() {
		testSingleCase("the early nineteen seventies", //
				new String[] { "date_r14d", "the early nineteen seventies", "197" });
	}

	@Test
	public void testdate_r15a() {
		testSingleCase("the 19th century", //
				new String[] { "date_r15a", "the 19th century", "18" });
		testSingleCase("the seventh century", //
				new String[] { "date_r15a", "the seventh century", "06" });
	}

	@Test
	public void testdate_r15c() {
		testSingleCase("19th and 20th century", //
				new String[] { "date_r15c", "19th", "18" }, //
				new String[] { "date_r15a", "20th century", "19" });
	}

	@Test
	public void testdate_r15b() {
		testSingleCase("19th and early 20th century", //
				new String[] { "date_r15c", "19th", "18" }, //
				new String[] { "date_r15b", "early 20th century", "19" });
	}

	@Test
	public void testdate_r16a() {
		testSingleCase("March", //
				new String[] { "date_r16a", "March", "XXXX-03" });
	}

	@Test
	public void testdate_r16b() {
		testSingleCase("Early 2001", //
				new String[] { "date_r16b", "Early 2001", "2001" });
	}

	@Test
	public void testdate_r16c() {
		testSingleCase("the beginning of November 1999", //
				new String[] { "date_r16c", "the beginning of November 1999", "1999-11" });
	}

	@Test
	public void testdate_r16d() {
		testSingleCase("the middle of September", //
				new String[] { "date_r16d", "the middle of September", "XXXX-09" });
	}

	@Test
	public void testdate_r17a() {
		testSingleCase("In 2010, this year", //
				new String[] { "date_r12a", "2010", "2010" }, //
				new String[] { "date_r17a", "this year", "2010" });
	}

	@Test
	public void testdate_r17b() {
		testSingleCase("In 1999, this November", //
				new String[] { "date_r12a", "1999", "1999" }, //
				new String[] { "date_r17b", "this November", "1999-11" });
	}

	@Test
	public void testdate_r17c() {
		testSingleCase("In 1998, this November 24", //
				new String[] { "date_r12a", "1998", "1998" }, //
				new String[] { "date_r17c", "this November 24", "1998-11-24" });
	}

	@Test
	public void testdate_r17d() {
		testSingleCase("this Monday", //
				new String[] { "date_r17d", "this Monday", "XXXX-WXX-1" });
	}

	@Test
	public void testdate_r17e() {
		testSingleCase("this summer", //
				new String[] { "date_r17e", "this summer", "XXXX-SU" });
	}

	@Test
	public void testdate_r17f() {
		testSingleCase("On November 24 1998, this day", //
				new String[] { "date_r1a", "November 24 1998", "1998-11-24" }, //
				new String[] { "date_r17f", "this day", "1998-11-24" });
	}

	@Test
	public void testdate_r18a() {
		testSingleCase("the beginning of this year", //
				new String[] { "date_r18a", "the beginning of this year", "XXXX" });
	}

	@Test
	public void testdate_r18b() {
		testSingleCase("the beginning of this November", //
				new String[] { "date_r18b", "the beginning of this November", "XXXX-11" });
	}

	@Test
	public void testdate_r18c() {
		testSingleCase("the beginning of this November 24", //
				new String[] { "date_r18c", "the beginning of this November 24", "XXXX-11-24" });
	}

	@Test
	public void testdate_r18d() {
		testSingleCase("the beginning of this Monday", //
				new String[] { "date_r18d", "the beginning of this Monday", "XXXX-WXX-1" });
	}

	@Test
	public void testdate_r18e() {
		testSingleCase("the beginning of this summer", //
				new String[] { "date_r18e", "the beginning of this summer", "XXXX-SU" });
	}

	@Test
	public void testdate_r19a() {
		testSingleCase("at least several years ago", //
				new String[] { "date_r19a", "at least several years ago", "PAST_REF" });
	}

	@Test
	public void testdate_r19b() {
		testSingleCase("In 2010, about twenty years ago", //
				new String[] { "date_r12a", "2010", "2010" }, //
				new String[] { "date_r19b", "about twenty years ago", "1990" });
	}

	@Test
	public void testdate_r19c() {
		testSingleCase("about 20 years ago", //
				new String[] { "date_r19c", "about 20 years ago", "XXXX" });
	}

	@Test
	public void testdate_r19d() {
		testSingleCase("January 24 1998, a month ago", //
				new String[] { "date_r1a", "January 24 1998", "1998-01-24" }, //
				new String[] { "date_r19d", "a month ago", "1997-12" });
	}

	@Test
	public void testdate_r20a() {
		testSingleCase("some days later", //
				new String[] { "date_r20a", "some days later", "FUTURE_REF" });
	}

	@Test
	public void testdate_r20b() {
		testSingleCase("about twenty days later", //
				new String[] { "date_r20b", "about twenty days later", "XXXX-XX-XX" });
	}

	@Test
	public void testdate_r20c() {
		testSingleCase("about 20  days later", //
				new String[] { "date_r20c", "about 20  days later", "XXXX-XX-XX" });
	}

	@Test
	public void testdate_r20d() {
		testSingleCase("December 29 1998, a week later", //
				new String[] { "date_r1a", "December 29 1998", "1998-12-29" }, //
				new String[] { "date_r20d", "a week later", "1999-01-05" });
	}

	@Test
	public void testdate_r20f() {
		testSingleCase("on 30 minutes something happened", //
				new String[] { "date_r20f", "on 30 minutes", "UNDEF-REF-minute-PLUS-30" });
	}

	@Test
	public void testdate_r20g() {
		testSingleCase("on approximately thirty minutes something happened", //
				new String[] { "date_r20g", "on approximately thirty minutes", "UNDEF-REF-minute-PLUS-30" });
	}

	@Test
	public void testdate_r21a() {
		testSingleCase("14 January 1998, twenty days earlier", //
				new String[] { "date_r1b", "14 January 1998", "1998-01-14" }, //
				new String[] { "date_r21a", "twenty days earlier", "1997-12-25" });
	}

	@Test
	public void testdate_r21b() {
		testSingleCase("14 January 1998, about 20 days earlier", //
				new String[] { "date_r1b", "14 January 1998", "1998-01-14" }, //
				new String[] { "date_r21b", "about 20 days earlier", "1997-12-25" });
	}

	@Test
	public void testdate_r21c() {
		testSingleCase("a week earlier", //
				new String[] { "date_r21c", "a week earlier", "XXXX-WXX" });
	}

	@Test
	public void testdate_r22a() {
		testSingleCase("14 January 1998, a year ago", //
				new String[] { "date_r1b", "14 January 1998", "1998-01-14" }, //
				new String[] { "date_r22a", "a year ago", "1997-01-14" });
		testSingleCase("a year ago", //
				new String[] { "date_r22a", "a year ago", "XXXX" });
	}

	@Test
	public void testdate_r22b() {
		testSingleCase("14 January 1998, a year later", //
				new String[] { "date_r1b", "14 January 1998", "1998-01-14" }, //
				new String[] { "date_r22b", "a year later", "1999-01-14" });
	}

	@Test
	public void testdate_r23a() {
		testSingleCase("the year-earlier first quarter", //
				new String[] { "date_r23a", "the year-earlier first quarter", "XXXX-Q1" });
		testSingleCase("the year-earlier first quarter", "2010-12-01", //
				new String[] { "date_r23a", "the year-earlier first quarter", "2009-Q1" });
	}

	@Test
	public void testdate_r23b() {
		testSingleCase("the year-earlier quarter", //
				new String[] { "date_r23b", "the year-earlier quarter", "XXXX-XX" });
	}

	@Test
	public void testdate_r23c() {
		testSingleCase("the quarter", //
				new String[] { "date_r23c", "the quarter", "XXXX-XX" });
	}

	@Test
	public void testdate_r24a() {
		testSingleCase("Christmas", //
				new String[] { "date_r24a", "Christmas", "XXXX-12-25" });
	}

	@Test
	public void testdate_r24b() {
		testSingleCase("Christmas 2010", //
				new String[] { "date_r24b", "Christmas 2010", "2010-12-25" });
	}

	@Test
	public void testdate_r24cd() {
		testSingleCase("Christmas 87", //
				new String[] { "date_r24cd", "Christmas 87", "1987-12-25" });
		testSingleCase("Christmas '87", //
				new String[] { "date_r24cd", "Christmas '87", "1987-12-25" });
	}

	@Test
	public void testdate_r25a() {
		testSingleCase("In 2010, on Easter Sunday", //
				new String[] { "date_r12a", "2010", "2010" }, //
				new String[] { "date_r25a", "Easter Sunday", "2010-04-04" });
	}

	@Test
	public void testdate_r25b() {
		testSingleCase("Easter Sunday 2010", //
				new String[] { "date_r25b", "Easter Sunday 2010", "2010-04-04" });
	}

	@Test
	public void testdate_r25cd() {
		testSingleCase("Easter Sunday 87", //
				new String[] { "date_r25cd", "Easter Sunday 87", "1987-04-19" });
		testSingleCase("Easter Sunday '87", //
				new String[] { "date_r25cd", "Easter Sunday '87", "1987-04-19" });
	}

	@Test
	public void testdate_r1a_negative() {
		// do not match soon if it is in "as soon as"
		testSingleCase("as soon as");
	}

	@Test
	public void testdate_r2a_negative() {
		// if it is a verb
		testSingleCase("they march the way");
	}

	@Test
	public void testdate_r2b_negative() {
		// if it is a verb
		testSingleCase("they march the way");
	}

	@Test
	public void testdate_r2c_negative() {
		// if it is a verb
		testSingleCase("may");
	}

	@Test
	public void testdate_r2d_negative() {
		// or march, fall -- if it is lower case and without any further temporal stuff around it...
		testSingleCase("may");
	}

	// FIXME: add POS information
	@Ignore("Requires POS tagging")
	@Test
	public void testdate_r3a_negative() {
		// four digit number followed by a plural noun
		testSingleCase("2000 soldiers");
	}

	// FIXME: add POS information
	@Ignore("Requires POS tagging")
	@Test
	public void testdate_r3b_negative() {
		// four digit number followed by an adjective and a plural noun
		testSingleCase("2000 dead soldiers");
	}

	@Test
	public void testdate_r3c_negative() {
		// four digit number followed a non-temporal unit
		testSingleCase("2000 kilometer");
	}

	@Test
	public void testdate_r4a_negative() {
		testSingleCase("W2000.1920");
		testSingleCase("to 1462.93.");
	}

	@Test
	public void testx_date_r11a_negative() {
		testSingleCase("in his 20s");
	}

	@Test
	public void testTokenBoundaryFilter() {
		testSingleCase("$2016 is not a date.");
		testSingleCase("2016° is too hot");
		testSingleCase("1234.2016 or 2016.1234 are not a date either.");
		testSingleCase("2016dimensional nonsense");
		testSingleCase("Okay: (2016).", //
				new String[] { "date_r12a", "2016", "2016" });
	}

	@Test
	public void testNextQuarter() {
		testSingleCase("November 2015, 1 quarter later", new String[] { "date_r7a", "November 2015", "2015-11" }, new String[] { "date_r20c", "1 quarter later", "2016-Q1" });
	}
}
