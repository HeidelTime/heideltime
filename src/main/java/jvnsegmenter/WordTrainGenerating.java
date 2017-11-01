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

import jvntextpro.data.TaggingData;
import jvntextpro.data.TrainDataGenerating;

// TODO: Auto-generated Javadoc
/**
 * The Class WordTrainGenerating.
 */
public class WordTrainGenerating extends TrainDataGenerating {
	
	/** The model dir. */
	String modelDir;
	
	/**
	 * Instantiates a new word train generating.
	 *
	 * @param modelDir the model dir
	 */
	public WordTrainGenerating(String modelDir){
		this.modelDir = modelDir;
		init();
	}
	
	/* (non-Javadoc)
	 * @see jvntextpro.data.TrainDataGenerating#init()
	 */
	@Override
	public void init() {
		// TODO Auto-generated method stub
		reader = new IOB2DataReader();
		tagger = new TaggingData();
		
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
				tagger.addContextGenerator(contextGen);
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String [] args){
		  //tagging
		if (args.length != 2){
			System.out.println("WordTrainGenerating [Model Dir] [File/Folder]");
			System.out.println("Generating training data for word segmentation with FlexCRFs++ or jvnmaxent (in JVnTextPro)");
			System.out.println("Model Dir: directory containing featuretemple file");
			System.out.println("Input File/Folder: file/folder name containing data manually tagged for training");
			return;
		}
		
		WordTrainGenerating trainGen = new WordTrainGenerating(args[0]);
		trainGen.generateTrainData(args[1], args[1]);		
	}

}
