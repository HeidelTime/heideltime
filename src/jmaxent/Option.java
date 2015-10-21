/*
 Copyright (C) 2010 by
 * 
 * 	Cam-Tu Nguyen 
 *  ncamtu@ecei.tohoku.ac.jp or ncamtu@gmail.com
 *
 *  Xuan-Hieu Phan  
 *  pxhieu@gmail.com 
 *
 *  College of Technology, Vietnamese University, Hanoi
 * 	Graduate School of Information Sciences, Tohoku University
 *
 * JVnTextPro-v.2.0 is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JVnTextPro-v.2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with  JVnTextPro-v.2.0); if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package jmaxent;

import java.io.*;
import java.util.*;

// TODO: Auto-generated Javadoc
/**
 * The Class Option.
 */
public class Option {

    // model directory
    /** The model dir. */
    public String modelDir = ".";
    // model file 
    /** The model file. */
    public String modelFile = "model.txt";
    
    /** The model separator. */
    public static String modelSeparator = "##########";
    // option file
    /** The option file. */
    public String optionFile = "option.txt";
    
    // training data, testing data file
    /** The train data file. */
    public String trainDataFile = "train.labeled";
    
    /** The test data file. */
    public String testDataFile = "test.labeled";
    
    /** The label separator. */
    public static String labelSeparator = "/";
    
    // training log
    /** The train log file. */
    public String trainLogFile = "trainlog.txt";
    
    /** The is logging. */
    public boolean isLogging = true;
    
    /** The num train exps. */
    public int numTrainExps = 0; // number of training examples
    
    /** The num test exps. */
    public int numTestExps = 0; // number of testing examples
    
    /** The num labels. */
    public int numLabels = 0; // number of class labels
    
    /** The num cps. */
    public int numCps = 0; // number of context predicates
    
    /** The num features. */
    public int numFeatures = 0; // number of features
    
    // thresholds for context predicate and feature cut-off
    /** The cp rare threshold. */
    public int cpRareThreshold = 1;
    
    /** The rare threshold. */
    public int fRareThreshold = 1;
    
    // training options
    /** The num iterations. */
    public int numIterations = 100; // number of training iterations
    
    /** The init lambda val. */
    public double initLambdaVal = 0.0; // intial value for feature weights
    
    /** The sigma square. */
    public double sigmaSquare = 100; // for smoothing
    
    /** The eps for convergence. */
    public double epsForConvergence = 0.0001; // for checking training termination
    
    /** The m for hessian. */
    public int mForHessian = 7;	// for L-BFGS corrections
    
    /** The debug level. */
    public int debugLevel = 1; // control output status information
    
    // evaluation options
    /** The evaluate during training. */
    public boolean evaluateDuringTraining = true; // evaluate during training
    
    /** The save best model. */
    public boolean saveBestModel = true; // save the best model with testing data
    
    /**
     * Instantiates a new option.
     */
    public Option() {
    }
    
    /**
     * Instantiates a new option.
     *
     * @param modelDir the model dir
     */
    public Option(String modelDir) {
	if (modelDir.endsWith(File.separator)) {
	    this.modelDir = modelDir.substring(0, modelDir.length() - 1);
	} else {
	    this.modelDir = modelDir;
	}
    }
    
