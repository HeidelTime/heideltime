/*
 * TempEval2Reader.java
 * 
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, University of Heidelberg. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU General Public License.
 * 
 * author: Jannik Strötgen
 * email:  stroetgen@uni-hd.de
 * 
 * The TempEval2 Reader reads TempEval-2 corpora.
 * For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
 */

package de.unihd.dbs.uima.reader.tempeval2reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;

/**
 * CollectionReader for TempEval Data 
 */
public class Tempeval2Reader extends CollectionReader_ImplBase {
	/**
	 * Logger for this class
	 */
	private static Logger logger = null;
	
	/**
	 * ComponentId
	 */
	private static final String compontent_id = "de.unihd.dbs.uima.reader.tempeval2reader";
	
	/**
	 * Parameter for files in the input directory
	 */
	public static final String FILE_BASE_SEGMENTATION  = "base-segmentation.tab";
	public static final String FILE_TIMEX_EXTENTS      = "timex-extents.tab";
	public static final String FILE_TIMEX_ATTRIBUTES   = "timex-attributes.tab";
	public static final String FILE_DCT                = "dct.tab";
	
	/**
	 * Needed information to create cas objects for all "documents"
	 */
	public Integer numberOfDocuments = 0;
	
	/**
	 * HashMap for all tokens of a document
	 */
	public HashMap<String, Token> hmToken = new HashMap<String, Token>();
	
	/**
	 * HashMap for all timexes tokens of a document
	 */
	public HashMap<String, String> hmTimexToken = new HashMap<String, String>();
	
	/**
	 * HashMap for all timexes and goldTimexes (with <TimexId, Timex>)
	 */
	public HashMap<String, Timex3> hmTimex = new HashMap<String, Timex3>();
	public HashMap<String, Dct> hmDct = new HashMap<String, Dct>();
	
	
  /**
   * Name of configuration parameter that must be set to the path of a directory
   * containing input files.
   */
  public static final String PARAM_INPUTDIR  = "InputDirectory";

  /**
   * List containing all filenames of "documents"
   */
  private List<String> filenames = new ArrayList<String>();
    
  
  /**
   * Current file number
   */
  private int currentIndex;

  /**
   * Parentheses are given as "-LRB-" ... reset to "(" ...
   */
  
  Boolean resettingParentheses = true;

  /**
   * 
   */
  public void initialize() throws ResourceInitializationException {

	  
	  // save doc names to list
	  List<File> inputFiles = getFilesFromInputDirectory();
	  
	  // get total document number and put all doc names into list "filenames"
	  numberOfDocuments = getNumberOfDocuments(inputFiles);
	  System.err.println("["+compontent_id+"] number of documents: "+numberOfDocuments);
  }

  
  

  
  
  public void getNext(CAS cas) throws IOException, CollectionException {
	  
	  // create jcas  
	  JCas jcas;
	  try {
		  jcas = cas.getJCas();
	  } catch (CASException e) {
		  throw new CollectionException(e);
	  }
	  
	  // clear HashMaps for new document
	  hmToken.clear();
	  hmTimexToken.clear();
	  hmTimex.clear();
	  hmDct.clear();
	  
	  // get current doc name 
	  String docname = filenames.get(currentIndex++);
	  
	  // save doc names to list
	  List<File> inputFiles = getFilesFromInputDirectory();
	  
	  // set documentText, sentences, tokens from file
	  setTextSentencesTokens(docname, inputFiles, jcas);
	  
	  // set document creation time (dct)
	  setDocumentCreationTime(docname, inputFiles, jcas);
	  
	  // set timexes
	  setTimexes(docname, inputFiles, jcas);
	  
	  

  }

