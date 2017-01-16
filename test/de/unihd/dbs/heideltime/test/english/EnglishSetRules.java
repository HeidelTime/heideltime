package de.unihd.dbs.heideltime.test.english;

import org.junit.Test;

public class EnglishSetRules extends AbstractHeideltimeTest {
	@Test
	public void testset_r1a() {
		testSingleCase("each day", //
				new String[] { "set_r1a", "each day", "P1D" });
	}

	@Test
	public void testset_r1b() {
		testSingleCase("every Monday", //
				new String[] { "set_r1b", "every Monday", "XXXX-WXX-1" });
	}

	@Test
	public void testset_r1c() {
		testSingleCase("each September", //
				new String[] { "set_r1c", "each September", "XXXX-09" });
	}

	@Test
	public void testset_r1d() {
		testSingleCase("every summer", //
				new String[] { "set_r1d", "every summer", "XXXX-SU" });
	}

	@Test
	public void testset_r2a() {
		testSingleCase("once a week", //
				new String[] { "set_r2a", "once a week", "P1W" });
	}

	@Test
	public void testset_r2b() {
		testSingleCase("twice a month", //
				new String[] { "set_r2b", "twice a month", "P1M" });
	}

	@Test
	public void testset_r2c() {
		testSingleCase("three times a month", //
				new String[] { "set_r2c", "three times a month", "P1M" });
	}

	@Test
	public void testset_r2d() {
		testSingleCase("40 times per month", //
				new String[] { "set_r2d", "40 times per month", "P1M" });
	}

	@Test
	public void testset_r2e() {
		testSingleCase("a month", //
				new String[] { "set_r2e", "a month", "P1M" });
	}

	@Test
	public void testset_r2f() {
		testSingleCase("a minute", //
				new String[] { "set_r2f", "a minute", "PT1M" });
	}

	@Test
	public void testset_r3a() {
		testSingleCase("every 5 years", //
				new String[] { "set_r3a", "every 5 years", "P5Y" });
	}

	@Test
	public void testset_r3b() {
		testSingleCase("every two days", //
				new String[] { "set_r3b", "every two days", "P2D" });
	}

	@Test
	public void testset_r4a() {
		testSingleCase("2 days each week", //
				new String[] { "set_r4a", "2 days each week", "P1W" });
	}

	@Test
	public void testset_r5a() {
		testSingleCase("annually", //
				new String[] { "set_r5a", "annually", "XXXX" });
	}

	@Test
	public void testset_r6a() {
		testSingleCase("Monday afternoons", //
				new String[] { "set_r6a", "Monday afternoons", "XXXX-WXX-1TAF" });
	}

	@Test
	public void testset_r6b() {
		// find: Monday nights
		testSingleCase("Monday and Tuesday nights", //
				new String[] { "set_r6b", "Monday", "XXXX-WXX-1TNI" }, //
				new String[] { "set_r6a", "Tuesday nights", "XXXX-WXX-2TNI" });
	}
}
