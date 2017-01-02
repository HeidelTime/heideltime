package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;

/**
 * 
 * This class fills the role of a manager of all the rule resources. It reads
 * the data from a file system and fills up a bunch of HashMaps with their
 * information.
 * 
 * @author jannik stroetgen
 * 
 */
public class RuleManager extends GenericResourceManager {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(RuleManager.class);
	
	protected static HashMap<String, RuleManager> instances = new HashMap<String, RuleManager>();

	// PATTERNS TO READ RESOURCES "RULES" AND "NORMALIZATION"
	Pattern paReadRules = Pattern.compile("RULENAME=\"(.*?)\",EXTRACTION=\"(.*?)\",NORM_VALUE=\"(.*?)\"(.*)");

	Pattern paAdditional = Pattern.compile("(?<=,)(OFFSET|NORM_QUANT|NORM_FREQ|NORM_MOD|POS_CONSTRAINT|EMPTY_VALUE|FAST_CHECK)=\"(.*?)\" *(?=,|$)");

	// EXTRACTION PARTS OF RULES (patterns loaded from files)
	HashMap<Pattern, String> hmDatePattern = new HashMap<Pattern, String>();
	HashMap<Pattern, String> hmDurationPattern = new HashMap<Pattern, String>();
	HashMap<Pattern, String> hmTimePattern = new HashMap<Pattern, String>();
	HashMap<Pattern, String> hmSetPattern = new HashMap<Pattern, String>();

	// NORMALIZATION PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDateNormalization = new HashMap<String, String>();
	HashMap<String, String> hmTimeNormalization = new HashMap<String, String>();
	HashMap<String, String> hmDurationNormalization = new HashMap<String, String>();
	HashMap<String, String> hmSetNormalization = new HashMap<String, String>();

	// OFFSET PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDateOffset = new HashMap<String, String>();
	HashMap<String, String> hmTimeOffset = new HashMap<String, String>();
	HashMap<String, String> hmDurationOffset = new HashMap<String, String>();
	HashMap<String, String> hmSetOffset = new HashMap<String, String>();

	// QUANT PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDateQuant = new HashMap<String, String>();
	HashMap<String, String> hmTimeQuant = new HashMap<String, String>();
	HashMap<String, String> hmDurationQuant = new HashMap<String, String>();
	HashMap<String, String> hmSetQuant = new HashMap<String, String>();

	// FREQ PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDateFreq = new HashMap<String, String>();
	HashMap<String, String> hmTimeFreq = new HashMap<String, String>();
	HashMap<String, String> hmDurationFreq = new HashMap<String, String>();
	HashMap<String, String> hmSetFreq = new HashMap<String, String>();

	// MOD PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDateMod = new HashMap<String, String>();
	HashMap<String, String> hmTimeMod = new HashMap<String, String>();
	HashMap<String, String> hmDurationMod = new HashMap<String, String>();
	HashMap<String, String> hmSetMod = new HashMap<String, String>();

	// POS PARTS OF RULES (patterns loaded from files)
	HashMap<String, String> hmDatePosConstraint = new HashMap<String, String>();
	HashMap<String, String> hmTimePosConstraint = new HashMap<String, String>();
	HashMap<String, String> hmDurationPosConstraint = new HashMap<String, String>();
	HashMap<String, String> hmSetPosConstraint = new HashMap<String, String>();
	
	// EMPTYVALUE part of rules
	HashMap<String, String> hmDateEmptyValue = new HashMap<String, String>();
	HashMap<String, String> hmTimeEmptyValue = new HashMap<String, String>();
	HashMap<String, String> hmDurationEmptyValue = new HashMap<String, String>();
	HashMap<String, String> hmSetEmptyValue = new HashMap<String, String>();
	
