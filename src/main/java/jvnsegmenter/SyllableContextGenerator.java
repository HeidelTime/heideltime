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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;

import jvntextpro.data.Sentence;
import jvntextpro.util.StringUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class SyllableContextGenerator.
 */
public class SyllableContextGenerator extends BasicContextGenerator {

	//constructor
	/**
	 * Instantiates a new syllable context generator.
	 *
	 * @param node the node
	 */
	public SyllableContextGenerator(Element node){
		readFeatureParameters(node);
	}
	
	/* (non-Javadoc)
	 * @see jvntextpro.data.ContextGenerator#getContext(jvntextpro.data.Sentence, int)
	 */
	@Override
	public String[] getContext(Sentence sent, int pos) {
	List<String> cps = new ArrayList<String>();
		
		for (int it = 0; it < cpnames.size(); ++it){			
			String cp = cpnames.get(it);
			Vector<Integer> paras = this.paras.get(it);
			String cpvalue = "";
			if (cp.equals("initial_cap")){
				cpvalue = ic(sent,pos,paras.get(0));
			}
			else if (cp.equals("all_cap")){
				cpvalue = ac(sent, pos, paras.get(0));
			}
			else if (cp.equals("mark")){
				cpvalue = mk(sent, pos, paras.get(0));
			}
			else if (cp.equals("first_obsrv")){
				if (pos + paras.get(0) == 0)
					cpvalue = "fi:" + paras.get(0);
			}
			
			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}
	
	/**
	 * Ic.
	 *
	 * @param sent the sent
	 * @param pos the pos
	 * @param i the i
	 * @return the string
	 */
	private String ic(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			String word = sent.getWordAt(pos + i);
			cp = "ic:" + word;
			
			if (!StringUtils.isFirstCap(word))
				cp = "";
		}
		else cp = "";
		
		return cp;
	}
	
	/**
	 * Ac.
	 *
	 * @param sent the sent
	 * @param pos the pos
	 * @param i the i
	 * @return the string
	 */
	private String ac(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			String word = sent.getWordAt(pos + i);
			cp = "ac:" + word;		
			
			boolean isAllCap = true;
			
			for (int j = 0 ; j < word.length(); ++j){
				if (word.charAt(j) == '_' || word.charAt(j) == '.') continue;
				
				if (!Character.isUpperCase(word.charAt(j))){
					isAllCap = false;
					break;
				}
			}
			
			if (!isAllCap)
				cp = "";
		}
		else cp = "";
		return cp;
	}
	
	/**
	 * Mk.
	 *
	 * @param sent the sent
	 * @param pos the pos
	 * @param i the i
	 * @return the string
	 */
	private String mk(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			String word = sent.getWordAt(pos + i);
			cp = "ma:" + word;			
			if (!StringUtils.isPunc(word))
				cp = "";
		}
		else cp = "";
		
		return cp;
	} 

}
