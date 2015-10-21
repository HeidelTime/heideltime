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

package jvnsensegmenter;

import java.util.*;
import java.io.*;

import jvntextpro.util.StringUtils;
// TODO: Auto-generated Javadoc

/**
 * The Class FeatureGenerator.
 *
 * @author TuNC
 */
public class FeatureGenerator {        
        
    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String [] args ){
        if (args.length != 3){            
            printUsage();
            System.exit(1);
        }
        
        boolean label = (args[0].toLowerCase().trim().equals("-lbl"));
        
        try{
            String inputWhat = args[1].toLowerCase().trim();
        
            if (inputWhat.equals("-inputfile"))
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(args[2]), "UTF-8"));
                
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(args[2] + ".tagged"), "UTF-8"));

                String text = "", line = "";                
                while ((line = in.readLine()) != null){
                    text += "\n" + line;
                }
                text = text.trim();

                //text normalization 
                text = text.replaceAll("([\t\n\r ])+", "$1");                
                text = text.replaceAll("[\\[\\]]", "");                
                text = text.replaceAll("<[^<>]*>", "");
                    
                List MarkList = new ArrayList();
                
                ArrayList recordList =  (ArrayList) doFeatureGen(
                        new HashMap(), text , MarkList, label) ;

                for (int i = 0; i < recordList.size(); ++i){
                
                    out.write(recordList.get(i).toString());
                    
                    out.write("\n");
                
                }

                in.close();
                out.close();
            }

            else if (inputWhat.equals("-inputdir")){
                
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(args[2] + ".tagged"), "UTF-8"));

                File inputDir = new File(args[2]);
                File [] childrent = inputDir.listFiles();
                
                for (int i = 0; i <childrent.length; ++i)
                {
                    //go through all the file in the input file and do feagen
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new FileInputStream(childrent[i]), "UTF-8"));

                    String text = "", line = "";                    
                    while ((line = in.readLine()) != null){
                        text += "\n" + line;
                    }                    
                    text = text.trim();
                    
                    //text normalization 
                    text = text.replaceAll("([\t\n\r ])+", "$1");                 
                    text = text.replaceAll("[\\[\\]{}]", "");                    
                    text = text.replaceAll("<[^<>]*>", "");

                    List MarkList = new ArrayList();
                    ArrayList recordList = (ArrayList) doFeatureGen(
                            new HashMap(), text , MarkList, label) ;

                    
                    for (int j = 0; j < recordList.size(); ++j){                    
                        out.write(recordList.get(j).toString());                    
                        out.write("\n");
                    }

                    in.close();                 
                }
                out.close();
            }
         
            else printUsage();
        }
        catch (Exception e)
        {
            System.out.println("In feature generator main : " + e.getMessage());
            return;
        }
        
    }
    
    /**
     * Prints the usage.
     */
    public static void printUsage(){
        System.out.println("Usage: FeatureGeneration -lbl/-unlbl -inputfile/-inputdir [input file/input dir]");
    }
    
    /**
     * Read abbr list.
     *
     * @param dataFile the data file
     * @param map the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void readAbbrList(String dataFile, Map map) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(dataFile));
		
		String line;
		while ((line = fin.readLine()) != null) {
		    StringTokenizer strTok = new StringTokenizer(line, " \t\r\n");
		    
		    if (strTok.countTokens() <= 0) {
			continue;
		    } 
		    
		    String token = strTok.nextToken();
		    
		    map.put(token.toLowerCase(), token.toLowerCase());
		}
    }    

    /**
     * Generate context predicates for a specified text, return string representing the context predicates.
     *
     * @param map the map
     * @param text the text
     * @param markList the mark list
     * @param label the label
     * @return the list
     */
    public static List doFeatureGen(Map map, String text , List markList, boolean label){
        markList.clear();
        
        //Find out positions of .!? and store them in the markList
        int nextPos = 0;
        while( (nextPos = StringUtils.findFirstOf(text, ".!?", nextPos + 1)) != -1) 
                markList.add(new Integer(nextPos));
        
        //Generate context predicates at those positions
        List results = new ArrayList();
        for (int i = 0; i < markList.size(); ++i){                        
        
            int curPos = ((Integer) markList.get(i)).intValue();            
            String record = genCPs(map, text, curPos);
            
            //Assign label to feature string if it is specified
            if (label){
                int idx = StringUtils.findFirstNotOf(text, " \t", curPos + 1);
            
                if (idx == -1 || (text.charAt(idx) == '\n')){
                    //end of sentence
                    record += " " + "y";
                }                    
                else record += " " + "n";                                
            }
            
            results.add(record);
        }        
        return results;
    }
    
    /**
     * get context predicates at a specified position in the sequence.
     *
     * @param map the map
     * @param text the text
     * @param position the position
     * @return the string
     */
    private static String genCPs(Map map, String text, int position){        
        //get the current token(containing this mark) and its suffix & prefix            
        String token = "", suffix = "", prefix = "";
        int idx1 = -1, idx2 = -1, idx;
        
        idx1 =  StringUtils.findLastOf(text, " \t\n\r", position);
        if (idx1 == -1) idx1 = 0;        
        idx2 = StringUtils.findFirstOf(text, " \t\n\r", position + 1);
        if (idx2 == -1) idx2 = text.length();
        
        token = text.substring(idx1 + 1, idx2);                      
        if (position + 1 < idx2)
            suffix = text.substring(position + 1, idx2).trim();        
        if (idx1 + 1 < position) 
            prefix = text.substring(idx1 + 1, position).trim();
        
        //get the previous token
        
        idx = idx2; // save idx2 for get preToken later
        
        //get the previous token            
        String  preToken = "";                
        if (idx1 != 0 ){
            idx2 = StringUtils.findLastNotOf(text, " \t\n\r", idx1);            
            idx1 = StringUtils.findLastOf(text, " \t\n\r", idx2);
            
            if (idx1 == -1) idx1 = 0;            
            if (idx2 != -1) 
                preToken = text.substring(idx1, idx2 + 1).trim();
        }
        
        //get the next token
        String nexToken = ""; 
        idx2 = idx;
        
        if (idx2 != text.length()){
            idx1 = StringUtils.findFirstNotOf(text, " \t\n\r", idx2 + 1);
            idx2 = StringUtils.findFirstOf(text, " \t\n\r", idx1);
            
            if (idx2 == -1) idx2 = text.length();
            if (idx1 != -1) 
                nexToken = text.substring(idx1, idx2).trim();            
        }        
        
        //generating context predicates
        String cps = "";
        // 01:tok=
		cps += " 01=" + token;
		// 02:tok-lower
		cps += " 02=" + token.toLowerCase();
		if (StringUtils.isFirstCap(token)) {
		    // 03:tok-first-cap
		    cps += " 03";
		}	
		if (map.containsKey(token.toLowerCase())) {
		    // 04:tok-in-abbrlist
		    cps += " 04";
		}	
		if (StringUtils.containNumber(token)) {
		    // 05:tok-has-num
		    cps += " 05";
		}
		if (StringUtils.containLetter(token)) {
		    // 06:tok-has-let
		    cps += " 06";
		}	
		if (StringUtils.containLetterAndDigit(token)) {
		    // 07:tok-has-let-num
		    cps += " 07";
		}	
		if (StringUtils.isAllNumber(token)) {
		    // 08:tok-is-all-num
		    cps += " 08";
		}	
		// 09:tok-countstop
		cps += " 09=" + Integer.toString(StringUtils.countStops(token));
		// 10:tok-countsign
		cps += " 10=" + Integer.toString(StringUtils.countPuncs(token));
	
		// 11:tok-pre
		cps += " 11=" + prefix;
		// 12:tok-pre-lower
		cps += " 12=" + prefix.toLowerCase();
		if (StringUtils.isFirstCap(prefix)) {
		    // 13:tok-pre-first-cap
		    cps += " 13";
		}	
		// 14:tok-suf
		cps += " 14=" + suffix;
		// 15:tok-suf-lower
		cps += " 15=" + suffix.toLowerCase();
		if (StringUtils.isFirstCap(suffix)) {
		    // 16:tok-suf-first-cap
		    cps += " 16";
		}
	
		if (preToken != "") {
		    // 17:pre-tok
		    cps += " 17=" + preToken;
		    // 18:pre-tok-lower
		    cps += " 18=" + preToken.toLowerCase();
		    if (StringUtils.isFirstCap(preToken)) {
			// 19:pre-tok-first-cap
			cps += " 19";
		    }	
		    if (map.containsKey(preToken.toLowerCase())) {
			// 20:pre-tok-in-abbrlist
			cps += " 20";
		    }	
		    if (StringUtils.containNumber(preToken)) {
			// 21:pre-tok-has-num
			cps += " 21";
		    }
		    if (StringUtils.containLetter(preToken)) {
			// 22:pre-tok-has-let
			cps += " 22";
		    }	
		    if (StringUtils.containLetterAndDigit(preToken)) {
			// 23:pre-tok-has-let-num
			cps += " 23";
		    }	
		    if (StringUtils.isAllNumber(preToken)) {
			// 24:pre-tok-is-allnum
			cps += " 24";
		    }	
		    // 25:pre-tok-countstop
		    cps += " 25=" + Integer.toString(StringUtils.countStops(preToken));
		    // 26:pre-tok-countsign
		    cps += " 26=" + Integer.toString(StringUtils.countPuncs(preToken));
		    
		} else {
		    // 27:pre-tok
		    cps += " 27=null";
		}
	
		if (nexToken != "") {
		    // 28:nex-tok
		    cps += " 28=" + nexToken;
		    // 29:nex-tok-lower
		    cps += " 29=" + nexToken.toLowerCase();
		    if (StringUtils.isFirstCap(nexToken)) {
			// 30:nex-tok-first-cap
			cps += " 30";
		    }	
		    if (map.containsKey(nexToken.toLowerCase())) {
			// 31:nex-tok-in-abbrlist
			cps += " 31";
		    }	
		    
		    if (nexToken.startsWith("\"") || nexToken.startsWith("''") || nexToken.startsWith("``") 
				|| nexToken.startsWith("'") || nexToken.startsWith("`")) {
			cps += " 39";
		    }
		    
		    if (StringUtils.isFirstCap(nexToken)) {
			cps += " 40";
		    }
		    
		    if (StringUtils.containNumber(nexToken)) {
			// 32:nex-tok-has-num
			cps += " 32";
		    }
		    if (StringUtils.containLetter(nexToken)) {
			// 33:nex-tok-has-let
			cps += " 33";
		    }	
		    if (StringUtils.containLetterAndDigit(nexToken)) {
			// 34:nex-tok-has-let-num
			cps += " 34";
		    }	
		    if (StringUtils.isAllNumber(nexToken)) {
			// 35:nex-tok-is-allnum
			cps += " 35";
		    }	
		    // 36:nex-tok-countstop
		    cps += " 36=" + Integer.toString(StringUtils.countStops(nexToken));
		    // 37:nex-tok-countsign
		    cps += " 37=" + Integer.toString(StringUtils.countPuncs(nexToken));
		    
		} else {
		    // 38:nex-tok
		    cps += " 38=null";
		}
	        
	        //extra context predicates for Vietnamese sensegment
	        
        //39:tok-has-@
        if (token.contains("@"))            
            cps += " 39";
        
        //40:len-of-prefix
        cps += " 40=" + prefix.length();
        
        //41:len-of-suffix
        cps += " 41=" + suffix.length();
        
        //42:tok-has-slash
        if (token.contains("/"))
            cps += " 42";
        
        //43:nex-tok-first_char
        if (nexToken != "")
            cps += " 43=" + nexToken.charAt(0);
        return cps.trim();
    }    
    
   }
