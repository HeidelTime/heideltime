/*
 * TreeTaggerWrapper.java
 *
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, University of Heidelberg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License.
 *
 * authors: Andreas Fay, Jannik Strötgen
 * email:  fay@stud.uni-heidelberg.de, stroetgen@uni-hd.de
 *
 * HeidelTime is a multilingual, cross-domain temporal tagger.
 * For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
 */ 

package de.unihd.dbs.heideltime.standalone.components.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.uima.jcas.JCas;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.Language;
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * Wrapper of tree tagger of the University of Stuttgart. Sets up Token objects
 * with POS-information as well as Sentence objects.
 * 
 * @author Andreas Fay, Jannik Strötgen, Heidelberg University
 * @version 1.01
 */
public class TreeTaggerWrapper implements PartOfSpeechTagger {
	


	@Override
	public void process(JCas jcas, Language language) {

		// Possible End-of-Sentence Tags
		HashSet<String> hsEndOfSentenceTag = new HashSet<String>();
		hsEndOfSentenceTag.add("SENT");   // ENGLISH, FRENCH, GREEK, 
		hsEndOfSentenceTag.add("$.");     // GERMAN, DUTCH
		hsEndOfSentenceTag.add("FS");     // SPANISH
		hsEndOfSentenceTag.add("_Z_Fst"); // ESTONIAN
		hsEndOfSentenceTag.add("_Z_Int"); // ESTONIAN
		hsEndOfSentenceTag.add("_Z_Exc"); // ESTONIAN
		
		BufferedWriter tmpFileWriter = null;

		File tmpDocument = null;
		File tmpTokens = null;

		BufferedReader in = null;
		String languageName = getLanguageName(language);

		try {
			// Create temp file containing the document text (necessary for tree tagger)
			tmpDocument = File.createTempFile("pos", null);
			tmpFileWriter =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDocument), "UTF-8"));
			tmpFileWriter.write(jcas.getDocumentText());
			tmpFileWriter.close();

			// Establish preconditions for tree tagger, i.e. tokenize
			String rootPath = Config.get(Config.TREETAGGERHOME);

			// english, italian, and french tagger models require additional splits (see tagger readme)
			String languageSwitch;
			switch (language) {
				case ENGLISH: languageSwitch = "-e "; break;
				case ITALIAN: languageSwitch = "-i "; break;
				case FRENCH:  languageSwitch = "-f "; break;
				default:      languageSwitch = ""; break;
			} 

			// perl requires(?) special hint for utf-8-encoded input/output (see http://perldoc.perl.org/perlrun.html#Command-Switches -C)
			// The input text is read in HeidelTimeStandalone.java and always translated into UTF-8,
			// i.e., switch always "-CSD "
			String utf8Switch = "-CSD ";
			
			// Files for tokenizer and part of speech tagger (standard values)
			String abbFileName   = languageName + "-abbreviations";
			String tokScriptName = "utf8-tokenize.perl";
			String parFileName   = languageName + ".par";

			// TreeTagger: german abbreviation file for utf-8
			// and 
			// TreeTagger: german par file for utf8
			if ((Language.GERMAN.equals(language)) && (!(utf8Switch.equals("")))){
				abbFileName = "german-abbreviations-utf8";
				parFileName = "german-utf8.par";
			}
			
			// Check for needed tree tagger files to be available
			// if not available, stop processing!
			Boolean abbFileFlag   = true;
			Boolean parFileFlag   = true;
			Boolean tokScriptFlag = true;
			File abbFile = new File(rootPath+"lib",abbFileName);
			File parFile = new File(rootPath+"lib",parFileName);
			File tokFile = new File(rootPath+"cmd",tokScriptName);
			if (!(abbFile.exists())){
				abbFileFlag = false;
				System.err.println("File missing to use TreeTagger tokenizer: " + abbFileName);
			}
			if (!(parFile.exists())){
				parFileFlag = false;
				System.err.println("File missing to use TreeTagger tokenizer: " + parFileName);
			}
			if (!(tokFile.exists())){
				tokScriptFlag = false;
				System.err.println("File missing to use TreeTagger tokenizer: " + tokScriptName);
			}

			if ((!(abbFileFlag)) || (!(parFileFlag)) || (!(tokScriptFlag))){
				System.err.println("\nCannot find tree tagger ("+rootPath+"cmd/" + tokScriptName + ")." +
				" Make sure that path to tree tagger is set correctly in config.props!");
				System.err.println("\nIf path is set correctly:\n");
				System.err.println("Maybe you need to download the TreeTagger tagger-scripts.tar.gz");
				System.err.println("from ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tagger-scripts.tar.gz");
				System.err.println("Extract this file and copy the missing file into the corresponding TreeTagger directories.");
				System.err.println("If missing, copy " + abbFileName   + " into " +  rootPath+"lib");
				System.err.println("If missing, copy " + parFileName   + " into " +  rootPath+"lib");
				System.err.println("If missing, copy " + tokScriptName + " into " +  rootPath+"cmd");
				System.exit(-1);
			}
			
			// run tokenization
			Process p = null;
			String command = "perl " + utf8Switch + rootPath + "cmd/" + tokScriptName + " " 
								+ languageSwitch + "-a " 
								+ rootPath + "lib/" + abbFileName + " " 
								+ tmpDocument;
				
			p = Runtime.getRuntime().exec(command);
			System.err.println("TreeTagger (tokenization) with: " + tokScriptName + " and " + abbFileName);
				
