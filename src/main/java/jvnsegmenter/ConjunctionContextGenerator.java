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

// TODO: Auto-generated Javadoc
/**
 * The Class ConjunctionContextGenerator.
 */
public class ConjunctionContextGenerator extends BasicContextGenerator {

	/**
	 * Instantiates a new conjunction context generator.
	 *
	 * @param node the node
	 */
	public ConjunctionContextGenerator(Element node){
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
			
			if (cp.equals("syll_conj_gen")){
				String prefix = "s:";
				String suffix = "";			
				for (int i = 0; i < paras.size(); ++i) {
					if (pos + paras.get(i) < 0 || pos + paras.get(i)>= sent.size()){
						cpvalue = "";
						continue;
					}
		
					prefix += paras.get(i) + ":";
					suffix += sent.getWordAt(pos + paras.get(i)) + ":";
				}			
				
				if (suffix.endsWith(":"))
					cpvalue = prefix + suffix.substring(0, suffix.length() - 1);
			}
			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}

}
