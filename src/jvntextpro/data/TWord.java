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
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class TWord.
 */
public class TWord {
	
	/** The token. */
	private String token;
	
	/** The tag. */
	private String tag = null; //null if this word is not tagged	
	
	// this is necessary for further processing
	// Example: in noun phase chunking
	// we may have information about pos tag (secondary tag)
	// chunking tags are main tags
	
	//or named entity recognition
	// we may have information about pos tag, chunk phase, verb phase,  (secondary tags) etc.
		
	/** The secondary tags. */
	private Vector<String> secondaryTags = null; 
	
	//constructors
	/**
	 * Instantiates a new t word.
	 *
	 * @param _word the _word
	 * @param _tag the _tag
	 */
	public TWord(String _word, String _tag){		
		token = _word.replaceAll(" ","_");;
		tag = _tag;		
	}
	
	/**
	 * Instantiates a new t word.
	 *
	 * @param _word the _word
	 */
	public TWord(String _word){
		token = _word.replaceAll(" ","_");		
	}
	
	//get methods
	/**
	 * Gets the word.
	 *
	 * @return the word
	 */
	public String getWord(){
		return token;
	}
	
	/**
	 * Gets the tag.
	 *
	 * @return the tag
	 */
	public String getTag(){
		return tag;
	}
	
	/**
	 * Sets the tag.
	 *
	 * @param t the new tag
	 */
	public void setTag(String t){
		tag = t;
	}
	
	/**
	 * Gets the secondary tag.
	 *
	 * @param i the i
	 * @return the secondary tag
	 */
	public String getSecondaryTag(int i){
		// i is the predefined position of secondary tag
		// For example: in named entity recognition (which are main tag)
		// i=0 can be used for POS tag
		// i=1 can be used for noun phase tag, etc...
		if (secondaryTags != null){
			return secondaryTags.get(i);
		}
		else return null;
	}
	
	//DEBUG
	/**
	 * Prints the.
	 */
	public void print(){
		System.out.println(token + "\t" + tag);
	}
	
	/**
	 * Prints the.
	 *
	 * @param out the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void print(Writer out) throws IOException{
		if (tag == null)
			System.out.println(tag);			
		out.write(token + "\t" + tag + "\n");		
	}
}
