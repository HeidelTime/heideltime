package de.unihd.dbs.heideltime.test.english;

import org.junit.Ignore;
import org.junit.Test;

public class EnglishDateHistoricTest extends AbstractHeideltimeTest {
	@Test
	public void testdate_historic_1a_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("190 BC", //
				new String[] { "date_historic_1a-BCADhint", "190 BC", "BC0190" });
	}

	@Test
	public void testdate_historic_1b_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("BC 190", //
				new String[] { "date_historic_1b-BCADhint", "BC 190", "BC0190" });
	}

	@Test
	public void testdate_historic_1c_BCADhint() {
		// find "190 BC"; 1- to 4-digit year
		testSingleCase("190 or 180 BC", //
				new String[] { "x_date_historic_1c-BCADhint", "190", "BC0190" }, //
				new String[] { "date_historic_1a-BCADhint", "180 BC", "BC0180" });
	}

	@Test
	public void testdate_historic_2a_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("March 190 BC", //
				new String[] { "date_historic_2a-BCADhint", "March 190 BC", "BC0190-03" });
	}

	@Test
	public void testdate_historic_2b() {
		// 3-digit year
		testSingleCase("March 190", //
				new String[] { "date_historic_2b", "March 190", "0190-03" });
	}

	@Test
	public void testdate_historic_2c() {
		// 2-digit year
		testSingleCase("in March 90", new String[] { "date_historic_2c", "March 90", "0090-03" });
	}

	@Test
	public void testdate_historic_2d() {
		// 2-digit year
		testSingleCase("March of 90", new String[] { "date_historic_2d", "March of 90", "0090-03" });
	}

	@Test
	public void testdate_historic_3a_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("March 29, 190 BC", //
				new String[] { "date_historic_3a-BCADhint", "March 29, 190 BC", "BC0190-03-29" });
	}

	@Test
	public void testdate_historic_3b_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("29 March 190 BC", //
				new String[] { "date_historic_3b-BCADhint", "29 March 190 BC", "BC0190-03-29" });
	}

	@Test
	public void testdate_historic_3c_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("29th of March 190 BC", //
				new String[] { "date_historic_3c-BCADhint", "29th of March 190 BC", "BC0190-03-29" });
	}

	@Test
	public void testdate_historic_3d() {
		// 3-digit year
		testSingleCase("March 29, 190", //
				new String[] { "date_historic_3d", "March 29, 190", "0190-03-29" });
	}

	@Test
	public void testdate_historic_3e() {
		// 2-digit year
		testSingleCase("March 29, 90", //
				new String[] { "date_historic_3e", "March 29, 90", "0090-03-29" });
	}

	@Test
	public void testdate_historic_4a_BCADhint() {
		// 1- to 4-digit year
		testSingleCase("summer of 190 BC", //
				new String[] { "date_historic_4a-BCADhint", "summer of 190 BC", "BC0190-SU" });
	}

	@Test
	public void testdate_historic_5a_BCADhint() {
		testSingleCase("the 2nd century BC", //
				new String[] { "date_historic_5a-BCADhint", "the 2nd century BC", "BC01" });
	}

	@Test
	public void testdate_historic_5b_BCADhint() {
		testSingleCase("beginning of the 2nd century BC", //
				new String[] { "date_historic_5b-BCADhint", "beginning of the 2nd century BC", "BC01" });
	}

	@Test
	public void testdate_historic_5ca_BCADhint() {
		// find "2nd century BC"
		testSingleCase("2nd or 3rd century BC", //
				new String[] { "date_historic_5c-BCADhint", "2nd", "BC01" }, //
				new String[] { "date_historic_5a-BCADhint", "3rd century BC", "BC02" });
	}

	@Test
	public void testdate_historic_5ad_BCADhint() {
		// find "beginning 2nd century BC"
		testSingleCase("beginning of the 2nd or 3rd century BC", //
				new String[] { "date_historic_5d-BCADhint", "beginning of the 2nd", "BC01" }, //
				new String[] { "date_historic_5a-BCADhint", "3rd century BC", "BC02" });
	}

	@Test
	public void testdate_historic_6a_BCADhint() {
		testSingleCase("1990s BC", //
				new String[] { "date_historic_6a-BCADhint", "1990s BC", "BC199" });
	}

	@Test
	public void testdate_historic_6b_BCADhint() {
		testSingleCase("190s BC", //
				new String[] { "date_historic_6b-BCADhint", "190s BC", "BC019" });
	}

	@Test
	public void testdate_historic_6c_BCADhint() {
		testSingleCase("90s BC", //
				new String[] { "date_historic_6c-BCADhint", "90s BC", "BC009" });
	}

	@Test
	public void testdate_historic_7ab() {
		// 3-digit year
		testSingleCase("in 190", new String[] { "date_historic_7ab", "190", "0190" });
	}

	@Test
	public void testdate_historic_7c() {
		// (2- to 4-digit year
		testSingleCase("\n190\n", new String[] { "date_historic_7c", "190", "0190" });
	}

	@Test
	public void testdate_historic_7d() {
		// 2-digit year
		testSingleCase("year of 90", //
				new String[] { "date_historic_7d", "year of 90", "0090" });
	}

	@Test
	public void testdate_historic_7e() {
		// 3-digit year
		testSingleCase("year of 190", //
				new String[] { "date_historic_7e", "year of 190", "0190" });
	}

	@Test
	public void testdate_historic_8ab() {
		// 2-digit year
		testSingleCase("in 90,", new String[] { "date_historic_8ab", "90", "0090" });
		testSingleCase("in 90", new String[] { "date_historic_8ab", "90", "0090" });
	}

	// FIXME: add POS tags for unit test
	@Ignore("Needs POS")
	@Test
	public void testdate_historic_0ab_negative() {
		// 2- to 4-digit year
		testSingleCase("in 90 cases");
		testSingleCase("in 90 nice cases");
		testSingleCase("in 90 nice law cases");
	}

	@Test
	public void testdate_historic_0d_negative() {
		// 2- to 4-digit year
		testSingleCase("in 90 percent"); // EMPTY!
	}
}
