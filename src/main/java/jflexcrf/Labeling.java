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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jvntextpro.data.DataReader;
import jvntextpro.data.DataWriter;
import jvntextpro.data.Sentence;
import jvntextpro.data.TaggingData;

// TODO: Auto-generated Javadoc
/**
 * The Class Labeling.
 */
public class Labeling {
	
	//-----------------------------------------------
	// Member Variables
	//-----------------------------------------------
	/** The model dir. */
	private String modelDir = "";	
	
	/** The tagger maps. */
	public Maps taggerMaps = null;
	
	/** The tagger dict. */
	public Dictionary taggerDict = null;
	
	/** The tagger f gen. */
	private FeatureGen taggerFGen = null;
	
	/** The tagger vtb. */
	private Viterbi taggerVtb = null;
	
	/** The tagger model. */
	private Model taggerModel = null;
	
	/** The data tagger. */
	private TaggingData dataTagger = null;
	
	/** The data reader. */
	private DataReader dataReader = null;
	
	/** The data writer. */
	private DataWriter dataWriter = null;
	
	//-----------------------------------------------
	// Initilization
	//-----------------------------------------------
	/**
	 * Instantiates a new labeling.
	 *
	 * @param modelDir the model dir
	 * @param dataTagger the data tagger
	 * @param dataReader the data reader
	 * @param dataWriter the data writer
	 */
	public Labeling (String modelDir, TaggingData dataTagger, 
			DataReader dataReader, DataWriter dataWriter){
		init(modelDir);
		this.dataTagger = dataTagger;
		this.dataWriter = dataWriter;
		this.dataReader = dataReader;		
	}
	
	/**
	 * Inits the.
	 *
	 * @param modelDir the model dir
	 * @return true, if successful
	 */
	public boolean init(String modelDir) {
		this.modelDir = modelDir;
		
		Option taggerOpt = new Option(this.modelDir);
		if (!taggerOpt.readOptions()) {
			return false;
		}

		taggerMaps = new Maps();
		taggerDict = new Dictionary();
		taggerFGen = new FeatureGen(taggerMaps, taggerDict);
		taggerVtb = new Viterbi();

		taggerModel = new Model(taggerOpt, taggerMaps, taggerDict, taggerFGen,
				taggerVtb);
		if (!taggerModel.init()) {
			System.out.println("Couldn't load the model");
			System.out.println("Check the <model directory> and the <model file> again");
			return false;
		}
		return true;
	}
	
	/**
	 * Sets the data reader.
	 *
	 * @param reader the new data reader
	 */
	public void setDataReader (DataReader reader){
		dataReader = reader;
	}
	
	/**
	 * Sets the data tagger.
	 *
	 * @param tagger the new data tagger
	 */
	public void setDataTagger(TaggingData tagger){
		dataTagger = tagger;
	}
	
	/**
	 * Sets the data writer.
	 *
	 * @param writer the new data writer
	 */
	public void setDataWriter(DataWriter writer){
		dataWriter = writer;
	}
	
	//---------------------------------------------------------
	// labeling methods
	//---------------------------------------------------------
	/**
	 * labeling observation sequences.
	 *
	 * @param data list of sequences with specified format which can be read by DataReader
	 * @return a list of sentences with tags annotated
	 */
	@SuppressWarnings("unchecked")
	public List seqLabeling(String data){
		List<Sentence> obsvSeqs = dataReader.readString(data);
		return labeling(obsvSeqs);
	}	
	
	/**
	 * labeling observation sequences.
	 *
	 * @param file the file
	 * @return  a list of sentences with tags annotated
	 */
	@SuppressWarnings("unchecked")
	public List seqLabeling(File file){
		List<Sentence> obsvSeqs = dataReader.readFile(file.getPath());
		return labeling(obsvSeqs);
	}
	
	/**
	 * labeling observation sequences.
	 *
	 * @param data the data
	 * @return string representing label sequences, the format is specified by writer
	 */
	@SuppressWarnings("unchecked")
	public String strLabeling(String data){
		List lblSeqs = seqLabeling(data);
		String ret = dataWriter.writeString(lblSeqs);
		return ret;
	}
	
	/**
	 * labeling observation sequences.
	 *
	 * @param file contains a list of observation sequence, this file has a format wich can be read by DataReader
	 * @return string representing label sequences, the format is specified by writer
	 */
	public String strLabeling(File file){
		List<Sentence> obsvSeqs = dataReader.readFile(file.getPath());
		List lblSeqs = labeling(obsvSeqs);
		String ret = dataWriter.writeString(lblSeqs);
		return ret;
	}
	
	/**
	 * Labeling.
	 *
	 * @param obsvSeqs the obsv seqs
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	private List labeling(List<Sentence> obsvSeqs){
		List labelSeqs = new ArrayList();
		
		for (int i = 0; i < obsvSeqs.size(); ++i){//ith sentence
			List sequence = new ArrayList();
			Sentence sentence = obsvSeqs.get(i);
			
			for (int j = 0; j < sentence.size(); ++j){//jth observation
				Observation obsv = new Observation();
				obsv.originalData = sentence.getWordAt(j);
				
				String [] strCps = dataTagger.getContext(sentence, j);
				
				ArrayList<Integer> tempCpsInt = new ArrayList<Integer>();

				for (int k = 0; k < strCps.length; k++) {
					Integer cpInt = (Integer) taggerMaps.cpStr2Int.get(strCps[k]);
					if (cpInt == null) {
						continue;
					}
					tempCpsInt.add(cpInt);
				}
				
				obsv.cps = new int[tempCpsInt.size()];
				for (int k = 0; k < tempCpsInt.size(); ++k){
					obsv.cps[k] = tempCpsInt.get(k).intValue();
				}
				sequence.add(obsv);
			}
			
			labelSeqs.add(sequence);
		}
		
		taggerModel.inferenceAll(labelSeqs);	
		
		//assign labels to list of sentences
		for (int i = 0; i < obsvSeqs.size(); ++i){
			Sentence sent = obsvSeqs.get(i);
			List seq = (List) labelSeqs.get(i);
			
			for (int j = 0; j < sent.size(); ++j){
				Observation obsrv = (Observation) seq.get(j);			
				String label = (String) taggerMaps.lbInt2Str.get(obsrv.modelLabel);
				
				sent.getTWordAt(j).setTag(label);
			}
		}
		
		return obsvSeqs;
	}
	
}
