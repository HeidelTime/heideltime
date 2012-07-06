/**
 * A class that contains the detailed information on all of the command line interface
 * switches entertained by HeidelTimeStandalone.
 */
package de.unihd.dbs.heideltime.standalone;

import java.util.Date;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

/**
 * @author Julian Zell
 *
 */
public enum CLISwitch {
	VERBOSITY	("Verbosity", "-v"),
	VERBOSITY2	("More Verbosity", "-vv"),
	ENCODING	("Encoding", "-e", "UTF-8"),
	OUTPUTTYPE	("Output Type", "-o", OutputType.TIMEML),
	LANGUAGE	("Language", "-l", Language.ENGLISH.toString()),
	DOCTYPE		("Document Type", "-t", DocumentType.NARRATIVES),
	DCT			("Document Creation Time", "-dct", new Date()),
	CONFIGFILE	("Configuration Path", "-c", "config.props"), 
	LOCALE		("Locale", "-locale", null),
	;
	
	private boolean hasFollowingValue = false;
	private boolean isActive = false;
	private String name;
	private String switchString;
	private Object value = null;
	private Object defaultValue;
	
	/**
	 * Constructor for switches that have a default value
	 * @param name
	 * @param switchString
	 * @param defaultValue
	 */
	CLISwitch(String name, String switchString, Object defaultValue) {
		this.hasFollowingValue = true;
		this.name = name;
		this.switchString = switchString;
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Constructor for switches that don't have a default value
	 * @param name
	 * @param switchString
	 */
	CLISwitch(String name, String switchString) {
		this.hasFollowingValue = false;
		this.name = name;
		this.switchString = switchString;
	}
	
	public static CLISwitch getEnumFromSwitch(String cliSwitch) {
		for(CLISwitch s : CLISwitch.values()) {
			if(s.getSwitchString().equals(cliSwitch)) {
				return s;
			}
		}
		return null;
	}
	
	/*
	 * getters/setters of private attributes
	 */
	
	public void setValue(String val) {
		value = val;
		isActive = true;
	}
	
	/**
	 * if this switch is supposed to have a value after it, spit out the saved value 
	 * or the default value if the value is unset. if it's not supposed to have a value,
	 * return null
	 * @return	String containing a value for the switch
	 */
	public Object getValue() {
		if(hasFollowingValue) {
			if(value != null)
				return value;
			else
				return defaultValue;
		} else {
			return null;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getSwitchString() {
		return switchString;
	}
	
	public boolean getHasFollowingValue() {
		return hasFollowingValue;
	}
	
	public boolean getIsActive() {
		return isActive;
	}
}
