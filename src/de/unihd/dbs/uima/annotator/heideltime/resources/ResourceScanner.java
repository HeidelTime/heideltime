package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;

public class ResourceScanner {
	private static ResourceScanner INSTANCE = null;

	/**
	 * singleton producer.
	 * @return singleton instance of ResourceScanner
	 */
	public static ResourceScanner getInstance() {
		if(INSTANCE == null) {
			synchronized(ResourceScanner.class) {
				if(INSTANCE == null) {
					INSTANCE = new ResourceScanner();
				}
			}
		}
		
		return INSTANCE;
	}
	
	private final String path = "resources";
	
	private Set<String> languages = new HashSet<String>();
	
	private Map<String, Map<String, InputStream>> repatterns = new HashMap<String, Map<String, InputStream>>();
	private Map<String, Map<String, InputStream>> normalizations = new HashMap<String, Map<String, InputStream>>();
	private Map<String, Map<String, InputStream>> rules = new HashMap<String, Map<String, InputStream>>();

	private ResourceScanner() {
		// scan the interior of a jar file
		File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		if (jarFile.isFile()) { // run from inside a .jar file
			JarFile jar = null;
			try {
				jar = new JarFile(jarFile);
			} catch(IOException e1) {
				e1.printStackTrace();
			}
			final Enumeration<JarEntry> entries = jar.entries();
			HashMap<String, JarEntry> jarContents = new HashMap<String, JarEntry>();

			while(entries.hasMoreElements()) {
				JarEntry je = entries.nextElement();
				String name = je.getName();
				
				jarContents.put(name, je);
			}
			
			try {
				jar.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.scanValidInsideResourcesFolder(jarContents);
		}
		
		// scan the exterior of the jar/class
		URL url = ResourceScanner.class.getResource("/" + path);
		if (url != null) {
			try {
				this.scanValidOutsideResourcesFolder(new File(url.toURI()));
			} catch (URISyntaxException ex) {  }
		} else if(jarFile.isDirectory()) {
			this.scanValidOutsideResourcesFolder(jarFile);
		}
		
		// populate languages list
		languages.addAll(repatterns.keySet());
	}

	public static void main(String[] args) {
		ResourceScanner rs = null;
		try {
			rs = new ResourceScanner();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void scanValidInsideResourcesFolder(HashMap<String, JarEntry> jarContents) {
		HashMap<String, JarEntry> topLevelEntries = new HashMap<String, JarEntry>();
		for(Entry<String, JarEntry> entry : jarContents.entrySet()) {
			if(entry.getValue().isDirectory() && entry.getKey().matches("^[^\\/]+/$")) {
				topLevelEntries.put(entry.getKey(), entry.getValue());
			}
		}
		
		for (Entry<String, JarEntry> entry : topLevelEntries.entrySet()) {
			String language = entry.getKey().substring(0, entry.getKey().length() - 1);
			
			Pattern repatternPattern = Pattern.compile(language + "/repattern/resources_repattern_(.+)\\.txt$");
			Pattern normalizationPattern = Pattern.compile(language + "/normalization/resources_normalization_(.+)\\.txt$");
			Pattern rulePattern = Pattern.compile(language + "/rules/resources_rules_(.+)\\.txt$");
			
			if (entry.getValue().isDirectory()) {
				Logger.printDetail(ResourceScanner.class, "Testing " + entry.getKey());
				/*
				 * our conditions for something being a resources folder: the resource
				 * folder must contain at least the following folders:
				 * 
				 * + <language name> 
				 * |- repattern 
				 * |- normalization 
				 * |- rules
				 */
				
				Boolean repatternExists = false;
				Boolean normalizationExists = false;
				Boolean ruleExists = false;
		
				for(String entryName : jarContents.keySet()) {
					if(!repatternExists && repatternPattern.matcher(entryName).matches()) {
						repatternExists = true;
					}
					if(!normalizationExists && normalizationPattern.matcher(entryName).matches()) {
						normalizationExists = true;
					}
					if(!ruleExists && rulePattern.matcher(entryName).matches()) {
						ruleExists = true;
					}
				}
				
				if(!repatternExists || !normalizationExists || !ruleExists) {
					Logger.printDetail(ResourceScanner.class, "We need at least one readable resource file of each type to run.");
					continue;
				}
		
				Logger.printDetail(ResourceScanner.class, "Valid resource folder.");

				// at this point, the folder is obviously a language resource folder => collect streams
				this.repatterns.put(language, new HashMap<String, InputStream>());
				this.normalizations.put(language, new HashMap<String, InputStream>());
				this.rules.put(language, new HashMap<String, InputStream>());
				for(Entry<String, JarEntry> je : jarContents.entrySet()) {
					Matcher m1 = repatternPattern.matcher(je.getKey());
					Matcher m2 = normalizationPattern.matcher(je.getKey());
					Matcher m3 = rulePattern.matcher(je.getKey()); 
					if(m1.matches()) {
						this.repatterns.get(language).put(m1.group(1), this.getClass().getClassLoader().getResourceAsStream(je.getKey()));
					} 
					if(m2.matches()) {
						this.normalizations.get(language).put(m2.group(1), this.getClass().getClassLoader().getResourceAsStream(je.getKey()));
					} 
					if(m3.matches()) {
						this.rules.get(language).put(m3.group(1), this.getClass().getClassLoader().getResourceAsStream(je.getKey()));
					}
				}
			}
		}
	}

	private void scanValidOutsideResourcesFolder(File resourcePath) {
		Pattern repatternPattern = Pattern.compile("resources_repattern_(.+)\\.txt$");
		Pattern normalizationPattern = Pattern.compile("resources_normalization_(.+)\\.txt$");
		Pattern rulePattern = Pattern.compile("resources_rules_(.+)\\.txt$");
	
		File[] pathContents = resourcePath.listFiles();

		for (File supposedLanguagePath : pathContents) {
			String language = supposedLanguagePath.getName();
			if (supposedLanguagePath.isDirectory()) {
				Logger.printDetail(ResourceScanner.class, "Testing " + supposedLanguagePath.getAbsolutePath());
		
				if (!supposedLanguagePath.exists()) {
					Logger.printDetail(ResourceScanner.class, "This path doesn't exist.");
					continue;
				}
		
				/*
				 * our conditions for something being a resources folder: the resource
				 * folder must contain at least the following folders:
				 * 
				 * + <language name> 
				 * |- repattern 
				 * |- normalization 
				 * |- rules
				 */
		
				File repatternFolder = new File(supposedLanguagePath, "repattern");
				File normalizationFolder = new File(supposedLanguagePath, "normalization");
				File ruleFolder = new File(supposedLanguagePath, "rules");
		
				if (!repatternFolder.exists() || !repatternFolder.canRead() || !repatternFolder.isDirectory()
						|| !normalizationFolder.exists() || !normalizationFolder.canRead() || !normalizationFolder.isDirectory() 
						|| !ruleFolder.exists() || !ruleFolder.canRead() || !ruleFolder.isDirectory()) {
					Logger.printDetail(ResourceScanner.class, "We need at least the folders repattern, normalization and rules in this folder.");
					
					continue;
				}
		
				/*
				 * furthermore, we require at least one repattern file, one
				 * normalization file and one rule file named in this pattern:
				 * 
				 * - resources_repattern_re<name of pattern>.txt
				 * - resources_normalization_norm<name of normalization>.txt
				 * - resources_rules_<date|time|duration|set>rules.txt
				 */
		
				FilenameFilter txtFilter = new FilenameFilter() {
					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".txt");
					}
				};
		
				File[] repatternFiles = repatternFolder.listFiles(txtFilter);
				File[] normalizationFiles = normalizationFolder.listFiles(txtFilter);
				File[] ruleFiles = ruleFolder.listFiles(txtFilter);
		
				if (repatternFiles.length == 0 || normalizationFiles.length == 0 || ruleFiles.length == 0
						|| !repatternFiles[0].exists() || !repatternFiles[0].canRead() || !repatternFiles[0].isFile()
						|| !normalizationFiles[0].exists() || !normalizationFiles[0].canRead() || !normalizationFiles[0].isFile()
						|| !ruleFiles[0].exists() || !ruleFiles[0].canRead() || !ruleFiles[0].isFile()) {
					Logger.printDetail(ResourceScanner.class, "We need at least one readable resource file of each type to run.");
					continue;
				}
		
				Logger.printDetail(ResourceScanner.class, "Valid resource folder.");

				// at this point, the folder is obviously a language resource folder => collect streams
				this.repatterns.put(language, new HashMap<String, InputStream>());
				for(File f : repatternFiles) {
					try {
						Matcher m = repatternPattern.matcher(f.getName());
						if(m.matches()) {
							this.repatterns.get(language).put(m.group(1), new FileInputStream(f));
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				
				this.normalizations.put(language, new HashMap<String, InputStream>());
				for(File f : normalizationFiles) {
					try {
						Matcher m = normalizationPattern.matcher(f.getName());
						if(m.matches()) {
							this.normalizations.get(language).put(m.group(1), new FileInputStream(f));
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				
				this.rules.put(language, new HashMap<String, InputStream>());
				for(File f : ruleFiles) {
					try {
						Matcher m = rulePattern.matcher(f.getName());
						if(m.matches()) {
							this.rules.get(language).put(m.group(1), new FileInputStream(f));
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public Map<String, InputStream> getRepatterns(String language) {
		return repatterns.get(language);
	}
	
	public Map<String, InputStream> getNormalizations(String language) {
		return normalizations.get(language);
	}
	
	public Map<String, InputStream> getRules(String language) {
		return rules.get(language);
	}
}
