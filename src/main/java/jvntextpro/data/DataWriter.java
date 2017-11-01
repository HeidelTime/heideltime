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
import java.util.Map;
 
// TODO: Auto-generated Javadoc
/**
 * The Class DataWriter.
 */
abstract public class DataWriter {
	
	/**
	 * Write file.
	 *
	 * @param lblSeqs the lbl seqs
	 * @param filename the filename
	 */
	public abstract void writeFile(List lblSeqs, String filename);
	
	/**
	 * Write string.
	 *
	 * @param lblSeqs the lbl seqs
	 * @return the string
	 */
	public abstract String writeString(List lblSeqs);
}
