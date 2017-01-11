package de.unihd.dbs.uima.annotator.heideltime.resources;

/**
 * 
 * Abstract class for all Resource Managers to inherit from. Contains basic
 * functionality such as file system access and some private members.
 *
 */
public abstract class GenericResourceManager {
	// language for the utilized resources
	protected final String LANGUAGE;
	// kind of resource -- e.g. repattern, normalization, rules
	protected String resourceType;
	
	/**
	 * Instantiates the Resource Manager with a resource type
	 * @param resourceType kind of resource to represent
	 */
	protected GenericResourceManager(String resourceType, String language) {
		this.resourceType = resourceType;
		this.LANGUAGE = language;
	}
	
	protected String replaceSpaces(String inText) {
		return inText.replaceAll(" ", "[\\\\u2000-\\\\u200A \\\\u202F\\\\u205F\\\\u3000\\\\u00A0\\\\u1680\\\\u180E]+");
	}
}
