package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class fills the role of a manager of all the rule resources. It reads the data from a file system and fills up a bunch of HashMaps with their information.
 * 
 * @author jannik stroetgen
 * 
 */
public class RuleManager extends GenericResourceManager {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(RuleManager.class);

	/** Static pool */
	protected static HashMap<String, RuleManager> instances = new HashMap<String, RuleManager>();

	/**
	 * singleton producer.
	 * 
	 * @return singleton instance of RuleManager
	 */
	public static RuleManager getInstance(Language language, boolean load_temponym_resources) {
		RuleManager rm = instances.get(language.getName());
		if (rm != null)
			return rm;
		synchronized (RuleManager.class) {
			rm = instances.get(language.getName());
			if (rm != null)
				return rm;
			rm = new RuleManager(language.getResourceFolder(), load_temponym_resources);
			instances.put(language.getName(), rm);
			return rm;
		}
	}

	List<Rule> hmDateRules = new ArrayList<>();
	List<Rule> hmDurationRules = new ArrayList<>();
	List<Rule> hmTimeRules = new ArrayList<>();
	List<Rule> hmSetRules = new ArrayList<>();
	List<Rule> hmTemponymRules = new ArrayList<>();

	/**
	 * Constructor calls the parent constructor that sets language/resource parameters and collects rules resources.
	 * 
	 * @param language
	 *                language of resources to be used
	 * @param load_temponym_resources
	 *                whether temponym resources are loaded
	 */
	protected RuleManager(String language, boolean load_temponym_resources) {
		// Process Generic constructor with rules parameter
		super("rules", language);

		// /////////////////////////////////////////////////
		// READ RULE RESOURCES FROM FILES AND STORE THEM //
		// /////////////////////////////////////////////////
		ResourceScanner rs = ResourceScanner.getInstance();
		ResourceMap hmResourcesRules = rs.getRules(language);
		readRules(hmResourcesRules, language, load_temponym_resources);
	}