  /**
   * @see org.apache.uima.collection.CollectionReader#hasNext()
   */
  public boolean hasNext() throws IOException, CollectionException {
    return currentIndex < numberOfDocuments;
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(currentIndex, numberOfDocuments, Progress.ENTITIES) };
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
  }

  
  public void setTimexes(String docname, List<File> inputFiles, JCas jcas) throws IOException{
	  String directory = (String) getConfigParameterValue(PARAM_INPUTDIR);
	  String filename_extents    = directory+"/"+FILE_TIMEX_EXTENTS;
	  String filename_attributes = directory+"/"+FILE_TIMEX_ATTRIBUTES;
	  
	  // timex extents
	  for (File file : inputFiles){
		  if (file.getAbsolutePath().equals(filename_extents)){
			  try {
				  String line;
				  BufferedReader bf = new BufferedReader (new FileReader(file));
				  
				  String fileName = "";
				  Integer sentId = -1;
				  Integer tokId  = -1;
				  String tokIdList = "BEGIN<-->";
				  String tag     = "";
				  String timexId = "";
				  Integer timexInstance = -1;
				  
				  while ((line = bf.readLine()) != null){
					  String[] parts        = line.split("\t");
					  if (parts[0].equals(docname)){
						  
						  // new timex3?
						  if (!((sentId == Integer.parseInt(parts[1])) &&
								  ((tokId+1) == Integer.parseInt(parts[2])) &&
								  (tag.equals(parts[3])) &&
								  (timexId.equals(parts[4])) &&
								  (timexInstance == Integer.parseInt(parts[5])))){
							  if (!(tokIdList.equals("BEGIN<-->"))){
								  if (!(fileName.equals(""))){
									  // add timex annotation
									  addTimex(fileName, sentId, tokIdList, timexId, timexInstance, jcas);
									  
									  // reset for next timex
									  tokIdList = "BEGIN<-->";
								  }
							  }
						  }
					  
					  
						  fileName      = parts[0];
						  sentId        = Integer.parseInt(parts[1]);
						  tokId         = Integer.parseInt(parts[2]);
						  tokIdList     = tokIdList + tokId +"<-->";
						  tag           = parts[3];
						  timexId       = parts[4];
						  timexInstance = Integer.parseInt(parts[5]);
					  }
					  
				  }if (!( sentId == -1)){
					  // add last timex annotation (if any)
					  addTimex(fileName, sentId, tokIdList, timexId, timexInstance, jcas);
				  }
			  }
			  catch (Exception e){
				  System.err.println(e);
				  throw new IOException(e);
				  
			  }
			  
						  
		  }
	  }
	  // timex attributes
	  for (File file : inputFiles){
		  if (file.getAbsolutePath().equals(filename_attributes)){
			  try {
				  String line;
				  BufferedReader bf = new BufferedReader (new FileReader(file));
				  
				  String fileName = "";
				  Integer sentId = -1;
				  Integer tokId  = -1;
				  String tag     = "";
				  String timexId = "";
				  Integer timexInstance = -1;
				  String timexType = "";
				  String timexValue = "";
				  
				  while ((line = bf.readLine()) != null){
					  String[] parts        = line.split("\t");
					  if (parts[0].equals(docname)){
						  
						  // new timex3?
						  if (!((sentId == Integer.parseInt(parts[1])) &&
								  ((tokId) == Integer.parseInt(parts[2])) &&
								  (tag.equals(parts[3])) &&
								  (timexId.equals(parts[4])) &&
								  (timexInstance == Integer.parseInt(parts[5])))){
							  if (!(fileName.equals(""))){
								  timexType = "";
								  timexValue = "";
							  }
						  
						  }
						  fileName      = parts[0];
						  sentId        = Integer.parseInt(parts[1]);
						  tokId         = Integer.parseInt(parts[2]);
						  tag           = parts[3];
						  timexId       = parts[4];
						  timexInstance = Integer.parseInt(parts[5]);
						  if (parts[6].equals("type")){
							  timexType     = parts[7];
						  }else if (parts[6].equals("value")){
							  timexValue    = parts[7];
						  }
						  
					  }
				  }
			  }
			  catch (IOException e){
				  throw new IOException(e);
			  }
		  }
	  }
  }
  
  
  
   public void addTimex(String fileName, Integer sentId, String tokIdList, String timexId, Integer timexInstance, JCas jcas){
	  // get first and last token
	  String[] tokens          = tokIdList.split("<-->");

	  Integer first            = Integer.parseInt(tokens[1]);
	  Integer last             = Integer.parseInt(tokens[tokens.length-1]);
	  String complexTokIdFirst = fileName+"_"+sentId+"_"+first;
	  String complexTokIdLast  = fileName+"_"+sentId+"_"+last;
	  
	  Token firstToken = hmToken.get(complexTokIdFirst);
	  Token lastToken  = hmToken.get(complexTokIdLast);
	  
	  Timex3 timex = new Timex3(jcas);
	  timex.setBegin(firstToken.getBegin());
	  timex.setEnd(lastToken.getEnd());
	  timex.setFilename(fileName);
	  timex.setSentId(sentId);
	  timex.setFirstTokId(first);
	  timex.setTimexId(timexId);
	  timex.setTimexInstance(timexInstance);
	  
	  // add first token and tokenIdList to HashMap
	  hmTimexToken.put(complexTokIdFirst, tokIdList);
	  
	  // add to HashMap
	  hmTimex.put(timexId, timex);
  }
  
  
  
  public void setDocumentCreationTime(String docname, List<File> inputFiles, JCas jcas) throws IOException{
	  String documentCreationTime = "";
	  
	  String directory = (String) getConfigParameterValue(PARAM_INPUTDIR);
	  String filename = directory+"/"+FILE_DCT;
	  for (File file : inputFiles) {
		if (file.getAbsolutePath().equals(filename)){
			try {
				String line;
				BufferedReader bf = new BufferedReader (new FileReader(file));
				while ((line = bf.readLine()) != null){
					String[] parts = line.split("(\t)+");
					String fileId  = parts[0];
					if (fileId.equals(docname)){
						documentCreationTime = parts[1];
						Dct dct = new Dct(jcas);
						String text = jcas.getDocumentText();
						dct.setBegin(0);
						dct.setEnd(text.length());
						dct.setFilename(fileId);
						dct.setValue(documentCreationTime);
						dct.setTimexId("t0");
						dct.addToIndexes();
						
						// add dct to HashMap
						hmDct.put("t0", dct);
					}
				}
			}
			catch (IOException e){
				throw new IOException(e);
			}
			
		}
	  }
  }
  
  public void setTextSentencesTokens(String docname, List<File> inputFiles, JCas jcas) throws IOException{
	  String text        = "";
	  String sentString  = "";
	  Integer positionCounter = 0;
	  Integer sentId = -1;
	  Integer lastSentId = -1;
	  
	  String directory = (String) getConfigParameterValue(PARAM_INPUTDIR);
	  String filename = directory+"/"+FILE_BASE_SEGMENTATION;
	  for (File file : inputFiles) {
		if (file.getAbsolutePath().equals(filename)){
			try {
				String line;
				BufferedReader bf = new BufferedReader (new FileReader(file));
				Boolean lastSentProcessed  = false;
				Boolean firstSentProcessed = false;
				String fileId = "";
				while ((line = bf.readLine()) != null){
					
					String[] parts = line.split("\t");
					fileId  = parts[0];
					sentId = Integer.parseInt(parts[1]);
					Integer tokId  = Integer.parseInt(parts[2]);
					String tokenString = parts[3];
					
					if (resettingParentheses == true){
						tokenString = resetParentheses(tokenString);
					}
					
					if (fileId.equals(docname)){
						
						// First Sentence, first Token
						if ((sentId == 0) && (tokId == 0)){
							firstSentProcessed = true;
							text = tokenString;
							sentString  = tokenString;
							positionCounter = addTokenAnnotation(tokenString, fileId, sentId, tokId, positionCounter, jcas);
						}
						
						// new Sentence, first Token
						else if (tokId == 0){
							positionCounter = addSentenceAnnotation(sentString, fileId, sentId-1, positionCounter, jcas);
							text = text+" "+tokenString;
							sentString  = tokenString;
							positionCounter = addTokenAnnotation(tokenString, fileId, sentId, tokId, positionCounter, jcas);
						}
						
						// within any sentence
						else{
							text = text+" "+tokenString;
							sentString  = sentString+" "+tokenString;
							positionCounter = addTokenAnnotation(tokenString, fileId, sentId, tokId, positionCounter, jcas);
						}
					}
					else{
						if ((firstSentProcessed) && (!(lastSentProcessed))){
							positionCounter = addSentenceAnnotation(sentString, docname, lastSentId, positionCounter, jcas);
							lastSentProcessed = true;
						}
					}
					lastSentId = sentId;
				}
				if (fileId.equals(docname)){
					positionCounter = addSentenceAnnotation(sentString, docname, lastSentId, positionCounter, jcas);
				}
			}catch (IOException e){
				throw new IOException(e);
			}
		}
	  }
	  jcas.setDocumentText(text);	  
  }
  
  public String resetParentheses(String tokenString){
	  if (tokenString.equals("-LRB-")){
		  tokenString = tokenString.replace("-LRB-", "(");
	  }
	  else if (tokenString.equals("-RRB-")){
		  tokenString = tokenString.replace("-RRB-", ")");
	  }
	  else if (tokenString.equals("-LSB-")){
		  tokenString = tokenString.replace("-LSB-","[");
	  }
	  else if (tokenString.equals("-RSB-")){
		  tokenString = tokenString.replace("-RSB-","]");
	  }
	  else if (tokenString.equals("-LCB-")){
		  tokenString = tokenString.replace("-LCB-","{");
	  }
	  else if (tokenString.equals("-RCB-")){
		  tokenString = tokenString.replace("-RCB-","}");
	  }
	  return tokenString;
  }
  
  public Integer addSentenceAnnotation(String sentenceString, String fileId, Integer sentId, Integer positionCounter, JCas jcas){
	  Sentence sentence = new Sentence(jcas);
	  Integer begin = positionCounter - sentenceString.length();
	  sentence.setFilename(fileId);
	  sentence.setSentenceId(sentId);
	  sentence.setBegin(begin);
	  sentence.setEnd(positionCounter);
	  sentence.addToIndexes();
	  return positionCounter;
  }
  
  
  
  /**
   * Add token annotation to jcas
   * @param tokenString
   * @param fileId
   * @param tokId
   * @param positionCounter
   * @param jcas
   * @return
   */
  public Integer addTokenAnnotation(String tokenString, String fileId, Integer sentId, Integer tokId, Integer positionCounter, JCas jcas){
		Token token = new Token(jcas);
		if (!((sentId == 0) && (tokId == 0))){
			positionCounter = positionCounter +1;
		}
		token.setBegin(positionCounter);
		positionCounter = positionCounter + tokenString.length();
		token.setEnd(positionCounter);
		token.setTokenId(tokId);
		token.setSentId(sentId);
		token.setFilename(fileId);
		token.addToIndexes();
		
		String id = fileId+"_"+sentId+"_"+tokId;
		hmToken.put(id, token);
		
		return positionCounter;
  }
  
  /**
   * count the number of different "documents" and save doc names in filenames
   * @param inputFiles
   * @return
   * @throws ResourceInitializationException
   */
  private Integer getNumberOfDocuments(List<File> inputFiles) throws ResourceInitializationException{
	  String directory = (String) getConfigParameterValue(PARAM_INPUTDIR);
	  String filename = directory+"/"+FILE_BASE_SEGMENTATION;
	  for (File file : inputFiles) {
		if (file.getAbsolutePath().equals(filename)){
			try {
				String line;
				BufferedReader bf = new BufferedReader (new FileReader(file));
				while ((line = bf.readLine()) != null){
					String docName = (line.split("\t"))[0];
					if (!(filenames.contains(docName))){
						filenames.add(docName);
					}
				}
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
		}
	  }	  
	  int  docCounter = filenames.size();
	  return docCounter;
  }
  
  
  private List<File> getFilesFromInputDirectory() {
	  // get directory and save 
	  File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
	  List<File> documentFiles = new ArrayList<File>();
	  
	  
	  // if input directory does not exist or is not a directory, throw exception
	  if (!directory.exists() || !directory.isDirectory()) {
		  logger.log(Level.WARNING, "getFilesFromInputDirectory() " + directory
				  + " does not exist. Client has to set configuration parameter '" + PARAM_INPUTDIR + "'.");
		  return null;
	  }

	  // get list of files (not subdirectories) in the specified directory
	  File[] dirFiles = directory.listFiles();
	  for (int i = 0; i < dirFiles.length; i++) {
		  if (!dirFiles[i].isDirectory()) {
			  documentFiles.add(dirFiles[i]);
		  }
	  }
	  return documentFiles;
  }
  
}

