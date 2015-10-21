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
 * The Class FeatureGen.
 */
public class FeatureGen {
    
    /** The features. */
    List features = null;	// list of features
    
    /** The fmap. */
    Map fmap = null;		// feature map
    
    /** The maps. */
    Maps maps = null;		// context predicate and label maps
    
    /** The dict. */
    Dictionary dict = null;	// dictionary
        
    /**
     * Instantiates a new feature gen.
     *
     * @param maps the maps
     * @param dict the dict
     */
    public FeatureGen(Maps maps, Dictionary dict) {
	this.maps = maps;
	this.dict = dict;	
    }
    
    // adding a feature
    /**
     * Adds the feature.
     *
     * @param f the f
     */
    public void addFeature(Feature f) {
	f.strId2IdxAdd(fmap);
	features.add(f);
    }
    
    /**
     * Num features.
     *
     * @return the int
     */
    public int numFeatures() {
	if (features == null) {
	    return 0;
	} else {
	    return features.size();
	}
    }
    
    /**
     * Read features.
     *
     * @param fin the fin
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void readFeatures(BufferedReader fin) throws IOException {
	if (features != null) {
	    features.clear();
	} else {
	    features = new ArrayList();
	}
	
	if (fmap != null) {
	    fmap.clear(); 
	} else {
	    fmap = new HashMap();
	}
	
	if (eFeatures != null) {
	    eFeatures.clear();
	} else {
	    eFeatures = new ArrayList();
	}
	
	if (sFeatures != null) {
	    sFeatures.clear();
	} else {
	    sFeatures = new ArrayList();
	}
	
	String line;
	
	// get the number of features
	if ((line = fin.readLine()) == null) {
	    System.out.println("Unknown number of features");
	    return;
	}
	int numFeatures = Integer.parseInt(line);
        System.out.println("Number of features: " + numFeatures);
	if (numFeatures <= 0) {
	    System.out.println("Invalid number of features");
	    return;
	}
	
	System.out.println("Reading features ...");
	
	// main loop for reading features
	for (int i = 0; i < numFeatures; i++) {
	    line = fin.readLine();
	    if (line == null) {
		// invalid feature line, ignore it
		continue;
	    }
	    
	    StringTokenizer strTok = new StringTokenizer(line, " ");
	    if (strTok.countTokens() != 3) {
		// invalid feature line, ignore it
		continue;
	    }
	    
	    // create a new feature by parsing the line
	    Feature f = new Feature(line, maps.cpStr2Int, maps.lbStr2Int);
	    
	    Integer fidx = (Integer)fmap.get(f.strId);
	    if (fidx == null) {
		// insert the feature into the feature map
        //        System.out.println("\tinsert into the feature map");
		fmap.put(f.strId, new Integer(f.idx));
		features.add(f);
		
		if (f.ftype == Feature.EDGE_FEATURE1) {
		    eFeatures.add(f);
		}
	    }
	
            else {
            	features.add(f);
                  //System.out.println(line + "-----------> [" + f.strId + "]");            
            }      
            
	}
	
	System.out.println("Reading " + Integer.toString(features.size()) + " features completed!");
	
	// read the line ###...
	line = fin.readLine();
    }
    
    // start to scan features at a particular position in a data sequence
    /**
     * Start scan features at.
     *
     * @param seq the seq
     * @param pos the pos
     */
    public void startScanFeaturesAt(List seq, int pos) {
	startScanSFeaturesAt(seq, pos);
	startScanEFeatures();
    }
    
    /**
     * Checks for next feature.
     *
     * @return true, if successful
     */
    public boolean hasNextFeature() {
	return (hasNextSFeature() || hasNextEFeature());
    }
    
    /**
     * Next feature.
     *
     * @return the feature
     */
    public Feature nextFeature() {
	Feature f = null;
    
	if (hasNextSFeature()) {
	    f = nextSFeature();
	} else if (hasNextEFeature()) {
	    f = nextEFeature();
	} else {
	    // do nothing
	}
	
	return f;
    }
    
    // start to scan state features
    /** The s features. */
    List sFeatures = null;
    
    /** The s feature idx. */
    int sFeatureIdx = 0;
    
    /**
     * Start scan s features at.
     *
     * @param seq the seq
     * @param pos the pos
     */
    void startScanSFeaturesAt(List seq, int pos) {	
	sFeatures.clear();
	sFeatureIdx = 0;
	
	Observation obsr = (Observation)seq.get(pos);
	    
	// scan over all context predicates
	for (int i = 0; i < obsr.cps.length; i++) {
	    Element elem = (Element)dict.dict.get(new Integer(obsr.cps[i]));
	    if (elem == null) {
		continue;
	    }
	    
	    if (!(elem.isScanned)) {
		// scan all labels for state feature
		Iterator it = elem.lbCntFidxes.keySet().iterator();
		while (it.hasNext()) {
		    Integer label = (Integer)it.next();
		    CountFeatureIdx cntFidx = (CountFeatureIdx)elem.lbCntFidxes.get(label);

		    if (cntFidx.fidx >= 0) {
			Feature sF = new Feature();
			sF.sFeature1Init(label.intValue(), obsr.cps[i]);
			sF.idx = cntFidx.fidx;
			
			elem.cpFeatures.add(sF);
		    }	    
		}		
		
		elem.isScanned = true;
	    }
	    
	    for (int j = 0; j < elem.cpFeatures.size(); j++) {
		sFeatures.add(elem.cpFeatures.get(j));
	    }
	}		
    }    
    
    /**
     * Checks for next s feature.
     *
     * @return true, if successful
     */
    boolean hasNextSFeature() {
	return (sFeatureIdx < sFeatures.size());
    }
    
    /**
     * Next s feature.
     *
     * @return the feature
     */
    Feature nextSFeature() {
	Feature sF = (Feature)sFeatures.get(sFeatureIdx);
	sFeatureIdx++;
	return sF;
    }
    
    // start to scan edge features
    /** The e features. */
    List eFeatures = null;
    
    /** The e feature idx. */
    int eFeatureIdx = 0;
    
    /**
     * Start scan e features.
     */
    void startScanEFeatures() {
	eFeatureIdx = 0;
    }
    
    /**
     * Checks for next e feature.
     *
     * @return true, if successful
     */
    boolean hasNextEFeature() {
	return (eFeatureIdx < eFeatures.size());
    }
    
    /**
     * Next e feature.
     *
     * @return the feature
     */
    Feature nextEFeature() {
	Feature eF = (Feature)eFeatures.get(eFeatureIdx);
	eFeatureIdx++;
	return eF;
    }
    
} // end of class FeatureGen

