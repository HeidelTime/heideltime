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
 * The Class Classification.
 */
public class Classification {

    /** The option. */
    public Option option = null;
    
    /** The data. */
    public Data data = null;
    
    /** The dict. */
    public Dictionary dict = null;
    
    /** The feagen. */
    public FeatureGen feagen = null;
    
    /** The inference. */
    public Inference inference = null;
    
    /** The model. */
    public Model model = null;
    
    /** The initialized. */
    public boolean initialized = false;
    
    /** The fin model. */
    private BufferedReader finModel = null;
    
    /** The int cps. */
    List intCps = null;
    
    /**
     * Instantiates a new classification.
     *
     * @param modelDir the model dir
     */
    public Classification(String modelDir) {
	option = new Option(modelDir);
	option.readOptions();
	
	 init();
    }
    
    /**
     * Checks if is initialized.
     *
     * @return true, if is initialized
     */
    public boolean isInitialized() {
	return initialized;
    }
    
    /**
     * Inits the.
     */
    public void init() {    
	try {
	    // open model file
	    finModel = option.openModelFile();
	    if (finModel == null) {
		System.out.println("Couldn't open model file");
		return;
	    }
	
	    data = new Data(option);
	    // read context predicate map
	    data.readCpMaps(finModel);
	    // read label map
	    data.readLbMaps(finModel);
	
	    dict = new Dictionary(option, data);
	    // read dictionary
	    dict.readDict(finModel);
	
	    feagen = new FeatureGen(option, data, dict);
	    // read features
	    feagen.readFeatures(finModel);
	
	    // create an inference object
	    inference = new Inference();
	    
	    // create a model object
	    model = new Model(option, data, dict, feagen, null, inference, null);
	    model.initInference();
	    
	    // close model file
	    finModel.close();
	    
	} catch(IOException e) {
	    System.out.println("Couldn't load the model, check the model file again");
	    System.out.println(e.toString());
	}
	
	intCps = new ArrayList();
	
	initialized = true;
    }
    
    /**
     * classify an observation.
     *
     * @param cps contains a list of context predicates
     * @return label
     */
    public String classify(String cps) {
		// cps contains a list of context predicates
	
		String modelLabel = "";
		int i;
		
		intCps.clear();		
		
		StringTokenizer strTok = new StringTokenizer(cps, " \t\r\n");	
		int count = strTok.countTokens();	
		
		for (i = 0; i < count; i++) {
		    String cpStr = strTok.nextToken();
		    Integer cpInt = (Integer)data.cpStr2Int.get(cpStr);
		    if (cpInt != null) {
			intCps.add(cpInt);
		    }
		}
		
		Observation obsr = new Observation(intCps);
		
		// classify
		inference.classify(obsr);
		
		String lbStr = (String)data.lbInt2Str.get(new Integer(obsr.modelLabel));
		if (lbStr != null) {
		    modelLabel = lbStr;
		}
	
		return modelLabel;	
    }    
    
    /**
     * Classify.
     *
     * @param cpArr the cp arr
     * @return the string
     */
    public String classify(String [] cpArr){
    	String modelLabel = "";
		//int i;
		
		intCps.clear();
		
		int curWordCp = -1;
		int dictLabel = -2;
		int dictCp = -1;
		Vector<Integer> dictCps = new Vector<Integer>();		
		
		for (String cpStr : cpArr) {
			Integer cpInt = (Integer)data.cpStr2Int.get(cpStr);
			
			if (cpInt != null) {
				intCps.add(cpInt);			
			
				if (cpStr.startsWith("w:0")){
					//current word	
					curWordCp = cpInt;		
				}
				else if (cpStr.startsWith("dict:0")){
					//current labels
					dictCp = cpInt;
					dictCps.add(dictCp);
					
					if (dictLabel == -1){
						//do nothing
					}
					else if (dictLabel == -2){
						//initial state
						String label = cpStr.substring("dict:0:".length());
						
						if (data.lbStr2Int.containsKey(label))
							dictLabel = (Integer) data.lbStr2Int.get(label);
						else dictLabel = -1;
					}
					else {//!=-1 && !=-2
						dictLabel = -1;
					}
				}
			}
		}
		
		//insert information about current cpid of w:0:<current_word>
		if (curWordCp != -1 && dictLabel >= 0) { //in training data			
			for (int i = 0; i < 3; ++i)
				intCps.add(dictCp);			
		}
		else {
			for (int i = 0; i < dictCps.size(); ++i){
				intCps.add(dictCps.get(i));
				intCps.add(dictCps.get(i));
			}
		}
		
		//create observation and start inference
		Observation obsr = new Observation(intCps);
		obsr.curWordCp = curWordCp;
		obsr.dictLabel = dictLabel;
		
		 if (obsr.curWordCp == -1 && obsr.dictLabel >= 0){    	
		    	//not in training data and 
		    	//there is only one corresponding label in dict		
			 obsr.modelLabel = obsr.dictLabel;
		}else inference.classify(obsr);
		
		String lbStr = (String)data.lbInt2Str.get(new Integer(obsr.modelLabel));
		if (lbStr != null) {
		    modelLabel = lbStr;
		}
	
		return modelLabel;	
    }
    
    /**
     * classify a list of observation.
     *
     * @param data contains a list of cps
     * @return the list
     */
    public List classify(List data) {
		List list = new ArrayList();
		
		for (int i = 0; i < data.size(); i++) {
		    list.add(classify((String)data.get(i)));
		}
		
		return list;
    }    
    
} // end of class Classification

