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
package jvntextpro.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

// TODO: Auto-generated Javadoc
/**
 * The Class TrainDataGenerating.
 */
public abstract class TrainDataGenerating {
	
	/** The reader. */
	protected DataReader reader;
	
	/** The tagger. */
	protected TaggingData tagger;
	
	/**
	 * Initialize reader, tagger for reading input data and generating context
	 * predicates for each observation.
	 */
	public abstract void init();
	
	/**
	 * Generate train data.
	 *
	 * @param inputPath the input path (file or dictionary)
	 * @param outputPath the output path
	 */
	public void generateTrainData(String inputPath, String outputPath){
		try{
			File file = new File(inputPath);
        	ArrayList<Sentence> data = new ArrayList<Sentence>();
        	if (file.isFile()){
        		System.out.println("Reading " + file.getName());
        		data = (ArrayList<Sentence>) reader.readFile(inputPath);
        	}
        	else if (file.isDirectory()){
        		String [] filenames = file.list();
        		for (String filename: filenames){
        			System.out.println("Reading " + filename);
        			ArrayList<Sentence> temp = (ArrayList<Sentence>) reader.readFile(file.getPath() + File.separator + filename);
        			data.addAll(temp);
        		}
        	}
        	
        	String result = "";
        	System.out.println(data.size() + "sentences read");
        	for (int i = 0; i < data.size(); ++i){
        		if (i % 20 == 0) System.out.println("Finished " + i + " in " + data.size() + " sentences");
        		Sentence sent = data.get(i);
        		
        		for (int j = 0; j < sent.size(); ++j){
        			//result += sent.getWordAt(j) + " ";
        			String line = "";
        			String context = tagger.getContextStr(sent, j);
        		    line = context + " ";
        			line += sent.getTagAt(j);
        			result += line + "\n";
        		}
        		result += "\n";
        	}
        	
        	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
        			new FileOutputStream(outputPath + ".tagged"), "UTF-8"));	        	
        	
        	writer.write(result);
        	writer.close();	     
		}
	  catch (Exception e){
        	System.out.println("Error while generating training data");
        	System.out.println(e.getMessage());
        	e.printStackTrace();
        }
	}
}
