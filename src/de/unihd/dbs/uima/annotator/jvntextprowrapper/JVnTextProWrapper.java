/**
 * This is a preprocessing engine for use in a UIMA pipeline. It will invoke
 * the JVnTextPro api that is supposed to be available in the classpath.
 */
package de.unihd.dbs.uima.annotator.jvntextprowrapper;

import java.io.File;
import java.util.List;

import jmaxent.Classification;
import jvnpostag.POSContextGenerator;
import jvnpostag.POSDataReader;
import jvnsegmenter.CRFSegmenter;
import jvnsensegmenter.JVnSenSegmenter;
import jvntextpro.JVnTextPro;
import jvntextpro.conversion.CompositeUnicode2Unicode;
import jvntextpro.data.DataReader;
import jvntextpro.data.TaggingData;
import jvntextpro.util.StringUtils;
import jvntokenizer.PennTokenizer;

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
	
	// private jvntextpro objects
	JVnSenSegmenter vnSenSegmenter = new JVnSenSegmenter();
	CRFSegmenter vnSegmenter = new CRFSegmenter();
	DataReader reader = new POSDataReader();
	TaggingData dataTagger = new TaggingData();
	Classification classifier = null;
	
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
		
		if(sentModelPath != null)
			if(!vnSenSegmenter.init(sentModelPath)) {
				Logger.printError(component, "Error initializing the sentence segmenter model: " + sentModelPath);
				System.exit(-1);
			}
		
		if(wordModelPath != null) 
			try {
				vnSegmenter.init(wordModelPath);
			} catch(Exception e) {
				Logger.printError(component, "Error initializing the word segmenter model: " + wordModelPath);
				System.exit(-1);
			}
		
		if(posModelPath != null) 
			try {
				dataTagger.addContextGenerator(new POSContextGenerator(posModelPath + File.separator + "featuretemplate.xml"));
				classifier = new Classification(posModelPath);	
			} catch(Exception e) {
				Logger.printError(component, "Error initializing the POS tagging model: " + posModelPath);
				System.exit(-1);
			}
	}
	
	/**
	 * Method that gets called to process the documents' cas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		CompositeUnicode2Unicode convertor = new CompositeUnicode2Unicode();
		String origText = jcas.getDocumentText();
		
		Integer offset = 0;
		
		/*
		 * partially taken from of JVnTextPro.java's process(String)
		 */
		String workedText = convertor.convert(origText);
		workedText = vnSenSegmenter.senSegment(workedText).trim();
		workedText = PennTokenizer.tokenize(workedText).trim();
		workedText = vnSegmenter.segmenting(workedText);
		workedText = (new JVnTextPro()).postProcessing(workedText).trim();
		List<jvntextpro.data.Sentence> sentences = jvnTagging(workedText);
		
		/*
		 * iterate over sentences given back by the JVnTextPro tagging method
		 */
		for(Integer i = 0; i < sentences.size(); ++i) {
			jvntextpro.data.Sentence sent = sentences.get(i);
			
			Sentence sentence = new Sentence(jcas);
			Boolean hasSentBegin = false;
			
			/*
			 * iterate over words within sentence
			 */
			for(Integer j = 0; j < sent.size(); ++j) {
				String word = sent.getWordAt(j);
				String tag = sent.getTagAt(j);
				Token token = new Token(jcas);
				
				if(word == null || word.length() < 1 || word.equals("_"))
					continue;
				
				Boolean hasBegin = false;
				String[] inTokenWords = word.split("_");
				/*
				 * iterate over sub-words, i.e. word = "armadillo_animal/N" => "armadillo", "animal"
				 */
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
				}
				
				/*
				 * call our sanitation routine that splits off punctuation marks from the end and
				 * the beginning of the token and creates additional tokens for each of them
				 */
				sanitizeToken(token, jcas);
				
				if(annotate_partofspeech) // if flag is true, then add pos info to indexes
					token.setPos(tag);
				
				if(annotate_tokens) // if flag is true, then add this token to indexes
					token.addToIndexes();
			}
			
			sentence.setEnd(offset);

			if(annotate_sentences) // if flag is true, then add sentence token to indexes
				sentence.addToIndexes();
		}
	}
	
	private Boolean sanitizeToken(Token t, JCas jcas) {
		Boolean workDone = false;
		
		// check the beginning of the token for punctuation and split off into a new token
		if(t.getCoveredText().matches("^\\p{Punct}.*") && t.getCoveredText().length() > 1) {
			Character thisChar = t.getCoveredText().charAt(0);
			t.setBegin(t.getBegin() + 1); // set corrected token boundary for the word
			Token puncToken = new Token(jcas); // create a new token for the punctuation character
			puncToken.setBegin(t.getBegin() - 1);
			puncToken.setEnd(t.getBegin());
			// check if we want to annotate pos or the token itself
			if(annotate_partofspeech)
				puncToken.setPos(""+thisChar);
			if(annotate_tokens)
				puncToken.addToIndexes();
			
			workDone = true;
		}
		
		// check the end of the token for punctuation and split off into a new token
		if(t.getCoveredText().matches(".*\\p{Punct}$") && t.getCoveredText().length() > 1) {
			Character thisChar = t.getCoveredText().charAt(t.getEnd() - t.getBegin() - 1);
			t.setEnd(t.getEnd() - 1); // set corrected token boundary for the word
			Token puncToken = new Token(jcas); // create a new token for the punctuation character
			puncToken.setBegin(t.getEnd());
			puncToken.setEnd(t.getEnd() + 1);
			// check if we want to annotate pos or the token itself
			if(annotate_partofspeech)
				puncToken.setPos(""+thisChar);
			if(annotate_tokens)
				puncToken.addToIndexes();
			
			workDone = true;
		}
		
		// get into a recursion to sanitize tokens as long as there are stray ones
		if(workDone) {
			workDone = sanitizeToken(t, jcas);
		}
		
		return workDone;
	}
	
	/**
	 * Taken from the JVnTextPro package and adapted to not output a string
	 * @param instr input string to be tagged
	 * @return tagged text
	 */
	public List<jvntextpro.data.Sentence> jvnTagging(String instr) {
		List<jvntextpro.data.Sentence> data = reader.readString(instr);
		for (int i = 0; i < data.size(); ++i) {
        	
			jvntextpro.data.Sentence sent = data.get(i);
    		for (int j = 0; j < sent.size(); ++j) {
    			String [] cps = dataTagger.getContext(sent, j);
    			String label = classifier.classify(cps);
    			
    			if (label.equalsIgnoreCase("Mrk")) {
    				if (StringUtils.isPunc(sent.getWordAt(j)))
    					label = sent.getWordAt(j);
    				else label = "X";
    			}
    			
    			sent.getTWordAt(j).setTag(label);
    		}
    	}
		
		return data;
	}
}
