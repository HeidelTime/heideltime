/**
 * This is a preprocessing engine for use in a UIMA pipeline. It will invoke
 * the tree-tagger binary that is supposed to be available on the system
 * through Java process access.
 */
package de.unihd.dbs.uima.annotator.treetagger;

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

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.annotator.heideltime.HeidelTimeException;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * @author Andreas Fay, Julian Zell
 *
 */
public class TreeTaggerWrapper extends JCasAnnotator_ImplBase {
	public static final String PARAM_LANGUAGE = "language";
	public static final String PARAM_IMPROVE_GERMAN_SENTENCES = "improvegermansentences";
	
	private Class<?> component = this.getClass();
	
	private Language language;
	
	private Boolean improveGerman = false;
	
	public void initialize(UimaContext aContext) {
		try {
			this.language = Language.getLanguageFromString((String) aContext.getConfigParameterValue(PARAM_LANGUAGE));
		} catch (HeidelTimeException e) {
			e.printStackTrace();
			Logger.printError(e.getMessage());
			System.exit(-1);
		}
		improveGerman = (Boolean) aContext.getConfigParameterValue(PARAM_IMPROVE_GERMAN_SENTENCES);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
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
		String languageName = language.getTreeTaggerLangName();

		try {
			// Create temp file containing the document text (necessary for tree tagger)
			tmpDocument = File.createTempFile("pos", null);
			tmpFileWriter =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDocument), "UTF-8"));
			tmpFileWriter.write(jcas.getDocumentText());
			tmpFileWriter.close();

			// Establish preconditions for tree tagger
			String rootPath = System.getenv("TREETAGGER_HOME");
			if(rootPath == null) {
				Logger.printError("TreeTagger environment variable is not present, aborting.");
				System.exit(-1);
			}

			// english, italian, and french tagger models require additional splits (see tagger readme)
			String languageSwitch = language.getTreeTaggerSwitch();

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
			if ((language.equals(Language.GERMAN)) && (!(utf8Switch.equals("")))){
				abbFileName = "german-abbreviations-utf8";
				parFileName = "german-utf8.par";
			}
			
			// Check for needed tree tagger files to be available
			// if not available, stop processing!
			Boolean abbFileFlag   = true;
			Boolean parFileFlag   = true;
			Boolean tokScriptFlag = true;
			File abbFile = new File(rootPath+"/lib",abbFileName);
			File parFile = new File(rootPath+"/lib",parFileName);
			File tokFile = new File(rootPath+"/cmd",tokScriptName);
			if (!(abbFile.exists())){
				abbFileFlag = false;
				Logger.printError(component, "File missing to use TreeTagger tokenizer: " + abbFileName);
			}
			if (!(parFile.exists())){
				parFileFlag = false;
				Logger.printError(component, "File missing to use TreeTagger tokenizer: " + parFileName);
			}
			if (!(tokFile.exists())){
				tokScriptFlag = false;
				Logger.printError(component, "File missing to use TreeTagger tokenizer: " + tokScriptName);
			}

			if ((!(abbFileFlag)) || (!(parFileFlag)) || (!(tokScriptFlag))){
				Logger.printError(component, "\nCannot find tree tagger ("+rootPath+"/cmd/" + tokScriptName + ")." +
				" Make sure that path to tree tagger is set correctly in config.props!");
				Logger.printError(component, "\nIf path is set correctly:\n");
				Logger.printError(component, "Maybe you need to download the TreeTagger tagger-scripts.tar.gz");
				Logger.printError(component, "from ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tagger-scripts.tar.gz");
				Logger.printError(component, "Extract this file and copy the missing file into the corresponding TreeTagger directories.");
				Logger.printError(component, "If missing, copy " + abbFileName   + " into " +  rootPath+"/lib");
				Logger.printError(component, "If missing, copy " + parFileName   + " into " +  rootPath+"/lib");
				Logger.printError(component, "If missing, copy " + tokScriptName + " into " +  rootPath+"/cmd");
				System.exit(-1);
			}
			
			// run tokenization
			Process p = null;
			String command = "perl " + utf8Switch + rootPath + "/cmd/" + tokScriptName + " " 
								+ languageSwitch + " -a " 
								+ rootPath + "/lib/" + abbFileName + " " 
								+ tmpDocument;
				
			p = Runtime.getRuntime().exec(command);
			Logger.printDetail(component, "TreeTagger (tokenization) with: " + tokScriptName + " and " + abbFileName);
				
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
				command = rootPath + "/bin/tree-tagger " 
							+ rootPath + "/lib/" + parFileName + " "
							+ tmpTokens	+ " -no-unknown";
				
				p = Runtime.getRuntime().exec(command);
				Logger.printDetail(component, "TreeTagger (pos tagging) with: " + parFileName);
				
