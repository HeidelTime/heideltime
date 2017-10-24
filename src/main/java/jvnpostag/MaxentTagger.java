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

package jvnpostag;

import java.io.File;
import java.util.List;

import jmaxent.Classification;
import jvntextpro.data.DataReader;
import jvntextpro.data.DataWriter;
import jvntextpro.data.Sentence;
import jvntextpro.data.TaggingData;
import jvntextpro.util.StringUtils;

public class MaxentTagger implements POSTagger {
	DataReader reader = new POSDataReader();
	DataWriter writer = new POSDataWriter();
	TaggingData dataTagger = new TaggingData();
	
	Classification classifier = null;
	
	public MaxentTagger(String modelDir){
		init(modelDir);
	}
	public void init(String modeldir) {
		// TODO Auto-generated method stub
		dataTagger.addContextGenerator(new POSContextGenerator(modeldir + File.separator + "featuretemplate.xml"));
		classifier = new Classification(modeldir);	
	}

	public String tagging(String instr) {
		// TODO Auto-generated method stub
		System.out.println("tagging ....");
		List<Sentence> data = reader.readString(instr);
		for (int i = 0; i < data.size(); ++i){
        	
    		Sentence sent = data.get(i);
    		for (int j = 0; j < sent.size(); ++j){
    			String [] cps = dataTagger.getContext(sent, j);
    			String label = classifier.classify(cps);
    			
    			if (label.equalsIgnoreCase("Mrk")){
    				if (StringUtils.isPunc(sent.getWordAt(j)))
    					label = sent.getWordAt(j);
    				else label = "X";
    			}
    			
    			sent.getTWordAt(j).setTag(label);
    		}
    	}
		
		return writer.writeString(data);
	}

	
	public String tagging(File file) {
		// TODO Auto-generated method stub
		List<Sentence> data = reader.readFile(file.getPath());
		for (int i = 0; i < data.size(); ++i){
        	
    		Sentence sent = data.get(i);
    		for (int j = 0; j < sent.size(); ++j){
    			String [] cps = dataTagger.getContext(sent, j);
    			String label = classifier.classify(cps);
    			
    			if (label.equalsIgnoreCase("Mrk")){
    				if (StringUtils.isPunc(sent.getWordAt(j)))
    					label = sent.getWordAt(j);
    				else label = "X";
    			}
    			
    			sent.getTWordAt(j).setTag(label);    
    			//System.out.println(sent.getTagAt(j));
    		}
    	}
		
		return writer.writeString(data);
	}

	public void setDataReader(DataReader reader){
		this.reader = reader;
	}
	
	public void setDataWriter(DataWriter writer){
		this.writer = writer;
	}
}
