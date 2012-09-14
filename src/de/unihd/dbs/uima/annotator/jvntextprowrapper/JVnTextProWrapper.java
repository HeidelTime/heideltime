/**
 * This is a preprocessing engine for use in a UIMA pipeline. It will invoke
 * the JVnTextPro api that is supposed to be available in the classpath.
 */
package de.unihd.dbs.uima.annotator.jvntextprowrapper;

import jvntextpro.JVnTextPro;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * @author Julian Zell
 *
 */
public class JVnTextProWrapper extends JCasAnnotator_ImplBase {
	private Class<?> component = this.getClass();

	// definitions of what names these parameters have in the wrapper's descriptor file
	public static final String PARAM_SENTSEGMODEL_PATH = "sent_model_path";
	public static final String PARAM_WORDSEGMODEL_PATH = "word_model_path";
	public static final String PARAM_POSMODEL_PATH = "pos_model_path";
	public static final String PARAM_ANNOTATE_TOKENS = "annotate_tokens";
	public static final String PARAM_ANNOTATE_SENTENCES = "annotate_sentences";
	public static final String PARAM_ANNOTATE_PARTOFSPEECH = "annotate_partofspeech";
	
	// switches for annotation parameters
	private Boolean annotate_tokens = false;
	private Boolean annotate_sentences = false;
	private Boolean annotate_partofspeech = false;
	private String sentModelPath = null;
	private String wordModelPath = null;
	private String posModelPath = null;
	
	// private jvntextpro object
	private JVnTextPro jvtp = null;
	
	/**
	 * initialization method where we fill configuration values and check some prerequisites
	 */
	public void initialize(UimaContext aContext) {
		// get configuration from the descriptor
		annotate_tokens = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_TOKENS);
		annotate_sentences = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_SENTENCES);
		annotate_partofspeech = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_PARTOFSPEECH);
		sentModelPath = (String) aContext.getConfigParameterValue(PARAM_SENTSEGMODEL_PATH);
		wordModelPath = (String) aContext.getConfigParameterValue(PARAM_WORDSEGMODEL_PATH);
		posModelPath = (String) aContext.getConfigParameterValue(PARAM_POSMODEL_PATH);
		
		jvtp = new JVnTextPro();
		
		if(sentModelPath != null)
			if(!jvtp.initSenSegmenter(sentModelPath)) {
				Logger.printError(component, "Error initializing the sentence segmenter model: "+sentModelPath);
				System.exit(-1);
			}
		
		if(wordModelPath != null) 
			if(!jvtp.initSegmenter(wordModelPath)) {
				Logger.printError(component, "Error initializing the word segmenter model: "+wordModelPath);
				System.exit(-1);
			}
		
		if(posModelPath != null) 
			if(!jvtp.initPosTagger(posModelPath)) {
				Logger.printError(component, "Error initializing the POS tagging model: "+posModelPath);
				System.exit(-1);
			}
	}
	
	/**
	 * Method that gets called to process the documents' cas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String origText = jcas.getDocumentText();
		
		Integer offset = 0;
		
		String[] sentStrings = jvtp.process(origText).split("\n");
		
		// iterate over sentence strings
		for(String sentString : sentStrings) {
			Sentence sentence = new Sentence(jcas);
			Boolean hasSentBegin = false;

			String[] tokenStrings = sentString.split(" ");
			// iterate over word strings
			for(String tokenString : tokenStrings) {
				Token token = new Token(jcas);
				
				String word = new String();
				String tag = new String();
				
				// special case if the token is "/", delimited by "/", tagged as "/" => "///" in text
				if(tokenString.equals("///")) {
					Integer beginning = origText.indexOf("/", offset);
					token.setBegin(beginning);
					token.setEnd(beginning+1);
					offset = beginning+1;
				} else if(tokenString.matches(".+/.+")) { // assume that the last found "/" is the postag-delimiter
					Integer delimPos = tokenString.lastIndexOf("/");
					word = tokenString.substring(0, delimPos);
					tag = tokenString.substring(delimPos+1);
					
					Boolean hasBegin = false;
					
					String[] inTokenWords = word.split("_");
					// iterate over sub-words, i.e. word = "armadillo_animal/N" => "armadillo", "animal"
					for(String subWord : inTokenWords) {
						offset = origText.indexOf(subWord, offset); // set offset to occurrence in original text
						
						if(hasSentBegin == false) { // beginning of the pos-tagged sentence
							sentence.setBegin(offset);
							hasSentBegin = true;
						}
						
						if(hasBegin == false) { // beginning of the pos-tagged token
							token.setBegin(offset);
							hasBegin = true;
						}

						offset = origText.indexOf(subWord, offset) + subWord.length(); // offset is now behind the word
						
						token.setEnd(offset); // word-token gets final value from the last sub-word
						sentence.setEnd(offset); // sentence gets final value from the last sub-word
					}
					
					if(annotate_partofspeech) // if flag is true, then add pos info to indexes
						token.setPos(tag);
					
					if(annotate_tokens) // if flag is true, then add this token to indexes
						token.addToIndexes();
					
				} else { // otherwise, the tagger gave us something we don't understand (yet?)
					continue; // jump to next token
				}
			}
			
			if(annotate_sentences) // if flag is true, then add sentence token to indexes
				sentence.addToIndexes();
		}
	}
}
