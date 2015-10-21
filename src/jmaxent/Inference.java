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

import java.util.*;

// TODO: Auto-generated Javadoc
/**
 * The Class Inference.
 */
public class Inference {

    /** The model. */
    public Model model = null;
    
    /** The num labels. */
    public int numLabels = 0;    
    
    // for classification
    /** The temp. */
    double[] temp = null;
    
    /**
     * Instantiates a new inference.
     */
    public Inference() {
	// do nothing
    }
    
    /**
     * Inits the.
     */
    public void init() {
    	numLabels = model.data.numLabels();
        temp = new double[numLabels];
    }
    
    /**
     * Classify.
     *
     * @param obsr the obsr
     */
    public void classify(Observation obsr) {
    
	int i;	
	for (i = 0; i < numLabels; i++) {
	    temp[i] = 0.0;
	}
	
	model.feaGen.startScanFeatures(obsr);
	while (model.feaGen.hasNextFeature()) {
	    Feature f = model.feaGen.nextFeature();
	    
	    temp[f.label] += model.lambda[f.idx] * f.val;
	}
	
	double max = temp[0];
	int maxLabel = 0;
	for (i = 1; i < numLabels; i++) {
	    if (max < temp[i]) {
		max = temp[i];
		maxLabel = i;
	    }
	}
	
	obsr.modelLabel = maxLabel;
    }
    
    /**
     * Do inference.
     *
     * @param data the data
     */
    public void doInference(List data) {
	for (int i = 0; i < data.size(); i++) {
	    Observation obsr = (Observation)data.get(i);
	    
	    classify(obsr);
	}
    }

} // end of class Inference

