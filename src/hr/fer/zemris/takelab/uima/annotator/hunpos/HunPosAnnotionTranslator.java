package hr.fer.zemris.takelab.uima.annotator.hunpos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;

public class HunPosAnnotionTranslator {

	private static final String HEIDELTIME_HOME = "HEIDELTIME_HOME";
	
	private static final String RESOURCE_RELATIVE = "resources/croatian/TagTranslation.conf";
	
	private List<HunPosAnnotationMapping> mappings;
	
	public HunPosAnnotionTranslator() {
		mappings = new ArrayList<HunPosAnnotationMapping>();
		loadTranslations();
	}
	
	private void loadTranslations() {
		String path = System.getenv(HEIDELTIME_HOME) + "/" + RESOURCE_RELATIVE;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(path)));
			Pattern reRule = Pattern.compile("^\\s*\"([^\"]+)\"\\s*=\\s*\"([^\"]+)\"\\s*$");
			String line;
			while((line = reader.readLine()) != null) {
				if(line.trim().isEmpty()) continue;
				
				Matcher m = reRule.matcher(line);
				if(!m.matches()) {
					Logger.printError("Error matching HunPos annotation translation rule : " + line);
					continue;
				}
				
				try {
					mappings.add(new HunPosAnnotationMapping(m.group(1), m.group(2)));
				} catch (Exception e) {
					Logger.printError("Invalid regex in HunPos annotation matching rule " + m.group(1));
					continue;
				}			
			}
		} catch (FileNotFoundException e) {
			Logger.printError("Cannot find the HunPos annotation translation rules file. Should be at: " + path);
		} catch (IOException e) {
			Logger.printError("Error reading HunPos annotation translation rules file at " + path);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				Logger.printError("An error occured while closing the file " + path);
			}
		}
		
	}
	
	public String translate(String annotation) {		
		for(HunPosAnnotationMapping mapping : this.mappings) {
			if(mapping.match(annotation)) {
				return mapping.getTranslation();
			}
		}
		
		//Welp, we failed, return it unchanged
		return annotation;
	}
}
