package de.unihd.dbs.uima.annotator.heideltime;

import java.util.ArrayList;

import org.apache.uima.UimaContext;
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
	private ArrayList<String> processorNames;
	// array of instantiated processors
	private ArrayList<GenericProcessor> processors;
	// self-identifying component for logging purposes
	private Class<?> component; 
	
	/**
	 * Private constructor, only to be called by the getInstance() method.
	 */
	private ProcessorManager() {
		this.processorNames = new ArrayList<String>();
		this.component = this.getClass();
		this.processors = new ArrayList<GenericProcessor>();
	}
	
	/**
	 * method to register a processor
	 * @param processor processor to be registered in the processormanager's list
	 */
	public void registerProcessor(String processor) {
		this.processorNames.add(processor);
	}
	
	/**
	 * Based on reflection, this method instantiates and initializes all of the
	 * registered Processors.
	 * @param jcas
	 */
	public void initializeAllProcessors(UimaContext aContext) {
		for(String s : processorNames) {
			try {
				Class<?> c = Class.forName(s);
				GenericProcessor p = (GenericProcessor) c.newInstance();
				p.initialize(aContext);
				processors.add(p);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.printError(component, "Unable to initialize registered Processor "+s+", got: "+e.toString());
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Based on reflection, this method instantiates and executes all of the
	 * registered Processors.
	 * @param jcas
	 */
	public void executeAllProcessors(JCas jcas) {
		for(GenericProcessor p : processors) {
			try {
				p.process(jcas);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.printError(component, "Unable to process registered Processor "+p.getClass().getName()+", got: "+e.toString());
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
