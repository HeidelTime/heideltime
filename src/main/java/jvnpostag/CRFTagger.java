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

import jflexcrf.Labeling;
import jvntextpro.data.DataReader;
import jvntextpro.data.DataWriter;
import jvntextpro.data.TaggingData;

public class CRFTagger implements POSTagger {
	DataReader reader = new POSDataReader();
	DataWriter writer = new POSDataWriter();
	
	TaggingData dataTagger = new TaggingData();
	
	Labeling labeling = null;
	
	public CRFTagger(String modelDir){
		init(modelDir);
	}
	
	public void init(String modelDir) {
		// TODO Auto-generated method stub
		dataTagger.addContextGenerator(new POSContextGenerator(modelDir + File.separator + "featuretemplate.xml"));
		labeling = new Labeling(modelDir, dataTagger, reader, writer);
	}

	public String tagging(String instr) {
		// TODO Auto-generated method stub
		return labeling.strLabeling(instr);
	}

	public String tagging(File file) {
		// TODO Auto-generated method stub
		return labeling.strLabeling(file);
	}
	
	public void setDataReader(DataReader reader){
		this.reader = reader;
	}
	
	public void setDataWriter(DataWriter writer){
		this.writer = writer;
	}

}
