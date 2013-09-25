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
import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.impl.ConfigurationManager_impl;
import org.apache.uima.resource.impl.ResourceManager_impl;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * @author Andreas Fay, Julian Zell
 *
 */
public class TreeTaggerWrapper extends JCasAnnotator_ImplBase {
	private Class<?> component = this.getClass();
	
	// definitions of what names these parameters have in the wrapper's descriptor file
	public static final String PARAM_LANGUAGE = "language";
	public static final String PARAM_ANNOTATE_TOKENS = "annotate_tokens";
	public static final String PARAM_ANNOTATE_SENTENCES = "annotate_sentences";
	public static final String PARAM_ANNOTATE_PARTOFSPEECH = "annotate_partofspeech";
	public static final String PARAM_IMPROVE_GERMAN_SENTENCES = "improvegermansentences";
	
	// language for this instance of the treetaggerwrapper
	private Language language;
	
	// switches for annotation parameters
	private Boolean annotate_tokens = false;
	private Boolean annotate_sentences = false;
	private Boolean annotate_partofspeech = false;
	private Boolean improve_german_sentences = false;
	
	// local treetagger properties container, see below
	private TreeTaggerProperties ttprops = new TreeTaggerProperties();
	
	/**
	 * An embedded class that contains all of the treetagger-related settings.
	 * @author Julian Zell
	 *
	 */
	private class TreeTaggerProperties {
		// treetagger language name for par files
		public String languageName = null;
		
		// absolute path of the treetagger
		public String rootPath = null;

		// Files for tokenizer and part of speech tagger (standard values)
		public String tokScriptName = null;
		public String parFileName = null;
		public String abbFileName = null;

		// english, italian, and french tagger models require additional splits (see tagger readme)
		public String languageSwitch = null;

		// perl requires(?) special hint for utf-8-encoded input/output (see http://perldoc.perl.org/perlrun.html#Command-Switches -C)
		// The input text is read in HeidelTimeStandalone.java and always translated into UTF-8,
		// i.e., switch always "-CSD"
		public String utf8Switch = "-CSD";
		
		// save System-specific separators for string generation
		public String newLineSeparator = System.getProperty("line.separator");
		public String fileSeparator = System.getProperty("file.separator");
	}
	
