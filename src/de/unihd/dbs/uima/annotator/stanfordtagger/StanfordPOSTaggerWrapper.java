/**
 * This is a preprocessing engine for use in a UIMA pipeline. It will invoke
 * functions from the Stanford POS Tagger to tokenize words and sentences
 * and add part of speech tags to the pipeline.
 */
package de.unihd.dbs.uima.annotator.stanfordtagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

/**
 * @author Julian Zell
 *
 */
public class StanfordPOSTaggerWrapper extends JCasAnnotator_ImplBase {
	private Class<?> component = this.getClass();
	
	// definitions of what names these parameters have in the wrapper's descriptor file
	public static final String PARAM_MODEL_PATH = "model_path";
	public static final String PARAM_CONFIG_PATH = "config_path";
	public static final String PARAM_ANNOTATE_TOKENS = "annotate_tokens";
	public static final String PARAM_ANNOTATE_SENTENCES = "annotate_sentences";
	public static final String PARAM_ANNOTATE_PARTOFSPEECH = "annotate_partofspeech";
	
	// switches for annotation parameters
	private String model_path;
	private String config_path;
	private Boolean annotate_tokens = false;
	private Boolean annotate_sentences = false;
	private Boolean annotate_partofspeech = false;
	
	// Maximum Entropy Tagger from the Stanford POS Tagger
	private MaxentTagger mt;
		
	/**
	 * initialization method where we fill configuration values and check some prerequisites
	 */
	public void initialize(UimaContext aContext) {
		// get configuration from the descriptor
		annotate_tokens = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_TOKENS);
		annotate_sentences = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_SENTENCES);
		annotate_partofspeech = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_PARTOFSPEECH);
		model_path = (String) aContext.getConfigParameterValue(PARAM_MODEL_PATH);
		config_path = (String) aContext.getConfigParameterValue(PARAM_CONFIG_PATH);

		// check if the model file exists
		if(model_path == null || (new File(model_path)).exists() == false) {
			Logger.printError(component, "The supplied model file for the Stanford Tagger could not be found.");
			System.exit(-1);
		}
		
		// try instantiating the MaxEnt Tagger
		try {
			if(config_path != null) { // configuration exists
				FileInputStream isr = new FileInputStream(config_path);
				Properties props = new Properties();
				props.load(isr);
				mt = new MaxentTagger(model_path, new TaggerConfig(props));
			} else { // instantiate without configuration file
				mt = new MaxentTagger(model_path);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.printError(component, "MaxentTagger could not be instantiated with the supplied model/config file.");
			System.exit(-1);
		}
	}
	
	/**
	 * Method that gets called to process the documents' cas objects
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Integer offset = 0; // a cursor of sorts to keep up with the position in the document text
		
		// grab the document text
		String docText = jcas.getDocumentText();
		
		// get [sentence-tokens[word-tokens]] from the MaxentTagger
		List<List<HasWord>> tokenArray = MaxentTagger.tokenizeText(new StringReader(docText));
		
		// iterate over sentences in this document
		for(List<HasWord> sentenceToken : tokenArray) {
			List<TaggedWord> taggedSentence = mt.tagSentence(sentenceToken);
			ListIterator<TaggedWord> twit = taggedSentence.listIterator();
			
			// create a sentence object. gets added to index or discarded depending on configuration
			Sentence sentence = new Sentence(jcas);
			sentence.setBegin(offset);
			
			// iterate over words in this sentence
			for(HasWord wordToken : sentenceToken) {
				Token t = new Token(jcas);
				
				// if pos is supposed to be added, iterate through the tagged tokens and set pos
				if(annotate_partofspeech) {
					TaggedWord tw = twit.next();
					t.setPos(tw.tag());
				}
				
				String thisWord = wordToken.word();
				
				if(docText.indexOf(thisWord, offset) < 0) {
					Logger.printDetail(component, "A previously tagged token wasn't found in the document text: \""+thisWord+"\". " +
							"This may be due to unpredictable punctuation tokenization; hence this token isn't tagged.");
					continue; // jump to next token: discards token
				} else {
					offset = docText.indexOf(thisWord, offset); // set cursor to the starting position of token in docText
					t.setBegin(offset);
				}
				
				offset += thisWord.length(); // move cursor to after the word
				t.setEnd(offset);
				
				// add tokens to indexes.
				if(annotate_tokens) {
					t.addToIndexes();
				}
			}
			
			// if flag is set, also tag sentences
			if(annotate_sentences) {
				sentence.setEnd(offset-1);
				sentence.addToIndexes();
			}
		}
	}
}
