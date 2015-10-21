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
import jvntextpro.data.TaggingData;
import jvntextpro.data.TrainDataGenerating;

// TODO: Auto-generated Javadoc
/**
 * The Class POSTrainGenerating.
 */
public class POSTrainGenerating extends TrainDataGenerating {
	
	/** The template file. */
	String templateFile;
	
	/**
	 * Instantiates a new pOS train generating.
	 *
	 * @param templateFile the template file (in xml format) to generate context predicates
	 * using POSContextGenerator
	 */
	public POSTrainGenerating(String templateFile){
		this.templateFile = templateFile;
		init();
	}
	
	/* (non-Javadoc)
	 * @see jvntextpro.data.TrainDataGenerating#init()
	 */
	@Override
	public void init() {
		// TODO Auto-generated method stub
		this.reader =  new POSDataReader(true);
		this.tagger = new TaggingData();
		tagger.addContextGenerator(new POSContextGenerator(templateFile));
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String [] args){
		  //tagging
		if (args.length != 2){
			System.out.println("POSTrainGenerating [template File] [File/Folder]");
			System.out.println("Generating training data for word segmentation with FlexCRFs++ or jvnmaxent (in JVnTextPro)");
			System.out.println("Template File: featuretemplate to generate context predicates");
			System.out.println("Input File/Folder: file/folder name containing data manually tagged for training");
			return;
		}
		
		POSTrainGenerating trainGen = new POSTrainGenerating(args[0]);
		trainGen.generateTrainData(args[1], args[1]);		
	}
}
