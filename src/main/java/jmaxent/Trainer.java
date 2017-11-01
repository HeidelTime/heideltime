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

// TODO: Auto-generated Javadoc
/**
 * The Class Trainer.
 */
public class Trainer {
    
    /**
     * The main method.
     *
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
	if (!checkArgs(args)) {
	    displayHelp();
	    return;
	}
	
	String modelDir = args[2];
	boolean isAll = (args[0].compareToIgnoreCase("-all") == 0);
	boolean isTrn = (args[0].compareToIgnoreCase("-trn") == 0);
	boolean isTst = (args[0].compareToIgnoreCase("-tst") == 0);
	boolean isCont = (args[0].compareToIgnoreCase("-cont") == 0); //TUNC
	
	// create option object
	Option option = new Option(modelDir);
	
	option.optionFile = args[4];
	
	option.readOptions();
	
	Data data = null;
	Dictionary dict = null;
	FeatureGen feaGen = null;
	Train train = null;
	Inference inference = null;
	Evaluation evaluation = null;
	Model model = null;
	
	PrintWriter foutModel = null;
	BufferedReader finModel = null;
	
	if (isAll) {
	    // both training and testing
	    
	    PrintWriter flog = option.openTrainLogFile();
	    if (flog == null) {
		System.out.println("Couldn't create training log file");
		return;
	    }
	    
	    foutModel = option.createModelFile();
	    if (foutModel == null) {
		System.out.println("Couldn't create model file");
		return;
	    }	    	    
	    
	    data = new Data(option);
	    data.readTrnData(option.modelDir + File.separator + option.trainDataFile);	    
	    data.readTstData(option.modelDir + File.separator + option.testDataFile);	    	    
	    
	    dict = new Dictionary(option, data);
	    dict.generateDict();
	    
	    feaGen = new FeatureGen(option, data, dict);
	    feaGen.generateFeatures();
	    
	    data.writeCpMaps(dict, foutModel);
	    data.writeLbMaps(foutModel);	    
	    
	    train = new Train();
	    inference = new Inference();
	    evaluation = new Evaluation();
	    
	    model = new Model(option, data, dict, feaGen, train, inference, evaluation);
	    model.doTrain(flog);	    	    
	    
	    model.doInference(model.data.tstData);
	    model.evaluation.evaluate(flog);
	    
	    dict.writeDict(foutModel);
	    feaGen.writeFeatures(foutModel);
	    
	    foutModel.close();
	}
	
	if (isTrn) {
	    // training only
	    
	    PrintWriter flog = option.openTrainLogFile();
	    if (flog == null) {
		System.out.println("Couldn't create training log file");
		return;
	    }	    

	    foutModel = option.createModelFile();
	    if (foutModel == null) {
		System.out.println("Couldn't create model file");
		return;
	    }
	    
	    data = new Data(option);
	    data.readTrnData(option.modelDir + File.separator + option.trainDataFile);	    
	    
	    dict = new Dictionary(option, data);
	    dict.generateDict();
	    
	    feaGen = new FeatureGen(option, data, dict);
	    feaGen.generateFeatures();
	    
	    data.writeCpMaps(dict, foutModel);
	    data.writeLbMaps(foutModel);	    	    

	    train = new Train();	    
	    
	    model = new Model(option, data, dict, feaGen, train, null, null);	    	    
	    model.doTrain(flog);	    
	    
	    dict.writeDict(foutModel);
	    feaGen.writeFeatures(foutModel);
	    
	    foutModel.close();
	}
	
	if (isTst) {
	    // testing only
	    
	    finModel = option.openModelFile();
	    if (finModel == null) {
		System.out.println("Couldn't open model file");
		return;
	    }	    
	    
	    data = new Data(option);
	    data.readCpMaps(finModel);
	    data.readLbMaps(finModel);	    
	    data.readTstData(option.modelDir + File.separator + option.testDataFile);	    
	
	    dict = new Dictionary(option, data);	    
	    dict.readDict(finModel);
	    
	    feaGen = new FeatureGen(option, data, dict);
	    feaGen.readFeatures(finModel);
	    
	    inference = new Inference();
	    evaluation = new Evaluation();
	    
	    model = new Model(option, data, dict, feaGen, null, inference, evaluation);	    
	    
	    model.doInference(model.data.tstData);
	    model.evaluation.evaluate(null);
	    
	    finModel.close();	    
	}	
	
	if (isCont){ //continue last training
		PrintWriter flog = option.openTrainLogFile(); //append
	    if (flog == null) {
		System.out.println("Couldn't create training log file");
		return;
	    }	 
		    
		finModel = option.openModelFile();
	    if (finModel == null) {
		System.out.println("Couldn't open model file");
		return;
	    }	    
	    
	    data = new Data(option);
	    data.readCpMaps(finModel);
	    data.readLbMaps(finModel);	    
	    data.readTstData(option.modelDir + File.separator + option.testDataFile);	    
	
	    dict = new Dictionary(option, data);	    
	    dict.readDict(finModel);
	    
	    feaGen = new FeatureGen(option, data, dict);
	    feaGen.readFeatures(finModel);
	    
	    inference = new Inference();
	    evaluation = new Evaluation();
	    
	    foutModel = option.createModelFile(); //overwrite the old model file
	    if (foutModel == null) {
			System.out.println("Couldn't create model file");
			return;
		    }
	    
	    model = new Model(option, data, dict, feaGen, train, inference, evaluation);
	    model.doTrain(flog);	    	    
	    
	    model.doInference(model.data.tstData);
	    model.evaluation.evaluate(flog);	
	    
	    foutModel.close();
	}
    } // end of the main method
    
    /**
     * Check args.
     *
     * @param args the args
     * @return true, if successful
     */
    public static boolean checkArgs(String[] args) {
	if (args.length < 5) {
	    return false;
	}
	
	if (!(args[0].compareToIgnoreCase("-all") == 0 ||
		    args[0].compareToIgnoreCase("-trn") == 0 ||
		    args[0].compareToIgnoreCase("-tst") == 0) || 
		    args[0].compareToIgnoreCase("-cont") == 0) {
	    return false;
	}
	
	if (args[1].compareToIgnoreCase("-d") != 0) {
	    return false;
	}
	
	if (args[3].compareToIgnoreCase("-o") != 0)
		return false; 
	
	return true;
    }
    
    /**
     * Display help.
     */
    public static void displayHelp() {
	System.out.println("Usage:");
	System.out.println("\tTrainer -all/-trn/-tst -d <model directory> -o <optionFile>");	
    }
}

