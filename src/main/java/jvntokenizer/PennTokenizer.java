/*
    Java version of Brill's Part-of-Speech Tagger
    Copyright (C) 2003-2004, Jimmy Lin

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, please visit 
    http://www.gnu.org/copyleft/gpl.html
*/

package jvntokenizer;

// TODO: Auto-generated Javadoc
/**
 * 
 * Tokenizer that conforms to the Penn Treebank conventions for tokenization.
 * 
 * @author Jimmy Lin
 *  
 */
 
public final class PennTokenizer {

	/**
	 * Tokenizes according to the Penn Treebank conventions.
	 *
	 * @param str the str
	 * @return the string
	 */
	public static String tokenize(String str) {		
		str = str.replaceAll("``", " `` ");
		str = str.replaceAll("''", "  '' ");
		str = str.replaceAll("\"", "  \" ");
		str = str.replaceAll("([?!\";#$&])", " $1 ");                
		str = str.replaceAll("\\.\\.\\.", " ... ");
		str = str.replaceAll("([^.])([.])([\\])}>\"']*)\\s*$", "$1 $2$3 ");		
		str = str.replaceAll("([\\[\\](){}<>])", " $1 ");
		str = str.replaceAll("--", " -- ");

		str = str.replaceAll("$", " ");
		str = str.replaceAll("^", " ");

		//str = str.replaceAll("\"", " '' ");
		str = str.replaceAll("([^'])' ", "$1 ' ");
		str = str.replaceAll("'([sSmMdD]) ", " '$1 ");
		str = str.replaceAll("'ll ", " 'll ");
		str = str.replaceAll("'re ", " 're ");
		str = str.replaceAll("'ve ", " 've ");
		str = str.replaceAll("n't ", " n't ");
		str = str.replaceAll("'LL ", " 'LL ");
		str = str.replaceAll("'RE ", " 'RE ");
		str = str.replaceAll("'VE ", " 'VE ");
		str = str.replaceAll("N'T ", " N'T ");

		str = str.replaceAll(" ([Cc])annot ", " $1an not ");
		str = str.replaceAll(" ([Dd])'ye ", " $1' ye ");
		str = str.replaceAll(" ([Gg])imme ", " $1im me ");
		str = str.replaceAll(" ([Gg])onna ", " $1on na ");
		str = str.replaceAll(" ([Gg])otta ", " $1ot ta ");
		str = str.replaceAll(" ([Ll])emme ", " $1em me ");
		str = str.replaceAll(" ([Mm])ore'n ", " $1ore 'n ");
		str = str.replaceAll(" '([Tt])is ", " $1 is ");
		str = str.replaceAll(" '([Tt])was ", " $1 was ");
		str = str.replaceAll(" ([Ww])anna ", " $1an na ");

		//"Nicole I. Kidman" gets tokenized as "Nicole I . Kidman"
		str = str.replaceAll(" ([A-Z])\\. ", " $1 . ");
		
		
		//written by TuNC from here
	    str = str.replaceAll(",([^0-9])", ", $1");
		str = str.replaceAll("'([^'])", "' $1");   
		str = str.replaceAll("([^\\xBB])(\\xBB)", "$1 $2");					
		str = str.replaceAll("(\\u201C)([^'])", "$1 $2");    				
		str = str.replaceAll("([^'])(\\u201D)", "$1 $2");
		
    	str = str.replaceAll("\\,([^0-9])", "\\, $1"); 			                
		str = str.replaceAll("([^\\s]),([\\s])", "$1 , $2");	 			//abc,<blank> -> abc ,		
		str = str.replaceAll("([^\\s:/0-9])/([^\\s:/0-9])", "$1 / $2"); 	//exception : url http://..., date-time: 12/3/98
		str = str.replaceAll("([^\\s0-9]+)-"," $1 -"); 						//abc-xyz -> abc - xyz; exception 12-3 (date-time)
		str = str.replaceAll("-([^\\s0-9]+)","- $1");
		str = str.replaceAll("([^\\s]):([\\s])", "$1 : $2"); 				// abc:<blank> -> abc : 
	    str = str.replaceAll("([^\\s]):([^0-9]+)", "$1 : $2"); 				//abc:xyz --> abc : xyz; exception: 12:03
	    str = str.replaceAll("([^0-9]+):([^\\s])", "$1 : $2");
    	str = str.replaceAll(" -([^\\s]+)", " - $1");
    	
    	str = str.replaceAll("|", "");
    	str = str.replaceAll("[\u2026\u201C\u201D]", "");   
    	str = str.replaceAll("([^\\p{L}0-9\\.\\,:\\-/])", " $1 "); 			//tokenize all unknown characters
		str = str.replaceAll("[ \t]+", " ");
		str = str.replaceAll("^\\s+", "");	
		str = str.replaceAll("\\. \\.\\.", " ... ");
		str = str.trim();
		
		return str;
	}
}

