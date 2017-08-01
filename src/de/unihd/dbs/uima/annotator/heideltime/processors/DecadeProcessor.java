package de.unihd.dbs.uima.annotator.heideltime.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.types.heideltime.Timex3;

public class DecadeProcessor extends GenericProcessor {
	/**
	 * Constructor just calls the parent constructor here.
	 */
	public DecadeProcessor() {
		super();
	}

	/**
	 * not needed here
	 */
	public void initialize(UimaContext aContext) {
		return;
	}

	/**
	 * all the functionality was put into evaluateCalculationFunctions().
	 */
	public void process(JCas jcas) {
		evaluateFunctions(jcas);
	}

	/**
	 * This function replaces function calls from the resource files with their TIMEX value.
	 * 
	 * @author Hans-Peter Pfeiffer
	 * @param jcas
	 */
	public void evaluateFunctions(JCas jcas) {
		// build up a list with all found TIMEX expressions
		List<Timex3> linearDates = new ArrayList<Timex3>();
		Iterable<Timex3> timexes = jcas.getAnnotationIndex(Timex3.type);

		// Create List of all Timexes of types "date" and "time"
		for (Timex3 timex : timexes)
			if (timex.getTimexType().equals("DATE"))
				linearDates.add(timex);

		//////////////////////////////////////////////
		// go through list of Date and Time timexes //
		//////////////////////////////////////////////
		// compile regex pattern for validating commands/arguments
		Matcher cmd_p = Pattern.compile("(\\w\\w\\w\\w)-(\\w\\w)-(\\w\\w)\\s+decadeCalc\\((\\d+)\\)").matcher("");

		for (int i = 0; i < linearDates.size(); i++) {
			Timex3 t_i = linearDates.get(i);
			String value_i = t_i.getTimexValue();
			Matcher cmd_m = cmd_p.reset(value_i);
			String valueNew = value_i;

			if (cmd_m.matches()) {
				String year = cmd_m.group(1);
				String argument = cmd_m.group(4);
				valueNew = year.substring(0, Math.min(2, year.length())) + argument.substring(0, 1);
			}

			t_i.removeFromIndexes();
			t_i.setTimexValue(valueNew);
			t_i.addToIndexes();
			linearDates.set(i, t_i);
		}
	}
}
