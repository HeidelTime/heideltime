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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jvntextpro.data.ContextGenerator;
import jvntextpro.data.Sentence;
import jvntextpro.util.StringUtils;
import jvntextpro.util.VnSyllParser;

public class POSContextGenerator extends ContextGenerator {
	
	//----------------------------------------------
	// Member variables
	//----------------------------------------------
	private static final String DEFAULT_E_DICT = "jvnpostag/ComputerDict.txt";
	Map word2dictags = new HashMap<String, List>();
	Vector<String> cpnames;
	Vector<Vector<Integer>> paras;
	
	//----------------------------------------------
	// Constructor and Override methods
	//----------------------------------------------
	public POSContextGenerator(String featureTemplateFile){
		readDict();
		readFeatureTemplate(featureTemplateFile);
	}
	
	@Override
	public String[] getContext(Sentence sent, int pos) {
		// TODO Auto-generated method stub
		List<String> cps = new ArrayList<String>();
		
		for (int it = 0; it < cpnames.size(); ++it){			
			String cp = cpnames.get(it);
			Vector<Integer> paras = this.paras.get(it);
			String cpvalue = "";
			if (cp.equals("w")){
				cpvalue = w(sent,pos,paras.get(0));
			}
			else if (cp.equals("wj")){
				cpvalue = wj(sent,pos,paras.get(0), paras.get(1));
			}
			else if (cp.equals("prf")){
				cpvalue = prf(sent,pos, paras.get(0));
			}
			else if (cp.equals("sff")){
				cpvalue = sff(sent,pos,paras.get(0));
			}
			else if (cp.equals("an")){
				cpvalue = an(sent,pos, paras.get(0));				
			}
			else if (cp.equals("hn")){
				cpvalue = hn(sent, pos, paras.get(0));
			}
			else if (cp.equals("hyph")){
				cpvalue = hyph(sent, pos, paras.get(0));
			}
			else if (cp.equals("slash")){
				cpvalue = slash(sent, pos, paras.get(0));
			}
			else if (cp.equals("com")){
				cpvalue = com(sent, pos, paras.get(0));
			}
			else if (cp.equals("ac")){
				cpvalue = ac(sent, pos, paras.get(0));				
			}
			else if (cp.equals("ic")){
				cpvalue = ic(sent, pos, paras.get(0));
			}
			else if (cp.equals("mk")){
				cpvalue = mk(sent, pos, paras.get(0));
			}
			else if (cp.equals("dict")){
				cps.add(dict(sent, pos, paras.get(0)));
			}
			else if (cp.equals("rr")){
				cpvalue = rr(sent, pos, paras.get(0));
			}
			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}
	
	//----------------------------------------------
	// IO methods
	//----------------------------------------------
	public boolean readDict(){
		try {
			URL url = POSContextGenerator.class.getClassLoader().getResource(DEFAULT_E_DICT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream(), "UTF-8"));
			word2dictags.clear();
						
			String line, temp = null;
			while ((line = reader.readLine()) != null ){
				String [] tokens = line.split("\t");
		
				String word, tag;
				if (tokens == null)
					continue;
				
				if (tokens.length != 2){
					continue;					
				}
				else if (tokens.length == 2){
					if (tokens[0].equals("")){
						if (temp == null)
							continue;
						else {
							//System.out.println(temp);
							word = temp;
							tag = tokens[1];
						}
					}
					else{ 
						word = tokens[0].trim().toLowerCase();
						tag = tokens[1].trim();
						temp = word;
					}
				}
				else continue;
				
				word = word.replace(" ","_");
				//System.out.println(word);
				List dictags = (List) word2dictags.get(word);
				if (dictags == null){
					dictags = new ArrayList<String>();
				}
				dictags.add(tag);
				word2dictags.put(word, dictags);
			}
			
			reader.close();
			return true;
		}
		
		catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean readFeatureTemplate(String file){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			InputStream stream = new FileInputStream(file);
			Document doc  = builder.parse(stream);
			
			Element root = doc.getDocumentElement();
			NodeList childrent = root.getChildNodes();
			cpnames = new Vector<String>();
			paras = new Vector<Vector<Integer>>();
			
			for (int i = 0; i < childrent.getLength(); i++)
				if (childrent.item(i) instanceof Element) {
					Element child = (Element) childrent.item(i);
					String value = child.getAttribute("value");
					
					//parse the value and get the parameters
					String [] parastr = value.split(":");
					Vector<Integer> para = new Vector<Integer>();
					for (int j = 1; j < parastr.length; ++j){
						para.add(Integer.parseInt(parastr[j]));
					}
					
					cpnames.add(parastr[0]);
					paras.add(para);
				}
			
		}
		catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//-----------------------------------------------
	// feature generating methods
	//-----------------------------------------------
		
	private String w(Sentence sent, int pos, int i){
		String cp = "w:" + Integer.toString(i) + ":"; 
		//if (pos + i == -1)
		//	cp += "BS";
		//else if (pos + i == sent.size())
		//	cp += "ES";
		if (0 <= (pos + i) && (pos + i) < sent.size())
			cp += sent.getWordAt(pos + i);
		else cp="";
		
		return cp;
	}
	
	private String wj(Sentence sent, int pos, int i, int j){
		String cp = "wj:" + Integer.toString(i) + ":" + Integer.toString(j) + ":";
		if ((pos + i) >= sent.size() || (pos + i) < 0 || 
				(pos + j) < 0 || (pos + j) >= sent.size())
			cp = "";
		else {
			cp += sent.getWordAt(pos + i) + ":" + sent.getWordAt(pos + j);
		}
		return cp;
	}
	
	private String prf(Sentence sent, int pos, int i){
		 String cp;
		 if (0 <= (pos + i) && (pos + i) < sent.size()){
			 cp = "prf:" + Integer.toString(i) + ":";
			 
			 String word = sent.getWordAt(pos + i);
			 String [] sylls = word.split("_");
			 if (sylls.length >= 2){
				 cp += sylls[0];
			 }
			 else cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String sff(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			 cp = "sff:" + Integer.toString(i) + ":";
			 
			 String word = sent.getWordAt(pos + i);
			 String [] sylls = word.split("_");
			 if (sylls.length >= 2){
				 cp += sylls[sylls.length - 1];
			 }
			 else cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String an(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			 cp = "an:" + Integer.toString(i);
			 
			String word = sent.getWordAt(pos + i);
			if (!StringUtils.isAllNumber(word))
				cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String hn(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "hn:" + Integer.toString(i);
			 
			String word = sent.getWordAt(pos + i);
			if (!StringUtils.containNumber(word))
				cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String hyph(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "hyph:" + Integer.toString(i);
			 
			String word = sent.getWordAt(pos + i);
			if (!word.contains("-"))
				cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String slash(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "hyph:" + Integer.toString(i);
			 
			String word = sent.getWordAt(pos + i);
			if (!word.contains("/"))
				cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String com(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "hyph:" + Integer.toString(i);
			 
			String word = sent.getWordAt(pos + i);
			if (!word.contains(":"))
				cp = "";
		 }
		 else cp = "";
		 
		 return cp;
	}
	
	private String ac(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "ac:" + Integer.toString(i);
			
			String word = sent.getWordAt(pos + i);
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
	
	private String ic(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "ic:" + Integer.toString(i);
			
			String word = sent.getWordAt(pos + i);
			if (!StringUtils.isFirstCap(word))
				cp = "";
		}
		else cp = "";
		
		return cp;
	}
	
	private String mk(Sentence sent, int pos, int i){
		String cp;
		if (0 <= (pos + i) && (pos + i) < sent.size()){
			cp = "mk:" + Integer.toString(i);
			String word = sent.getWordAt(pos + i);
			if (!StringUtils.isPunc(word))
				cp = "";
		}
		else cp = "";
		
		return cp;
	} 
	
	private String dict(Sentence sent, int pos, int i){
		String cp = "";
		
		if (0 <= (pos + i) && (pos + i) < sent.size()){			
			String word = sent.getWordAt(pos + i);
			if (word2dictags.containsKey(word)){
				List tags = (List) word2dictags.get(word);
				
				for (int j = 0; j < tags.size(); ++j){
					cp += "dict:" + Integer.toString(i) + ":" + tags.get(j) + " ";
				}
			}
		}
		
		return cp.trim();
	}
	
	private String rr(Sentence sent, int pos, int i){
		String cp = "";
		
		if (0 <= (pos + i) && (pos + i) < sent.size()){			
			String word = sent.getWordAt(pos + i);
			String [] sylls = word.split("_");
			
			if (sylls.length == 2){ //consider 2-syllable words
				VnSyllParser parser1 = new VnSyllParser(sylls[0]);
				VnSyllParser parser2 = new VnSyllParser(sylls[1]);
				
				if (parser1.isValidVnSyllable() && parser2.isValidVnSyllable()){
					if (parser1.getNonToneSyll().equalsIgnoreCase(parser2.getNonToneSyll())){
						cp += "fr:" + Integer.toString(i) + " ";
					}
					else if (parser1.getRhyme().equalsIgnoreCase(parser2.getRhyme())){
						cp += "pr:" + Integer.toString(i) + " ";
					}
				}
			}
		}
		
		
		return cp.trim();
	}
}
