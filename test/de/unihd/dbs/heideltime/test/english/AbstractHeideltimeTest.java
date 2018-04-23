package de.unihd.dbs.heideltime.test.english;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.components.impl.JCasFactoryImpl;
import de.unihd.dbs.heideltime.standalone.components.impl.UimaContextImpl;
import de.unihd.dbs.uima.annotator.heideltime.DocumentType;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * Abstract base class for unit testing Heideltime annotations.
 * 
 * @author Erich Schubert
 */
public class AbstractHeideltimeTest {

	protected JCasFactoryImpl jcasFactory;
	protected HeidelTime heideltime;
	private boolean debugTokenization = false;
	static final Pattern LINEWRAP = Pattern.compile("\\s*[\\n\\r]+\\s*");
	static final Pattern WORDS = Pattern.compile("(?U)([^\\s\\w]*)([\\w/]+(?:\\.\\d+)?)([^\\s\\w]*)");

	@Before
	public void init() {
		try {
			if (!Config.isInitialized())
				HeidelTimeStandalone.readConfigFile("test/test.props");
			TypeSystemDescription[] descriptions = new TypeSystemDescription[] {
					UIMAFramework.getXMLParser().parseTypeSystemDescription(new XMLInputSource(this.getClass().getClassLoader().getResource(Config.get(Config.TYPESYSTEMHOME)))) };
			jcasFactory = new JCasFactoryImpl(descriptions);
			heideltime = new HeidelTime();
			heideltime.initialize(new UimaContextImpl(Language.ENGLISH, DocumentType.COLLOQUIAL, false));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public AbstractHeideltimeTest() {
		super();
	}

	protected JCas tokenize(String fragment) {
		JCas jcas = null;
		try {
			jcas = jcasFactory.createJCas();
			jcas.setDocumentText(fragment);
		} catch (Exception e) {
			fail("Cas object could not be generated");
		}
		int last = 0;
		for (Matcher sm = LINEWRAP.matcher(fragment); sm.find();) {
			int ss = sm.start(), se = sm.end();
			if (last < ss)
				tokenizeSentence(fragment, jcas, last, ss);
			last = se;
		}
		if (last < fragment.length())
			tokenizeSentence(fragment, jcas, last, fragment.length());
		return jcas;
	}

	private void tokenizeSentence(String fragment, JCas jcas, int ss, int se) {
		// A single sentence:
		Sentence s = new Sentence(jcas);
		s.setBegin(ss);
		s.setEnd(se);
		s.addToIndexes();
		// Hard-coded tokenization:
		for (Matcher m = WORDS.matcher(fragment).region(ss, se); m.find();) {
			for (int i = 1; i <= 3; i++) {
				int start = m.start(i), end = m.end(i);
				if (start == end)
					continue;
				Token t = new Token(jcas);
				t.setBegin(start);
				t.setEnd(end);
				t.setPos("");
				t.addToIndexes();
				if (debugTokenization)
					System.out.print(fragment.substring(start, end) + "<=>");
			}
		}
		if (debugTokenization)
			System.out.println();
	}

	protected JCas analyze(String fragment, String dctv) {
		try {
			JCas jcas = tokenize(fragment);
			if (dctv != null) {
				Dct dct = new Dct(jcas);
				dct.setValue(dctv);
				dct.addToIndexes();
			}
			heideltime.process(jcas);
			// intervaltagger.process(jcas);
			return jcas;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void testSingleCase(String fragment, String[]... expectf) {
		testSingleCase(fragment, null, expectf);
	}

	protected void testSingleCase(String fragment, String dctv, String[]... expectf) {
		JCas jcas = analyze(fragment, dctv);
		AnnotationIndex<Timex3> times = jcas.getAnnotationIndex(Timex3.type);
		int cnt = 0;
		for (Timex3 timex3 : times) {
			++cnt;
			String mrule = timex3.getFoundByRule().replaceAll("-(relative|explicit)", "");
			String mstr = fragment.substring(timex3.getBegin(), timex3.getEnd());
			String mres = timex3.getTimexValue();
			boolean samerule = false, samestring = false, sameres = false;
			for (String[] expect : expectf) {
				samerule |= expect[0].equals(mrule);
				samestring |= (expect.length > 1 ? expect[1] : "").equals(mstr);
				sameres |= (expect.length > 2) ? expect[2].equals(mres) : false;
			}
			if (!samerule || !samestring || !sameres) {
				System.err.println("Received: " + timex3);
				for (String[] expect : expectf) {
					System.err.println("Expected: " + String.join("\t", expect));
				}
			}
			assertTrue("Fragment >>" + fragment + "<< matched in a different part: >>" + mstr + "<< (rule " + mrule + ")", samestring);
			assertTrue("Fragment >>" + fragment + "<< returned a different result: >>" + mres + "<< (rule " + mrule + ")", sameres);
			assertTrue("Fragment >>" + fragment + "<< matched by different rule: " + mrule, samerule);
		}
		assertEquals("Number of results do not match.", expectf.length, cnt);
	}
}