    /**
     * Read options.
     *
     * @return true, if successful
     */
    public boolean readOptions() {
	String filename = modelDir + File.separator + optionFile;
	BufferedReader fin = null;
	String line;
	
	try {
	    fin = new BufferedReader(new FileReader(filename));
	    
	    System.out.println("Reading options ...");
	    
	    // read option lines
	    while ((line = fin.readLine()) != null) {
		String trimLine = line.trim();
		if (trimLine.startsWith("#")) {
		    // comment line
		    continue;
		}		    
		
		//System.out.println(line);
		
		StringTokenizer strTok = new StringTokenizer(line, "= \t\r\n");
		int len = strTok.countTokens();
		if (len != 2) {
		    // invalid parameter line, ignore it
		    continue;
		}
		
		String strOpt = strTok.nextToken();
		String strVal = strTok.nextToken();
		
		if (strOpt.compareToIgnoreCase("trainDataFile") == 0) {
		    trainDataFile = strVal;
		    
		} else if (strOpt.compareToIgnoreCase("testDataFile") == 0) {
		    testDataFile = strVal;
		    
		} else if (strOpt.compareToIgnoreCase("isLogging") == 0) {
		    if (!(strVal.compareToIgnoreCase("true") == 0 ||
				strVal.compareToIgnoreCase("false") == 0)) {
			continue;
		    }
		    isLogging = Boolean.valueOf(strVal).booleanValue();
		
		} else if (strOpt.compareToIgnoreCase("cpRareThreshold") == 0) {
		    int numTemp = Integer.parseInt(strVal);
		    cpRareThreshold = numTemp;
		
		} else if (strOpt.compareToIgnoreCase("fRareThreshold") == 0) {
		    int numTemp = Integer.parseInt(strVal);
		    fRareThreshold = numTemp;
				
		} else if (strOpt.compareToIgnoreCase("numIterations") == 0) {
		    int numTemp = Integer.parseInt(strVal);
		    numIterations = numTemp;
		
		} else if (strOpt.compareToIgnoreCase("initLambdaVal") == 0) {
		    double numTemp = Double.parseDouble(strVal);
		    initLambdaVal = numTemp;
		
		} else if (strOpt.compareToIgnoreCase("sigmaSquare") == 0) {
		    double numTemp = Double.parseDouble(strVal);
		    sigmaSquare = numTemp;
		
		} else if (strOpt.compareToIgnoreCase("epsForConvergence") == 0) {
		    double numTemp = Double.parseDouble(strVal);
		    epsForConvergence = numTemp;
		
		} else if (strOpt.compareToIgnoreCase("mForHessian") == 0) {
		    int numTemp = Integer.parseInt(strVal);
		    mForHessian = numTemp;

		} else if (strOpt.compareToIgnoreCase("evaluateDuringTraining") == 0) {
		    if (!(strVal.compareToIgnoreCase("true") == 0 ||
				strVal.compareToIgnoreCase("false") == 0)) {
			continue;
		    }
		    evaluateDuringTraining = Boolean.valueOf(strVal).booleanValue();
		
		} else if (strOpt.compareToIgnoreCase("saveBestModel") == 0) {
		    if (!(strVal.compareToIgnoreCase("true") == 0 ||
				strVal.compareToIgnoreCase("false") == 0)) {
			continue;
		    }
		    saveBestModel = Boolean.valueOf(strVal).booleanValue();
		
		} else if (strOpt.compareToIgnoreCase("trainLogFile") == 0){
			trainLogFile = strVal;
		    // for future use
		}
		else if (strOpt.compareToIgnoreCase("modelFile") == 0){
			modelFile = strVal;
		}
		else{
			//for future use
		}
		
	    }
	    
	    System.out.println("Reading options completed!");
	
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return false;
	}
	
	return true;
    }
    
    /**
     * Open train log file.
     *
     * @return the prints the writer
     */
    public PrintWriter openTrainLogFile() {
	String filename = modelDir + File.separator + trainLogFile;
	PrintWriter fout = null;
	
	try {
	    fout = new PrintWriter(new OutputStreamWriter( (new FileOutputStream(filename)), "UTF-8"));	    
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return null;
	}
	
	return fout;
    }
    
    /**
     * Open model file.
     *
     * @return the buffered reader
     */
    public BufferedReader openModelFile() {
	String filename = modelDir + File.separator + modelFile;
	BufferedReader fin = null;
	
	try {
	    fin = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
	    
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return null;
	}
	
	return fin;	
    }
    
    /**
     * Creates the model file.
     *
     * @return the prints the writer
     */
    public PrintWriter createModelFile() {
	String filename = modelDir + File.separator + modelFile;
	PrintWriter fout = null;
	
	try {
	    fout = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
	    
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return null;
	}
	
	return fout;
    }
    
    /**
     * Write options.
     *
     * @param fout the fout
     */
    public void writeOptions(PrintWriter fout) {
	fout.println("OPTION VALUES:");	
	fout.println("==============");
	fout.println("Model directory: " + modelDir);
	fout.println("Model file: " + modelFile);
	fout.println("Option file: " + optionFile);
	fout.println("Training log file: " + trainLogFile + " (this one)");
	fout.println("Training data file: " + trainDataFile);
	fout.println("Testing data file: " + testDataFile);
	fout.println("Number of training examples " + Integer.toString(numTrainExps));
	fout.println("Number of testing examples " + Integer.toString(numTestExps));
	fout.println("Number of class labels: " + Integer.toString(numLabels));	
	fout.println("Number of context predicates: " + Integer.toString(numCps));
	fout.println("Number of features: " + Integer.toString(numFeatures));
	fout.println("Rare threshold for context predicates: " + Integer.toString(cpRareThreshold));	
	fout.println("Rare threshold for features: " + Integer.toString(fRareThreshold));
	fout.println("Number of training iterations: " + Integer.toString(numIterations));
	fout.println("Initial value of feature weights: " + Double.toString(initLambdaVal));
	fout.println("Sigma square: " + Double.toString(sigmaSquare));
	fout.println("Epsilon for convergence: " + Double.toString(epsForConvergence));
	fout.println("Number of corrections in L-BFGS: " + Integer.toString(mForHessian));
	if (evaluateDuringTraining) {
	    fout.println("Evaluation during training: true");
	} else {
	    fout.println("Evaluation during training: false");
	}
	if (saveBestModel) {
	    fout.println("Save the best model towards testing data: true");
	} else {
	    fout.println("Save the best model towards testing data: false");
	}
	fout.println();
    }

} // end of class Option

