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
 * The Class Feature.
 */
public class Feature {

    /** The idx. */
    public int idx = -1; // feature index
    
    /** The str id. */
    public String strId = ""; // string identifier
    
    /** The label. */
    public int label = -1; // label
    
    /** The cp. */
    public int cp = -1; // context predicate
    
    /** The val. */
    public float val = 1; // feature value
    
    /** The wgt. */
    public double wgt = 0.0; // feature weight

    /**
     * Instantiates a new feature.
     */
    public Feature() {
    }
    
    /**
     * Instantiates a new feature.
     *
     * @param label the label
     * @param cp the cp
     */
    public Feature(int label, int cp) {
	FeatureInit(label, cp);
    }
    
    /**
     * Instantiates a new feature.
     *
     * @param label the label
     * @param cp the cp
     * @param fmap the fmap
     */
    public Feature(int label, int cp, Map fmap) {
	FeatureInit(label, cp);
	strId2IdxAdd(fmap);
    }

    // create a feature from a string (e.g., reading from model file)    
    /**
     * Instantiates a new feature.
     *
     * @param str the str
     * @param cpStr2Int the cp str2 int
     * @param lbStr2Int the lb str2 int
     */
    public Feature(String str, Map cpStr2Int, Map lbStr2Int) {	
	FeatureInit(str, cpStr2Int, lbStr2Int);
    }
    
    // create a feature from a string and add it to the feature map
    /**
     * Instantiates a new feature.
     *
     * @param str the str
     * @param cpStr2Int the cp str2 int
     * @param lbStr2Int the lb str2 int
     * @param fmap the fmap
     */
    public Feature(String str, Map cpStr2Int, Map lbStr2Int, Map fmap) {
	FeatureInit(str, cpStr2Int, lbStr2Int);
	strId2IdxAdd(fmap);
    }
    
    /**
     * Feature init.
     *
     * @param label the label
     * @param cp the cp
     */
    public void FeatureInit(int label, int cp) {
	this.label = label;
	this.cp = cp;
	strId = Integer.toString(label) + " " + Integer.toString(cp);
    }
    
    /**
     * Feature init.
     *
     * @param str the str
     * @param cpStr2Int the cp str2 int
     * @param lbStr2Int the lb str2 int
     */
    public void FeatureInit(String str, Map cpStr2Int, Map lbStr2Int) {
	StringTokenizer strTok = new StringTokenizer(str, " \t\r\n");
	// <label> <cp> <idx> <wgt>
	
	int len = strTok.countTokens();
	if (len != 4) {
	    return;
	}
	
	String labelStr = strTok.nextToken();
	String cpStr = strTok.nextToken();
	int idx = Integer.parseInt(strTok.nextToken());
	float val = 1;
	double wgt = Double.parseDouble(strTok.nextToken());
	
	Integer labelInt = (Integer)lbStr2Int.get(labelStr);
	Integer cpInt = (Integer)cpStr2Int.get(cpStr);
	FeatureInit(labelInt.intValue(), cpInt.intValue());
	
	this.idx = idx;
	this.val = val;
	this.wgt = wgt;
    }
    
    // mapping from string identifier to feature index
    /**
     * Str id2 idx.
     *
     * @param fmap the fmap
     * @return the int
     */
    public int strId2Idx(Map fmap) {
	Integer idxInt = (Integer)fmap.get(strId);
	if (idxInt != null) {
	    this.idx = idxInt.intValue();
	}
	
	return this.idx;
    }
    
    // mapping from string identifier to feature index (adding feature to the map
    // if the mapping does not exist
    /**
     * Str id2 idx add.
     *
     * @param fmap the fmap
     * @return the int
     */
    public int strId2IdxAdd(Map fmap) {
	strId2Idx(fmap);
	
	if (idx < 0) {
	    idx = fmap.size();
	    fmap.put(strId, new Integer(idx));
	}
	
	return idx;
    }
    
    // return the feature index
    /**
     * Index.
     *
     * @param fmap the fmap
     * @return the int
     */
    public int index(Map fmap) {
	return strId2Idx(fmap);
    }
    
    // convert feature to string: "<label> <cp> <idx> <weight>"
    /**
     * To string.
     *
     * @param cpInt2Str the cp int2 str
     * @param lbInt2Str the lb int2 str
     * @return the string
     */
    public String toString(Map cpInt2Str, Map lbInt2Str) {
	String str = "";
	
	String labelStr = (String)lbInt2Str.get(new Integer(label));
	if (labelStr != null) {
	    str += labelStr + " ";
	}
	
	String cpStr = (String)cpInt2Str.get(new Integer(cp));
	if (cpStr != null) {
	    str += cpStr + " ";
	}
	
	str += Integer.toString(idx) + " " + Double.toString(wgt);
	
	return str;
    }
    
} // end of class Feature

