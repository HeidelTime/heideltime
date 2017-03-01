package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.utilities.RegexpOptimizer;
import de.unihd.dbs.uima.annotator.heideltime.utilities.RegexpOptimizer.OptimizerException;

/**
 * 
 * This class fills the role of a manager of all the RePattern resources. It reads the data from a file system and fills up a bunch of HashMaps with their information.
 * 
 * @author jannik stroetgen
 *
 */
public class RePatternManager extends GenericResourceManager {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(RePatternManager.class);

	protected static HashMap<String, RePatternManager> instances = new HashMap<String, RePatternManager>();

	// STORE PATTERNS AND NORMALIZATIONS
	private TreeMap<String, String> hmAllRePattern;

	private HashMap<String, Pattern> compiled;

	/**
	 * Constructor calls the parent constructor that sets language/resource parameters and collects resource repatterns.
	 * 
	 * @param language
	 * @param load_temponym_resources
	 */
	private RePatternManager(String language, Boolean load_temponym_resources) {
		// calls the Generic constructor with repattern parameter
		super("repattern", language);
		// initialize the member map of all repatterns
		hmAllRePattern = new TreeMap<String, String>();
		compiled = new HashMap<String, Pattern>();

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
	 * 
	 * @return singleton instance of RePatternManager
	 */
	public static RePatternManager getInstance(Language language, Boolean load_temponym_resources) {
		if (!instances.containsKey(language.getName())) {
			RePatternManager nm = new RePatternManager(language.getResourceFolder(), load_temponym_resources);
			instances.put(language.getName(), nm);
		}

		return instances.get(language.getName());
	}

	/**
	 * READ THE REPATTERN FROM THE FILES. The files have to be defined in the HashMap hmResourcesRePattern.
	 * 
	 * @param hmResourcesRePattern
	 *                RePattern resources to be interpreted
	 * @param load_temponym_resources
	 *                whether temponym resources are to be read
	 */
	private void readRePatternResources(ResourceMap hmResourcesRePattern, boolean load_temponym_resources) {
		//////////////////////////////////////
		// READ REGULAR EXPRESSION PATTERNS //
		//////////////////////////////////////
		for (String resource : hmResourcesRePattern.keySet()) {
			// read pattern resources with "Temponym" only if temponym tagging is selected
			if (!load_temponym_resources && resource.contains("Temponym")) {
				LOG.trace("No Temponym tagging selected. Skipping pattern resource: {}", resource);
				continue;
			}
			LOG.debug("Adding pattern resource: {}", resource);
			// create a buffered reader for every repattern resource file
			try (InputStream is = hmResourcesRePattern.getInputStream(resource); //
					InputStreamReader isr = new InputStreamReader(is, "UTF-8"); //
					BufferedReader br = new BufferedReader(isr)) {
				List<String> patterns = new ArrayList<String>();
				for (String line; (line = br.readLine()) != null;)
					// disregard comments
					if (!line.startsWith("//") && !line.equals(""))
						patterns.add(line);
				patterns = optimizePatterns(resource, patterns);
				hmAllRePattern.put(resource, String.join("|", patterns));
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		////////////////////////////
		// FINALIZE THE REPATTERN //
		////////////////////////////
		for (String which : hmAllRePattern.keySet())
			finalizeRePattern(which, hmAllRePattern.get(which));
	}

	/**
	 * Optimize a set of patterns into a more efficient regexp, because of Java.
	 *
	 * @author Erich Schubert
	 * @param inpatterns
	 *                Input patterns
	 * @return Optimized regular expression set
	 */
	private List<String> optimizePatterns(String name, List<String> inpatterns) {
		// Since we already have some rules written as res,
		// We try to expand some basic constructs first.
		try {
			ArrayList<String> expanded = new ArrayList<>();
			for (String s : inpatterns) {
				try {
					RegexpOptimizer.expandPatterns(s, x -> expanded.add(x.toString()));
				} catch (OptimizerException e) {
					// More specific message than below.
					LOG.warn("Pattern '{}' for '{}' contains a too complex regexp construct, cannot optimize: {}", s, name, e.getMessage());
					return inpatterns;
				}
			}
			if (expanded.isEmpty()) {
				LOG.info("Regexp pattern {} is empty.", name);
				return Collections.emptyList();
			}
			String pattern = RegexpOptimizer.combinePatterns(expanded);
			LOG.debug("Combined {} into: {}", name, pattern);
			return Arrays.asList(pattern);
		} catch (OptimizerException e) {
			LOG.warn("Pattern '{}' contains a too complex regexp construct, cannot optimize: {}", name, e.getMessage());
			return inpatterns;
		}
	}

	/**
	 * Pattern containing regular expression is finalized, i.e., created correctly and added to hmAllRePattern.
	 * 
	 * @param name
	 *                key name
	 * @param rePattern
	 *                repattern value
	 */
	private void finalizeRePattern(String name, String rePattern) {
		String orig = rePattern;
		// create correct regular expression
		// rePattern = rePattern.replaceFirst("\\|", "");
		/*
		 * this was added to reduce the danger of getting unusable groups from user-made repattern files with group-producing parentheses (i.e. "(foo|bar)" while matching against the
		 * documents.
		 */
		// rePattern = rePattern.replaceAll("\\(([^?])", "(?:$1");
		rePattern = "(" + rePattern + ")";
		// rePattern = rePattern.replaceAll("\\\\", "\\\\\\\\");
		// add rePattern to hmAllRePattern
		hmAllRePattern.put(name, rePattern);
		try {
			Pattern c = Pattern.compile(rePattern);
			int groupcount = c.matcher("").groupCount();
			if (groupcount != 1)
				LOG.error("rePattern {} contains unexpected groups: {}\nPattern: {}", name, groupcount - 1, orig);
			compiled.put(name, c);
		} catch (PatternSyntaxException e) {
			LOG.error("Failed to compile RePattern {}:\n{}\nbefore transformations: {}", name, rePattern, orig);
			throw e;
		}
	}

	/**
	 * proxy method to access the hmAllRePattern member
	 * 
	 * @param key
	 *                key to check for
	 * @return whether the map contains the key
	 */
	public boolean containsKey(String key) {
		return hmAllRePattern.containsKey(key);
	}

	/**
	 * proxy method to access the compiled hmAllRePattern member
	 * 
	 * @param key
	 *                Key to retrieve data from
	 * @return String from the map
	 */
	public Pattern getCompiled(String key) {
		return compiled.get(key);
	}

	/**
	 * proxy method to access the hmAllRePattern member
	 * 
	 * @param key
	 *                Key to retrieve data from
	 * @return String from the map
	 */
	public String get(String key) {
		return hmAllRePattern.get(key);
	}

}
