package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;

/**
 * This ResourceVerifier is an attempt at writing a program that verifies at 
 * least some of the possible pitfalls when writing resource files for 
 * HeidelTime.
 * 
 * Given the regex nature of HeidelTime's resources, a complete
 * white box test is hard to implement at best. Consequently, we'll try to 
 * focus on spotting common syntax errors and the like. 
 * 
 * @author Julian Zell
 *
 */
@SuppressWarnings("unchecked")
public class ResourceVerifier {
	private Language language = null;
	LinkedList<File> patternFiles = new LinkedList<File>();
	LinkedList<File> normFiles = new LinkedList<File>();
	LinkedList<File> ruleFiles = new LinkedList<File>();

	LinkedList<String> patterns = new LinkedList<String>();
	LinkedList<String> normalizations = new LinkedList<String>();
	LinkedList<String> rules = new LinkedList<String>();

	private final Pattern RULES_PATTERN = 
			Pattern.compile("RULENAME=\"((?:\\\\\"|[^\"])+)\",EXTRACTION=\"((?:\\\\\"|[^\"])+)\",NORM_VALUE=\"((?:\\\\\"|[^\"])+)\"(.*)");
	private final Pattern NORMALIZATIONS_PATTERN = 
			Pattern.compile("\"((?:\\\"|[^\"])+)\",\"((?:\\\\\"|[^\"])+)\"\\s*");

	/**
	 * main method to jump into when running this program by itself
	 * @param args
	 */
	public static void main(String[] args) {
		ResourceVerifier verifier = new ResourceVerifier();
		
		if(args.length < 1 || Language.WILDCARD == Language.getLanguageFromString(args[0])) {
			System.err.println("Please supply a language parameter that we comprehend");
			System.exit(-1);
		}
		
		verifier.setLanguage(Language.getLanguageFromString(args[0]));

		System.out.println("initialized with language " + verifier.getLanguage());
		
		System.out.println("parsing used_resources.txt...");
		verifier.readResources();
		System.out.println("checking if resource files exist and are readable...");
		verifier.filesExist();
		System.out.println("reading in resources...");
		verifier.readFiles();
		System.out.println("checking pattern => normalization association");
		//verifier.checkPatternNormalizationAssoc();
		System.out.println("checking rule validity...");
		verifier.checkRulesSyntax();
		
		
		
		verifier.getStringsFromRegexPattern("f[oo]", null);
		
	}
	
	private void checkRulesSyntax() {
		for(File f : ruleFiles) {
			Integer lineNr = 0;
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String line = null;
			
				while(null != (line = br.readLine())) {
					++lineNr;
					
					if(!line.startsWith("//") && line.trim().length() > 0) {
						Matcher m = RULES_PATTERN.matcher(line);
						if(!m.matches())
							warn("discovered an improper rule: [" + lineNr + "] " + line);
					}
				}
			} catch (IOException e) {
				warn("couldn't read file " + f.getName() + " properly.");
			}
		}
	}

	public LinkedList<String> getStringsFromRegexPattern(String in, LinkedList<String> list) {
		//initialize the container for the created strings
		if(list == null)
			list = new LinkedList<String>(); 
		
		// handle [] expressions. we'll allow things like [0-9] (ranges) as well as [aAbB] character groups.
		Pattern p = Pattern.compile("\\[([\\w\\-]+)\\]");
		Matcher m = p.matcher(in);
		if(m.matches()) {
			String charGroup = m.group(1);
			Pattern rangePattern = Pattern.compile("^((\\w)\\-(\\w))+$");
			Matcher rangeMatcher = rangePattern.matcher(charGroup);
			if(rangeMatcher.matches()) {
				
			}
		}
		
		return null;
	}
	
	private void checkPatternNormalizationAssoc() {
		for(String pattern : patterns) {
			Boolean matched = false;
			if(pattern.startsWith("//") || pattern.trim().equals(""))
				continue;
			
			for(String normalization : normalizations) {
				Matcher m = NORMALIZATIONS_PATTERN.matcher(normalization);
				if(m.matches()) {
					Pattern p2 = Pattern.compile(m.group(1));
					Matcher m2 = p2.matcher(pattern);
					if(m2.matches())
						matched = true;
				}
			}
			
			if(!matched)
				warn("could not find a normalization for pattern: "+ pattern);
		}
	}

	private void readFiles() {
		for(LinkedList<File> l : new LinkedList[] { patternFiles, normFiles, ruleFiles })
			for(File f : l) 
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(f.getPath())));
					String line = null;
					while(null != (line = br.readLine())) {
						if(l.equals(patternFiles))
							patterns.push(line);
						else if(l.equals(normFiles))
							normalizations.push(line);
						else if(l.equals(ruleFiles))
							rules.push(line);
					}
				} catch (IOException e) {
					warn("couldn't read in file " + f.getName());
					System.exit(-1);
				}
		
		tell("patterns: " + patterns.size() + "; norm: " + normalizations.size() + "; rules: " + rules.size());
	}

	public void checkIfProperRegex(String str) throws PatternSyntaxException {
		try {
			Pattern.compile(str);
		} catch(PatternSyntaxException pse) {
			warn("The pattern \"" + str + "\" could not be compiled.");
		}
	}
	
	/**
	 * checks whether a given string is a valid representation of a HeidelTime rule given
	 * our syntax
	 * @param str input line from a rule resource file
	 */
	public void checkRuleResourceSyntax(String str) {
		Matcher matcher = RULES_PATTERN.matcher(str);
		
		if(!matcher.matches() && str.trim() != "" && str.startsWith("//"))
			warn("rule \"" + str + "\" is not of a recognizable rule format");
	}
	
	public void checkNormalizationResourceSyntax(String str) {
		Matcher matcher = NORMALIZATIONS_PATTERN.matcher(str);
		
		if(!matcher.matches() && str.trim() != "" && str.startsWith("//"))
			warn("normalization domain \"" + str + "\" is not of a valid normalization format");
	}
	
	
	public void readResources() {
		for(String resourceType : new String[] {"repattern", "normalization", "rules"}) {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("used_resources.txt")));
			try {
				String pathDelim = System.getProperty("file.separator");
				for (String line; (line = br.readLine()) != null; ) {
					Pattern paResource = Pattern.compile(".\\"+pathDelim+"?\\"+pathDelim+language.getName()+"\\"+pathDelim+resourceType+"\\"+pathDelim+"resources_"+resourceType+"_"+"(.*?)\\.txt");
					for (MatchResult ro : Toolbox.findMatches(paResource, line)) {
						if(resourceType.equals("repattern"))
							patternFiles.push(new File(ro.group(0)));
						else if(resourceType.equals("normalization"))
							normFiles.push(new File(ro.group(0)));
						else if(resourceType.equals("rules"))
							ruleFiles.push(new File(ro.group(0)));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				warn("used_resources.txt could not be found.");
				System.exit(-1);
			}
		}
	}
	
	public void filesExist() {
		for(LinkedList<File> l : new LinkedList[] { patternFiles, normFiles, ruleFiles })
			for(File f : l)
				if(!f.exists() || !f.canRead()) {
					warn("file " + f.getName() + " not found or readable");
					System.exit(-1);
				}
	}
	
	private void warn(Object msg) {
		System.err.println("[WARN] " + msg);
	}
	
	private void tell(Object msg) {
		System.out.println("[OKAY] " + msg);
	}
	
	/* 
	 * getters/setters
	 */
	
	public Language setLanguage(Language language) {
		this.language = language;
		return language;
	}
	
	public Language getLanguage() {
		return language;
	}
}
