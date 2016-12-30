package hr.fer.zemris.takelab.uima.annotator.hunpos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HunPosAnnotionTranslator {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(HunPosAnnotionTranslator.class);
	
	private List<HunPosAnnotationMapping> mappings;
	
	public HunPosAnnotionTranslator() {
		mappings = new ArrayList<HunPosAnnotationMapping>();
		loadTranslations();
	}
	
	private void loadTranslations() {
		BufferedReader reader = null;
		InputStream is = null;
		try {
			File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
			if(jarFile.isFile()) {
				is = getClass().getClassLoader().getResourceAsStream("croatian/TagTranslation.conf"); 
			} else {
				File tagFile = new File(new File(jarFile, ".."), "resources/croatian/TagTranslation.conf");
				is = new FileInputStream(tagFile);
			}
			
			reader = new BufferedReader(new InputStreamReader(is));
			Pattern reRule = Pattern.compile("^\\s*\"([^\"]+)\"\\s*=\\s*\"([^\"]+)\"\\s*$");
			String line;
			while((line = reader.readLine()) != null) {
				if(line.trim().isEmpty()) continue;
				
				Matcher m = reRule.matcher(line);
				if(!m.matches()) {
					LOG.error("Error matching HunPos annotation translation rule : " + line);
					continue;
				}
				
				try {
					mappings.add(new HunPosAnnotationMapping(m.group(1), m.group(2)));
				} catch (Exception e) {
					LOG.error("Invalid regex in HunPos annotation matching rule " + m.group(1), e);
					continue;
				}			
			}
		} catch (FileNotFoundException e) {
			LOG.error("Cannot find the HunPos annotation translation rules file.", e);
		} catch (IOException e) {
			LOG.error("Error reading HunPos annotation translation rules file.", e);
		} finally {
			try {
				if(reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				LOG.error("An error occured while closing the file.", e);
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
