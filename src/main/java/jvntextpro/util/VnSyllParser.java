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
package jvntextpro.util;

import java.util.*;

// TODO: Auto-generated Javadoc
class TONE {
	public static final TONE NO_TONE = new TONE(0);

	public static final TONE ACUTE = new TONE(1);

	public static final TONE ACCENT = new TONE(2);

	public static final TONE QUESTION = new TONE(3);

	public static final TONE TILDE = new TONE(4);

	public static final TONE DOT = new TONE(5);

	public static TONE getTone(int v) {
		switch (v) {
		case 0:
			return NO_TONE;
		case 1:
			return ACUTE;
		case 2:
			return ACCENT;
		case 3:
			return QUESTION;
		case 4:
			return TILDE;
		case 5:
			return DOT;
		default:
			return NO_TONE;
		}
	}

	public int getValue() {
		return value;
	}

	private TONE(int v) {
		value = v;
	}

	private int value;
}

/*
 * This class parse a vietnamese syllable in UTF-8 encoding
 */
/**
 * The Class VnSyllParser.
 */
public class VnSyllParser {
	// Member Data
	/** The Constant vnFirstConsonants. */
	private static final String vnFirstConsonants = "ngh|ng|gh|ph|ch|tr|nh|kh|th|m|b|v|t|\u0111|n|x|s|l|h|r|d|gi|g|q|k|c";

	/** The Constant vnLastConsonants. */
	private static final String vnLastConsonants = "ng|nh|ch|p|t|c|m|n|u|o|y|i";

	/** The Constant vnMainVowels. */
	private static final String vnMainVowels = "i\u00EA|y\u00EA|ia|ya|\u01B0\u01A1|\u01B0a|u\u00F4|ua|oo|\u00EA|e|a|\u01B0|\u0103|o|\u01A1|\u00E2|\u00F4|u|i|y|";

	/** The Constant vnSecondaryVowels. */
	private static final String vnSecondaryVowels = "o|u";

	/** The Constant ZERO. */
	public static final String ZERO = "";

	/** The vn vowels. */
	private static String vnVowels = "a\u00E1\u00E0\u1EA3\u00E3\u1EA1"
			+ "\u0103\u1EAF\u1EB1\u1EB3\u1EB5\u1EB7"
			+ "\u00E2\u1EA5\u1EA7\u1EA9\u1EAB\u1EAD"
			+ "e\u00E9\u00E8\u1EBB\u1EBD\u1EB9"
			+ "\u00EA\u1EBF\u1EC1\u1EC3\u1EC5\u1EC7"
			+ "i\u00ED\u00EC\u1EC9\u0129\u1ECB"
			+ "o\u00F3\u00F2\u1ECF\u00F5\u1ECD"
			+ "\u00F4\u1ED1\u1ED3\u1ED5\u1ED7\u1ED9"
			+ "\u01A1\u1EDB\u1EDD\u1EDF\u1EE1\u1EE3"
			+ "u\u00FA\u00F9\u1EE7\u0169\u1EE5"
			+ "\u01B0\u1EE9\u1EEB\u1EED\u1EEF\u1EF1"
			+ "y\u00FD\u1EF3\u1EF7\u1EF9\u1EF5";

	/** The al first consonants. */
	private static ArrayList alFirstConsonants;

	/** The al last consonants. */
	private static ArrayList alLastConsonants;

	/** The al main vowels. */
	private static ArrayList alMainVowels;

	/** The al secondary vowels. */
	private static ArrayList alSecondaryVowels;

	/** The str syllable. */
	private String strSyllable;

	/** The str main vowel. */
	private String strMainVowel;

	/** The str secondary vowel. */
	private String strSecondaryVowel;

	/** The str first consonant. */
	private String strFirstConsonant;

	/** The str last consonant. */
	private String strLastConsonant;

	/** The tone. */
	private TONE tone = TONE.NO_TONE;

	/** The i cur pos. */
	private int iCurPos;

	/** The valid vi syll. */
	private boolean validViSyll;

	// private boolean validSyll;

	// Public Methods
	/**
	 * Instantiates a new vn syll parser.
	 *
	 * @param syll the syll
	 */
	public VnSyllParser(String syll) {
		init();
		parseVnSyllable(syll);
	}

	/**
	 * Instantiates a new vn syll parser.
	 */
	public VnSyllParser() {
		init();
	}

	/**
	 * Parses the vn syllable.
	 *
	 * @param syll the syll
	 */
	public void parseVnSyllable(String syll) {
		strSyllable = syll;
		strMainVowel = "";
		strSecondaryVowel = "";
		strFirstConsonant = "";
		strLastConsonant = "";
		iCurPos = 0;
		validViSyll = true;

		parseFirstConsonant();
		parseSecondaryVowel();
		parseMainVowel();
		parseLastConsonant();
	}

	/**
	 * Gets the first consonant.
	 *
	 * @return the first consonant
	 */
	public String getFirstConsonant() {
		return strFirstConsonant;
	}

	/**
	 * Gets the second vowel.
	 *
	 * @return the second vowel
	 */
	public String getSecondVowel() {
		return strSecondaryVowel;
	}

	/**
	 * Gets the main vowel.
	 *
	 * @return the main vowel
	 */
	public String getMainVowel() {
		return strMainVowel;
	}

	/**
	 * Gets the last consonant.
	 *
	 * @return the last consonant
	 */
	public String getLastConsonant() {
		return strLastConsonant;
	}

	/**
	 * Gets the tone.
	 *
	 * @return the tone
	 */
	public TONE getTone() {
		return tone;
	}

