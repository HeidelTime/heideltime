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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Element;

import jvntextpro.data.Sentence;

// TODO: Auto-generated Javadoc
/**
 * The Class RegexContextGenerator.
 */
public class RegexContextGenerator extends BasicContextGenerator {
	//----------------------------
	//variables
	//----------------------------
	// Regular Expression Pattern string
	/** The str number pattern. */
	private static String strNumberPattern = "[+-]?\\d+([,.]\\d+)*";
	
	/** The str short date pattern. */
	private static String strShortDatePattern = "\\d+[/-:]\\d+";
	
	/** The str long date pattern. */
	private static String strLongDatePattern = "\\d+[/-:]\\d+[/-:]\\d+";
	
	/** The str percentage pattern. */
	private static String strPercentagePattern = strNumberPattern + "%";
	
	/** The str currency pattern. */
	private static String strCurrencyPattern = "\\p{Sc}" + strNumberPattern;
	
	/** The str vi currency pattern. */
	private static String strViCurrencyPattern = strNumberPattern + "[ \t]*\\p{Sc}";

	// Regular Expression Pattern
	/** The ptn number. */
	private static Pattern ptnNumber;
	
	/** The ptn short date. */
	private static Pattern ptnShortDate;
	
	/** The ptn long date. */
	private static Pattern ptnLongDate;
	
	/** The ptn percentage. */
	private static Pattern ptnPercentage;
	
	/** The ptn currency. */
	private static Pattern ptnCurrency;
	
	/** The ptn vi currency. */
	private static Pattern ptnViCurrency;
	
	//----------------------------
	//methods
	//----------------------------
	/**
	 * Instantiates a new regex context generator.
	 *
	 * @param node the node
	 */
	public RegexContextGenerator(Element node){
		readFeatureParameters(node);
	}
	
	/* (non-Javadoc)
	 * @see jvntextpro.data.ContextGenerator#getContext(jvntextpro.data.Sentence, int)
	 */
	@Override
	public String[] getContext(Sentence sent, int pos) {
		// generate context predicates
		List<String> cps = new ArrayList<String>();
		
		
		// get the context information from sequence
		for (int it = 0; it < cpnames.size(); ++it){			
			String cp = cpnames.get(it);
			Vector<Integer> paras = this.paras.get(it);
			String cpvalue = "";
			
			String suffix = "", regex = "";
			String word = "";
			boolean outOfArrayIndex = false;
			for (int i = 0; i < paras.size(); ++i) {
				if (pos + paras.get(i) < 0 || pos + paras.get(i)>= sent.size()){
					cpvalue = "";
					outOfArrayIndex = true;
					break;
				}
	
				suffix += paras.get(i) + ":";
				word += sent.getWordAt(pos + paras.get(i)) + " ";
			}
			if (outOfArrayIndex) continue;
			
			word = word.trim().toLowerCase();
			suffix = suffix.substring(0, suffix.length() - 1);
			suffix = ":" + suffix;
	
			// Match to a specific pattern
			regex = patternMatching(cp, word);
			if (!regex.equals("")) {
				cpvalue = "re" + suffix + regex;
			}
			
			if (!cpvalue.equals("")) cps.add(cpvalue);
		}
		String [] ret = new String[cps.size()];		
		return cps.toArray(ret);
	}

	//----------------------------
	// utility methods
	//----------------------------
	/**
	 * Pattern compile.
	 */
	private static void patternCompile() {
		try {
			ptnNumber = Pattern.compile(strNumberPattern);
			ptnShortDate = Pattern.compile(strShortDatePattern);
			ptnLongDate = Pattern.compile(strLongDatePattern);
			ptnPercentage = Pattern.compile(strPercentagePattern);
			ptnCurrency = Pattern.compile(strCurrencyPattern);
			ptnViCurrency = Pattern.compile(strViCurrencyPattern);
		} catch (PatternSyntaxException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}

	}

	/**
	 * Pattern matching.
	 *
	 * @param ptnName the ptn name
	 * @param input the input
	 * @return the string
	 */
	private static String patternMatching(String ptnName, String input) {
		String suffix = "";
		if (ptnNumber == null)
			patternCompile();

		Matcher matcher;
		if (ptnName.equals("number")) {
			matcher = ptnNumber.matcher(input);
			if (matcher.matches())
				suffix = ":number";
		} else if (ptnName.equals("short_date")) {
			matcher = ptnShortDate.matcher(input);
			if (matcher.matches())
				suffix = ":short-date";
		} else if (ptnName.equals("long_date")) {
			matcher = ptnLongDate.matcher(input);
			if (matcher.matches())
				suffix = ":long-date";
		} else if (ptnName.equals("percentage")) {
			matcher = ptnPercentage.matcher(input);
			if (matcher.matches())
				suffix = ":percentage";
		} else if (ptnName.equals("currency")) {
			matcher = ptnCurrency.matcher(input);
			if (matcher.matches())
				suffix = ":currency";
			else {
				matcher = ptnViCurrency.matcher(input);
				if (matcher.matches()) {
					suffix = ":currency";
				}
			}
		}
		return suffix;
	}
}
