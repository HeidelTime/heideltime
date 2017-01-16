package de.unihd.dbs.heideltime.test.english;

import org.junit.Test;

public class EnglishDurationTest extends AbstractHeideltimeTest {
	@Test
	public void testduration_r1ad() {
		testSingleCase("less than sixty days", //
				new String[] { "duration_r1a", "less than sixty days", "P60D" });
		testSingleCase("less than sixty minutes", //
				new String[] { "duration_r1d", "less than sixty minutes", "PT1H" });
	}

	@Test
	public void testduration_r1e12() {
		testSingleCase("less than 60 days", //
				new String[] { "duration_r1e1", "less than 60 days", "P60D" });
		testSingleCase("less than 60 minutes", //
				new String[] { "duration_r1e2", "less than 60 minutes", "PT1H" });
	}

	@Test
	public void testduration_r1cf() {
		testSingleCase("several days", //
				new String[] { "duration_r1c", "several days", "PXD" });
		testSingleCase("several minutes", //
				new String[] { "duration_r1f", "several minutes", "PTXM" });
	}

	@Test
	public void testduration_r2ad() {
		testSingleCase("at least the last twenty years", //
				new String[] { "duration_r2a", "at least the last twenty years", "P20Y" });
		testSingleCase("at least the last twenty minutes", //
				new String[] { "duration_r2d", "at least the last twenty minutes", "PT20M" });
	}

	@Test
	public void testduration_r2be() {
		testSingleCase("at least the last 20 years", //
				new String[] { "duration_r2b", "at least the last 20 years", "P20Y" });
		testSingleCase("at least the last 20 minutes", //
				new String[] { "duration_r2e", "at least the last 20 minutes", "PT20M" });
	}

	@Test
	public void testduration_r2cf() {
		testSingleCase("at least the last several years", //
				new String[] { "duration_r2c", "at least the last several years", "PXY" });
		testSingleCase("at least the last several minutes", //
				new String[] { "duration_r2f", "at least the last several minutes", "PTXM" });
	}

	@Test
	public void testduration_r3a() {
		testSingleCase("a three-year period", //
				new String[] { "duration_r3a", "a three-year period", "P3Y" });
	}

	@Test
	public void testduration_r3b() {
		testSingleCase("a 300 year period", //
				new String[] { "duration_r3b", "a 300 year period", "P300Y" });
	}

	@Test
	public void testduration_r5b1() {
		testSingleCase("two and six days", //
				new String[] { "duration_r5b1", "two", "P2D" }, //
				new String[] { "duration_r1a", "six days", "P6D" });
	}

	@Test
	public void testduration_r1a_negative() {
		testSingleCase("about 200 years older"); // EMPTY!
	}

	@Test
	public void testduration_r1b_negative() {
		testSingleCase("several days old"); // EMPTY!
	}

	@Test
	public void testduration_r1c_negative() {
		testSingleCase("59-year-old"); // EMPTY!
	}
}
