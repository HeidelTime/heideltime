package de.unihd.dbs.heideltime.standalone.components.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.uima.jcas.JCas;
import org.python.util.PythonInterpreter;

import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;

public class PythonTaggerWrapper implements PartOfSpeechTagger {

	private PythonInterpreter interp;
	private String scriptFile;
	
	@Override
	public void process(JCas jcas) throws Exception {
		interp.set("jcas", jcas); // make jcas available in script
		interp.execfile(scriptFile);
	}

	@Override
	public void initialize(Properties settings) throws IllegalArgumentException, FileNotFoundException {
		interp = new PythonInterpreter();
		scriptFile = (String) settings.get(PYTHON_SCRIPT_PATH);
	    if(scriptFile == null) {
	    	throw new IllegalArgumentException(
                    "Python script path not defined in the settings.");
	    }
	    if(! scriptFile.endsWith(".py")) {
	    	throw new IllegalArgumentException(
	    			String.format("'%s' is not a valid python script name.", scriptFile));
	    }
	    if (! new File(scriptFile).isFile()) {
	    	throw new FileNotFoundException(
	    			String.format("The python script '%s' was not found.", scriptFile));
	    }
	    
	}

	@Override
	public void reset() {
		interp.close();
	}

}