	/**
	 * Gets the rhyme.
	 *
	 * @return the rhyme
	 */
	public String getRhyme(){
		return strSecondaryVowel + strMainVowel + strLastConsonant;
	}
	
	/**
	 * Gets the non tone syll.
	 *
	 * @return the non tone syll
	 */
	public String getNonToneSyll(){
		return strFirstConsonant + strSecondaryVowel + strMainVowel + strLastConsonant;
	}
	
	/**
	 * Checks if is valid vn syllable.
	 *
	 * @return true, if is valid vn syllable
	 */
	public boolean isValidVnSyllable() {
		return validViSyll ;
	}

	// Private Methods
	/**
	 * Parses the first consonant.
	 */
	private void parseFirstConsonant() {
		// find first of (vnfirstconsonant)
		// if not found, first consonant = ZERO
		// else the found consonant
		Iterator iter = alFirstConsonants.iterator();
		while (iter.hasNext()) {
			String strFirstCon = (String) iter.next();
			if (strSyllable.startsWith(strFirstCon, iCurPos)) {
				strFirstConsonant = strFirstCon;
				iCurPos += strFirstCon.length();
				return;
			}
		}
		strFirstConsonant = ZERO;
	}

	/**
	 * Parses the secondary vowel.
	 */
	private void parseSecondaryVowel() {
		if (!validViSyll)
			return;
		// get the current and next character in the syllable string
		char curChar, nextChar;
		if (iCurPos > strSyllable.length() - 1) {
			validViSyll = false;
			return;
		}
		curChar = strSyllable.charAt(iCurPos);

		if (iCurPos == strSyllable.length() - 1)
			nextChar = '$';
		else
			nextChar = strSyllable.charAt(iCurPos + 1);

		// get the tone and the original vowel (without tone)
		TONE tone = TONE.NO_TONE;
		int idx1 = vnVowels.indexOf(curChar);
		int idx2 = vnVowels.indexOf(nextChar);

		if (idx1 == -1)
			return;// current char is not a vowel
		tone = TONE.getTone(idx1 % 6);
		curChar = vnVowels.charAt((idx1 / 6) * 6);

		if (idx2 == -1) { // next char is not a vowel
			strSecondaryVowel = ZERO;
			return;
		}
		nextChar = vnVowels.charAt((idx2 / 6) * 6);
		if (tone.getValue() == TONE.NO_TONE.getValue())
			tone = TONE.getTone(idx2 % 6);

		// Check the secondary vowel
		if (curChar == 'o') {
			if (nextChar == 'a' || nextChar == 'e') {
				strSecondaryVowel += curChar;
				iCurPos++;
			} else
				strSecondaryVowel = ZERO; // oo
			return;
		} else if (curChar == 'u') {
			if (nextChar != 'i' && nextChar != '$') {
				strSecondaryVowel += curChar;
				iCurPos++;
			} else
				strSecondaryVowel = ZERO;
			return;
		}
	}

	/**
	 * Parses the main vowel.
	 */
	private void parseMainVowel() {
		if (!validViSyll)
			return;
		if (iCurPos > strSyllable.length() - 1) {
			validViSyll = false;
			return;
		}

		String strVowel = "";
		for (int i = iCurPos; i < strSyllable.length(); ++i) {
			int idx = vnVowels.indexOf(strSyllable.charAt(i));
			if (idx == -1)
				break;

			strVowel += vnVowels.charAt((idx / 6) * 6);
			if (tone.getValue() == TONE.NO_TONE.getValue())
				tone = TONE.getTone(idx % 6);
		}

		Iterator iter = alMainVowels.iterator();
		while (iter.hasNext()) {
			String tempVowel = (String) iter.next();
			if (strVowel.startsWith(tempVowel)) {
				strMainVowel = tempVowel;
				iCurPos += tempVowel.length();
				return;
			}
		}
		validViSyll = false;
		return;
	}

	/**
	 * Parses the last consonant.
	 */
	private void parseLastConsonant() {
		if (!validViSyll)
			return;
		if (iCurPos > strSyllable.length())
			strLastConsonant = ZERO;
		String strCon = strSyllable.substring(iCurPos, strSyllable.length());

		if (strCon.length() > 3) {
			validViSyll = false;
			return;
		}

		Iterator iter = alLastConsonants.iterator();
		while (iter.hasNext()) {
			String tempLastCon = (String) iter.next();
			if (strCon.equals(tempLastCon)) {
				strLastConsonant = tempLastCon;
				iCurPos += strLastConsonant.length();
				return;
			}
		}
		strLastConsonant = ZERO;
		if (iCurPos >= strSyllable.length())
			validViSyll = true;
		else validViSyll = false;
		
		return;
	}

	/**
	 * Inits the.
	 */
	private static void init() {
		if (alFirstConsonants == null) {
			alFirstConsonants = new ArrayList();
			alLastConsonants = new ArrayList();
			alMainVowels = new ArrayList();
			alSecondaryVowels = new ArrayList();

			initArrayList(alFirstConsonants, vnFirstConsonants);
			initArrayList(alLastConsonants, vnLastConsonants);
			initArrayList(alMainVowels, vnMainVowels);
			initArrayList(alSecondaryVowels, vnSecondaryVowels);
		}
	}

	/**
	 * Inits the array list.
	 *
	 * @param al the al
	 * @param str the str
	 */
	private static void initArrayList(ArrayList al, String str) {
		StringTokenizer strTknr = new StringTokenizer(str, "|");
		while (strTknr.hasMoreTokens()) {
			al.add(strTknr.nextToken());
		}
	}
}