	/**
	 * uimacontext to make secondary initialize() method possible.
	 * -> programmatic, non-uima pipeline usage.
	 * @author julian
	 *
	 */
	private class TreeTaggerContext extends RootUimaContext_impl {
		public TreeTaggerContext(Language language, Boolean annotateTokens, Boolean annotateSentences, 
				Boolean annotatePartOfSpeech, Boolean improveGermanSentences) {
			super();

			// Initialize config
			ConfigurationManager configManager = new ConfigurationManager_impl();

			// Initialize context
			this.initializeRoot(null, new ResourceManager_impl(), configManager);

			// Set session
			configManager.setSession(this.getSession());
			
			// Set necessary variables
			configManager.setConfigParameterValue(makeQualifiedName(PARAM_LANGUAGE), language.getName());
			configManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_TOKENS), annotateTokens);
			configManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_PARTOFSPEECH), annotatePartOfSpeech);
			configManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_SENTENCES), annotateSentences);
			configManager.setConfigParameterValue(makeQualifiedName(PARAM_IMPROVE_GERMAN_SENTENCES), improveGermanSentences);
		}
	}
	
	/**
	 * secondary initialize() to use wrapper outside of a uima pipeline
	 * @param language
	 * @param treeTaggerHome
	 * @param annotateTokens
	 * @param annotateSentences
	 * @param annotatePartOfSpeech
	 * @param improveGermanSentences
	 */
	public void initialize(Language language, String treeTaggerHome, Boolean annotateTokens, 
			Boolean annotateSentences, Boolean annotatePartOfSpeech, Boolean improveGermanSentences) {
		this.setHome(treeTaggerHome);
		
		TreeTaggerContext ttContext = new TreeTaggerContext(language, annotateTokens, 
				annotateSentences, annotatePartOfSpeech, improveGermanSentences);
		
		this.initialize(ttContext); 
		
	}
	
	/**
	 * initialization method where we fill configuration values and check some prerequisites
	 */
	public void initialize(UimaContext aContext) {
		// check if the supplied language is one that we can currently handle
		this.language = Language.getLanguageFromString((String) aContext.getConfigParameterValue(PARAM_LANGUAGE));
		
		// get configuration from the descriptor
		annotate_tokens = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_TOKENS);
		annotate_sentences = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_SENTENCES);
		annotate_partofspeech = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_PARTOFSPEECH);
		improve_german_sentences = (Boolean) aContext.getConfigParameterValue(PARAM_IMPROVE_GERMAN_SENTENCES);
		
		// set some configuration based upon these values
		ttprops.languageName = language.getTreeTaggerLangName();
		if(ttprops.rootPath == null)
			ttprops.rootPath = System.getenv("TREETAGGER_HOME");
		ttprops.tokScriptName = "utf8-tokenize.perl";
		
		// parameter file
		if(!(new File(ttprops.rootPath+ttprops.fileSeparator+"lib", ttprops.languageName + "-utf8.par").exists())) // get UTF8 version if it exists
			ttprops.parFileName = ttprops.languageName + ".par";
		else
			ttprops.parFileName = ttprops.languageName + "-utf8.par";
		
		// abbreviation file
		if(!(new File(ttprops.rootPath+ttprops.fileSeparator+"lib", ttprops.languageName + "-abbreviations-utf8").exists())) // get UTF8 version if it exists
			ttprops.abbFileName = ttprops.languageName + "-abbreviations";
		else
			ttprops.abbFileName = ttprops.languageName + "-abbreviations-utf8";
		
		ttprops.languageSwitch = language.getTreeTaggerSwitch();
		
		// handle the treetagger path from the environment variables
		if(ttprops.rootPath == null) {
			Logger.printError("TreeTagger environment variable is not present, aborting.");
			System.exit(-1);
		}

		// Check for whether the required treetagger parameter files are present
		Boolean abbFileFlag   = true;
		Boolean parFileFlag   = true;
		Boolean tokScriptFlag = true;
		File abbFile = new File(ttprops.rootPath+ttprops.fileSeparator+"lib", ttprops.abbFileName);
		File parFile = new File(ttprops.rootPath+ttprops.fileSeparator+"lib", ttprops.parFileName);
		File tokFile = new File(ttprops.rootPath+ttprops.fileSeparator+"cmd", ttprops.tokScriptName);
		if (!(abbFileFlag = abbFile.exists())) {
			Logger.printError(component, "File missing to use TreeTagger tokenizer: " + ttprops.abbFileName);
		}
		if (!(parFileFlag = parFile.exists())) {
			Logger.printError(component, "File missing to use TreeTagger tokenizer: " + ttprops.parFileName);
		}
		if (!(tokScriptFlag = tokFile.exists())) {
			Logger.printError(component, "File missing to use TreeTagger tokenizer: " + ttprops.tokScriptName);
		}

		if (!abbFileFlag || !parFileFlag || !tokScriptFlag) {
			Logger.printError(component, "Cannot find tree tagger (" + ttprops.rootPath + ttprops.fileSeparator 
					+ "cmd" + ttprops.fileSeparator + ttprops.tokScriptName + ")." +
			" Make sure that path to tree tagger is set correctly in config.props!");
			Logger.printError(component, "If path is set correctly:");
			Logger.printError(component, "Maybe you need to download the TreeTagger tagger-scripts.tar.gz");
			Logger.printError(component, "from http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tagger-scripts.tar.gz");
			Logger.printError(component, "Extract this file and copy the missing file into the corresponding TreeTagger directories.");
			Logger.printError(component, "If missing, copy " + ttprops.abbFileName   + " into " +  ttprops.rootPath+ttprops.fileSeparator+"lib");
			Logger.printError(component, "If missing, copy " + ttprops.parFileName   + " into " +  ttprops.rootPath+ttprops.fileSeparator+"lib");
			Logger.printError(component, "If missing, copy " + ttprops.tokScriptName + " into " +  ttprops.rootPath+ttprops.fileSeparator+"cmd");
			System.exit(-1);
		}
	}
	
	/**
	 * Method that gets called to process the documents' cas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// if the annotate_tokens flag is set, annotate the tokens and add them to the jcas
		if(annotate_tokens)
			tokenize(jcas);

		/* if the annotate_partofspeech flag is set, annotate partofspeech and,
		 * if specified, also tag sentences based upon the partofspeech tags. 
		 */
		if(annotate_partofspeech) 
			doTreeTag(jcas);
		
		// if the improve_german_sentences flag is set, improve the sentence tokens made by the treetagger
		if(improve_german_sentences) 
			improveGermanSentences(jcas);
	}
	
	/**
	 * tokenizes a given JCas object's document text using the treetagger program
	 * and adds the recognized tokens to the JCas object. 
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void tokenize(JCas jcas) {
		BufferedWriter tmpFileWriter = null;

		File tmpDocument = null;

		BufferedReader in = null;

		try {
			// Create temp file containing the document text
			tmpDocument = File.createTempFile("pos", null);
			tmpFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDocument), "UTF-8"));
			tmpFileWriter.write(jcas.getDocumentText());
			tmpFileWriter.close();
			
			// assemble a command line for the tokenization script and execute it
			ArrayList<String> command = new ArrayList<String>();
			command.add("perl");
			if(ttprops.utf8Switch != "")
				command.add(ttprops.utf8Switch);
			command.add(ttprops.rootPath + ttprops.fileSeparator + "cmd" + ttprops.fileSeparator + ttprops.tokScriptName);
			if(ttprops.languageSwitch != "")
				command.add(ttprops.languageSwitch);
			command.add("-a");
			command.add(ttprops.rootPath + ttprops.fileSeparator + "lib" + ttprops.fileSeparator + ttprops.abbFileName);
			command.add(tmpDocument.getAbsolutePath());
			
			String[] commandStr = new String[command.size()];
			command.toArray(commandStr);
			
			Process p = Runtime.getRuntime().exec(commandStr);
			Logger.printDetail(component, "TreeTagger (tokenization) with: " + ttprops.tokScriptName + " and " + ttprops.abbFileName);
			
			// read tokenized text to add tokens to the jcas
			in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			String s;
			int tokenOffset = 0;
			// loop through all the lines in the treetagger output
			while ((s = in.readLine()) != null) {
				// charset missmatch fallback: signal (invalid) s
				if (jcas.getDocumentText().indexOf(s, tokenOffset) < 0)
					throw new RuntimeException("Opps! Could not find token "+s+
							" in JCas after tokenizing with TreeTagger." +
							" Hmm, there may exist a charset missmatch!" +
							" Default encoding is " + Charset.defaultCharset().name() + 
							" and should always be UTF-8 (use -Dfile.encoding=UTF-8)." +
							" If input document is not UTF-8 use -e option to set it according to the input, additionally.");

				// create tokens and add them to the jcas's indexes.
				Token newToken = new Token(jcas);
				newToken.setBegin(jcas.getDocumentText().indexOf(s, tokenOffset));
				newToken.setEnd(newToken.getBegin() + s.length());
				newToken.addToIndexes();
				tokenOffset = newToken.getEnd();
			}
			// clean up
			in.close();
			p.destroy();
			tmpDocument.delete();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// I/O Housekeeping
			if (tmpFileWriter != null) {
				try {
					tmpFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Delete temp files
				tmpDocument.delete();
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
	 * based on tokens from the jcas object, adds part of speech (POS) and sentence
	 * tags to the jcas object using the treetagger program.
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void doTreeTag(JCas jcas) {
		File tmpDocument = null;
		BufferedWriter tmpFileWriter;
		ArrayList<Token> tokens = new ArrayList<Token>();
		
		try {
			// create a temporary file and write our pre-existing tokens to it.
			tmpDocument = File.createTempFile("postokens", null);
			tmpFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDocument), "UTF-8"));

			// iterate over existing tokens
			FSIterator ai = jcas.getAnnotationIndex(Token.type).iterator();
			while(ai.hasNext()) {
				Token t = (Token) ai.next();
				
				tokens.add(t);
				tmpFileWriter.write(t.getCoveredText() + ttprops.newLineSeparator);
			}
			
			tmpFileWriter.close();
		} catch(IOException e) {
			Logger.printError("Something went wrong creating a temporary file for the treetagger to process.");
			System.exit(-1);
		}

		// Possible End-of-Sentence Tags
		HashSet<String> hsEndOfSentenceTag = new HashSet<String>();
		hsEndOfSentenceTag.add("SENT");   // ENGLISH, FRENCH, GREEK, 
		hsEndOfSentenceTag.add("$.");     // GERMAN, DUTCH
		hsEndOfSentenceTag.add("FS");     // SPANISH
		hsEndOfSentenceTag.add("_Z_Fst"); // ESTONIAN
		hsEndOfSentenceTag.add("_Z_Int"); // ESTONIAN
		hsEndOfSentenceTag.add("_Z_Exc"); // ESTONIAN
		
		try {
			// assemble a command line based on configuration and execute the POS tagging.
			ArrayList<String> command = new ArrayList<String>();
			command.add(ttprops.rootPath + ttprops.fileSeparator + "bin" + ttprops.fileSeparator + "tree-tagger");
			command.add(ttprops.rootPath + ttprops.fileSeparator + "lib" + ttprops.fileSeparator + ttprops.parFileName);
			command.add(tmpDocument.getAbsolutePath());
			command.add("-no-unknown");
			
			String[] commandStr = new String[command.size()];
			command.toArray(commandStr);
			
			Process p = Runtime.getRuntime().exec(commandStr);
			Logger.printDetail(component, "TreeTagger (pos tagging) with: " + ttprops.parFileName);
				
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			
			Sentence sentence = null;
			// iterate over all the output lines and tokens array (which have the same source and are hence symmetric)
			int i = 0;
			String s = null;
			while ((s = in.readLine()) != null) {
				// grab a token
				Token token = tokens.get(i++);
				// modified (Aug 29, 2011): Handle empty tokens (such as empty lines) in input file
				while (token.getCoveredText().equals("")){
					token.setPos("");
					token.addToIndexes();
					token = tokens.get(i++);
				}
				// remove tokens, otherwise they are in the index twice
				token.removeFromIndexes(); 
				// set part of speech tag and add to indexes again
				token.setPos(s);
				token.addToIndexes();
				
				// if part of the configuration, also add sentences to the jcas document
				if(annotate_sentences) {
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
			}
			in.close();
			p.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Delete temporary files
			tmpDocument.delete();
		}

	}
	
	public void setHome(String home) {
		this.ttprops.rootPath = home; 
	}

	/**
	 * improve german sentences; the treetagger splits german sentences incorrectly on some occasions
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void improveGermanSentences(JCas jcas) {
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
		
		HashSet<de.unihd.dbs.uima.types.heideltime.Sentence> hsRemoveAnnotations = new HashSet<de.unihd.dbs.uima.types.heideltime.Sentence>();
		HashSet<de.unihd.dbs.uima.types.heideltime.Sentence> hsAddAnnotations    = new HashSet<de.unihd.dbs.uima.types.heideltime.Sentence>();
		
		Boolean changes = true;
		while (changes) {
			changes = false;
			FSIndex annoHeidelSentences = jcas.getAnnotationIndex(de.unihd.dbs.uima.types.heideltime.Sentence.type);
			FSIterator iterHeidelSent   = annoHeidelSentences.iterator();
			while (iterHeidelSent.hasNext()){
				de.unihd.dbs.uima.types.heideltime.Sentence s1 = (de.unihd.dbs.uima.types.heideltime.Sentence) iterHeidelSent.next();
				int substringOffset = java.lang.Math.max(s1.getCoveredText().length()-4,1);
				if (s1.getCoveredText().substring(substringOffset).matches(".*[\\d]+\\.[\\s\\n]*$")){
					if (iterHeidelSent.hasNext()){
						de.unihd.dbs.uima.types.heideltime.Sentence s2 = (de.unihd.dbs.uima.types.heideltime.Sentence) iterHeidelSent.next();
						iterHeidelSent.moveToPrevious();
						for (String beg : hsSentenceBeginnings){
							if (s2.getCoveredText().startsWith(beg)){
								de.unihd.dbs.uima.types.heideltime.Sentence s3 = new de.unihd.dbs.uima.types.heideltime.Sentence(jcas);
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
			for (de.unihd.dbs.uima.types.heideltime.Sentence s : hsRemoveAnnotations){
				s.removeFromIndexes(jcas);
			}
			hsRemoveAnnotations.clear();
			for (de.unihd.dbs.uima.types.heideltime.Sentence s : hsAddAnnotations){
				s.addToIndexes(jcas);
			}
			hsAddAnnotations.clear();
		}
	}
}
