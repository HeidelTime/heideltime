package de.unihd.dbs.heideltime.standalone.components;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.uima.jcas.JCas;

/**
 * Interface for a common UIMA annotator.
 * 
 * @author Julian Zell, University of Heidelberg
 */
public interface UIMAAnnotator {
	/**
	 * Initializes the jcas object.
	 * 
	 * @param jcas
	 * @param language Language of document
	 * @throws FileNotFoundException 
	 * @throws IllegalArgumentException 
	 * @throws IOException 
	 */
	public abstract void initialize(Properties settings) throws IllegalArgumentException, FileNotFoundException, IOException;
	
	/**
	 * Processes jcas object.
	 * 
	 * @param jcas
	 * @param language Language of document
	 * @throws Exception 
	 */
	public abstract void process(JCas jcas) throws Exception;
}