//			// print error stream (problems, if system encoding not utf-8)
//			BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream())); 
//			String e;
//			while ((e = err.readLine()) != null) {
//				System.err.println("TreeTagger (tokenization) error stream: "+e);
//			}
			
			// read tokenized text for pos tagging
			in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));

			// Prepare temp file for tokens and token array
			tmpTokens = File.createTempFile("postokens", null);
			tmpFileWriter =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpTokens), "UTF-8"));
			String s;
			ArrayList<Token> tokens = new ArrayList<Token>();
			int tokenOffset = 0;
			while ((s = in.readLine()) != null) {
				// charset missmatch fallback: signal (invalid) s
				if (jcas.getDocumentText().indexOf(s, tokenOffset) < 0)
					throw new RuntimeException("Opps! Could not find token "+s+
							" in JCas after tokenizing with TreeTagger." +
							" Hmm, there may exist a charset missmatch!" +
							" Default encoding is " + Charset.defaultCharset().name() + 
							" and should always be UTF-8 (use -Dfile.encoding=UTF-8)." +
							" If input document is not UTF-8 use -e option to set it according to the input, additionally.");

				// Write tokens to temp file
				tmpFileWriter.write(s + "\n");

				// Collect tokens
				Token newToken = new Token(jcas);
				newToken.setBegin(jcas.getDocumentText()
						.indexOf(s, tokenOffset));
				newToken.setEnd(newToken.getBegin() + s.length());
				tokenOffset = newToken.getEnd();
				tokens.add(newToken);
				
			}
			in.close();
			tmpFileWriter.close();
			p.destroy();
			tmpDocument.delete();

			// Apply tree tagger
			try{
				command = rootPath + "bin/tree-tagger " 
							+ rootPath + "lib/" + parFileName + " "
							+ tmpTokens	+ " -no-unknown";
				
				p = Runtime.getRuntime().exec(command);
				System.err.println("TreeTagger (pos tagging) with: " + parFileName);
				
//				// print error stream (problems, if system encoding not utf-8)
//				BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//				String e;
//				while ((e = err.readLine()) != null) {
//					System.err.println("TreeTagger (pos tagging) error stream: "+e);
//				}
				
			}catch (IOException ex){
				System.err.println("Cannot find tree tagger. Make sure that path to tree tagger is set correctly in config.props");
				ex.printStackTrace();
				System.exit(-1);
			}
			
			in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			int i = 0;
			Sentence sentence = null;
			while ((s = in.readLine()) != null) {
				// Finalize token information by adding POS-information
				Token token = tokens.get(i++);
				// modified (Aug 29, 2011): Handle empty tokens (such as empty lines) in input file
				while (token.getCoveredText().equals("")){
					token.setPos("");
					token.addToIndexes();
					token = tokens.get(i++);
				}
				token.setPos(s);
				token.addToIndexes();
				
				// Establish sentence structure
				if (sentence == null) {
					sentence = new Sentence(jcas);
					sentence.setBegin(token.getBegin());
				}

				// Finish current sentence if end-of-sentence pos was found or document ended
				if (hsEndOfSentenceTag.contains(s) || i == tokens.size()) {
					sentence.setEnd(token.getEnd());
					sentence.addToIndexes();
					
					// Make sure current sentence is not active anymore so that a new one might be created
					sentence = null;
				}
			}
			in.close();
			p.destroy();
			tmpTokens.delete();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Householding
			if (tmpFileWriter != null) {
				try {
					tmpFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Delete temp files
				tmpDocument.delete();
				tmpTokens.delete();
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Get language name used by tree tagger based on used language
	 * 
	 * @param language
	 * @return
	 */
	private String getLanguageName(Language language) {
		switch (language) {
		default:
			return language.toString();

		case ENGLISH:
			return "english";
			
		case
			ENGLISHCOLL:
			return "english";
		
		case
			ENGLISHSCI:
			return "english";
		
		case GERMAN:
			return "german";
			
		case DUTCH:
			return "dutch";
		}
	}
}