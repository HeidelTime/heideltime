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

import jvntextpro.data.Sentence;
import jvntextpro.util.VnSyllParser;

import org.w3c.dom.Element;

// TODO: Auto-generated Javadoc
/**
 * The Class VietnameseContextGenerator.
 */
public class VietnameseContextGenerator extends BasicContextGenerator{

	//------------------------------
	//Methods
	//------------------------------
	/**
	 * Instantiates a new vietnamese context generator.
	 *
	 * @param node the node
	 */
	public VietnameseContextGenerator(Element node){
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
			
			String word = "";
			for (int i = 0; i < paras.size(); ++i) {
				if (pos + paras.get(i) < 0 || pos + paras.get(i)>= sent.size()){
					cpvalue = "";
					continue;
				}
	
				word += sent.getWordAt(pos + paras.get(i)) + " ";
			}
			word = word.trim().toLowerCase();
			
			VnSyllParser parser = new VnSyllParser(word);
			if (!parser.isValidVnSyllable() && cp.equals("not_valid_vnsyll"))
				cpvalue = "nvs:" + word;
			
			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}
}
