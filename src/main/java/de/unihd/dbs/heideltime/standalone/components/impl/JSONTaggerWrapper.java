package de.unihd.dbs.heideltime.standalone.components.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;


/**
 * The JSON tagger wrapper reads JSON files containing the sentence annotations and the POS annotations
 * The path to these two files should be indicated in the environment 
 * 
 * @author Lise Rebout, CRIM
 */
public class JSONTaggerWrapper implements PartOfSpeechTagger {
	
	private String configFile;
	private Hashtable<String, List<String>> defs = new Hashtable<String, List<String>>();


	public String SENTENCES = "sentences";
	public String SENTENCE_BEGIN = "sentence_begin";
	public String SENTENCE_END = "sentence_end";
	public String TOKENS = "tokens";
	public String TOKEN_BEGIN = "token_begin";
	public String TOKEN_END = "token_end";
	public String TOKEN_POS = "token_pos";

	
	@Override
	public void initialize(Properties settings) throws IllegalArgumentException, IOException {
		configFile = (String) settings.get(JSON_CONFIG_PATH);
		if(configFile == null) {
	    	throw new IllegalArgumentException(
                    "The JSON configfile path was not defined in the settings.");
	    }
	    if (! new File(configFile).isFile()) {
	    	throw new FileNotFoundException(
	    			String.format("The JSON configfile '%s' was not found.", configFile));
	    }
	    
	    try (BufferedReader br =
                new BufferedReader(new FileReader(configFile))) {
	    	String line = br.readLine();
	    	while (line != null) {
	    		String[] def = line.split("\t");
	    		if (def.length < 2 && line.length() != 0)
	    			throw new IOException(String.format("The JSON configfile '%s' is not valid.", configFile));
	    		String field = def[0];
	    		String[] path = def[1].split(" ");
	    		defs.put(field, Arrays.asList(path));    		
	    		line = br.readLine();
	    	}
	    	if (	defs.get(SENTENCE_BEGIN)== null 
	    			|| defs.get(SENTENCE_END)== null 
	    			|| defs.get(TOKEN_BEGIN)== null 
	    			|| defs.get(TOKEN_END)== null 
	    			||defs.get(TOKEN_POS)== null
	    			||defs.get(TOKENS)== null
	    			||defs.get(SENTENCES)== null)
	    		throw new IOException(String.format("The JSON configfile '%s' is not valid.", configFile));
	    }
	}

	@Override
	public void process(JCas jcas) throws Exception {
		throw new Exception("Missing arguments : sentence annotation file path and part-of-speech annotation file path.");   
	}

	public void process(JCas jcas, String token_annotation_filepath, String sentence_annotation_filepath) throws Exception {
		JSONParser parser = new JSONParser();

		// getting sentences
		List<String> path_to_sentences = defs.get(SENTENCES);
		JSONArray sentences;
		if (!path_to_sentences.get(0).equalsIgnoreCase("None"))
			sentences = (JSONArray) getInfo(parser.parse(new FileReader(sentence_annotation_filepath)), path_to_sentences);
		else 
			sentences= (JSONArray) parser.parse(new FileReader(sentence_annotation_filepath));
		for (Object o : sentences) {
			Sentence s = new Sentence(jcas, (int)(long)getInfo(o, defs.get(SENTENCE_BEGIN)), (int)(long)getInfo(o, defs.get(SENTENCE_END)));
			s.addToIndexes();		
		}
		
		// getting tokens
		List<String> path_to_tokens = defs.get(TOKENS);
		JSONArray tokens ;
		if (!path_to_tokens.get(0).equalsIgnoreCase("None"))
			tokens = (JSONArray) getInfo(parser.parse(new FileReader(token_annotation_filepath)), path_to_tokens);
		else 
			tokens= (JSONArray) parser.parse(new FileReader(token_annotation_filepath));
		for (Object o : tokens) {
			String pos = (String) getInfo(o, defs.get(TOKEN_POS));
			if ( pos != null  
				&& pos.length() > 0
				){
				Token t = new Token(jcas, (int)(long)getInfo(o, defs.get(TOKEN_BEGIN)), (int)(long)getInfo(o, defs.get(TOKEN_END)));
				t.setPos(pos);
				t.addToIndexes();
			}
		}    
	}
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	/**
	 * Navigate through a JSON object until it reaches the needed information
	 * @param o	JSonObject to navigate
	 * @param pathToInfo	List of Strings indicating the path to the information :
	 * 		if the string is a number, it is understood as an index in a JSONArray
	 * 		Otherwise, it is understood as a key of a JSONObject
	 * @return the information contained in the JSON in the form of an object
	 * 
	 * @author Lise Rebout, CRIM
	 */
	protected static Object getInfo (Object o, List<String> pathToInfo) {
    	Object info = o;
		for (String s : pathToInfo) {
    		try {
    			int i = Integer.parseUnsignedInt(s);
    			info = ((JSONArray)info).get(i);
    		} catch (NumberFormatException e) {
    			info = ((JSONObject)info).get(s);
    		}
    	}
		return info;				
	}

}
