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
package jvntextpro.conversion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * This class provides functionality to convert from
 * a composite unicode string in vietnamese to a unicode string.
 *
 * @author TuNC
 */
public class CompositeUnicode2Unicode {
	
	/** The cps uni2 uni. */
	Map<String, String> cpsUni2Uni;
	
	/** The Constant DEFAULT_MAP_RESOURCE. */
	private static final String DEFAULT_MAP_RESOURCE = "jvntextpro/conversion/Composite2Unicode.txt";
	
	//---------------------------------------------------------------
	//Constructor
	//----------------------------------------------------------------
	
	/**
	 * Instantiates a new composite unicode2 unicode.
	 */
	public CompositeUnicode2Unicode(){	
		try{
			cpsUni2Uni = new HashMap<String, String>();
			
			URL url = CompositeUnicode2Unicode.class.getClassLoader().getResource(DEFAULT_MAP_RESOURCE);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream(), "UTF-8"));
			
			String line;
			while ((line = reader.readLine()) != null){
				String [] onemap = line.split("\t");
				
				if (onemap.length != 2) continue;
				cpsUni2Uni.put(onemap[0], onemap[1]);
			}
			
			reader.close();
		}
		catch (Exception e){
			System.err.println("Loading composite to unicode map fail: " + e.getMessage());
			cpsUni2Uni = null;
		}
	}
	
	//---------------------------------------------------------------
	//Public method
	//----------------------------------------------------------------

	/**
	 * Convert a vietnamese string with composite unicode encoding to unicode encoding.
	 *
	 * @param text string in vietnamese with composite unicode encoding
	 * @return string with unicode encoding
	 */
	public String convert(String text){
		String ret = text;
		
		if (cpsUni2Uni == null) return ret;
		
		Iterator<String> it = cpsUni2Uni.keySet().iterator();
		while(it.hasNext()){
			String cpsChar = it.next();
			ret = ret.replaceAll(cpsChar, cpsUni2Uni.get(cpsChar));
		}
		
		return ret;
		
	}
}
