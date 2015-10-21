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
 * The Class Model.
 */
public class Model {
    
    /** The option. */
    public Option option = null;
    
    /** The data. */
    public Data data = null;
    
    /** The dict. */
    public Dictionary dict = null;
    
    /** The fea gen. */
    public FeatureGen feaGen = null;
    
    /** The train. */
    public Train train = null;
    
    /** The inference. */
    public Inference inference = null;
    
    /** The evaluation. */
    public Evaluation evaluation = null;
    
    // feature weight
    /** The lambda. */
    double[] lambda = null;
    
    /**
     * Instantiates a new model.
     */
    public Model() {	
	// do nothing
    }
    
    /**
     * Instantiates a new model.
     *
     * @param option the option
     * @param data the data
     * @param dict the dict
     * @param feaGen the fea gen
     * @param train the train
     * @param inference the inference
     * @param evaluation the evaluation
     */
    public Model(Option option, Data data, Dictionary dict, FeatureGen feaGen,
		Train train, Inference inference, Evaluation evaluation) {
	this.option = option;
	this.data = data;
	this.dict = dict;
	this.feaGen = feaGen;
	this.evaluation = evaluation;

	if (train != null) {
	    this.train = train;
	    this.train.model = this;	    
	    this.train.init();
	}

	if (inference != null) {	
	    this.inference = inference;
	    this.inference.model = this;
	    this.inference.init();
	}
	
	if (evaluation != null) {
	    this.evaluation = evaluation;
	    this.evaluation.model = this;
	    this.evaluation.init();
	}
    }
        
    /**
     * Do train.
     *
     * @param fout the fout
     */
    public void doTrain(PrintWriter fout) {
	if (lambda == null) {
	    lambda = new double[feaGen.numFeatures()];
	}
	
	// call this to train
	train.doTrain(fout);
	
	// call this to update the feature weights
	updateFeatures();
    }
    
    /**
     * Update features.
     */
    public void updateFeatures() {
	for (int i = 0; i < feaGen.features.size(); i++) {
	    Feature f = (Feature)feaGen.features.get(i);
	    f.wgt = lambda[f.idx];    
	}
    }
    
    /**
     * Inits the inference.
     */
    public void initInference() {
	if (lambda == null) {
	    System.out.println("numFetures: " + feaGen.numFeatures());
	    lambda = new double[feaGen.numFeatures() + 1];
	    
	    // reading feature weights from the feature list
	    for (int i = 0; i < feaGen.features.size(); i++) {
		Feature f = (Feature)feaGen.features.get(i);

		lambda[f.idx] = f.wgt;
	    }	    	    
	}    
    }
    
    /**
     * Do inference.
     *
     * @param data the data
     */
    public void doInference(List data) {
	if (lambda == null) {
	    lambda = new double[feaGen.numFeatures()];
	    
	    // reading feature weights from the feature list
	    for (int i = 0; i < feaGen.features.size(); i++) {
		Feature f = (Feature)feaGen.features.get(i);
		lambda[f.idx] = f.wgt;    
	    }	    
	}
	
	inference.doInference(data);	
    }
        
} // end of class Model

