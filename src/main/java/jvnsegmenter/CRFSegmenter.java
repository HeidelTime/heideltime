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
package jvnsegmenter;

import java.io.File;
import java.util.Vector;

import org.w3c.dom.Element;

import jflexcrf.Labeling;
import jvntextpro.data.DataReader;
import jvntextpro.data.DataWriter;
import jvntextpro.data.TaggingData;

// TODO: Auto-generated Javadoc
/**
 * The Class CRFSegmenter.
 */
public class CRFSegmenter {
	
	/** The reader. */
	DataReader reader = new WordDataReader();
	
	/** The writer. */
	DataWriter writer = new WordDataWriter();
	
	/** The data tagger. */
	TaggingData dataTagger = new TaggingData();
	
	/** The labeling. */
	Labeling labeling = null;
	
	/**
	 * Instantiates a new cRF segmenter.
	 *
	 * @param modelDir the model dir
	 */
	public CRFSegmenter(String modelDir){
		init(modelDir);
	}
	
	/**
	 * Instantiates a new cRF segmenter.
	 */
	public CRFSegmenter() {
		//do nothing until now
	}

	/**
	 * Inits the.
	 *
	 * @param modelDir the model dir
	 */
	public void init(String modelDir) {
		//Read feature template file
		String templateFile = modelDir + File.separator + "featuretemplate.xml";
		Vector<Element> nodes = BasicContextGenerator.readFeatureNodes(templateFile); 
		
		for (int i = 0; i < nodes.size(); ++i){
			Element node = nodes.get(i);
			String cpType = node.getAttribute("value");
			BasicContextGenerator contextGen = null;
			
			if (cpType.equals("Conjunction")){
				contextGen = new ConjunctionContextGenerator(node);
			}
			else if (cpType.equals("Lexicon")){
				contextGen = new LexiconContextGenerator(node);	
				LexiconContextGenerator.loadVietnameseDict(modelDir + File.separator + "VNDic_UTF-8.txt");
				LexiconContextGenerator.loadViLocationList(modelDir + File.separator + "vnlocations.txt");
				LexiconContextGenerator.loadViPersonalNames(modelDir + File.separator + "vnpernames.txt");
			}
			else if (cpType.equals("Regex")){
				contextGen = new RegexContextGenerator(node);
			}
			else if (cpType.equals("SyllableFeature")){
				contextGen = new SyllableContextGenerator(node);
			}
			else if (cpType.equals("ViSyllableFeature")){
				contextGen = new VietnameseContextGenerator(node);
			}
			
			if (contextGen != null)
				dataTagger.addContextGenerator(contextGen);
		}
		
		//create context generators
		labeling = new Labeling(modelDir, dataTagger, reader, writer);
	}

	/**
	 * Segmenting.
	 *
	 * @param instr the instr
	 * @return the string
	 */
	public String segmenting(String instr) {
		return labeling.strLabeling(instr);
	}

	/**
	 * Segmenting.
	 *
	 * @param file the file
	 * @return the string
	 */
	public String segmenting(File file) {
		return labeling.strLabeling(file);
	}
	
	/**
	 * Sets the data reader.
	 *
	 * @param reader the new data reader
	 */
	public void setDataReader(DataReader reader){
		this.reader = reader;
	}
	
	/**
	 * Sets the data writer.
	 *
	 * @param writer the new data writer
	 */
	public void setDataWriter(DataWriter writer){
		this.writer = writer;
	}

}
