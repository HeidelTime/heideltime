package de.unihd.dbs.uima.annotator.heideltime.processors;

import org.apache.uima.jcas.JCas;
/**
 * 
 * Abstract class to for all Processors to inherit from. A processor is a
 * modular, self-sufficient piece of code that was added to HeidelTime to
 * fulfill a specific function.
 * @author julian zell
 *
 */
public abstract class GenericProcessor {
	protected Package component;
	
	/**
	 * Constructor that sets the component for logger use.
	 * Any inheriting class should run this via super()
	 */
	public GenericProcessor() {
		this.component = this.getClass().getPackage();
	}
	
	/**
	 * starts the processing of the processor.
	 * @param jcas
	 */
	public abstract void process(JCas jcas);
}
