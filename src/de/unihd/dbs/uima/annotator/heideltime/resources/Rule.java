package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.util.regex.Pattern;

/**
 * Class representing a single rule.
 * 
 * @author Erich Schubert
 */
public class Rule implements Comparable<Rule> {
	/**
	 * Constructor with mandatory parameters.
	 *
	 * @param name Name
	 * @param pattern Pattern
	 * @param normalization Normalization
	 */
	public Rule(String name, Pattern pattern, String normalization) {
		this.name = name;
		this.pattern = pattern;
		this.normalization = normalization;
	}

	/** Rule name */
	String name;
	
	/** Extraction pattern */
	Pattern pattern;
	
	/** Normalization */
	String normalization;
	
	/** Offset pattern*/
	String offset;
	
	/** Quant */
	String quant;
	
	/** Freq */
	String freq;
	
	/** Mod */
	String mod;
	
	/** Position constraint */
	String posConstratint;
	
	/** Empty value */
	String emptyValue;
	
	/** Fast check */
	Pattern fastCheck;

	public String getName() {
		return name;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getNormalization() {
		return normalization;
	}

	public String getOffset() {
		return offset;
	}

	public String getQuant() {
		return quant;
	}

	public String getFreq() {
		return freq;
	}

	public String getMod() {
		return mod;
	}

	public String getPosConstratint() {
		return posConstratint;
	}

	public String getEmptyValue() {
		return emptyValue;
	}

	public Pattern getFastCheck() {
		return fastCheck;
	}

	@Override
	public int compareTo(Rule other) {
		return name.compareTo(other.name);
	}
}
