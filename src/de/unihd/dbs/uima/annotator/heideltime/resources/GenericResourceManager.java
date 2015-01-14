package de.unihd.dbs.uima.annotator.heideltime.resources;

/**
 * 
 * Abstract class for all Resource Managers to inherit from. Contains basic
 * functionality such as file system access and some private members.
 *
 */
public abstract class GenericResourceManager {
	// language for the utilized resources
	protected String LANGUAGE;
	// kind of resource -- e.g. repattern, normalization, rules
	protected String resourceType;
	// local package for logging output
	protected Class<?> component;
	
	/**
	 * Instantiates the Resource Manager with a resource type
	 * @param resourceType kind of resource to represent
	 */
	protected GenericResourceManager(String resourceType, String language) {
		this.resourceType = resourceType;
		this.LANGUAGE = language;
		this.component = this.getClass();
	}
}
