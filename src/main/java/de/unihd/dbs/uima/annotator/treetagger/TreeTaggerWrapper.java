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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.impl.ConfigurationManager_impl;
import org.apache.uima.resource.impl.ResourceManager_impl;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;
import de.unihd.dbs.uima.annotator.treetagger.TreeTaggerTokenizer.Flag;

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
	public static final String PARAM_CHINESE_TOKENIZER_PATH = "ChineseTokenizerPath";
	
	// language for this instance of the treetaggerwrapper
	private Language language;
	
	// switches for annotation parameters
	private Boolean annotate_tokens = false;
	private Boolean annotate_sentences = false;
	private Boolean annotate_partofspeech = false;
	
	// local treetagger properties container, see below
	private TreeTaggerProperties ttprops = new TreeTaggerProperties();
	private TreeTaggerProcess ttProc = null;
	
	// processing threads for I/O
	private TreeTaggerWriter ttwriter;
	private TreeTaggerReader ttreader;
	
	/**
	 * uimacontext to make secondary initialize() method possible.
	 * -> programmatic, non-uima pipeline usage.
	 * @author julian
	 *
	 */
	private class TreeTaggerContext extends RootUimaContext_impl {
		private ConfigurationManager mConfigManager;
		
		// shorthand for when we don't want to supply a cnTokPath
		@SuppressWarnings("unused")
		public TreeTaggerContext(Language language, Boolean annotateTokens, Boolean annotateSentences, 
				Boolean annotatePartOfSpeech, Boolean improveGermanSentences) {
			this(language, annotateTokens, annotateSentences, annotatePartOfSpeech, 
					improveGermanSentences, null);
		}
		
		public TreeTaggerContext(Language language, Boolean annotateTokens, Boolean annotateSentences, 
				Boolean annotatePartOfSpeech, Boolean improveGermanSentences, String cnTokPath) {
			super();

			// Initialize config
			mConfigManager = new ConfigurationManager_impl();

			// Initialize context
			this.initializeRoot(null, new ResourceManager_impl(), mConfigManager);

			// Set session
			mConfigManager.setSession(this.getSession());
			
			// Set necessary variables
			mConfigManager.setConfigParameterValue(makeQualifiedName(PARAM_LANGUAGE), language.getName());
			mConfigManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_TOKENS), annotateTokens);
			mConfigManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_PARTOFSPEECH), annotatePartOfSpeech);
			mConfigManager.setConfigParameterValue(makeQualifiedName(PARAM_ANNOTATE_SENTENCES), annotateSentences);
			mConfigManager.setConfigParameterValue(makeQualifiedName(PARAM_CHINESE_TOKENIZER_PATH), cnTokPath);
		}
		
		@Override
		public ConfigurationManager getConfigurationManager() {
			return mConfigManager;
		}
	}
	
	/**
	 * secondary initialize() to use wrapper outside of a uima pipeline
	 * shorthand for when we don't want to specify a cnTokPath
	 */
	public void initialize(Language language, String treeTaggerHome, Boolean annotateTokens, 
			Boolean annotateSentences, Boolean annotatePartOfSpeech, Boolean improveGermanSentences) {
		this.initialize(language, treeTaggerHome, annotateTokens, annotateSentences, annotatePartOfSpeech,
				improveGermanSentences, null);
	}
	
	/**
	 * secondary initialize() to use wrapper outside of a uima pipeline
	 * 
	 * @param language Language/parameter file to use for the TreeTagger
	 * @param treeTaggerHome Path to the TreeTagger folder
	 * @param annotateTokens Whether to annotate tokens
	 * @param annotateSentences Whether to annotate sentences
	 * @param annotatePartOfSpeech Whether to annotate POS tags
	 * @param improveGermanSentences Whether to do improvements for german sentences
	 */
	public void initialize(Language language, String treeTaggerHome, Boolean annotateTokens, 
			Boolean annotateSentences, Boolean annotatePartOfSpeech, Boolean improveGermanSentences, String cnTokPath) {
		this.setHome(treeTaggerHome);
		
		TreeTaggerContext ttContext = new TreeTaggerContext(language, annotateTokens, 
				annotateSentences, annotatePartOfSpeech, improveGermanSentences, cnTokPath);
		
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
		String cnTokPath = (String) aContext.getConfigParameterValue(PARAM_CHINESE_TOKENIZER_PATH);
		
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
		if(new File(ttprops.rootPath+ttprops.fileSeparator+"lib", ttprops.languageName + "-abbreviations-utf8").exists()) { // get UTF8 version if it exists
			ttprops.abbFileName = ttprops.languageName + "-abbreviations-utf8";
		} else {
			ttprops.abbFileName = ttprops.languageName + "-abbreviations";
		}
		
		ttprops.languageSwitch = language.getTreeTaggerSwitch();
		if(cnTokPath != null && !cnTokPath.equals(""))
			ttprops.chineseTokenizerPath = new File(cnTokPath);
		else
			ttprops.chineseTokenizerPath = new File(ttprops.rootPath, "cmd");
		
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
			if(language.equals(Language.CHINESE) || language.equals(Language.RUSSIAN)) {
				abbFileFlag = true;
				ttprops.abbFileName = null;
			} else {
				Logger.printError(component, "File missing to use TreeTagger tokenizer: " + ttprops.abbFileName);
			}
		}
		if (!(parFileFlag = parFile.exists())) {
			Logger.printError(component, "File missing to use TreeTagger tokenizer: " + ttprops.parFileName);
		}
		if (!(tokScriptFlag = tokFile.exists())) {
			if(language.equals(Language.CHINESE))
				tokScriptFlag = true;
			else
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
			if(language.equals(Language.CHINESE))
				tokenizeChinese(jcas); // chinese needs different tokenization
			else
				tokenize(jcas);

		/* if the annotate_partofspeech flag is set, annotate partofspeech and,
		 * if specified, also tag sentences based upon the partofspeech tags. 
		 */
		if(annotate_partofspeech) 
			doTreeTag(jcas);
		
		// if the improve_german_sentences flag is set, improve the sentence tokens made by the treetagger
		if(this.language == Language.GERMAN)
			improveGermanSentences(jcas);
		
		// if French, improve the sentence tokens made by the TreeTagger with settings for French
		if (this.language == Language.FRENCH)
			improveFrenchSentences(jcas);
		
	}
	
	/**
	 * tokenizes a given JCas object's document text using the treetagger program
	 * and adds the recognized tokens to the JCas object. 
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void tokenize(JCas jcas) {
		// read tokenized text to add tokens to the jcas
		Logger.printDetail(component, "TreeTagger (tokenization) with: " + ttprops.abbFileName);
		
		EnumSet<Flag> flags = Flag.getSet(ttprops.languageSwitch);
		TreeTaggerTokenizer ttt; ttprops.abbFileName = "english-abbreviations";
		if(ttprops.abbFileName != null) {
			ttt = new TreeTaggerTokenizer(ttprops.rootPath + ttprops.fileSeparator + "lib" + ttprops.fileSeparator + ttprops.abbFileName, flags);
		} else {
			ttt = new TreeTaggerTokenizer(null, flags);
		}
		
		String docText = jcas.getDocumentText().replaceAll("\n\n", "\nEMPTYLINE\n");
		List<String> tokenized = ttt.tokenize(docText);
		
		int tokenOffset = 0;
		// loop through all the lines in the treetagger output
		for(String s : tokenized) {
			// charset missmatch fallback: signal (invalid) s
			if ((!(s.equals("EMPTYLINE"))) && (jcas.getDocumentText().indexOf(s, tokenOffset) < 0)) {
				Logger.printError(component, "Tokenization was interrupted because the token \"" + s 
						+ "\" could not be found in the original text. The reason for this might be "
						+ "that the encoding of the document is not UTF-8. This token was skipped and "
						+ "if it was part of a temporal expression, will not be extracted.");
				continue;
			}
			// create tokens and add them to the jcas's indexes.
			Token newToken = new Token(jcas);
			if (s.equals("EMPTYLINE")){
				newToken.setBegin(tokenOffset);
				newToken.setEnd(tokenOffset);
				newToken.setPos("EMPTYLINE");
				if (annotate_partofspeech){
					newToken.addToIndexes();
				}
			}
			else{
				newToken.setBegin(jcas.getDocumentText().indexOf(s, tokenOffset));
				newToken.setEnd(newToken.getBegin() + s.length());
				newToken.addToIndexes();
				tokenOffset = newToken.getEnd();
			}
		}
	}
	
	/**
	 * tokenizes a given JCas object's document text using the chinese tokenization
	 * script and adds the recognized tokens to the JCas object. 
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void tokenizeChinese(JCas jcas) {
		try {
			// read tokenized text to add tokens to the jcas
			Process proc = ttprops.getChineseTokenizationProcess();
			Logger.printDetail(component, "Chinese tokenization: " + ttprops.chineseTokenizerPath);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), "UTF-8"));
			
			Integer tokenOffset = 0;
			// loop through all the lines in the stdout output
			String[] inSplits = jcas.getDocumentText().split("[\\r\\n]+");
			for(String inSplit : inSplits) {
				out.write(inSplit);
				out.newLine();
				out.flush();
				
				// do one initial read
				String s = in.readLine();
				do {
					// break out of the loop if we've read a null
					if(s == null)
						break;
					
					String[] outSplits = s.split("\\s+");
					for(String tok : outSplits) {
						if(jcas.getDocumentText().indexOf(tok, tokenOffset) < 0)
							throw new RuntimeException("Could not find token " + tok +
									" in JCas after tokenizing with Chinese tokenization script.");
						
						// create tokens and add them to the jcas's indexes.
						Token newToken = new Token(jcas);
						newToken.setBegin(jcas.getDocumentText().indexOf(tok, tokenOffset));
						newToken.setEnd(newToken.getBegin() + tok.length());
						newToken.addToIndexes();
						tokenOffset = newToken.getEnd();
					}
					
					// break out of the loop if the next read will block
					if(!in.ready())
						break;
					
					s = in.readLine();
				} while(true);
			}
			
			// clean up
			in.close();
			proc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * based on tokens from the jcas object, adds part of speech (POS) and sentence
	 * tags to the jcas object using the treetagger program.
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void doTreeTag(JCas jcas) {
		try {
			if(ttProc == null) {
				ttProc = new TreeTaggerProcess(ttprops.getTreeTaggingProcess());
			}
			
			Logger.printDetail(component, "TreeTagger (pos tagging) with: " + ttprops.parFileName);
			
			AnnotationIndex ai = jcas.getAnnotationIndex(Token.type);
			List<String> tokenStrings = new ArrayList<>();
			List<Token> tokens = new ArrayList<>();
			for(FSIterator fsi = ai.iterator(); fsi.hasNext();) {
				Token token = (Token) fsi.next();
				tokenStrings.add(token.getCoveredText());
				tokens.add(token);
			}
			
			ttreader = new TreeTaggerReader(tokens, ttProc.getStdout(), jcas, annotate_sentences);
			ttwriter = new TreeTaggerWriter(tokenStrings, ttProc.getStdin());
			
			Thread rThread = new Thread(ttreader);
			Thread wThread = new Thread(ttwriter);
			
			rThread.start();
			wThread.start();
			
			rThread.join();
			wThread.join();
		} catch(IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * based on tokens from the jcas object, adds part of speech (POS) and sentence
	 * tags to the jcas object using the treetagger program.
	 * @param jcas JCas object supplied by the pipeline
	 */
	@SuppressWarnings({"unused"})
	private void doTreeTagOld(JCas jcas) {
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
				if (!(t.getBegin() == t.getEnd())){
					tmpFileWriter.write(t.getCoveredText() + ttprops.newLineSeparator);
				}
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
		hsEndOfSentenceTag.add("ew"); // CHINESE
		
		try {
			Process p = ttprops.getTreeTaggingProcess(tmpDocument);
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
					// if part of the configuration, also add sentences to the jcas document
					if ((annotate_sentences) && (token.getPos() != null && token.getPos().equals("EMPTYLINE"))) {
						// Establish sentence structure
						if (sentence == null) {
							sentence = new Sentence(jcas);
							sentence.setBegin(token.getBegin());
						}
		
						// Finish current sentence if end-of-sentence pos was found or document ended
						sentence.setEnd(token.getEnd());
						if (sentence.getBegin() < sentence.getEnd()){
							sentence.addToIndexes();
						}
						
						// Make sure current sentence is not active anymore so that a new one might be created
						sentence = null;
//						sentence = new Sentence(jcas);
					}
					token.removeFromIndexes();
					token = tokens.get(i++);
				}
				// remove tokens, otherwise they are in the index twice
				token.removeFromIndexes(); 
				// set part of speech tag and add to indexes again
				if (!(token.getCoveredText().equals(""))){
					token.setPos(s);
					token.addToIndexes();
				}
				
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
			while (i < tokens.size()){
				if (!(sentence == null)){
					sentence.setEnd(tokens.get(tokens.size()-1).getEnd());
					sentence.addToIndexes();
				}
				Token token = tokens.get(i++);
				if (token.getPos() != null && token.getPos().equals("EMPTYLINE")){
					token.removeFromIndexes();
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
	
	private void improveFrenchSentences(JCas jcas) {
		HashSet<de.unihd.dbs.uima.types.heideltime.Sentence> hsRemoveAnnotations = new HashSet<de.unihd.dbs.uima.types.heideltime.Sentence>();
		HashSet<de.unihd.dbs.uima.types.heideltime.Sentence> hsAddAnnotations    = new HashSet<de.unihd.dbs.uima.types.heideltime.Sentence>();
		
		HashSet<String> hsSentenceBeginnings = new HashSet<String>();
		hsSentenceBeginnings.add("J.-C.");
		hsSentenceBeginnings.add("J-C.");
		hsSentenceBeginnings.add("NSJC");
		
		Boolean changes = true;
		while (changes) {
			changes = false;
			FSIndex annoHeidelSentences = jcas.getAnnotationIndex(de.unihd.dbs.uima.types.heideltime.Sentence.type);
			FSIterator iterHeidelSent   = annoHeidelSentences.iterator();
			while (iterHeidelSent.hasNext()){
				de.unihd.dbs.uima.types.heideltime.Sentence s1 = (de.unihd.dbs.uima.types.heideltime.Sentence) iterHeidelSent.next();
				
				if ((s1.getCoveredText().endsWith("av.")) ||
						(s1.getCoveredText().endsWith("Av.")) ||
						(s1.getCoveredText().endsWith("apr.")) ||
						(s1.getCoveredText().endsWith("Apr.")) ||
						(s1.getCoveredText().endsWith("avant.")) ||
						(s1.getCoveredText().endsWith("Avant."))){
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
		
	

	/**
	 * improve german sentences; the treetagger splits german sentences incorrectly on some occasions
	 * @param jcas JCas object supplied by the pipeline
	 */
	private void improveGermanSentences(JCas jcas) {
		/* 
		 * these POS tag sequences will decide whether we want to merge two sentences
		 * that have (supposedly wrongfully) been split.
		 */
		HashSet<String[]> posRules = new HashSet<String[]>();
		posRules.add(new String[] {"CARD", "\\$.", "NN"});
		posRules.add(new String[] {"CARD", "\\$.", "NE"});
		
		FSIterator sentIter = jcas.getAnnotationIndex(Sentence.type).iterator();
		
		// compare two sentences at a time in order to have access to all POS tags
		HashSet<HashSet<Sentence>> toMerge = new HashSet<HashSet<Sentence>>();
		Sentence prevSent = null, thisSent = null;
		while(sentIter.hasNext()) {
			if(thisSent == null) {
				thisSent = (Sentence) sentIter.next();
				continue;
			}
			
			prevSent = thisSent;
			thisSent = (Sentence) sentIter.next();
			/* 
			 * select the last two tokens within the previous sentence as well as the
			 * first of the current one and check for matches.
			 */
			Token penultimateToken = null, ultimateToken = null, firstToken = null;
			FSIterator tokIter = jcas.getAnnotationIndex(Token.type).subiterator(thisSent);
			if(tokIter.hasNext()) {
				firstToken = (Token) tokIter.next();
			}
			
			tokIter = jcas.getAnnotationIndex(Token.type).subiterator(prevSent);
			while(tokIter.hasNext()) {
				if(ultimateToken == null) {
					ultimateToken = (Token) tokIter.next();
					continue;
				}
				penultimateToken = ultimateToken;
				ultimateToken = (Token) tokIter.next();
			}
			
			// check that all tokens for further analysis are present. if not: skip
			if(penultimateToken == null || ultimateToken == null || firstToken == null) {
				continue;
			}
			
			// check rules, memorize sentences to be merged
			for(String[] posRule : posRules) {
				/* 
				 * either one of the pre-defined POS rules fit, or the first token's 
				 * covered text begins with lower case characters.
				 */
				if((penultimateToken.getPos() != null && penultimateToken.getPos().matches(posRule[0]) &&
						ultimateToken.getPos() != null && ultimateToken.getPos().matches(posRule[1]) &&
						firstToken.getPos() != null && firstToken.getPos().matches(posRule[2]))
						||
						(firstToken.getCoveredText().matches("^[a-z/].*"))) {
					/* 
					 * check whether one of the previous candidate pairs already 
					 * contains one of our sentences.
					 */
					Boolean candidateExisted = false;
					for(HashSet<Sentence> mergeCandidate : toMerge) {
						if(mergeCandidate.contains(thisSent) || mergeCandidate.contains(prevSent)) {
							// we add both here because sets ignore duplicates
							mergeCandidate.add(prevSent);
							mergeCandidate.add(thisSent);
							
							candidateExisted = true;
							break;
						}
					}
					
					/* 
					 * if one of the sentences was not already to be merged with another,
					 * create a new merge candidate set
					 */
					if(!candidateExisted) {
						HashSet<Sentence> newCandidate = new HashSet<Sentence>();
						newCandidate.add(prevSent);
						newCandidate.add(thisSent);
						
						toMerge.add(newCandidate);
					}
					
					break; // don't need to do the next rules; already merging.
				}
			}
		}
		
		// iterate over the previously collected merge candidates
		
		for(HashSet<Sentence> mergeCandidate : toMerge) {
			// find the earliest beginning and latest end for the set of sentences
			Integer beginIndex = Integer.MAX_VALUE, endIndex = Integer.MIN_VALUE;

			Sentence mergedSent = new Sentence(jcas);
			for(Sentence s : mergeCandidate) {
				if(s.getBegin() < beginIndex) {
					beginIndex = s.getBegin();
				}
				
				if(s.getEnd() > endIndex) {
					endIndex = s.getEnd();
				}
				
				s.removeFromIndexes();
			}
			
			// set values, add to jcas
			mergedSent.setBegin(beginIndex);
			mergedSent.setEnd(endIndex);
			mergedSent.addToIndexes();
		}
	}
	
	public void quit() {
		ttProc.close();
		ttProc = null;
	}
}
