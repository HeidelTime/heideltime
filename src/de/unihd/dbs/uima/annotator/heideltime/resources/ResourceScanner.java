package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceScanner {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ResourceScanner.class);
	
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
	
	private Map<String, ResourceMap> repatterns = new HashMap<String, ResourceMap>();
	private Map<String, ResourceMap> normalizations = new HashMap<String, ResourceMap>();
	private Map<String, ResourceMap> rules = new HashMap<String, ResourceMap>();

	private ResourceScanner() {
		String jarFilePath = null;
		try {
			jarFilePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (jarFilePath != null) {
			File jarFile = new File(jarFilePath);
			if (jarFile.isFile()) {
				// scan the interior of a jar file
				JarFile jar = null;
				try {
					jar = new JarFile(jarFile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				final Enumeration<JarEntry> entries = jar.entries();
				HashMap<String, JarEntry> jarContents = new HashMap<String, JarEntry>();

				while (entries.hasMoreElements()) {
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

				// scan the "resources" folder outside of a jar file

				File outFolder = jarFile.getParentFile();
				this.scanValidOutsideResourcesFolder(outFolder);
			} else {
				// scan the immediate folders of the local classpath
				this.scanValidOutsideResourcesFolder(jarFile);
				// scan the folder "../resources" if it exists
				File outFolder = new File(jarFile.getParentFile(), path);
				if (outFolder.exists()) {
					this.scanValidOutsideResourcesFolder(outFolder);
				}
			}
		}
		
		// populate languages list
		languages.addAll(repatterns.keySet());
	}
	

	public static void main(String[] args) {
		try {
			new ResourceScanner();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
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
				LOG.trace("Testing {}", entry.getKey());
				/*
				 * our conditions for something being a resources folder: the resource
				 * folder must contain at least the following folders:
				 * 
				 * + <language name> 
				 * |- repattern 
				 * |- normalization 
				 * |- rules
				 */
				
				boolean repatternExists = false;
				boolean normalizationExists = false;
				boolean ruleExists = false;
		
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
					LOG.debug("We need at least one readable resource file of each type to run (in {})", entry.getKey());
					continue;
				}
		
				LOG.trace("Valid resource folder: {}", entry.getKey());

				// at this point, the folder is obviously a language resource folder => collect streams
				this.repatterns.put(language, new ResourceMap());
				this.normalizations.put(language, new ResourceMap());
				this.rules.put(language, new ResourceMap());
				for(Entry<String, JarEntry> je : jarContents.entrySet()) {
					Matcher m1 = repatternPattern.matcher(je.getKey());
					Matcher m2 = normalizationPattern.matcher(je.getKey());
					Matcher m3 = rulePattern.matcher(je.getKey()); 
					if(m1.matches()) {
						this.repatterns.get(language).putInnerFile(m1.group(1), je.getKey());
					} 
					if(m2.matches()) {
						this.normalizations.get(language).putInnerFile(m2.group(1), je.getKey());
					} 
					if(m3.matches()) {
						this.rules.get(language).putInnerFile(m3.group(1), je.getKey());
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
				LOG.trace("Testing {}", supposedLanguagePath);
		
				if (!supposedLanguagePath.exists()) {
					LOG.debug("This path doesn't exist.");
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
					LOG.debug("We need at least the folders repattern, normalization and rules in folder '{}'.", supposedLanguagePath);					
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
					LOG.debug("We need at least one readable resource file of each type to run in '{}'", supposedLanguagePath);
					continue;
				}
		
				LOG.trace("Valid resource folder: {}", supposedLanguagePath);

				// at this point, the folder is obviously a language resource folder => collect streams
				this.repatterns.put(language, new ResourceMap());
				for(File f : repatternFiles) {
					Matcher m = repatternPattern.matcher(f.getName());
					if(m.matches()) {
						this.repatterns.get(language).putOuterFile(m.group(1), f);
					}
				}
				
				this.normalizations.put(language, new ResourceMap());
				for(File f : normalizationFiles) {
					Matcher m = normalizationPattern.matcher(f.getName());
					if(m.matches()) {
						this.normalizations.get(language).putOuterFile(m.group(1), f);
					}
				}
				
				this.rules.put(language, new ResourceMap());
				for(File f : ruleFiles) {
					Matcher m = rulePattern.matcher(f.getName());
					if(m.matches()) {
						this.rules.get(language).putOuterFile(m.group(1), f);
					}
				}
			}
		}
	}
	
	public ResourceMap getRepatterns(String language) {
		return repatterns.get(language);
	}
	
	public ResourceMap getNormalizations(String language) {
		return normalizations.get(language);
	}
	
	public ResourceMap getRules(String language) {
		return rules.get(language);
	}
	
	public final Set<String> getDetectedResourceFolders() {
		return languages;
	}
}
