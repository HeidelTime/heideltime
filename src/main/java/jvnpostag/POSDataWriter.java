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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import jvntextpro.data.DataWriter;
import jvntextpro.data.Sentence;

public class POSDataWriter extends DataWriter {
	
	public void writeFile(List lblSeqs, String filename){		
		try {
			String ret = writeString(lblSeqs);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			out.write(ret);
			out.close();
		}
		catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public String writeString(List lblSeqs){
		String ret = "";
		for (int i = 0; i < lblSeqs.size(); ++i){
			Sentence sent = (Sentence) lblSeqs.get(i);
			
			for (int j = 0; j < sent.size(); ++j){
				ret += sent.getWordAt(j) + "/" + sent.getTagAt(j) + " ";
			}
			ret = ret.trim() + "\n";
		}		
		
		return ret.trim();
	}
	
}
