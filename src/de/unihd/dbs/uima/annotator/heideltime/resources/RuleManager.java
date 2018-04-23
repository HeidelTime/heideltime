package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class fills the role of a manager of all the rule resources. It reads the data from a file system and fills up a bunch of HashMaps with their information.
 * 
 * @author jannik stroetgen
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

	/**
	 * Exception thrown when a pattern could not be built.
	 */
	public static class InvalidPatternException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InvalidPatternException(String msg) {
			super(msg);
		}

		public InvalidPatternException(String msg, Throwable cause) {
			super(msg, cause);
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
				case "intervalrules": // Handled separately.
					continue;
				default:
					LOG.debug("Resource type not recognized by HeidelTime: {}", resource);
					continue;
				}

				LOG.debug("Adding rule resource: {}", resource);
				lines: for (String line; (line = br.readLine()) != null;) {
					// skip comments or empty lines in resource files
					if (line.startsWith("//") || line.equals(""))
						continue;

					LOG.trace("Reading rules... {}", line);
					// check each line for the name, extraction, and
					// normalization part, others are optional
					maReadRules.reset(line);
					if (!maReadRules.find()) {
						LOG.error("Cannot read the following line of rule resource {} line: {}", resource, line);
						continue lines;
					}
					String rule_name = maReadRules.group(1);
					String rule_extraction = maReadRules.group(2);
					String rule_normalization = maReadRules.group(3);

					// throw an error if the rule's name already exists
					for (Rule existing : rules)
						if (existing.name.equals(rule_name)) {
							LOG.warn("WARNING: Duplicate rule name detected. This rule is being ignored: {}", line);
							continue lines;
						}

					rule_extraction = expandVariables(rule_name, rule_extraction, rpm);
					rule_extraction = replaceSpaces(rule_extraction);
					Pattern pattern = null;
					try {
						LOG.trace("Compiling pattern {}: {}", rule_name, rule_extraction);
						pattern = Pattern.compile(rule_extraction);
					} catch (java.util.regex.PatternSyntaxException e) {
						LOG.error("Compiling rules resulted in errors.", e);
						LOG.error("Problematic rule is {}", rule_name);
						LOG.error("Cannot compile pattern: {}", rule_extraction);
						throw new InvalidPatternException("Pattern compilation error in '" + rule_name + "'", e);
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
								rule_fast_check = expandVariables(rule_name, rule_fast_check, rpm);
								rule_fast_check = replaceSpaces(rule_fast_check);
								try {
									rule.fastCheck = Pattern.compile(rule_fast_check);
								} catch (java.util.regex.PatternSyntaxException e) {
									LOG.error("Compiling rules resulted in errors.", e);
									LOG.error("Problematic rule is {}", rule_name);
									LOG.error("Cannot compile pattern: {}", rule_fast_check);
									throw new InvalidPatternException("Pattern compilation error in '" + rule_name + "'", e);
								}
							} else {
								LOG.warn("Unknown additional constraint: {}", maAdditional.group());
							}
						}
					}

					rules.add(rule);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
				throw new RuntimeException("Cannot load patterns: " + e.getMessage(), e);
			}
		}
	}

	private static final Pattern paVariable = Pattern.compile("%(?:(re[a-zA-Z0-9]+)|\\((re[a-zA-Z0-9]+(?:\\|re[a-zA-Z0-9]+)*)\\))");
	private static final Pattern paSplit = Pattern.compile("%?\\|");

	public static String expandVariables(CharSequence rule_name, String str, RePatternManager rpm) {
		Matcher matcher = paVariable.matcher(str);
		// Shortcut if no matches:
		if (!matcher.find())
			return str;
		StringBuilder buf = new StringBuilder(1000);
		int pos = 0;
		do {
			List<String> pats = new ArrayList<>();
			String g1 = matcher.group(1);
			if (g1 != null) {
				// Only one group matched.
				String rep = rpm.get(g1);
				if (rep == null) {
					LOG.error("Error expanding rule '{}': RePattern not defined: '%{}'", rule_name, g1);
					throw new InvalidPatternException("Rule '" + rule_name + "' referenced missing pattern '" + g1 + "'");
				}
				pats.add(rep);
			} else {
				// Split, lookup, and join group(2).
				String[] parts = paSplit.split(matcher.group(2));
				for (int i = 0; i < parts.length; i++) {
					String rep = rpm.get(parts[i]);
					if (rep == null) {
						LOG.error("Error expanding rule '{}': RePattern not defined: '%{}'", rule_name, parts[i]);
						throw new InvalidPatternException("Rule '" + rule_name + "' referenced missing pattern '" + parts[i] + "'");
					}
					pats.add(rep);
				}
			}
			if (pats.size() > 1)
				pats = RePatternManager.optimizePatterns(rule_name, pats);
			int start = matcher.start(), end = matcher.end();
			if (pos < start)
				buf.append(str, pos, start);
			Iterator<String> it = pats.iterator();
			buf.append('(').append(it.next()); // first
			while (it.hasNext())
				buf.append('|').append(it.next());
			buf.append(')');
			pos = end;
		} while (matcher.find());
		if (pos < str.length())
			buf.append(str, pos, str.length());
		return buf.toString();
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
