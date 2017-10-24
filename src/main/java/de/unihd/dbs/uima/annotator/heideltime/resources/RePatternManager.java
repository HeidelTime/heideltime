package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
/**
 * 
 * This class fills the role of a manager of all the RePattern resources.
 * It reads the data from a file system and fills up a bunch of HashMaps
 * with their information.
 * @author jannik stroetgen
 *
 */
public class RePatternManager extends GenericResourceManager {
	protected static HashMap<String, RePatternManager> instances = new HashMap<String, RePatternManager>();
	
	// STORE PATTERNS AND NORMALIZATIONS
	private TreeMap<String, String> hmAllRePattern;

	/**
	 * Constructor calls the parent constructor that sets language/resource
	 * parameters and collects resource repatterns.
	 * @param language
	 * @param load_temponym_resources
	 */
	private RePatternManager(String language, Boolean load_temponym_resources) {
		// calls the Generic constructor with repattern parameter
		super("repattern", language);
		// initialize the member map of all repatterns
		hmAllRePattern = new TreeMap<String, String>();

		//////////////////////////////////////////////////////
		// READ PATTERN RESOURCES FROM FILES AND STORE THEM //
		//////////////////////////////////////////////////////
		ResourceScanner rs = ResourceScanner.getInstance();
		ResourceMap hmResourcesRePattern = rs.getRepatterns(language);
		for (String which : hmResourcesRePattern.keySet()) {
			hmAllRePattern.put(which, "");
		}
		readRePatternResources(hmResourcesRePattern, load_temponym_resources);
	}

	/**
	 * singleton producer.
	 * @return singleton instance of RePatternManager
	 */
	public static RePatternManager getInstance(Language language, Boolean load_temponym_resources) {
		if(!instances.containsKey(language.getName())) {
			RePatternManager nm = new RePatternManager(language.getResourceFolder(), load_temponym_resources);
			instances.put(language.getName(), nm);
		}
		
		return instances.get(language.getName());
	}
	
	
	/**
	 * READ THE REPATTERN FROM THE FILES. The files have to be defined in the HashMap hmResourcesRePattern.
	 * @param hmResourcesRePattern RePattern resources to be interpreted
	 * @param load_temponym_resources whether temponym resources are to be read
	 */
	private void readRePatternResources(ResourceMap hmResourcesRePattern, Boolean load_temponym_resources) {

		//////////////////////////////////////
		// READ REGULAR EXPRESSION PATTERNS //
		//////////////////////////////////////
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			for (String resource : hmResourcesRePattern.keySet()) {
				// read pattern resources with "Temponym" only if temponym tagging is selected
				if ( (!(resource.contains("Temponym"))) ||
						((load_temponym_resources) && (resource.contains("Temponym")))){
					Logger.printDetail(component, "Adding pattern resource: "+resource);
					// create a buffered reader for every repattern resource file
					is = hmResourcesRePattern.getInputStream(resource);
					isr = new InputStreamReader(is, "UTF-8");
					br = new BufferedReader(isr);
					LinkedList<String> patterns = new LinkedList<String>();
					for (String line; (line = br.readLine()) != null; ) {
						// disregard comments
						if (!line.startsWith("//") && !line.equals("")) {
							patterns.add(replaceSpaces(line));
						}
					}
				
				
					
					// sort the repatterns by length in ascending order
					Collections.sort(patterns, new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							String o1effective = o1.replaceAll("\\[[^\\]]*\\]", "X")
									.replaceAll("\\?", "")
									.replaceAll("\\\\.(?:\\{([^\\}])+\\})?", "X$1");
							String o2effective = o2.replaceAll("\\[[^\\]]*\\]", "X")
									.replaceAll("\\?", "")
									.replaceAll("\\\\.(?:\\{([^\\}])+\\})?", "X$1");
							
							if(o1effective.length() < o2effective.length())
								return 1;
							else if(o1effective.length() > o2effective.length())
								return -1;
							else
								return 0;
						}
					});
										
					StringBuilder sb = new StringBuilder();
					String devPattern = "";
					for(String pat : patterns) {
						sb.append("|");
						sb.append(pat);
					}
					devPattern = sb.toString();
					hmAllRePattern.put(resource, devPattern);
				}
				else {
					Logger.printDetail(component, "No Temponym Tagging selected. Skipping pattern resource: "+resource);
				}
			}
			////////////////////////////
			// FINALIZE THE REPATTERN //
			////////////////////////////
			for (String which : hmAllRePattern.keySet()) {
				if ( (!(which.contains("Temponym"))) ||
						((load_temponym_resources) && (which.contains("Temponym")))){
					finalizeRePattern(which, hmAllRePattern.get(which));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null) {
					br.close();
				}
				if(isr != null) {
					isr.close();
				}
				if(is != null) {
					is.close();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Pattern containing regular expression is finalized, i.e., created correctly and added to hmAllRePattern.
	 * @param name key name
	 * @param rePattern repattern value
	 */
	private void finalizeRePattern(String name, String rePattern) {
		// create correct regular expression
		rePattern = rePattern.replaceFirst("\\|", "");
		/* this was added to reduce the danger of getting unusable groups from user-made repattern
		 * files with group-producing parentheses (i.e. "(foo|bar)" while matching against the documents. */
		rePattern = rePattern.replaceAll("\\(([^\\?])", "(?:$1");
		rePattern = "(" + rePattern + ")";
		rePattern = rePattern.replaceAll("\\\\", "\\\\\\\\");
		// add rePattern to hmAllRePattern
		hmAllRePattern.put(name, rePattern);
	}
	
	/**
	 * proxy method to access the hmAllRePattern member
	 * @param key key to check for
	 * @return whether the map contains the key
	 */
	public Boolean containsKey(String key) {
		return hmAllRePattern.containsKey(key);
	}

	/**
	 * proxy method to access the hmAllRePattern member
	 * @param key Key to retrieve data from
	 * @return String from the map
	 */
	public String get(String key) {
		return hmAllRePattern.get(key);
	}

}
