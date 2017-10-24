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

import java.util.List;


// TODO: Auto-generated Javadoc
//return list of sentences
/**
 * The Class DataReader.
 */
abstract public class DataReader {
	
	/**
	 * Read file.
	 *
	 * @param datafile the datafile
	 * @return the list
	 */
	public abstract List<Sentence> readFile(String datafile);
	
	/**
	 * Read string.
	 *
	 * @param dataStr the data str
	 * @return the list
	 */
	public abstract List<Sentence> readString(String dataStr);
}