	// FASTCHECK part of rules
	HashMap<String, Pattern> hmDateFastCheck = new HashMap<String, Pattern>();
	HashMap<String, Pattern> hmTimeFastCheck = new HashMap<String, Pattern>();
	HashMap<String, Pattern> hmDurationFastCheck = new HashMap<String, Pattern>();
	HashMap<String, Pattern> hmSetFastCheck = new HashMap<String, Pattern>();

	// TEMPONYM RULES (loaded from resource files)
	HashMap<Pattern, String> hmTemponymPattern = new HashMap<Pattern, String>();
	HashMap<String, String> hmTemponymNormalization = new HashMap<String, String>();
	HashMap<String, String> hmTemponymOffset = new HashMap<String, String>();
	HashMap<String, String> hmTemponymQuant = new HashMap<String, String>();
	HashMap<String, String> hmTemponymFreq = new HashMap<String, String>();
	HashMap<String, String> hmTemponymMod = new HashMap<String, String>();
	HashMap<String, String> hmTemponymPosConstraint = new HashMap<String, String>();
	HashMap<String, String> hmTemponymEmptyValue = new HashMap<String, String>();
	HashMap<String, Pattern> hmTemponymFastCheck = new HashMap<String, Pattern>();
	
	/**
	 * Constructor calls the parent constructor that sets language/resource
	 * parameters and collects rules resources.
	 * 
	 * @param language
	 *            language of resources to be used
	 * @param load_temponym_resources
	 *            whether temponym resources are loaded
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
	 * singleton producer.
	 * 
	 * @return singleton instance of RuleManager
	 */
	public static RuleManager getInstance(Language language, boolean load_temponym_resources) {
		if(!instances.containsKey(language.getName())) {
			RuleManager nm = new RuleManager(language.getResourceFolder(), load_temponym_resources);
			instances.put(language.getName(), nm);
		}
		
		return instances.get(language.getName());
	}