//				// print error stream (problems, if system encoding not utf-8)
//				BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//				String e;
//				while ((e = err.readLine()) != null) {
//					System.err.println("TreeTagger (pos tagging) error stream: "+e);
//				}
				
			}catch (IOException ex){
				Logger.printError(component, "Cannot find tree tagger. Make sure that path to tree tagger is set correctly in config.props");
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
			// Housekeeping
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

		if(improveGerman)
			improveGermanSentences(jcas);
	}
	
	public void improveGermanSentences(JCas jcas) {
		// IMPROVE SENTENCE BOUNDARIES (GERMAN SENTENCE SPLITTER)
		HashSet<String> hsSentenceBeginnings = new HashSet<String>();
		
		hsSentenceBeginnings.add("Januar");
		hsSentenceBeginnings.add("Februar");
		hsSentenceBeginnings.add("März");
		hsSentenceBeginnings.add("April");
		hsSentenceBeginnings.add("Mai");
		hsSentenceBeginnings.add("Juni");
		hsSentenceBeginnings.add("Juli");
		hsSentenceBeginnings.add("August");
		hsSentenceBeginnings.add("September");
		hsSentenceBeginnings.add("Oktober");
		hsSentenceBeginnings.add("November");
		hsSentenceBeginnings.add("Dezember");
		hsSentenceBeginnings.add("Jahrhundert");
		hsSentenceBeginnings.add("Jahr");
		hsSentenceBeginnings.add("Monat");
		hsSentenceBeginnings.add("Woche");

		HashSet<Sentence> hsRemoveAnnotations = new HashSet<Sentence>();
		HashSet<Sentence> hsAddAnnotations    = new HashSet<Sentence>();
		
		Boolean changes = true;
		while (changes){
			changes = false;
			FSIndex annoHeidelSentences = jcas.getAnnotationIndex(Sentence.type);
			FSIterator iterHeidelSent   = annoHeidelSentences.iterator();
			while (iterHeidelSent.hasNext()){
				Sentence s1 = (Sentence) iterHeidelSent.next();
				int substringOffset = java.lang.Math.max(s1.getCoveredText().length()-4,1);
				if (s1.getCoveredText().substring(substringOffset).matches(".*[\\d]+\\.[\\s\\n]*$")){
					if (iterHeidelSent.hasNext()){
						Sentence s2 = (Sentence) iterHeidelSent.next();
						iterHeidelSent.moveToPrevious();
						for (String beg : hsSentenceBeginnings){
							if (s2.getCoveredText().startsWith(beg)){
								Sentence s3 = new Sentence(jcas);
								s3.setBegin(s1.getBegin());
								s3.setEnd(s2.getEnd());
								hsAddAnnotations.add(s3);
								hsRemoveAnnotations.add(s1);
								hsRemoveAnnotations.add(s2);
								changes = true;
								break;
							}
						}
					}
				}
			}
			for (Sentence s : hsRemoveAnnotations){
				s.removeFromIndexes(jcas);
			}
			hsRemoveAnnotations.clear();
			for (Sentence s : hsAddAnnotations){
				s.addToIndexes(jcas);
			}
			hsAddAnnotations.clear();
		}
	}

}
