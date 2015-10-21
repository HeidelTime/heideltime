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
 * The Class Option.
 */
public class Option {
    
    /** The Constant FIRST_ORDER. */
    static public final int FIRST_ORDER = 1;
    // second-order Markov is not supported currently
    /** The Constant SECOND_ORDER. */
    static public final int SECOND_ORDER = 2;
    
    /** The Constant inputSeparator. */
    public static final String inputSeparator = "|";
    
    /** The Constant outputSeparator. */
    public static final String outputSeparator = "|";
    
    // model directory, default is current dir
    /** The model dir. */
    public String modelDir = ".";
    // model file (mapping, dictionary, and features)
    /** The model file. */
    public final String modelFile = "model.txt";
    // option file
    /** The option file. */
    public final String optionFile = "option.txt";
    
    /** The order. */
    public int order = FIRST_ORDER;	// 1: first-order Markov; 2: second-order Markov
    
    /**
     * Instantiates a new option.
     */
    public Option() {
    }
    
    /**
     * Instantiates a new option.
     *
     * @param modelDir the model dir
     */
    public Option(String modelDir) {
	if (modelDir.endsWith(File.separator)) {
	    this.modelDir = modelDir.substring(0, modelDir.length() - 1);
	} else {
	    this.modelDir = modelDir;
	}
    }
    
    /**
     * Read options.
     *
     * @return true, if successful
     */
    public boolean readOptions() {
	String filename = modelDir + File.separator + optionFile;
	BufferedReader fin = null;
	String line;
	
	try {
	    fin = new BufferedReader(new FileReader(filename));

	    System.out.println("Reading options ...");
	    	    
	    // read option lines
	    while ((line = fin.readLine()) != null) {
		String trimLine = line.trim();
		if (trimLine.startsWith("#")) {
		    // comment line
		    continue;
		}
		
		StringTokenizer strTok = new StringTokenizer(line, "= \t\r\n");
		int len = strTok.countTokens();
		if (len != 2) {
		    // invalid parameter line, ignore it
		    continue;
		}
		
		String strOpt = strTok.nextToken();
		String strVal = strTok.nextToken();
		
		if (strOpt.compareToIgnoreCase("order") == 0) {
		    int numTemp = Integer.parseInt(strVal);
		    order = numTemp;
		}		
	    }
	    
	    System.out.println("Reading options completed!");
	
	} catch (IOException e) {
	    System.out.println("Couldn't open and read option file: " + optionFile);
	    System.out.println(e.toString());
	    return false;
	}
	
	return true;	
    }
    
    /**
     * Open model file.
     *
     * @return the buffered reader
     */
    public BufferedReader openModelFile() {
	String filename = modelDir + File.separator + modelFile;
	BufferedReader fin = null;
	
	try {
	    fin = new BufferedReader(new FileReader(filename));
	    
	} catch (IOException e) {
	    System.out.println("Couldn't open model file: " + filename);
	    System.out.println(e.toString());
	    fin = null;
	}
	
	return fin;
    }

} // end of class Option

