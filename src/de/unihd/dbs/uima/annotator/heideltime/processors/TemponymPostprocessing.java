package de.unihd.dbs.uima.annotator.heideltime.processors;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;

/**
 * This class removes TIMEX3 annotations for temponyms and adds TIMEX3INTERVAL annotations containing (earliest|latest)(Begin|End) information.
 * 
 * @author jannik stroetgen
 */
public class TemponymPostprocessing {
	private static final Logger LOG = LoggerFactory.getLogger(TemponymPostprocessing.class);

	private static final Pattern p = Pattern.compile("\\[(.*?), (.*?), (.*?), (.*?)\\]");

	public static void handleIntervals(JCas jcas) {
		HashSet<Timex3> timexes = new HashSet<>();

		Matcher mr = p.matcher("");
		// iterate over all TEMPONYMS
		AnnotationIndex<Timex3> timex3s = jcas.getAnnotationIndex(Timex3.type);
		for (Timex3 t : timex3s) {
			if (!t.getTimexType().equals("TEMPONYM"))
				continue;
			LOG.debug("TEMPONYM: {}", t.getCoveredText());
			// create a timex3interval for each temponym
			Timex3Interval ti = new Timex3Interval(jcas);

			ti.setBegin(t.getBegin());
			ti.setEnd(t.getEnd());
			ti.setTimexType(t.getTimexType());
			ti.setAllTokIds(t.getAllTokIds());
			ti.setTimexFreq(t.getTimexFreq());
			ti.setTimexMod(t.getTimexMod());
			ti.setTimexQuant(t.getTimexQuant());
			// set a new id
			String id = t.getTimexId();
			int newId = Integer.parseInt(id.replace("t", ""));
			newId += 100000;
			ti.setTimexId("t" + newId);

			// get the (earliest|last)(begin|end) information
			for (mr.reset(t.getTimexValue()); mr.find();) {
				ti.setTimexValueEB(mr.group(1));
				ti.setTimexValueLB(mr.group(2));
				ti.setTimexValueEE(mr.group(3));
				ti.setTimexValueLE(mr.group(4));
			}
			// System.err.println("temponym: " + t.getTimexValue());
			if (ti.getTimexValueEB().equals(ti.getTimexValueLB()) && //
					ti.getTimexValueLB().equals(ti.getTimexValueEE()) && //
					ti.getTimexValueEE().equals(ti.getTimexValueLE())) {
				ti.setTimexValue(ti.getTimexValueEB());
				t.setTimexValue(ti.getTimexValueEB());
			} else { // what's the best single value for an interval!?
				t.setEmptyValue(t.getTimexValue());
				ti.setTimexValue(ti.getTimexValueLE());
				t.setTimexValue(ti.getTimexValueLE());
			}
			ti.setFoundByRule(t.getFoundByRule());
			ti.addToIndexes();
			timexes.add(t);
		}
		// shall the standard timexes really be removed?
		for (Timex3 t : timexes)
			t.removeFromIndexes();
	}
}