	/**
	 * READ THE RULES FROM THE FILES. The files have to be defined in the HashMap hmResourcesRules.
	 * 
	 * @param hmResourcesRules
	 *                rules to be interpreted
	 * @param load_temponym_resources
	 *                whether temponym resources are loaded
	 */
	public void readRules(ResourceMap hmResourcesRules, String language, boolean load_temponym_resources) {
		RePatternManager rpm = RePatternManager.getInstance(Language.getLanguageFromString(language), load_temponym_resources);
		// PATTERNS TO READ RESOURCES "RULES" AND "NORMALIZATION"
		Matcher maReadRules = Pattern.compile("^RULENAME=\"(.*?)\",EXTRACTION=\"(.*?)\",NORM_VALUE=\"(.*?)\"(.*)").matcher("");

		Matcher maAdditional = Pattern.compile("(?<=,)(OFFSET|NORM_QUANT|NORM_FREQ|NORM_MOD|POS_CONSTRAINT|EMPTY_VALUE|FAST_CHECK)=\"(.*?)\" *(?=,|$)").matcher("");

		Matcher maVariable = Pattern.compile("%(re[a-zA-Z0-9]*)").matcher("");
		
		for (String resource : hmResourcesRules.keySet()) {
			try (InputStream is = hmResourcesRules.getInputStream(resource); //
					InputStreamReader isr = new InputStreamReader(is, "UTF-8"); //
					BufferedReader br = new BufferedReader(isr)) {
				List<Rule> rules;
				switch (resource) {
				case "daterules":
					rules = hmDateRules;
					break;
				case "durationrules":
					rules = hmDurationRules;
					break;
				case "setrules":
					rules = hmSetRules;
					break;
				case "timerules":
					rules = hmTimeRules;
					break;
				case "temponymrules":
					rules = hmTemponymRules;
					break;
				default:
					LOG.debug("Resource type not recognized by HeidelTime: {}", resource);
					continue;
				}

				LOG.debug("Adding rule resource: {}", resource);
				lines: for (String line; (line = br.readLine()) != null;) {
					// skip comments or empty lines in resource files
					if (line.startsWith("//") || line.equals(""))
						continue;

					LOG.debug("Reading rules... {}", line);
					// check each line for the name, extraction, and
					// normalization part, others are optional
					maReadRules.reset(line);
					if (!maReadRules.find()) {
						LOG.error("Cannot read the following line of rule resource {} line: {}", resource, line);
						continue lines;
					}
					String rule_name = maReadRules.group(1);
					String rule_extraction = replaceSpaces(maReadRules.group(2));
					String rule_normalization = maReadRules.group(3);

					// throw an error if the rule's name already exists
					for (Rule existing : rules)
						if (existing.name.equals(rule_name)) {
							LOG.warn("WARNING: Duplicate rule name detected. This rule is being ignored: {}", line);
							continue lines;
						}

					// //////////////////////////////////////////////////////////////////
					// RULE EXTRACTION PARTS ARE TRANSLATED INTO REGULAR
					// EXPRESSSIONS //
					// //////////////////////////////////////////////////////////////////
					// create pattern for rule extraction part
					for(maVariable.reset(rule_extraction); maVariable.find(); ) {
						if (LOG.isDebugEnabled())
							LOG.debug("replacing patterns... {}", maVariable.group());
						String varname = maVariable.group(1);
						String rep = rpm.get(varname);
						if (rep == null) {
							LOG.error("Error creating rule: {}", rule_name);
							LOG.error("The following pattern used in this rule does not exist, does it? %{}", varname);
							System.exit(1);
						}
						rule_extraction = rule_extraction.replaceAll("%" + varname, rep);
					}
					// FIXME: this causes false positives inside e.g. character groups!
					rule_extraction = rule_extraction.replaceAll(" ", "\\s+");
					Pattern pattern = null;
					try {
						pattern = Pattern.compile(rule_extraction);
					} catch (java.util.regex.PatternSyntaxException e) {
						LOG.error("Compiling rules resulted in errors.", e);
						LOG.error("Problematic rule is {}", rule_name);
						LOG.error("Cannot compile pattern: {}", rule_extraction);
						System.exit(1);
					}
					// Pattern pattern = Pattern.compile(rule_extraction);
					Rule rule = new Rule(rule_name, pattern, rule_normalization);

					// ///////////////////////////////////
					// CHECK FOR ADDITIONAL CONSTRAINS //
					// ///////////////////////////////////
					if (maReadRules.group(4) != null) {
						maAdditional.reset(line);
						while (maAdditional.find()) {
							String rulename = maAdditional.group(1);
							if (rulename.equals("OFFSET")) {
								rule.offset = maAdditional.group(2);
							} else if (rulename.equals("NORM_QUANT")) {
								rule.quant = maAdditional.group(2);
							} else if (rulename.equals("NORM_FREQ")) {
								rule.freq = maAdditional.group(2);
							} else if (rulename.equals("NORM_MOD")) {
								rule.mod = maAdditional.group(2);
							} else if (rulename.equals("POS_CONSTRAINT")) {
								rule.posConstratint = maAdditional.group(2);
							} else if (rulename.equals("EMPTY_VALUE")) {
								rule.emptyValue = maAdditional.group(2);
							} else if (rulename.equals("FAST_CHECK")) {
								String rule_fast_check = maAdditional.group(2);
								// create pattern for rule fast check part -- similar to extraction part
								// thus using paVariable and rpm
								for(maVariable.reset(rule_fast_check); maVariable.find(); ) {
									if (LOG.isDebugEnabled())
										LOG.debug("replacing patterns... {}", maVariable.group());
									String varname = maVariable.group(1);
									String rep = rpm.get(varname);
									if (rep == null) {
										LOG.error("Error creating rule: {}", rule_name);
										LOG.error("The following pattern used in this rule does not exist, does it? %{}", varname);
										System.exit(1);
									}
									rule_fast_check = rule_fast_check.replaceAll("%" + varname, rep);
								}
								rule_fast_check = rule_fast_check.replaceAll(" ", "\\s+");
								try {
									rule.fastCheck = Pattern.compile(rule_fast_check);
								} catch (java.util.regex.PatternSyntaxException e) {
									LOG.error("Compiling rules resulted in errors.", e);
									LOG.error("Problematic rule is {}", rule_name);
									LOG.error("Cannot compile pattern: {}", rule_fast_check);
									System.exit(1);
								}
							} else {
								LOG.warn("Unknown additional constraint: {}", maAdditional.group());
							}
						}
					}

					rules.add(rule);
				}
				Collections.sort(rules);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			System.exit(1);
		}
		}
	}

	public final List<Rule> getHmDateRules() {
		return hmDateRules;
	}

	public final List<Rule> getHmDurationRules() {
		return hmDurationRules;
	}

	public final List<Rule> getHmTimeRules() {
		return hmTimeRules;
	}

	public final List<Rule> getHmSetRules() {
		return hmSetRules;
	}

	public final List<Rule> getHmTemponymRules() {
		return hmTemponymRules;
	}
}
