package de.unihd.dbs.uima.annotator.heideltime;

import java.util.ArrayList;

import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.annotator.heideltime.processors.GenericProcessor;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
/**
 * This class implements a singleton "Addon Manager". Any subroutine (Processor) that
 * may be added to HeidelTime's code to achieve a specific goal which is self-sufficient,
 * i.e. creates new Timexes by itself and without the use of HeidelTime's non-resource
 * related functionality should be registered in an instance of this class. HeidelTime's
 * annotator will then execute every registered "Processor"'s process() function.
 * Note that these Processes will be instantiated and processed after the resource
 * collection and before HeidelTime's cleanup methods.
 * @author julian zell
 *
 */
public class ProcessorManager {
	// singleton instance
	private static final ProcessorManager INSTANCE = new ProcessorManager();
	// list of processes' package names
	private ArrayList<String> processors;
	// self-identifying component for logging purposes
	private Class<?> component; 
	
	/**
	 * Private constructor, only to be called by the getInstance() method.
	 */
	private ProcessorManager() {
		this.processors = new ArrayList<String>();
		this.component = this.getClass();
	}
	
	/**
	 * method to register a processor
	 * @param processor processor to be registered in the processormanager's list
	 */
	public void registerProcessor(String processor) {
		this.processors.add(processor);
	}
	
	/**
	 * Based on reflection, this method instantiates and executes all of the
	 * registered Processors.
	 * @param jcas
	 */
	public void executeAllProcessors(JCas jcas) {
		for(String s : processors) {
			try {
				Class<?> c = Class.forName(s);
				GenericProcessor p = (GenericProcessor) c.newInstance();
				p.process(jcas);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.printError(component, "Unable to instantiate registered Processor "+s);
				System.exit(-1);
			}
		}
	}
	
	/**
	 * getInstance method of the singleton pattern
	 * @return ProcessorManager
	 */
	public static ProcessorManager getInstance() {
		return ProcessorManager.INSTANCE;
	}
}
