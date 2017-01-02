package de.unihd.dbs.uima.annotator.heideltime.utilities;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChineseNumbers {
	/** Class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ChineseNumbers.class);

	static HashMap<Character, Character> CHINESE_NUMBERS_MAP = new HashMap<>();

	static {
		CHINESE_NUMBERS_MAP.put('零', '0');
		CHINESE_NUMBERS_MAP.put('一', '1');
		CHINESE_NUMBERS_MAP.put('二', '2');
		CHINESE_NUMBERS_MAP.put('三', '3');
		CHINESE_NUMBERS_MAP.put('四', '4');
		CHINESE_NUMBERS_MAP.put('五', '5');
		CHINESE_NUMBERS_MAP.put('六', '6');
		CHINESE_NUMBERS_MAP.put('七', '7');
		CHINESE_NUMBERS_MAP.put('八', '8');
		CHINESE_NUMBERS_MAP.put('九', '9');
		// Unicode arabic-lookalikes (wide)
		CHINESE_NUMBERS_MAP.put('０', '0');
		CHINESE_NUMBERS_MAP.put('１', '1');
		CHINESE_NUMBERS_MAP.put('２', '2');
		CHINESE_NUMBERS_MAP.put('３', '3');
		CHINESE_NUMBERS_MAP.put('４', '4');
		CHINESE_NUMBERS_MAP.put('５', '5');
		CHINESE_NUMBERS_MAP.put('６', '6');
		CHINESE_NUMBERS_MAP.put('７', '7');
		CHINESE_NUMBERS_MAP.put('８', '8');
		CHINESE_NUMBERS_MAP.put('９', '9');
		// Allow real arabic, too.
		CHINESE_NUMBERS_MAP.put('0', '0');
		CHINESE_NUMBERS_MAP.put('1', '1');
		CHINESE_NUMBERS_MAP.put('2', '2');
		CHINESE_NUMBERS_MAP.put('3', '3');
		CHINESE_NUMBERS_MAP.put('4', '4');
		CHINESE_NUMBERS_MAP.put('5', '5');
		CHINESE_NUMBERS_MAP.put('6', '6');
		CHINESE_NUMBERS_MAP.put('7', '7');
		CHINESE_NUMBERS_MAP.put('8', '8');
		CHINESE_NUMBERS_MAP.put('9', '9');
	}

	public static String normalize(String chinese) {
		String outString = "";
		for (int i = 0; i < chinese.length(); i++) {
			char thisChar = chinese.charAt(i);
			Character rep = CHINESE_NUMBERS_MAP.get((Character) thisChar);
			if (rep != null) {
				outString += rep;
			} else {
				// System.out.println(chineseNumerals.entrySet());
				LOG.error("Found an error in the resources: " + chinese + " contains " + "a character that is not defined in the Chinese numerals map. Normalization may be mangled.");
				outString += thisChar;
			}
		}
		return outString;
	}
}