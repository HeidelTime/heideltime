package de.unihd.dbs.uima.annotator.heideltime.resources;

import de.unihd.dbs.uima.annotator.heideltime.HeidelTimeException;

public enum Language {
	
	/*
	 * set the languages here
	 */
	ENGLISH		("english", "english", "english", "-e"),
	GERMAN		("german", "german", "german", ""),
	DUTCH		("dutch", "dutch", "dutch", ""),
	ENGLISHCOLL	("englishcoll", "englishcoll", "english", "-e"),
	ENGLISHSCI	("englishsci", "englishsci", "english", "-e"),
	ITALIAN		("italian", "italian", "italian", "-i"),
	FRENCH		("french", "french", "french", "-f"),
	; // ends the enum element list
	
	private String languageName;
	private String treeTaggerSwitch;
	private String treeTaggerLangName;
	private String resourceFolder;
	
	Language(String languageName, String resourceFolder, String treeTaggerLangName, String treeTaggerSwitch) {
		this.languageName = languageName;
		this.resourceFolder = resourceFolder;
		this.treeTaggerLangName = treeTaggerLangName;
		this.treeTaggerSwitch = treeTaggerSwitch;
	}
	
	public final static Language getLanguageFromString(String name) throws HeidelTimeException {
		for(Language l : Language.values()) {
			if(name.equals(l.getName())) {
				return l;
			}
		}
		throw new HeidelTimeException("Language not found in Language ENUM. Update?");
	}
	
	public final String getName() {
		return this.languageName;
	}
	
	public final String getTreeTaggerSwitch() {
		return this.treeTaggerSwitch;
	}
	
	public final String getTreeTaggerLangName() {
		return this.treeTaggerLangName;
	}
	
	public final String getResourceFolder() {
		return this.resourceFolder;
	}
}