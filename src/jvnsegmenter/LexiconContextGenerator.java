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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;

import jvntextpro.data.Sentence;

// TODO: Auto-generated Javadoc
/**
 * The Class LexiconContextGenerator.
 */
public class LexiconContextGenerator extends BasicContextGenerator {
	//------------------------------
	//Variables
	//------------------------------
	/** The hs vietnamese dict. */
	private static HashSet hsVietnameseDict;
	
	/** The hs vi family names. */
	private static HashSet hsViFamilyNames;

	/** The hs vi middle names. */
	private static HashSet hsViMiddleNames;

	/** The hs vi last names. */
	private static HashSet hsViLastNames;

	/** The hs vi locations. */
	private static HashSet hsViLocations;

	//------------------------------
	//Methods
	//------------------------------
	/**
	 * Instantiates a new lexicon context generator.
	 *
	 * @param node the node
	 */
	public LexiconContextGenerator(Element node){
		readFeatureParameters(node);
	}
	
	/* (non-Javadoc)
	 * @see jvntextpro.data.ContextGenerator#getContext(jvntextpro.data.Sentence, int)
	 */
	@Override
	public String[] getContext(Sentence sent, int pos) {
		// TODO Auto-generated method stub
		// get the context information from sequence
		List<String> cps = new ArrayList<String>();
		
		for (int it = 0; it < cpnames.size(); ++it){			
			String cp = cpnames.get(it);
			Vector<Integer> paras = this.paras.get(it);
			String cpvalue = "";
			
			String suffix = "";
			String word = "";
			boolean outOfArrayIndex = false;
			for (int i = 0; i < paras.size(); ++i) {
				if (pos + paras.get(i) < 0 || pos + paras.get(i)>= sent.size()){
					cpvalue = "";
					outOfArrayIndex = true;
					break;
				}
	
				suffix += paras.get(i) + ":";
				word += sent.getWordAt(pos + paras.get(i)) + " ";
			}
			word = word.trim();			
			if (suffix.endsWith(":"))
				suffix = suffix.substring(0, suffix.length() - 1);
			
			if (outOfArrayIndex) continue;
			
			if (cp.equals("vietnamese_dict")) {
				word = word.toLowerCase();
				if (inVietnameseDict(word)){
					cpvalue = "d:" + suffix;					
				}
			} else if (cp.equals("family_name")) {
				if (inViFamilyNameList(word))
					cpvalue = "fam:" + suffix;
			} else if (cp.equals("middle_name")) {
				if (inViMiddleNameList(word))
					cpvalue = "mdl:" + suffix;
			} else if (cp.equals("last_name")) {
				if (inViLastNameList(word))
					cpvalue = "lst:" + suffix;
			} else if (cp.equals("location")) {
				if (inViLocations(word))
					cpvalue = "loc:" + suffix;
			}

			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}

	//------------------------------
	// static methods
	//------------------------------
	/**
	 * In vietnamese dict.
	 *
	 * @param word the word
	 * @return true, if successful
	 */
	public static boolean inVietnameseDict(String word) {
		return hsVietnameseDict.contains(word);
	}

	/**
	 * In vi family name list.
	 *
	 * @param word the word
	 * @return true, if successful
	 */
	public static boolean inViFamilyNameList(String word) {
		return hsViFamilyNames.contains(word);
	}

	/**
	 * In vi middle name list.
	 *
	 * @param word the word
	 * @return true, if successful
	 */
	public static boolean inViMiddleNameList(String word) {
		return hsViMiddleNames.contains(word);
	}

	/**
	 * In vi last name list.
	 *
	 * @param word the word
	 * @return true, if successful
	 */
	public static boolean inViLastNameList(String word) {
		return hsViLastNames.contains(word);
	}

	/**
	 * In vi locations.
	 *
	 * @param word the word
	 * @return true, if successful
	 */
	public static boolean inViLocations(String word) {
		return hsViLocations.contains(word);
	}

	/**
	 * Load vietnamese dict.
	 *
	 * @param filename the filename
	 */
	public static void loadVietnameseDict(String filename) {
		try {
			FileInputStream in = new FileInputStream(filename);
			if (hsVietnameseDict == null) {
				hsVietnameseDict = new HashSet();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.substring(0, 2).equals("##")) {
						String word = line.substring(2);
						word = word.toLowerCase();
						hsVietnameseDict.add(word);
					}
				}
			}
			// Print lacviet_dict into lacviet.dict file
		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Load vi personal names.
	 *
	 * @param filename the filename
	 */
	public static void loadViPersonalNames(String filename) {
		try {
			FileInputStream in = new FileInputStream(filename);
			if (hsViFamilyNames == null) {

				hsViFamilyNames = new HashSet();
				hsViLastNames = new HashSet();
				hsViMiddleNames = new HashSet();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.equals(""))
						continue;

					//line = line.toLowerCase();
					int idxSpace = line.indexOf(' ');
					int lastIdxSpace = line.lastIndexOf(' ');

					if (idxSpace != -1) {
						String strFamilyName = line.substring(0, idxSpace);
						hsViFamilyNames.add(strFamilyName);
					}

					if ((idxSpace != -1) && (lastIdxSpace > idxSpace + 1)) {
						String strMiddleName = line.substring(idxSpace + 1,
								lastIdxSpace - 1);
						hsViMiddleNames.add(strMiddleName);
					}

					if (lastIdxSpace != -1) {
						String strLastName = line.substring(lastIdxSpace + 1,
								line.length());
						hsViLastNames.add(strLastName);
					}
				}
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.print(e.getMessage());
		}
	}

	/**
	 * Load vi location list.
	 *
	 * @param filename the filename
	 */
	public static void loadViLocationList(String filename) {
		try {
			FileInputStream in = new FileInputStream(filename);
			if (hsViLocations == null) {
				hsViLocations = new HashSet();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					String word = line.trim();				
					hsViLocations.add(word);
				}
			}
		} catch (Exception e) {
			System.err.print(e.getMessage());
		}
	}
}