	/**
	 * READ THE RULES FROM THE FILES. The files have to be defined in the
	 * HashMap hmResourcesRules.
	 * 
	 * @param hmResourcesRules
	 *            rules to be interpreted
	 * @param load_temponym_resources
	 *            whether temponym resources are loaded
	 */
	public void readRules(ResourceMap hmResourcesRules, String language, boolean load_temponym_resources) {
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		ArrayList<String> resourceKeys = new ArrayList<String>(hmResourcesRules.keySet());
		
		// sort DATE > TIME > DURATION > SET > rest
		Collections.sort(resourceKeys, new Comparator<String>() {
			@Override
			public int compare(String arg0, String arg1) {
				if("daterules".equals(arg0)) {
					return -1;
				} else if("timerules".equals(arg0) && !"daterules".equals(arg1)) {
					return -1;
				} else if("durationrules".equals(arg0) && !"daterules".equals(arg1) && !"timerules".equals(arg1)) {
					return -1;
				} else if("setrules".equals(arg0) && !"daterules".equals(arg1) && !"timerules".equals(arg1) && !"durationrules".equals(arg1)) {
					return -1;
				}
				return 1;
			}
		});
		
		Pattern paVariable = Pattern.compile("%(re[a-zA-Z0-9]*)");
		
		try {
			for (String resource : resourceKeys) {
				is = hmResourcesRules.getInputStream(resource);
				isr = new InputStreamReader(is, "UTF-8");
				br = new BufferedReader(isr);
				
				LOG.debug("Adding rule resource: {}", resource);
				for (String line; (line = br.readLine()) != null;) {
					// skip comments or empty lines in resource files
					if (line.startsWith("//") || line.equals(""))
						continue;
					
					boolean correctLine = false;
					LOG.debug("Reading rules... {}", line);
					// check each line for the name, extraction, and
					// normalization part, others are optional
					for (MatchResult r : Toolbox.findMatches(paReadRules, line)) {
						correctLine = true;
						String rule_name = r.group(1);
						String rule_extraction = replaceSpaces(r.group(2));
						String rule_normalization = r.group(3);
						String rule_offset = "";
						String rule_quant = "";
						String rule_freq = "";
						String rule_mod = "";
						String pos_constraint = "";
						String rule_empty_value = "";
						String rule_fast_check = "";
						
						// throw an error if the rule's name already exists
						if(hmDatePattern.containsValue(rule_name) ||
								hmDurationPattern.containsValue(rule_name) ||
								hmSetPattern.containsValue(rule_name) ||
								hmTimePattern.containsValue(rule_name)) {
							LOG.warn("WARNING: Duplicate rule name detected. This rule is being ignored: {}", line);
						}

						// //////////////////////////////////////////////////////////////////
						// RULE EXTRACTION PARTS ARE TRANSLATED INTO REGULAR
						// EXPRESSSIONS //
						// //////////////////////////////////////////////////////////////////
						// create pattern for rule extraction part
						RePatternManager rpm = RePatternManager.getInstance(Language.getLanguageFromString(language), load_temponym_resources);
						for (MatchResult mr : Toolbox.findMatches(paVariable, rule_extraction)) {
							LOG.debug("replacing patterns... {}", mr.group());
							if (!(rpm.containsKey(mr.group(1)))) {
								LOG.error("Error creating rule: {}", rule_name);
								LOG.error("The following pattern used in this rule does not exist, does it? %{}", mr.group(1));
								System.exit(1);
							}
							rule_extraction = rule_extraction.replaceAll("%" + mr.group(1), rpm.get(mr.group(1)));
						}
						rule_extraction = rule_extraction.replaceAll(" ", "[\\\\s]+");
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

						// ///////////////////////////////////
						// CHECK FOR ADDITIONAL CONSTRAINS //
						// ///////////////////////////////////
						Pattern patternFast = null;
						if (!(r.group(4) == null)) {
							Matcher ro = paAdditional.matcher(line);
							while(ro.find()) {
								String rulename = ro.group(1);
								if (rulename.equals("OFFSET")) {
									rule_offset = ro.group(2);
								}
								else if (rulename.equals("NORM_QUANT")) {
									rule_quant = ro.group(2);
								}
								else if (rulename.equals("NORM_FREQ")) {
									rule_freq = ro.group(2);
								}
								else if (rulename.equals("NORM_MOD")) {
									rule_mod = ro.group(2);
								}
								else if (rulename.equals("POS_CONSTRAINT")) {
									pos_constraint = ro.group(2);
								}
								else if (rulename.equals("EMPTY_VALUE")) {
									rule_empty_value = ro.group(2);
								}
								else if (rulename.equals("FAST_CHECK")) {
									rule_fast_check = ro.group(2);
									// create pattern for rule fast check part -- similar to extraction part
									// thus using paVariable and rpm
									for (MatchResult mr : Toolbox.findMatches(paVariable, rule_fast_check)) {
										LOG.debug("replacing patterns... {}", mr.group());
										if (!(rpm.containsKey(mr.group(1)))) {
											LOG.error("Error creating rule: {}", rule_name);
											LOG.error("The following pattern used in this rule does not exist, does it? %{}", mr.group(1));
											System.exit(1);
										}
										rule_fast_check = rule_fast_check.replaceAll("%" + mr.group(1), rpm.get(mr.group(1)));
									}
									rule_fast_check = rule_fast_check.replaceAll(" ", "[\\\\s]+");
									patternFast = null;
									try {
										patternFast = Pattern.compile(rule_fast_check);
									} catch (java.util.regex.PatternSyntaxException e) {
										LOG.error("Compiling rules resulted in errors.", e);
										LOG.error("Problematic rule is {}", rule_name);
										LOG.error("Cannot compile pattern: {}", rule_fast_check);
										System.exit(1);
									}
								}
								else {
									LOG.warn("Unknown additional constraint: {}", ro.group());
								}
							}
						}

						// ///////////////////////////////////////////
						// READ DATE RULES AND MAKE THEM AVAILABLE //
						// ///////////////////////////////////////////
						if (resource.equals("daterules")) {
							// get extraction part
							hmDatePattern.put(pattern, rule_name);
							// get normalization part
							hmDateNormalization.put(rule_name,
									rule_normalization);
							// get offset part
							if (!(rule_offset.equals(""))) {
								hmDateOffset.put(rule_name, rule_offset);
							}
							// get quant part
							if (!(rule_quant.equals(""))) {
								hmDateQuant.put(rule_name, rule_quant);
							}
							// get freq part
							if (!(rule_freq.equals(""))) {
								hmDateFreq.put(rule_name, rule_freq);
							}
							// get mod part
							if (!(rule_mod.equals(""))) {
								hmDateMod.put(rule_name, rule_mod);
							}
							// get pos constraint part
							if (!(pos_constraint.equals(""))) {
								hmDatePosConstraint.put(rule_name,
										pos_constraint);
							}
							// get empty value part
							if (!(rule_empty_value.equals(""))) {
								hmDateEmptyValue.put(rule_name,
										rule_empty_value);
							}
							// get fast check part
							if (!(rule_fast_check.equals(""))) {
								hmDateFastCheck.put(rule_name,
										patternFast);
							}
						}

						// ///////////////////////////////////////////////
						// READ DURATION RULES AND MAKE THEM AVAILABLE //
						// ///////////////////////////////////////////////
						else if (resource.equals("durationrules")) {
							// get extraction part
							hmDurationPattern.put(pattern, rule_name);
							// get normalization part
							hmDurationNormalization.put(rule_name,
									rule_normalization);
							// get offset part
							if (!(rule_offset.equals(""))) {
								hmDurationOffset.put(rule_name, rule_offset);
							}
							// get quant part
							if (!(rule_quant.equals(""))) {
								hmDurationQuant.put(rule_name, rule_quant);
							}
							// get freq part
							if (!(rule_freq.equals(""))) {
								hmDurationFreq.put(rule_name, rule_freq);
							}
							// get mod part
							if (!(rule_mod.equals(""))) {
								hmDurationMod.put(rule_name, rule_mod);
							}
							// get pos constraint part
							if (!(pos_constraint.equals(""))) {
								hmDurationPosConstraint.put(rule_name,
										pos_constraint);
							}
							// get empty value part
							if (!(rule_empty_value.equals(""))) {
								hmDurationEmptyValue.put(rule_name,
										rule_empty_value);
							}
							// get fast check part
							if (!(rule_fast_check.equals(""))) {
								hmDurationFastCheck.put(rule_name,
										patternFast);
							}
						}

						// //////////////////////////////////////////
						// READ SET RULES AND MAKE THEM AVAILABLE //
						// //////////////////////////////////////////
						else if (resource.equals("setrules")) {
							// get extraction part
							hmSetPattern.put(pattern, rule_name);
							// get normalization part
							hmSetNormalization.put(rule_name,
									rule_normalization);
							// get offset part
							if (!rule_offset.equals("")) {
								hmSetOffset.put(rule_name, rule_offset);
							}
							// get quant part
							if (!rule_quant.equals("")) {
								hmSetQuant.put(rule_name, rule_quant);
							}
							// get freq part
							if (!rule_freq.equals("")) {
								hmSetFreq.put(rule_name, rule_freq);
							}
							// get mod part
							if (!rule_mod.equals("")) {
								hmSetMod.put(rule_name, rule_mod);
							}
							// get pos constraint part
							if (!pos_constraint.equals("")) {
								hmSetPosConstraint.put(rule_name,
										pos_constraint);
							}
							// get empty value part
							if (!(rule_empty_value.equals(""))) {
								hmSetEmptyValue.put(rule_name,
										rule_empty_value);
							}
							// get fast check part
							if (!(rule_fast_check.equals(""))) {
								hmSetFastCheck.put(rule_name,
										patternFast);
							}
						}

						// ///////////////////////////////////////////
						// READ TIME RULES AND MAKE THEM AVAILABLE //
						// ///////////////////////////////////////////
						else if (resource.equals("timerules")) {
							// get extraction part
							hmTimePattern.put(pattern, rule_name);
							// get normalization part
							hmTimeNormalization.put(rule_name,
									rule_normalization);
							// get offset part
							if (!rule_offset.equals("")) {
								hmTimeOffset.put(rule_name, rule_offset);
							}
							// get quant part
							if (!rule_quant.equals("")) {
								hmTimeQuant.put(rule_name, rule_quant);
							}
							// get freq part
							if (!rule_freq.equals("")) {
								hmTimeFreq.put(rule_name, rule_freq);
							}
							// get mod part
							if (!rule_mod.equals("")) {
								hmTimeMod.put(rule_name, rule_mod);
							}
							// get pos constraint part
							if (!pos_constraint.equals("")) {
								hmTimePosConstraint.put(rule_name,
										pos_constraint);
							}
							// get empty value part
							if (!(rule_empty_value.equals(""))) {
								hmTimeEmptyValue.put(rule_name,
										rule_empty_value);
							}
							// get fast check part
							if (!(rule_fast_check.equals(""))) {
								hmTimeFastCheck.put(rule_name,
										patternFast);
							}
						}
						// //////////////////////////////////////////////
						// READ TEMPONYM RULES AND MAKE THEM AVAILABLE //
						// //////////////////////////////////////////////
						else if (resource.equals("temponymrules")) {
							// get extraction part
							hmTemponymPattern.put(pattern, rule_name);
							// get normalization part
							hmTemponymNormalization.put(rule_name,
									rule_normalization);
							// get offset part
							if (!(rule_offset.equals(""))) {
								hmTemponymOffset.put(rule_name, rule_offset);
							}
								// get quant part
							if (!(rule_quant.equals(""))) {
								hmTemponymQuant.put(rule_name, rule_quant);
							}
							// get freq part
							if (!(rule_freq.equals(""))) {
								hmTemponymFreq.put(rule_name, rule_freq);
							}
							// get mod part
							if (!(rule_mod.equals(""))) {
								hmTemponymMod.put(rule_name, rule_mod);
							}
							// get pos constraint part
							if (!(pos_constraint.equals(""))) {
								hmTemponymPosConstraint.put(rule_name,
										pos_constraint);
							}
							// get empty value part
							if (!(rule_empty_value.equals(""))) {
								hmTemponymEmptyValue.put(rule_name,
										rule_empty_value);
							}
							// get fast check part
							if (!(rule_fast_check.equals(""))) {
								hmTemponymFastCheck.put(rule_name,
										patternFast);
							}
						} else {
							LOG.debug("Resource not recognized by HeidelTime: {}", resource);
						}
					}

					// /////////////////////////////////////////
					// CHECK FOR PROBLEMS WHEN READING RULES //
					// /////////////////////////////////////////
					if (!correctLine) {
						LOG.error("Cannot read the following line of rule resource {} line: {}", resource, line);
					}

				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
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
				LOG.error(e.getMessage(), e);
			}
		}
	}

	public final HashMap<Pattern, String> getHmDatePattern() {
		return hmDatePattern;
	}

	public final HashMap<Pattern, String> getHmDurationPattern() {
		return hmDurationPattern;
	}

	public final HashMap<Pattern, String> getHmTimePattern() {
		return hmTimePattern;
	}

	public final HashMap<Pattern, String> getHmSetPattern() {
		return hmSetPattern;
	}

	public final HashMap<String, String> getHmDateNormalization() {
		return hmDateNormalization;
	}

	public final HashMap<String, String> getHmTimeNormalization() {
		return hmTimeNormalization;
	}

	public final HashMap<String, String> getHmDurationNormalization() {
		return hmDurationNormalization;
	}

	public final HashMap<String, String> getHmSetNormalization() {
		return hmSetNormalization;
	}

	public final HashMap<String, String> getHmDateOffset() {
		return hmDateOffset;
	}

	public final HashMap<String, String> getHmTimeOffset() {
		return hmTimeOffset;
	}

	public final HashMap<String, String> getHmDurationOffset() {
		return hmDurationOffset;
	}

	public final HashMap<String, String> getHmSetOffset() {
		return hmSetOffset;
	}

	public final HashMap<String, String> getHmDateQuant() {
		return hmDateQuant;
	}

	public final HashMap<String, String> getHmTimeQuant() {
		return hmTimeQuant;
	}

	public final HashMap<String, String> getHmDurationQuant() {
		return hmDurationQuant;
	}

	public final HashMap<String, String> getHmSetQuant() {
		return hmSetQuant;
	}

	public final HashMap<String, String> getHmDateFreq() {
		return hmDateFreq;
	}

	public final HashMap<String, String> getHmTimeFreq() {
		return hmTimeFreq;
	}

	public final HashMap<String, String> getHmDurationFreq() {
		return hmDurationFreq;
	}

	public final HashMap<String, String> getHmSetFreq() {
		return hmSetFreq;
	}

	public final HashMap<String, String> getHmDateMod() {
		return hmDateMod;
	}

	public final HashMap<String, String> getHmTimeMod() {
		return hmTimeMod;
	}

	public final HashMap<String, String> getHmDurationMod() {
		return hmDurationMod;
	}

	public final HashMap<String, String> getHmSetMod() {
		return hmSetMod;
	}

	public final HashMap<String, String> getHmDatePosConstraint() {
		return hmDatePosConstraint;
	}

	public final HashMap<String, String> getHmTimePosConstraint() {
		return hmTimePosConstraint;
	}

	public final HashMap<String, String> getHmDurationPosConstraint() {
		return hmDurationPosConstraint;
	}

	public final HashMap<String, String> getHmSetPosConstraint() {
		return hmSetPosConstraint;
	}

	public final HashMap<String, String> getHmDateEmptyValue() {
		return hmDateEmptyValue;
	}

	public final HashMap<String, String> getHmTimeEmptyValue() {
		return hmTimeEmptyValue;
	}

	public final HashMap<String, String> getHmDurationEmptyValue() {
		return hmDurationEmptyValue;
	}

	public final HashMap<String, String> getHmSetEmptyValue() {
		return hmSetEmptyValue;
	}

	public final HashMap<Pattern, String> getHmTemponymPattern() {
		return hmTemponymPattern;
	}
	
	public final HashMap<String, String> getHmTemponymNormalization() {
		return hmTemponymNormalization;
	}
	
	public final HashMap<String, String> getHmTemponymOffset() {
		return hmTemponymOffset;
	}
	
	public final HashMap<String, String> getHmTemponymQuant() {
		return hmTemponymQuant;
	}
	
	public final HashMap<String, String> getHmTemponymFreq() {
		return hmTemponymFreq;
	}
	
	public final HashMap<String, String> getHmTemponymMod() {
		return hmTemponymMod;
	}
	
	public final HashMap<String, String> getHmTemponymPosConstraint() {
		return hmTemponymPosConstraint;
	}
	
	public final HashMap<String, String> getHmTemponymEmptyValue() {
		return hmTemponymEmptyValue;
	}

	public final HashMap<String, Pattern> getHmDateFastCheck() {
		return hmDateFastCheck;
	}

	public final HashMap<String, Pattern> getHmTimeFastCheck() {
		return hmTimeFastCheck;
	}

	public final HashMap<String, Pattern> getHmDurationFastCheck() {
		return hmDurationFastCheck;
	}

	public final HashMap<String, Pattern> getHmSetFastCheck() {
		return hmSetFastCheck;
	}

	public final HashMap<String, Pattern> getHmTemponymFastCheck() {
		return hmTemponymFastCheck;
	}
}
