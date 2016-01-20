package de.unihd.dbs.heideltime.standalone.components.impl;

import java.util.Properties;

import org.apache.uima.jcas.JCas;

import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import de.unihd.dbs.uima.annotator.alllanguagestokenizer.AllLanguagesTokenizer;

public class AllLanguagesTokenizerWrapper implements PartOfSpeechTagger {
	
	private AllLanguagesTokenizer alt = new AllLanguagesTokenizer();

	@Override
	public void initialize(Properties settings) {
		// nothing to do since this preprocessor is language-agnostic
	}

	@Override
	public void process(JCas jcas) {
		try {
			alt.process(jcas);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset() {
		// not necessary
	}

}
