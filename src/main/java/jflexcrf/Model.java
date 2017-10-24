/*
 Copyright (C) 2010 by
 * 
 * 	Cam-Tu Nguyen	ncamtu@ecei.tohoku.ac.jp ncamtu@gmail.com
 *  Xuan-Hieu Phan  pxhieu@gmail.com 
 
 *  College of Technology, Vietnamese University, Hanoi
 * 
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 *
 *  JVnTextPro-v.2.0 is a free software; you can redistribute it and/or modify
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

package jflexcrf;

import java.io.*;
import java.util.*;

// TODO: Auto-generated Javadoc
/**
 * The Class Model.
 */
public class Model {
    
    /** The tagger opt. */
    public Option taggerOpt = null;
    
    /** The tagger maps. */
    public Maps taggerMaps = null;
    
    /** The tagger dict. */
    public Dictionary taggerDict = null;
    
    /** The tagger f gen. */
    public FeatureGen taggerFGen = null;
    
    /** The tagger vtb. */
    public Viterbi taggerVtb = null;
    
    // feature weight
    /** The lambda. */
    double[] lambda = null;
    
    /**
     * Instantiates a new model.
     */
    public Model() {
    }
    
    /**
     * Instantiates a new model.
     *
     * @param taggerOpt the tagger opt
     * @param taggerMaps the tagger maps
     * @param taggerDict the tagger dict
     * @param taggerFGen the tagger f gen
     * @param taggerVtb the tagger vtb
     */
    public Model(Option taggerOpt, Maps taggerMaps, Dictionary taggerDict, 
		FeatureGen taggerFGen, Viterbi taggerVtb) {
	this.taggerOpt = taggerOpt;
	this.taggerMaps = taggerMaps;
	this.taggerDict = taggerDict;
	this.taggerFGen = taggerFGen;
	this.taggerVtb = taggerVtb;	
    }
    
    // load the model
    /**
     * Inits the.
     *
     * @return true, if successful
     */
    public boolean init() {
	// open model file to load model here ... complete later
	BufferedReader fin = null;
	String modelFile = taggerOpt.modelDir + File.separator + taggerOpt.modelFile;
	
	try {
	    fin = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), "UTF-8"));
	    
	    // read context predicate map and label map
	    taggerMaps.readCpMaps(fin);
	    
	    System.gc();

	    taggerMaps.readLbMaps(fin);
	    
	    System.gc();
	    
	    // read dictionary 
	    taggerDict.readDict(fin);
	    
	    System.gc();
	    
	    // read features
	    taggerFGen.readFeatures(fin);
	    
	    System.gc();
	    
	    // close model file
	    fin.close();
	    
	} catch (IOException e) {
	    System.out.println("Couldn't open model file: " + modelFile);
	    System.out.println(e.toString());
	    
	    return false;	    
	}
	
	// update feature weights
	if (lambda == null) {
	    int numFeatures = taggerFGen.numFeatures();
	    lambda = new double[numFeatures];
	    for (int i = 0; i < numFeatures; i++) {
		Feature f = (Feature)taggerFGen.features.get(i);
                //System.out.println(f.idx);
		lambda[f.idx] = f.wgt;                
	    }
	}
    
	// call init method of Viterbi object
	if (taggerVtb != null) {
	    taggerVtb.init(this);
	}
	
	return true;
    }
    
    /**
     * Inference.
     *
     * @param seq the seq
     */
    public void inference(List seq) {
	taggerVtb.viterbiInference(seq);
    }
    
    /**
     * Inference all.
     *
     * @param data the data
     */
    public void inferenceAll(List data) {
	System.out.println("Starting inference ...");

	long start, stop, elapsed;
	start = System.currentTimeMillis();
	
	for (int i = 0; i < data.size(); i++) {
	    System.out.println("sequence " + Integer.toString(i + 1));
	    List seq = (List)data.get(i);
	    inference(seq);	    
	}
	
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	
	System.out.println("Inference " + Integer.toString(data.size()) + " sequences completed!");
	System.out.println("Inference time: " + Double.toString((double)elapsed / 1000) + " seconds");
    }
    
} // end of class Model

