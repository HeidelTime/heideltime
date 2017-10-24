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

import java.io.File;

import jvntextpro.data.DataReader;
import jvntextpro.data.DataWriter;

public interface POSTagger {
	
	//--------------------------------
	// initialization 
	//--------------------------------
	public void init(String modelfile);
	
	//-------------------------------
	//tagging methods
	//-------------------------------
	
	/**
	 * Annotate string with part-of-speech tags
	 * @param instr string has been done word segmentation
	 * @return string annotated with part-of-speech tags
	 */
	public String tagging(String instr);
	
	
	/**
	 * Annotate content of file with part-of-speech tags
	 * @param file of which content has been done word segmentation
	 * @return string annotated with part-of-speech tags
	 */
	public String tagging(File file);
	
	/**
	 * Set data writer and reader to this pos tagger
	 * this is used to be adaptable to different format of input/output data
	 */
	public void setDataReader(DataReader reader);
	
	public void setDataWriter(DataWriter writer);
}
