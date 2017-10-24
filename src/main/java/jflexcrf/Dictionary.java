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
 * The Class Dictionary.
 */
public class Dictionary {
    
    /** The dict. */
    public Map dict = null;			// map between context predicate and element
    
    /**
     * Instantiates a new dictionary.
     */
    Dictionary() {
	dict = new HashMap();
    }
    
    // read dictionary from model file
    /**
     * Read dict.
     *
     * @param fin the fin
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void readDict(BufferedReader fin) throws IOException {	
	// clear any previous content
	dict.clear();
	    
	String line;
	
	// get dictionary size
	if ((line = fin.readLine()) == null) {
	    System.out.println("No dictionary size information");
	    return;
	}	
	int dictSize = Integer.parseInt(line);
	if (dictSize <= 0) {
	    System.out.println("Invalid dictionary size");
	}
	
	System.out.println("Reading dictionary ...");
	
	// main loop for reading dictionary content
	for (int i = 0; i < dictSize; i++) {
	    line = fin.readLine();
	    if (line == null) {
		System.out.println("Invalid dictionary line");
		return;
	    }
	    
	    StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
	    int len = strTok.countTokens();
	    if (len < 2) {
		// invalid line
		continue;
	    }	    
	    
	    StringTokenizer cpTok = new StringTokenizer(strTok.nextToken(), ":");
	    int cp = Integer.parseInt(cpTok.nextToken());
	    int cpCount = Integer.parseInt(cpTok.nextToken());
	    
	    // create a new element
	    Element elem = new Element();
	    elem.count = cpCount;
	    elem.chosen = 1;
	    
	    while (strTok.hasMoreTokens()) {
		StringTokenizer lbTok = new StringTokenizer(strTok.nextToken(), ":");
		
		int order = Integer.parseInt(lbTok.nextToken());
		int label = Integer.parseInt(lbTok.nextToken());
		int count = Integer.parseInt(lbTok.nextToken());
		int fidx = Integer.parseInt(lbTok.nextToken());
		CountFeatureIdx cntFeaIdx = new CountFeatureIdx(count, fidx);
		
		if (order == Option.FIRST_ORDER) {
		    elem.lbCntFidxes.put(new Integer(label), cntFeaIdx);		
		} else if (order == Option.SECOND_ORDER) {
		    // do nothing, second-order Markov is not supported
		}		    
	    }

	    // insert the element to the dictionary
	    dict.put(new Integer(cp), elem);
	}
	
	System.out.println("Reading dictionary (" + Integer.toString(dict.size()) + " entries) completed!");
	
	// read the line ###...
	line = fin.readLine();
    }
    
    /**
     * Size.
     *
     * @return the int
     */
    public int size() {
	if (dict == null) {
	    return 0;
	} else {
	    return dict.size();
	}
    }
    
} // end of class Dictionary

