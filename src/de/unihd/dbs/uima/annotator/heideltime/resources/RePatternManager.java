package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.TreeMap;
/**
 * 
 * This class fills the role of a manager of all the RePattern resources.
 * It reads the data from a file system and fills up a bunch of HashMaps
 * with their information.
 *
 */
public class RePatternManager extends GenericResourceManager {
	private static RePatternManager INSTANCE = null;
	
	// STORE PATTERNS AND NORMALIZATIONS
	private TreeMap<String, String> hmAllRePattern;

	/**
	 * Constructor calls the parent constructor that sets language/resource
	 * parameters and collects resource repatterns.
	 * @param language
	 */
	private RePatternManager(String language) {
		// calls the Generic constructor with repattern parameter
		super("repattern");
		// initialize the member map of all repatterns
		hmAllRePattern = new TreeMap<String, String>();

		//////////////////////////////////////////////////////
		// READ PATTERN RESOURCES FROM FILES AND STORE THEM //
		//////////////////////////////////////////////////////
		HashMap<String, String> hmResourcesRePattern = readResourcesFromDirectory();
		for (String which : hmResourcesRePattern.keySet()) {
			hmAllRePattern.put(which, "");
		}
		readRePatternResources(hmResourcesRePattern);
	}

	/**
	 * singleton producer.
	 * @return singleton instance of RePatternManager
	 */
	public static RePatternManager getInstance() {
		if(RePatternManager.INSTANCE == null)
			RePatternManager.INSTANCE = new RePatternManager(LANGUAGE);
		
		return RePatternManager.INSTANCE;
	}
	
	
	/**
	 * READ THE REPATTERN FROM THE FILES. The files have to be defined in the HashMap hmResourcesRePattern.
	 * @param hmResourcesRePattern RePattern resources to be interpreted
	 */
	private void readRePatternResources(HashMap<String, String> hmResourcesRePattern) {
		
		//////////////////////////////////////
		// READ REGULAR EXPRESSION PATTERNS //
		//////////////////////////////////////
		try {
			for (String resource : hmResourcesRePattern.keySet()) {
				System.err.println("["+component+"] Adding pattern resource: "+resource);
				// create a buffered reader for every repattern resource file
				BufferedReader in = new BufferedReader(new InputStreamReader 
						(this.getClass().getClassLoader().getResourceAsStream(hmResourcesRePattern.get(resource)),"UTF-8"));
				for (String line; (line=in.readLine()) != null; ) {
					if (!line.startsWith("//")) {
						boolean correctLine = false;
						if (!(line.equals(""))) {
							correctLine = true;
							for (String which : hmAllRePattern.keySet()) {
								if (resource.equals(which)) {
									String devPattern = hmAllRePattern.get(which);
									devPattern = devPattern + "|" + line;
									hmAllRePattern.put(which, devPattern);
								}
							}
						}
						if ((correctLine == false) && (!(line.matches("")))) {
							System.err.println("["+component+"] Cannot read one of the lines of pattern resource "+resource);
							System.err.println("["+component+"] Line: "+line);
						}
					}
				}
			}
			////////////////////////////
			// FINALIZE THE REPATTERN //
			////////////////////////////
			for (String which : hmAllRePattern.keySet()) {
				finalizeRePattern(which, hmAllRePattern.get(which));
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		rePattern = "(" + rePattern + ")";
		// add rePattern to hmAllRePattern
		hmAllRePattern.put(name, rePattern.replaceAll("\\\\", "\\\\\\\\"));
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
