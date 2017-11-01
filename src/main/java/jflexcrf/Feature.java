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
 * The Class Feature.
 */
public class Feature {
    // feature types; second-order Markov is not supported
    /** The Constant UNKNOWN_FEATURE. */
    static public final int UNKNOWN_FEATURE = 0;
    
    /** The Constant EDGE_FEATURE1. */
    static public final int EDGE_FEATURE1 = 1;
    // static public final int EDGE_FEATURE2 = 2;
    /** The Constant STAT_FEATURE1. */
    static public final int STAT_FEATURE1 = 3;
    // static public final int STAT_FEATURE2 = 4;
    
    /** The ftype. */
    int ftype = UNKNOWN_FEATURE; // feature type
    
    /** The idx. */
    int idx = -1;	// feature index
    
    /** The str id. */
    String strId = "";	// string identifier
    
    /** The y. */
    int y = -1;		// current label
    
    /** The yp. */
    int yp = -1;	// previous label
    
    /** The cp. */
    int cp = -1;	// context predicate
    
    /** The val. */
    float val = 1;	// feature value
    
    /** The wgt. */
    double wgt = 0.0;	// feature weight
    
    // edge feature (type 1) initialization
    /**
     * E feature1 init.
     *
     * @param y the y
     * @param yp the yp
     */
    public void eFeature1Init(int y, int yp) {
	ftype = EDGE_FEATURE1;
	idx = -1;
	this.y = y;
	this.yp = yp;
	val = 1;
	wgt = 0.0;	
	strId = "e1_" + Integer.toString(y) + "_" + Integer.toString(yp);
    }
    
    /**
     * E feature1 init.
     *
     * @param y the y
     * @param yp the yp
     * @param fmap the fmap
     */
    public void eFeature1Init(int y, int yp, Map fmap) {
	eFeature1Init(y, yp);
	strId2IdxAdd(fmap);
    }    
    
    // state feature (type 1) initialization
    /**
     * S feature1 init.
     *
     * @param y the y
     * @param cp the cp
     */
    public void sFeature1Init(int y, int cp) {
	ftype = STAT_FEATURE1;
	idx = -1;
	this.y = y;
	this.cp = cp;
	val = 1;
	wgt = 0.0;
	strId = "s1_" + Integer.toString(y) + "_" + Integer.toString(cp);
    }
    
    /**
     * S feature1 init.
     *
     * @param y the y
     * @param yp the yp
     * @param fmap the fmap
     */
    public void sFeature1Init(int y, int yp, Map fmap) {
	sFeature1Init(y, cp);
	strId2IdxAdd(fmap);
    }
    
    /**
     * Instantiates a new feature.
     */
    public Feature() {
    }
    
    // feature constructor that parses an input line
    /**
     * Instantiates a new feature.
     *
     * @param line the line
     * @param cpStr2Int the cp str2 int
     * @param lbStr2Int the lb str2 int
     */
    public Feature(String line, Map cpStr2Int, Map lbStr2Int) {

	StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
	int len = strTok.countTokens();
	
	String strIdStr = strTok.nextToken();
	int idx = Integer.parseInt(strTok.nextToken());
	float val = 1;
	double wgt = Double.parseDouble(strTok.nextToken());
	
	// parsing string identifier
	StringTokenizer strIdTok = new StringTokenizer(strIdStr, "_");
	String prefix = strIdTok.nextToken();
	
	if (prefix.compareToIgnoreCase("e1") == 0) {
	    // edge feature type 1
	    Integer yInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    Integer ypInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    
	    if (yInt != null && ypInt != null) {
		eFeature1Init(yInt.intValue(), ypInt.intValue());
	    }
	
	} else if (prefix.compareToIgnoreCase("s1") == 0) {
	    // state feature type 1
	    Integer yInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    Integer cpInt = (Integer)cpStr2Int.get(strIdTok.nextToken());
	    
	    if (yInt != null && cpInt != null) {
		sFeature1Init(yInt.intValue(), cpInt.intValue());
	    }
			    
	} 
	
	this.idx = idx;
	this.val = val;
	this.wgt = wgt;	
    }
    
    // feature constructor that parses an input line (adding to the feature map)
    /**
     * Instantiates a new feature.
     *
     * @param line the line
     * @param cpStr2Int the cp str2 int
     * @param lbStr2Int the lb str2 int
     * @param fmap the fmap
     */
    public Feature(String line, Map cpStr2Int, Map lbStr2Int, Map fmap) {

	StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
	int len = strTok.countTokens();
	
	String strIdStr = strTok.nextToken();
	int idx = Integer.parseInt(strTok.nextToken());
	float val = 1;
	double wgt = Double.parseDouble(strTok.nextToken());
	
	// parsing string identifier
	StringTokenizer strIdTok = new StringTokenizer(strIdStr, "_");
	String prefix = strIdTok.nextToken();
	
	if (prefix.compareToIgnoreCase("e1") == 0) {
	    // edge feature type 1
	    Integer yInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    Integer ypInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    
	    if (yInt != null && ypInt != null) {
		eFeature1Init(yInt.intValue(), ypInt.intValue());
	    }
	
	} else if (prefix.compareToIgnoreCase("s1") == 0) {
	    // state feature type 1
	    Integer yInt = (Integer)lbStr2Int.get(strIdTok.nextToken());
	    Integer cpInt = (Integer)cpStr2Int.get(strIdTok.nextToken());
	    
	    if (yInt != null && cpInt != null) {
		sFeature1Init(yInt.intValue(), cpInt.intValue());
	    }
	    
	}
	
	this.idx = idx;
	this.val = val;
	this.wgt = wgt;	
    
	strId2IdxAdd(fmap);
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
     * @return the int
     */
    public int index() {
	return idx;
    }
    
    // return the feature index (lookup the map)
    /**
     * Index.
     *
     * @param fmap the fmap
     * @return the int
     */
    public int index(Map fmap) {
	return strId2Idx(fmap);
    }
    
    // convert feature to string "<identifier> <index> <weight>"
    /**
     * To string.
     *
     * @param cpInt2Str the cp int2 str
     * @param lbInt2Str the lb int2 str
     * @return the string
     */
    public String toString(Map cpInt2Str, Map lbInt2Str) {
	String str = "";
	
	if (ftype == EDGE_FEATURE1) {
	    // edge feature type 1
	    str = "e1_";
	    
	    String yStr = (String)lbInt2Str.get(new Integer(y));
	    if (yStr != null) {
		str += yStr + "_";
	    }
	    
	    String  ypStr = (String)lbInt2Str.get(new Integer(yp));
	    if (ypStr != null) {
		str += ypStr;
	    }	    
	
	} else if (ftype == STAT_FEATURE1) {
	    // state feature type 1
	    str = "s1_";
	    
	    String yStr = (String)lbInt2Str.get(new Integer(y));
	    if (yStr != null) {
		str += yStr + "_";
	    }
	    
	    String cpStr = (String)cpInt2Str.get(new Integer(cp));
	    if (cpStr != null) {
		str += cpStr;
	    }
	    
	} 
	
	str += " " + Integer.toString(idx) + " " + Double.toString(wgt);
	return str;
    }
        
} // end of class Feature

