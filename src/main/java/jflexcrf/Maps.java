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
 * The Class Maps.
 */
public class Maps {
    
    /** The cp str2 int. */
    public Map cpStr2Int = null;
    
    /** The cp int2 str. */
    public Map cpInt2Str = null;
    
    /** The lb str2 int. */
    public Map lbStr2Int = null;
    
    /** The lb int2 str. */
    public Map lbInt2Str = null;
    
    /**
     * Instantiates a new maps.
     */
    public Maps() {
    }
    
    /**
     * Read cp maps.
     *
     * @param fin the fin
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void readCpMaps(BufferedReader fin) throws IOException {
	if (cpStr2Int != null) {
	    cpStr2Int.clear();
	} else {
	    cpStr2Int = new HashMap();
	}
	
	if (cpInt2Str != null) {
	    cpInt2Str.clear();
	} else {
	    cpInt2Str = new HashMap();
	}
	
	String line;
	
	// get size of context predicate map
	if ((line = fin.readLine()) == null) {
	    System.out.println("No context predicate map size information");
	    return;
	}
	int numCps = Integer.parseInt(line);
	if (numCps <= 0) {
	    System.out.println("Invalid mapping size");
	    return;
	}
	
	System.out.println("Reading the context predicate maps ...");
		
	for (int i = 0; i < numCps; i++) {
	    line = fin.readLine();
	    if (line == null) {
		System.out.println("Invalid context predicate mapping line");
		return;
	    }
	    
	    StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
	    if (strTok.countTokens() != 2) {
		continue;
	    }
	    
	    String cpStr = strTok.nextToken();
	    String cpInt = strTok.nextToken();
	    
	    cpStr2Int.put(cpStr, new Integer(cpInt));
	    cpInt2Str.put(new Integer(cpInt), cpStr);
	}
	
	System.out.println("Reading context predicate maps (" + Integer.toString(cpStr2Int.size()) + 
		    " entries) completed!");
	
	// read the line ###...
	line = fin.readLine();
    }
    
    /**
     * Read lb maps.
     *
     * @param fin the fin
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void readLbMaps(BufferedReader fin) throws IOException {
	if (lbStr2Int != null) {
	    lbStr2Int.clear();
	} else {
	    lbStr2Int = new HashMap();
	}
	
	if (lbInt2Str != null) {
	    lbInt2Str.clear();
	} else {
	    lbInt2Str = new HashMap();
	}

	String line;
	
	// get size of label map
	if ((line = fin.readLine()) == null) {
	    System.out.println("No label map size information");
	    return;
	}
	int numLabels = Integer.parseInt(line);
	if (numLabels <= 0) {
	    System.out.println("Invalid label map size");
	    return;
	}
	
	System.out.println("Reading label maps ...");
	
	for (int i = 0; i < numLabels; i++) {
	    line = fin.readLine();
	    if (line == null) {
		System.out.println("Invalid label map line");
		return;
	    }
	    
	    StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
	    if (strTok.countTokens() != 2) {
		continue;
	    }
	    
	    String lbStr = strTok.nextToken();
	    String lbInt = strTok.nextToken();
	    
	    lbStr2Int.put(lbStr, new Integer(lbInt));
	    lbInt2Str.put(new Integer(lbInt), lbStr);
	}
	
	System.out.println("Reading label maps (" + Integer.toString(lbStr2Int.size()) + 
		    " entries) completed!");
	
	// read the line ###...
	line = fin.readLine();
    }
    
    /**
     * Num cps.
     *
     * @return the int
     */
    public int numCps() {
	if (cpStr2Int == null) {
	    return 0;
	} else {
	    return cpStr2Int.size();
	}
    }
    
    /**
     * Num labels.
     *
     * @return the int
     */
    public int numLabels() {
	if (lbStr2Int == null) {
	    return 0;
	} else {
	    return lbStr2Int.size();
	}
    }
    
} // end of class Maps

