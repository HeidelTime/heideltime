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
 * The Class Dictionary.
 */
public class Dictionary {

    /** The dict. */
    public Map dict = null;
    
    /** The option. */
    public Option option = null; // reference to option object
    
    /** The data. */
    public Data data = null; // reference to data object
    
    /**
     * Instantiates a new dictionary.
     */
    public Dictionary() {
	dict = new HashMap();
    }
    
    /**
     * Instantiates a new dictionary.
     *
     * @param option the option
     * @param data the data
     */
    public Dictionary(Option option, Data data) {
	this.option = option;
	this.data = data;
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
		
		int label = Integer.parseInt(lbTok.nextToken());
		int count = Integer.parseInt(lbTok.nextToken());
		int fidx = Integer.parseInt(lbTok.nextToken());
		CountFIdx cntFIdx = new CountFIdx(count, fidx);
		
		elem.lbCntFidxes.put(new Integer(label), cntFIdx);
	    }
	    
	    // insert the element to the dictionary
	    dict.put(new Integer(cp), elem);
	}
	
	System.out.println("Reading dictionary (" + Integer.toString(dict.size()) +
		    " entries) completed!");
		    
	// read the line ###...
	line = fin.readLine();
    }
    
    // write dictionary to model file
    /**
     * Write dict.
     *
     * @param fout the fout
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeDict(PrintWriter fout) throws IOException {
	Iterator it = null;
	int count = 0;

	for (it = dict.keySet().iterator(); it.hasNext(); ) {
	    Integer cpInt = (Integer)it.next();
	    Element elem = (Element)dict.get(cpInt);
	    
	    if (elem.chosen == 1) {
		count++;
	    }
	}
	
	// write the dictionary size
	fout.println(Integer.toString(count));	
	
	for (it = dict.keySet().iterator(); it.hasNext(); ) {    
	
	    Integer cpInt = (Integer)it.next();
	    Element elem = (Element)dict.get(cpInt);
	    
	    if (elem.chosen == 0) {
		continue;
	    }
	    
	    // write the context predicate and its count
	    fout.print(cpInt.toString() + ":" + Integer.toString(elem.count));
	    
	    for (Iterator lbIt = elem.lbCntFidxes.keySet().iterator(); lbIt.hasNext(); ) {
		Integer labelInt = (Integer)lbIt.next();
		CountFIdx cntFIdx = (CountFIdx)elem.lbCntFidxes.get(labelInt);
	
		if (cntFIdx.fidx < 0) {
		    continue;
		}
	
		fout.print(" " + labelInt.toString() + ":" + 
			    Integer.toString(cntFIdx.count) + ":" +
			    Integer.toString(cntFIdx.fidx));		
	    }
	    
	    fout.println();
	}
	
	// write the line ###...
	fout.println(Option.modelSeparator);
    }
    
    // add a context predicate (and the label it supports) to dictionary
    /**
     * Adds the dict.
     *
     * @param cp the cp
     * @param label the label
     * @param count the count
     */
    public void addDict(int cp, int label, int count) {
	Element elem = (Element)dict.get(new Integer(cp));
	
	if (elem == null) {
	    // if the context predicate is not found
	    elem = new Element();
	    elem.count = count;
	    
	    CountFIdx cntFIdx = new CountFIdx(count, -1);
	    elem.lbCntFidxes.put(new Integer(label), cntFIdx);
	    
	    // insert the new element to the dict
	    dict.put(new Integer(cp), elem);
	    
	} else {
	    // update the total count
	    elem.count += count;
	    
	    CountFIdx cntFIdx = (CountFIdx)elem.lbCntFidxes.get(new Integer(label));
	    if (cntFIdx == null) {
		// the label not found
		cntFIdx = new CountFIdx(count, -1);
		elem.lbCntFidxes.put(new Integer(label), cntFIdx);
		
	    } else {
		// if label found, update the count only
		cntFIdx.count += count;
	    }
	}
    }
    
    // generating dictionary from training data
    /**
     * Generate dict.
     */
    public void generateDict() {
	if (data.trnData == null) {
	    System.out.println("No data available for generating dictionary");
	    return;
	}
	
	// scan all data observations of the training data
	for (int i = 0; i < data.trnData.size(); i++) {
	    Observation obsr = (Observation)data.trnData.get(i);
	    
	    for (int j = 0; j < obsr.cps.length; j++) {
		addDict(obsr.cps[j], obsr.humanLabel, 1);
	    }
	}
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

