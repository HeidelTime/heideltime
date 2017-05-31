package de.unihd.dbs.heideltime.test.english;

import org.junit.Test;

public class EnglishTimeTest extends AbstractHeideltimeTest {
	@Test
	public void testtime_r1a() {
		testSingleCase("2009-12-19T17:00:00", //
				new String[] { "time_r1a", "2009-12-19T17:00:00", "2009-12-19T17:00:00" });
		testSingleCase("2009-12-19 17:00:00", //
				new String[] { "time_r1a", "2009-12-19 17:00:00", "2009-12-19T17:00:00" });
	}

	@Test
	public void testtime_r1b() {
		testSingleCase("2009-12-19T17:00", //
				new String[] { "time_r1b", "2009-12-19T17:00", "2009-12-19T17:00" });
	}

	@Test
	public void testtime_r1c() {
		testSingleCase("12/29/2000 20:29", //
				new String[] { "time_r1c", "12/29/2000 20:29", "2000-12-29T20:29" });
	}

	@Test
	public void testtime_r1d() {
		testSingleCase("12/29/2000 20:29:29", //
				new String[] { "time_r1d", "12/29/2000 20:29:29", "2000-12-29T20:29:29" });
	}

	@Test
	public void testtime_r1e() {
		testSingleCase("12/29/2000 20:29:29.79", //
				new String[] { "time_r1e", "12/29/2000 20:29:29.79", "2000-12-29T20:29:29.79" });
	}

	@Test
	public void testtime_r2a() {
		testSingleCase("09-24-99 1145EST", //
				new String[] { "time_r2a", "09-24-99 1145EST", "1999-09-24T11:45-05" });
	}

	@Test
	public void testtime_r2b() {
		testSingleCase("November 24, 2011 1535 GMT", //
				new String[] { "time_r2b", "November 24, 2011 1535 GMT", "2011-11-24T15:35" });
	}

	@Test
	public void testtime_r2d() {
		testSingleCase("Wed, 29 Dec 2004 00:28:16 +0000", //
				new String[] { "time_r2d", "Wed, 29 Dec 2004 00:28:16 +0000", "2004-12-29T00:28:16+00" });
		testSingleCase("Sat, 29 Jan 2005 17:21:13 -0600", //
				new String[] { "time_r2d", "Sat, 29 Jan 2005 17:21:13 -0600", "2005-01-29T17:21:13-06" });
		testSingleCase("1 Feb 2005 16:13:33 +1300", //
				new String[] { "time_r2d", "1 Feb 2005 16:13:33 +1300", "2005-02-01T16:13:33+13" });
	}

	@Test
	public void testtime_r3a() {
		testSingleCase("midnight Monday", //
				new String[] { "time_r3a", "midnight Monday", "XXXX-XX-XXT24:00" });
		// TODO: 'monday' is lost?
	}

	@Test
	public void testtime_r3b() {
		testSingleCase("Monday night", //
				new String[] { "time_r3b", "Monday night", "XXXX-XX-XXTNI" });
		// TODO: 'monday' is lost?
	}

	@Test
	public void testtime_r3b2() {
		testSingleCase("early Friday morning", //
				new String[] { "time_r3b2", "early Friday morning", "XXXX-XX-XXTMO" });
		// TODO: 'friday' is lost?
	}

	@Test
	public void testtime_r3c() {
		testSingleCase("midnight today", //
				new String[] { "time_r3c", "midnight today", "XXXX-XX-XXT24:00" });
	}

	@Test
	public void testtime_r3d() {
		testSingleCase("yesterday morning", //
				new String[] { "time_r3d", "yesterday morning", "XXXX-XX-XXTMO" });
	}

	@Test
	public void testtime_r3d2() {
		testSingleCase("late yesterday evening", //
				new String[] { "time_r3d2", "late yesterday evening", "XXXX-XX-XXTEV" });
	}

	@Test
	public void testtime_r3e() {
		testSingleCase("last Friday morning", //
				new String[] { "time_r3e", "last Friday morning", "XXXX-XX-XXTMO" });
		// TODO: 'friday' is lost?
	}

	@Test
	public void testtime_r4a() {
		testSingleCase("earlier this afternoon", //
				new String[] { "time_r4a", "earlier this afternoon", "XXXX-XX-XXTAF" });
		testSingleCase("later last night", //
				new String[] { "time_r4a", "later last night", "XXXX-XX-XXTNI" });
	}

	@Test
	public void testtime_r4b() {
		testSingleCase("tonight", //
				new String[] { "time_r4b", "tonight", "XXXX-XX-XXTNI" });
	}

	@Test
	public void testtime_r5a() {
		testSingleCase("circa 9 a.m.", //
				new String[] { "time_r5a", "circa 9 a.m.", "XXXX-XX-XXT09:00" });
	}

	@Test
	public void testtime_r5b() {
		testSingleCase("11 PM", //
				new String[] { "time_r5b", "11 PM", "XXXX-XX-XXT23:00" });
	}

	@Test
	public void testtime_r5c() {
		testSingleCase("11:30 a.m.", //
				new String[] { "time_r5c", "11:30 a.m.", "XXXX-XX-XXT11:30" });
	}

	@Test
	public void testtime_r5d() {
		testSingleCase("9:30 p.m.", //
				new String[] { "time_r5d", "9:30 p.m.", "XXXX-XX-XXT21:30" });
	}

	@Test
	public void testtime_r5e() {
		testSingleCase("10:30:34 a.m.", //
				new String[] { "time_r5e", "10:30:34 a.m.", "XXXX-XX-XXT10:30:34" });
	}

	@Test
	public void testtime_r5f() {
		testSingleCase("10:30:34 p.m.", //
				new String[] { "time_r5f", "10:30:34 p.m.", "XXXX-XX-XXT22:30:34" });
	}

	@Test
	public void testtime_r6a() {
		testSingleCase("9 am Wednesday", //
				new String[] { "time_r6a", "9 am Wednesday", "XXXX-XX-XXT09:00" });
	}

	@Test
	public void testtime_r6b() {
		testSingleCase("9 pm Wednesday", //
				new String[] { "time_r6b", "9 pm Wednesday", "XXXX-XX-XXT21:00" });
	}

	@Test
	public void testtime_r6c() {
		testSingleCase("9:30 a.m. Wednesday", //
				new String[] { "time_r6c", "9:30 a.m. Wednesday", "XXXX-XX-XXT09:30" });
	}

	@Test
	public void testtime_r6d() {
		testSingleCase("9:30 p.m. Wednesday", //
				new String[] { "time_r6d", "9:30 p.m. Wednesday", "XXXX-XX-XXT21:30" });
	}

	@Test
	public void testtime_r7a() {
		testSingleCase("16:00 CET", //
				new String[] { "time_r7a", "16:00 CET", "XXXX-XX-XXT16:00" });
		testSingleCase("1600 CET", //
				new String[] { "time_r7a", "1600 CET", "XXXX-XX-XXT16:00" });
	}

	@Test
	public void testtime_r8a() {
		testSingleCase("the morning of April 18, 1775", //
				new String[] { "time_r8a", "the morning of April 18, 1775", "1775-04-18TMO" });
	}

	@Test
	public void testtime_r8b() {
		testSingleCase("the morning of April 18", //
				new String[] { "time_r8b", "the morning of April 18", "XXXX-04-18TMO" });
	}
}
