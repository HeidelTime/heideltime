package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;
/**
 * 
 * Abstract class for all Resource Managers to inherit from. Contains basic
 * functionality such as file system access and some private members.
 *
 */
public abstract class GenericResourceManager {
	// language for the utilized resources
	public static String LANGUAGE;
	// kind of resource -- e.g. repattern, normalization, rules
	protected String resourceType;
	// local package for logging output
	protected Class component;
	
	/**
	 * Instantiates the Resource Manager with a resource type
	 * @param resourceType kind of resource to represent
	 */
	protected GenericResourceManager(String resourceType) {
		this.resourceType = resourceType;
		this.component = this.getClass();
	}

	/**
	 * Reads resource files of the type resourceType from the "used_resources.txt" file and returns a HashMap
	 * containing information to access these resources.
	 * @return HashMap containing filename/path tuples
	 */
	protected HashMap<String, String> readResourcesFromDirectory() {

		HashMap<String, String> hmResources = new HashMap<String, String>();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("used_resources.txt")));
		try {
			for (String line; (line=br.readLine()) != null; ) {
				Pattern paResource = Pattern.compile("\\./"+LANGUAGE+"/"+resourceType+"/resources_"+resourceType+"_"+"(.*?)\\.txt");
				for (MatchResult ro : Toolbox.findMatches(paResource, line)){
					String foundResource  = ro.group(1);
					String pathToResource = LANGUAGE+"/"+resourceType+"/resources_"+resourceType+"_"+foundResource+".txt";
					hmResources.put(foundResource, pathToResource);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			Logger.printError(component, "Failed to read a resource from used_resources.txt.");
			System.exit(-1);
		}
		return hmResources;
	}

}
