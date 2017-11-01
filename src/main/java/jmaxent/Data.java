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

import jvntextpro.util.StringUtils;


// TODO: Auto-generated Javadoc
/**
 * The Class Data.
 */
public class Data {

    /** The option. */
    Option option = null;

    /** The lb str2 int. */
    public Map lbStr2Int = null;
    
    /** The lb int2 str. */
    public Map lbInt2Str = null;
    
    /** The cp str2 int. */
    public Map cpStr2Int = null;
    
    /** The cp int2 str. */
    public Map cpInt2Str = null;

    /** The trn data. */
    public List trnData = null;
    
    /** The tst data. */
    public List tstData = null;
    
    /** The ulb data. */
    public List ulbData = null;
    
    /**
     * Instantiates a new data.
     *
     * @param option the option
     */
    public Data(Option option) {
	this.option = option;
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
	
	// get size of the map
	if ((line = fin.readLine()) == null) {
	    System.out.println("No context predicate map size information");
	    return;
	}
	
	int numCps = Integer.parseInt(line);
	if (numCps <= 0) {
	    System.out.println("Invalid context predicate mapping size");
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
	
	System.out.println("Reading context predicate maps (" + 
		    Integer.toString(cpStr2Int.size()) + " entries) completed!");
	
	// read the line ###...
	line = fin.readLine();	
	
	option.numCps = cpStr2Int.size();
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
     * Write cp maps.
     *
     * @param dict the dict
     * @param fout the fout
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeCpMaps(Dictionary dict, PrintWriter fout) throws IOException {
	Iterator it = null;
    
	if (cpStr2Int == null) {
	    return;
	}
	
	int count = 0;
	for (it = cpStr2Int.keySet().iterator(); it.hasNext(); ) {
	    String cpStr = (String)it.next();
	    Integer cpInt = (Integer)cpStr2Int.get(cpStr);
	    
	    Element elem = (Element)dict.dict.get(cpInt);
	    if (elem != null) {
		if (elem.chosen == 1) {
		    count++;
		}
	    }
	}
		
	// write the map size
	fout.println(Integer.toString(count));
	
	for (it = cpStr2Int.keySet().iterator(); it.hasNext(); ) {
	    String cpStr = (String)it.next();
	    Integer cpInt = (Integer)cpStr2Int.get(cpStr);
	    
	    Element elem = (Element)dict.dict.get(cpInt);
	    if (elem != null) {
		if (elem.chosen == 1) {
		    fout.println(cpStr + " " + cpInt.toString());
		}
	    }	    
	}
	
	// write the line ###...
	fout.println(Option.modelSeparator);
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
	
	// get size of the map
	if ((line = fin.readLine()) == null) {
	    System.out.println("No label map size information");
	    return;
	}
	
	int numLabels = Integer.parseInt(line);
	if (numLabels <= 0) {
	    System.out.println("Invalid label mapping size");
	    return;
	}
	
	System.out.println("Reading the context predicate maps ...");
	
	for (int i = 0; i < numLabels; i++) {
	    line = fin.readLine();
	    if (line == null) {
		System.out.println("Invalid context predicate mapping line");
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
	
	System.out.println("Reading label maps (" + 
		    Integer.toString(lbStr2Int.size()) + " entries) completed!");
	
	// read the line ###...
	line = fin.readLine();
	
	option.numLabels = lbStr2Int.size();	    
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
    
    /**
     * Write lb maps.
     *
     * @param fout the fout
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeLbMaps(PrintWriter fout) throws IOException {
	if (lbStr2Int == null) {
	    return;
	}
	
	// write the map size
	fout.println(Integer.toString(lbStr2Int.size()));
	
	for (Iterator it = lbStr2Int.keySet().iterator(); it.hasNext(); ) {
	    String lbStr = (String)it.next();
	    Integer lbInt = (Integer)lbStr2Int.get(lbStr);
	    
	    fout.println(lbStr + " " + lbInt.toString());
	}    
	
	// write the line ###...
	fout.println(Option.modelSeparator);	
    }
    
    /**
     * Read trn data.
     *
     * @param dataFile the data file
     */
    public void readTrnData(String dataFile) {
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
	
	if (trnData != null) {
	    trnData.clear();
	} else {
	    trnData = new ArrayList();
	}

	// open data file	
	BufferedReader fin = null;
	
	try {
	    fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
	//    BufferedWriter flog = new BufferedWriter(new OutputStreamWriter(
//	/	new FileOutputStream((new File(dataFile)).getParent() + File.separator + "log.txt"), "UTF-8"));
	    
	    System.out.println("Reading training data ...");
	    
	    String line;
	    while ((line = fin.readLine()) != null) {
		StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
		int len = strTok.countTokens();
				
		if (len <= 1) {
		    // skip this invalid line
		    continue;
		}
		
		List strCps = new ArrayList();
		for (int i = 0; i < len - 1; i++) {
		    strCps.add(strTok.nextToken());
		}
		
		String labelStr = strTok.nextToken();
//		
//		String [] tags = {"N", "Np", "Nc", "Nu", "V", "A", "P", "L", "M", 
//		"R", "E", "C", "I", "T", "U", "Y", "X", "LBKT", "RBKT"};
//
//
//		//System.out.println("--" + labelStr);
//		//if (!StringUtils.isSign(labelStr)){
//			boolean flag = false;
//			for (String tag : tags){
//				if (labelStr.equalsIgnoreCase(tag)){
//					flag = true;
//				}
//			}
//			
//			if (!flag){
//			//	flog.write(line + "\n");
//				//System.out.println("--" + labelStr);
//			}
//		//}
		
		List intCps = new ArrayList();
		
		for (int i = 0; i < strCps.size(); i++) {	
		    String cpStr = (String)strCps.get(i);		    
		    Integer cpInt = (Integer)cpStr2Int.get(cpStr);		    
		    if (cpInt != null) {
			intCps.add(cpInt);
		    } else {
			intCps.add(new Integer(cpStr2Int.size()));
			cpStr2Int.put(cpStr, new Integer(cpStr2Int.size()));
			cpInt2Str.put(new Integer(cpInt2Str.size()), cpStr);
		    }
		}
		
		Integer labelInt = (Integer)lbStr2Int.get(labelStr);
		if (labelInt == null) {
		    labelInt = new Integer(lbStr2Int.size());
		    
//		    System.out.println("hey:" + labelStr);
//		    flog.write(labelStr + "\t" + line + "\n");
		    lbStr2Int.put(labelStr, labelInt);
		    lbInt2Str.put(labelInt, labelStr);
		}
		
		int[] cps = new int[intCps.size()];
		for (int i = 0; i < cps.length; i++) {
		    cps[i] = ((Integer)intCps.get(i)).intValue();
		}
		
		Observation obsr = new Observation(labelInt.intValue(), cps);
		
		// add this observation to the data
		trnData.add(obsr);
	    }
	    
	    System.out.println("Reading " + Integer.toString(trnData.size()) +
			" training data examples completed!");
	  // flog.close();
	
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return;
	}

	option.numCps = cpStr2Int.size();
	option.numLabels = lbStr2Int.size();	
	option.numTrainExps = trnData.size();	
    }
    
    /**
     * Read tst data.
     *
     * @param dataFile the data file
     */
    public void readTstData(String dataFile) {
	if (tstData != null) {
	    tstData.clear();
	} else {
	    tstData = new ArrayList();
	}

	// open data file	
	BufferedReader fin = null;
	
	try {
	    fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));	    
	    System.out.println("Reading testing data ...");
	    
	    String line;
	    while ((line = fin.readLine()) != null) {
		StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
		int len = strTok.countTokens();
		
		if (len <= 1) {
		    // skip this invalid line
		    continue;
		}
		
		List strCps = new ArrayList();
		for (int i = 0; i < len - 1; i++) {
		    strCps.add(strTok.nextToken());
		}
		
		String labelStr = strTok.nextToken();

		List intCps = new ArrayList();
		
		for (int i = 0; i < strCps.size(); i++) {	
		    String cpStr = (String)strCps.get(i);
		    Integer cpInt = (Integer)cpStr2Int.get(cpStr);		    
		    if (cpInt != null) {
			intCps.add(cpInt);
		    } else {
			// do nothing
		    }
		}
		
		Integer labelInt = (Integer)lbStr2Int.get(labelStr);
		if (labelInt == null) {
		    System.out.println("Reading testing observation, label not found or invalid");
		    return;
		}
		
		int[] cps = new int[intCps.size()];
		for (int i = 0; i < cps.length; i++) {
		    cps[i] = ((Integer)intCps.get(i)).intValue();
		}
		
		Observation obsr = new Observation(labelInt.intValue(), cps);
		
		// add this observation to the data
		tstData.add(obsr);
	    }
	    
	    System.out.println("Reading " + Integer.toString(tstData.size()) +
			" testing data examples completed!");	    
	
	} catch (IOException e) {
	    System.out.println(e.toString());
	    return;
	}
	
	option.numTestExps = tstData.size();		
    }

/*    
    public void writeTstData(String dataFile) {
    }
    
    public void readUlbData(String dataFile) {
    }
    
    public void writeUlbDataWithModelLabel(String dataFile) {
    }
*/
      
} // end of class Data

