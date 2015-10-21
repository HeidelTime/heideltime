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

import jmaxent.*;
import java.util.*;
import java.io.*;

// TODO: Auto-generated Javadoc
/**
 * The Class JVnSenSegmenter.
 */
public class JVnSenSegmenter {
    
    /** The positive label. */
    public static String positiveLabel = "y";
    
    /** The classifier. */
    public Classification classifier = null;
    
    /** The fea gen. */
    public FeatureGenerator feaGen = null;    
    
    /**
     * Creates a new instance of JVnSenSegmenter.
     *
     * @param modelDir the model dir
     * @return true, if successful
     */
  
    public boolean init(String modelDir){
    	try {
	    	classifier = new Classification(modelDir);
	        feaGen = new FeatureGenerator();
	        classifier.init();
	        return true;
    	}
    	catch(Exception e){
    		System.out.println("Error while initilizing classifier: " + e.getMessage());
    		return false;
    	}
    }
    
    /**
     * Sen segment.
     *
     * @param text the text
     * @return the string
     */
    public String senSegment(String text){
        //text normalization         
        text = text.replaceAll("([\t \n])+", "$1");
        //System.out.println(text);
        
        //generate context predicates
        List markList = new ArrayList();
        List data = FeatureGenerator.doFeatureGen(new HashMap(), text, markList, false);

        if (markList.isEmpty())
            return text + "\n";
        
        //classify
        List labels = classifier.classify(data);
	 
        String result = text.substring(0, ((Integer)markList.get(0)).intValue());

        for (int i =0; i < markList.size(); ++i){        
            int curPos = ((Integer) markList.get(i)).intValue();            

            if ( ((String)labels.get(i)).equals(positiveLabel)){
                result += " " + text.charAt(curPos) + "\n";            
            }
            else result += text.charAt(curPos);

            if (i < markList.size() - 1){                    
                int nexPos = ((Integer) markList.get(i + 1)).intValue();                                
                result += text.substring(curPos + 1, nexPos);           
            }
        }

        int finalMarkPos = ((Integer) markList.get(markList.size() - 1)).intValue();
        result += text.substring(finalMarkPos + 1, text.length());

        //System.out.println(result);
        result = result.replaceAll("\n ", "\n");        
        result = result.replaceAll("\n\n", "\n");
        result = result.replaceAll("\\.\\. \\.", "...");
        return result;
    }
    
    /**
     * Sen segment.
     *
     * @param text the text
     * @param senList the sen list
     */
    public void senSegment(String text, List senList){
        senList.clear();
        String resultStr = senSegment(text);
    
        StringTokenizer senTknr = new StringTokenizer(resultStr, "\n");
        while(senTknr.hasMoreTokens()){
            senList.add(senTknr.nextToken());
        }        
    }

/**
 * main method of JVnSenSegmenter
 * to use this tool from command line.
 *
 * @param args the arguments
 */
    public static void main(String args[]){
        if (args.length != 4){            
            displayHelp();
            System.exit(1);
        }
        
        try{
            JVnSenSegmenter senSegmenter = new JVnSenSegmenter();
            senSegmenter.init(args[1]);
            
            String option = args[2];
            if (option.equalsIgnoreCase("-inputfile"))
            {
               senSegmentFile(args[3], args[3] + ".sent", senSegmenter);
            }
            
            else if (option.equalsIgnoreCase("-inputdir")){
                //segment only files ends with .txt
                File inputDir = new File(args[3]);
                File [] childrent = inputDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".txt");
                    }
                });
                
                for (int i = 0; i <childrent.length; ++i)
                {
                    System.out.println("Segmenting sentences in " + childrent[i]);
                    senSegmentFile(childrent[i].getPath(), childrent[i].getPath() + ".sent", senSegmenter);
                }                
            }
            else 
                displayHelp();
        }
        catch (Exception e)
        {            
            System.out.println(e.getMessage());
            return;
        }
        
    }
    
    /**
     * Segment sentences.
     *
     * @param infile the infile
     * @param outfile the outfile
     * @param senSegmenter the sen segmenter
     */
    private static void senSegmentFile(String infile, String outfile, JVnSenSegmenter senSegmenter ){
    	try{
    	 BufferedReader in = new BufferedReader(new InputStreamReader(
                 new FileInputStream(infile), "UTF-8"));
         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                 new FileOutputStream(outfile), "UTF-8"));

         String para = "", line = "", text = "";
         while ((line = in.readLine()) != null){                    
             if (!line.equals("")){
                 if (line.charAt(0) == '#'){
                     //skip comment line
                     text += line + "\n";
                     continue;
                 }
                 
                 para = senSegmenter.senSegment(line).trim();                                        
                 text += para.trim() + "\n\n";
             }
             else{
                 //blank line
                 text += "\n";                        
             }
         }
         text = text.trim();
         
         out.write(text);
         out.newLine();
         
         in.close();
         out.close();
    	}
    	catch (Exception e){
    		System.out.println("Error in sensegment file " + infile);
    	}
    }
    
    /**
     * Display help.
     */
    public static void displayHelp(){
    System.out.println("Usage:");
	System.out.println("\tCase 1: JVnSenSegmenter -modeldir <model directory> -inputfile <input data file>");
	System.out.println("\tCase 2: JVnSenSegmenter -modeldir <model directory> -inputdir <input data directory>");
	System.out.println("Where:");
	System.out.println("\t<model directory> is the directory contain the model and option files");
	System.out.println("\t<input data file> is the file containing input text that need to");
	System.out.println("\thave sentences segmented (each sentence on a line)");
	System.out.println("\t<input data directory> is the directory containing multiple input .tkn files");
	System.out.println();
    }     
}
