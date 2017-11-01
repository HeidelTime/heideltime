package de.unihd.dbs.heideltime.standalone.components.impl;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

public class JSONTaggerWrapperTest {

	private JSONTaggerWrapper wrapper;
	private JCasFactory jcasFactory;

	@Before
	public void setUp() throws Exception {
		
		
		TypeSystemDescription[] typeSystemDescriptions = new TypeSystemDescription[] {
				UIMAFramework
						.getXMLParser()
						.parseTypeSystemDescription(
								new XMLInputSource(ClassLoader.getSystemResource("HeidelTime_TypeSystem.xml").getPath()
										)) };
		jcasFactory= new JCasFactoryImpl(typeSystemDescriptions);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProcess() throws Exception {
		Properties settings = new Properties();
		settings.put(PartOfSpeechTagger.JSON_CONFIG_PATH, ClassLoader.getSystemResource("config_JSONReader.txt").getPath());
		
		wrapper = new JSONTaggerWrapper();
		wrapper.initialize(settings);
		
		
		JCas jcas = jcasFactory.createJCas();
		String doc_path = ClassLoader.getSystemResource("doc.txt").getPath();
		byte[] encoded = Files.readAllBytes(Paths.get(doc_path));
		String document =  new String(encoded, "UTF-8");
		jcas.setDocumentText(document);
		String token_annotation_filepath =  ClassLoader.getSystemResource("pos.json").getPath();
		String sentence_annotation_filepath =  ClassLoader.getSystemResource("sentences.json").getPath();
		wrapper.process(jcas, token_annotation_filepath, sentence_annotation_filepath);
		FSIterator iterSentence = jcas.getAnnotationIndex(Sentence.type).iterator();
		int nb_annotations = 0;
		while(iterSentence.hasNext()) {
			nb_annotations++;
			iterSentence.next();
		}
		assertEquals("The number of tagged sentences is different : ", nb_annotations, 1);
		
		FSIterator iterToken = jcas.getAnnotationIndex(Token.type).iterator();
		nb_annotations = 0;
		while(iterToken.hasNext()) {
			nb_annotations++;
			iterToken.next();
		}
		assertEquals("The number of tagged tokens is different : ",nb_annotations, 1);
	}
	
	@Test
	public void testProcess_2() throws Exception {
		Properties settings = new Properties();
		settings.put(PartOfSpeechTagger.JSON_CONFIG_PATH, ClassLoader.getSystemResource("config_JSONReader2.txt").getPath());
		
		wrapper = new JSONTaggerWrapper();
		wrapper.initialize(settings);
		
		
		JCas jcas = jcasFactory.createJCas();
		String doc_path = ClassLoader.getSystemResource("doc.txt").getPath();
		byte[] encoded = Files.readAllBytes(Paths.get(doc_path));
		String document =  new String(encoded, "UTF-8");
		jcas.setDocumentText(document);
		String token_annotation_filepath =  ClassLoader.getSystemResource("pos2.json").getPath();
		String sentence_annotation_filepath =  ClassLoader.getSystemResource("sentences2.json").getPath();
		wrapper.process(jcas, token_annotation_filepath, sentence_annotation_filepath);
		FSIterator iterSentence = jcas.getAnnotationIndex(Sentence.type).iterator();
		int nb_annotations = 0;
		while(iterSentence.hasNext()) {
			nb_annotations++;
			iterSentence.next();
		}
		assertEquals("The number of tagged sentences is different : ", nb_annotations, 1);
		
		FSIterator iterToken = jcas.getAnnotationIndex(Token.type).iterator();
		nb_annotations = 0;
		while(iterToken.hasNext()) {
			nb_annotations++;
			iterToken.next();
		}
		assertEquals("The number of tagged tokens is different : ",nb_annotations, 1);
	}
	
}
