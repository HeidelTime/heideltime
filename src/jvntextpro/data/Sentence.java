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

package jvntextpro.data;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Class Sentence.
 */
public class Sentence {
	
	/** The sentence. */
	List<TWord> sentence = null;
	
	/**
	 * Instantiates a new sentence.
	 */
	public Sentence(){
		sentence = new ArrayList<TWord>();
	}
	
	/**
	 * Adds the t word.
	 *
	 * @param tword the tword
	 */
	public void addTWord(TWord tword){
		sentence.add(tword);
	}
	
	/**
	 * Adds the t word.
	 *
	 * @param word the word
	 * @param tag the tag
	 */
	public void addTWord(String word, String tag){
		TWord tword = new TWord(word, tag);
		sentence.add(tword);
	}
	
	/**
	 * Adds the t word.
	 *
	 * @param word the word
	 */
	public void addTWord(String word){
		sentence.add(new TWord(word));
	}
	
	/**
	 * Gets the word at.
	 *
	 * @param pos the pos
	 * @return the word at
	 */
	public String getWordAt(int pos){
		return sentence.get(pos).getWord();
	}
	
	/**
	 * Gets the tag at.
	 *
	 * @param pos the pos
	 * @return the tag at
	 */
	public String getTagAt(int pos){
		return sentence.get(pos).getTag();
	}
	
	/**
	 * Gets the t word at.
	 *
	 * @param pos the pos
	 * @return the t word at
	 */
	public TWord getTWordAt(int pos){
		return sentence.get(pos);
	}
	
	/**
	 * Clear.
	 */
	public void clear(){
		sentence.clear();
	}
	
	/**
	 * Size.
	 *
	 * @return the int
	 */
	public int size(){
		return sentence.size();
	}
	
	//DEBUG
	/**
	 * Prints the.
	 */
	public void print(){
		for (int i = 0; i < sentence.size(); ++i){
			sentence.get(i).print();
		}
		
		System.out.print("\n");
	}
	
	/**
	 * Prints the.
	 *
	 * @param out the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void print(Writer out) throws IOException{
		for (int i = 0; i < sentence.size(); ++i){
			sentence.get(i).print(out);
		}
		out.write("\n");
	}
}
