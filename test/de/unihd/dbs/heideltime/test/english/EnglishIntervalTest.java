package de.unihd.dbs.heideltime.test.english;

import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.unihd.dbs.heideltime.standalone.components.impl.StandaloneConfigContext;
import de.unihd.dbs.uima.annotator.intervaltagger.IntervalTagger;

public class EnglishIntervalTest extends AbstractHeideltimeTest {
	protected IntervalTagger intervaltagger;

	@Override
	public void init() {
		super.init();
		try {
			intervaltagger = new IntervalTagger();
			StandaloneConfigContext aContext = new StandaloneConfigContext();

			// construct a context for the uima engine
			aContext.setConfigParameterValue(IntervalTagger.PARAM_LANGUAGE, "english");
			aContext.setConfigParameterValue(IntervalTagger.PARAM_INTERVALS, Boolean.TRUE);
			aContext.setConfigParameterValue(IntervalTagger.PARAM_INTERVAL_CANDIDATES, Boolean.FALSE);

			intervaltagger.initialize(aContext);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testinterval_01() {
		testSingleCase("from 1999 to 2012", //
				new String[] { "interval_01", "from 1999 to 2012" });
	}

	@Test
	public void testinterval_02() {
		testSingleCase("between March and May", //
				new String[] { "interval_02", "between March and May" });
	}

	@Test
	public void testinterval_03() {
		testSingleCase("20.3.2003 - 1.5.2003", //
				new String[] { "interval_03", "20.3.2003 - 1.5.2003" });
	}

	@Test
	public void testinterval_04() {
		testSingleCase("20.3.2003 to 1.5.2003", //
				new String[] { "interval_04", "20.3.2003 to 1.5.2003" });
	}

	@Test
	public void testinterval_05() {
		testSingleCase("on 20.3.2003 the war began and it lasted until 1.5.2003", //
				new String[] { "interval_05", "on 20.3.2003 the war began and it lasted until 1.5.2003" });
	}

	@Test
	public void testinterval_06() {
		testSingleCase("for December after leaving in February", //
				new String[] { "interval_06", "for December after leaving in February" });
	}

	@Test
	public void testinterval_07() {
		testSingleCase("began on March 20 in 2003 and ended on May 1", //
				new String[] { "interval_07", "began on March 20 in 2003 and ended on May 1" });
	}

	@Test
	public void testinterval_08() {
		testSingleCase("in 1999/2000", //
				new String[] { "interval_08", "in 1999/2000" });
	}

	@Test
	public void testinterval_09() {
		testSingleCase("War ended in May, after fighting from March on", //
				new String[] { "interval_09", "War ended in May, after fighting from March on" });
	}

	@Test
	public void testinterval_10() {
		testSingleCase("March, April and May", //
				new String[] { "interval_10", "March, April and May" });
	}

	@Test
	public void testinterval_11() {
		testSingleCase("Monday, Thuesday, Wednesday and Thursday", //
				new String[] { "interval_11", "Monday, Thuesday, Wednesday and Thursday" });
	}

	protected JCas analyze(String fragment) {
		try {
			JCas jcas = tokenize(fragment);
			heideltime.process(jcas);
			intervaltagger.process(jcas);
			return jcas;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
