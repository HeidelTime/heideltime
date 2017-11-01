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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class StringUtils.
 */
public class StringUtils {
		
	/**
	 * Find the first occurrence .
	 *
	 * @param container the string on which we search
	 * @param chars the string which we search for the occurrence
	 * @param begin the start position to search from
	 * @return the position where chars first occur in the container
	 */
	public static int findFirstOf (String container, String chars, int begin){        
        int minIdx = -1;        
        for (int i = 0; i < chars.length() && i >= 0; ++i){
            int idx = container.indexOf(chars.charAt(i), begin);            
            if ( (idx < minIdx && idx != -1) || minIdx == -1){                    
                    minIdx = idx;
            }
        }
        return minIdx;
    }
	   
    /**
     * Find the last occurrence.
     *
     * @param container the string on which we search 
     * @param charSeq the string which we search for the occurrence 
     * @param begin the start position in container to search from
     * @return the position where charSeq occurs for the last time in container (from right to left).
     */
    public static int findLastOf (String container, String charSeq, int begin){        
		//find the last occurrence of one of characters in charSeq from begin backward
        for (int i = begin; i < container.length() && i >= 0; --i){
            if (charSeq.contains("" + container.charAt(i)))
                return i;
        }
        return -1;        
    }
    
    /**
     * Find the first occurrence of characters not in the charSeq from begin 
     *
     * @param container the container
     * @param chars the chars
     * @param begin the begin
     * @return the int
     */
    public static int findFirstNotOf(String container, String chars, int begin){
		//find the first occurrence of characters not in the charSeq	from begin forward	
		for (int i = begin; i < container.length() && i >=0; ++i) 
		   if (!chars.contains("" + container.charAt(i)))
				return i;
		return -1;
    }
    
    /**
     * Find last not of.
     *
     * @param container the container
     * @param charSeq the char seq
     * @param end the end
     * @return the int
     */
    public static int findLastNotOf(String container, String charSeq, int end){
        for (int i = end; i < container.length() && i >= 0; --i){
            if (!charSeq.contains("" + container.charAt(i)))
                return i;        
        }
        return -1;
    } 
    
    //Syllable Features 
    /**
     * Contain number.
     *
     * @param str the str
     * @return true, if successful
     */
    public static boolean containNumber(String str) {
		for (int i = 0; i < str.length(); i++) {
		    if (Character.isDigit(str.charAt(i))) {
			return true;
		    }
		}		
		return false;
    }    
    
    /**
     * Contain letter.
     *
     * @param str the str
     * @return true, if successful
     */
    public static boolean containLetter(String str) {
		for (int i = 0; i < str.length(); i++) {
		    if (Character.isLetter(str.charAt(i))) {
			return true;
		    }
		}
		
		return false;
    }
    
    /**
     * Contain letter and digit.
     *
     * @param str the string
     * @return true, if str consists both letters & digits
     */
    public static boolean containLetterAndDigit(String str) {
    	return (containLetter(str) && containNumber(str));
    }
            
    /**
     * Checks if is all number.
     *
     * @param str the string
     * @return true, if str consists all numbers
     */
    public static boolean isAllNumber(String str) {
    	boolean hasNumber = false;
		for (int i = 0; i < str.length(); i++) {
		    if (!(Character.isDigit(str.charAt(i)) || 
				str.charAt(i) == '.' || str.charAt(i) == ',' || str.charAt(i) == '%'
				|| str.charAt(i) == '$' || str.charAt(i) == '_')) {
			return false;
		    }
		    else if (Character.isDigit(str.charAt(i)))
		    	hasNumber = true;
		}
		
		if (hasNumber == true)
			return true;
		else return false;		
    }
    
    /**
     * Checks if is first cap.
     *
     * @param str the string
     * @return true, if str has the first character capitalized
     */
    public static boolean isFirstCap(String str) {
    	if (isAllCap(str)) return false;
    	
		if (str.length() > 0 && Character.isLetter(str.charAt(0)) &&
				Character.isUpperCase(str.charAt(0))) {
		    return true;
		}
		
		return false;
    }
    
    
    /**
     * Checks if is all capitalized.
     *
     * @param str the string
     * @return true, if is all characters capitalized 
     */
    public static boolean isAllCap(String str) {
		if (str.length() <= 0) {
		    return false;
		}
		
		for (int i = 0; i < str.length(); i++) {
		    if (!Character.isLetter(str.charAt(i)) ||
		    		!Character.isUpperCase(str.charAt(i))) {
				    return false;				
		    }
		}
		
		return true;	
    }
    
    /**
     * Checks if is not first capitalized.
     *
     * @param str the str
     * @return true, if is not first capitalized
     */
    public static boolean isNotFirstCap(String str) {
    	return !isFirstCap(str);
    }    
    
    /**
     * Ends with sign.
     *
     * @param str the string token to test
     * @return true, if this token is ended with punctuation (such as ?:\;)  
     */
    public static boolean endsWithPunc(String str) {
		if (str.endsWith(".") || str.endsWith("?") || str.endsWith("!") ||
			    str.endsWith(",") || str.endsWith(":") || str.endsWith("\"") || 
			    str.endsWith("'") || str.endsWith("''") || str.endsWith(";")) {
		    return true;
		}
		
		return false;
    }

    /**
     * Ends with stop.
     *
     * @param str the string
     * @return true, if this token is ended with stop '.'
     */
    public static boolean endsWithStop(String str) {
	if (str.endsWith(".") || str.endsWith("?") || str.endsWith("!")) {
	    return true;
	}
	
	return false;
    }
    
    /**
     * Count stops.
     *
     * @param str string
     * @return how many stops '.' str contains
     */
    public static int countStops(String str) {
		int count = 0;
	    
		for (int i = 0; i < str.length(); i++) {
		    if (str.charAt(i) == '.' || str.charAt(i) == '?' || str.charAt(i) == '!') {
			count++;
		    }
		}
		
		return count;
    }
    
    /**
     * Count signs.
     *
     * @param str string 
     * @return the number of punctuation marks in this token
     */
    public static int countPuncs(String str) {
		int count = 0;
	    
		for (int i = 0; i < str.length(); i++) {
		    if (str.charAt(i) == '.' || str.charAt(i) == '?' || str.charAt(i) == '!' ||
				str.charAt(i) == ',' || str.charAt(i) == ':' || str.charAt(i) == ';') {
			count++;
		    }
		}
		
		return count;
    }
    
    /**
     * Checks if is stop.
     *
     * @param str string
     * @return true, if the input is the stop character '.'
     */
    public static boolean isStop(String str) {
		if (str.compareTo(".") == 0) {
		    return true;
		}
	
		if (str.compareTo("?") == 0) {
		    return true;
		}
		
		if (str.compareTo("!") == 0) {
		    return true;
		}
		
		return false;
    }
    
    /**
     * Checks if is punctuation.
     *
     * @param str the string token to test
     * @return true, if the input is one of the punctuation marks 
     */
    public static boolean isPunc(String str) {
    	if (str == null) return false;
    	str = str.trim();
    	
    	for (int i = 0; i < str.length(); ++i){
    		char c = str.charAt(i);
    		if (Character.isDigit(c) || Character.isLetter(c)){
    			return false;
    		}
    	}
		return true;
    }
    
    /**
     * Join the <tt>String</tt> representations of an array of objects, with the specified
     * separator.
     *
     * @param objects the objects
     * @param sep the sep
     * @return  newly created .
     */
	public static String join( Object[] objects, char sep )
	{
		if( objects.length == 0 )
		{
			return "";
		}
		StringBuffer buffer = new StringBuffer( objects[0].toString() );
		for (int i = 1; i < objects.length; i++)
		{
			buffer.append( sep );
			buffer.append( objects[i].toString() );
		}
		return buffer.toString();
	}
	
	/**
	 * Join the <tt>String</tt> representations of a collection of objects, with the specified
	 * separator.
	 *
	 * @param col the col
	 * @param sep the sep
	 * @return  newly created .
	 */
	public static String join( Collection col, char sep )
	{
		if( col.isEmpty() )
		{
			return "";
		}
		StringBuffer buffer = new StringBuffer();
		boolean first = true; 
		for (Object o : col)
		{
			if( first )
			{
				first = false;
			}
			else
			{
				buffer.append( sep );
			}
			buffer.append( o.toString() );
		}
		return buffer.toString();
	}
	
	// ---------------------------------------------------------
	// String Manipulation
	// ---------------------------------------------------------
	
	/**
	 * Capitalises the first letter of a given string.
	 *  
	 * @param s  the input string
	 * 
	 * @return   the capitalized string
	 */
	public static String capitalizeWord( String s )
	{
		// validate
		if( (s == null) || (s.length() == 0) )
		{
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
	
	/** 
	 * Encloses the specified <tt>String</tt> in single quotes.
	 * 
	 * @param s  the input string
	 * 
	 * @return the quoted String
	 */
	public static String quote( String s )
	{
		return '\'' + s + '\''; 
	}

	/** 
	 * Encloses the specified <tt>String</tt> in double quotes.
	 * 
	 * @param s  the input string
	 * 
	 * @return the quoted String
	 */
	public static String doubleQuote( String s )
	{
		return '"' + s + '"'; 
	}

	/**
	 * Pad the specified <tt>String</tt> with spaces on the right-hand side.
	 * 
	 * @param s       String to add spaces
	 * @param length  Desired length of string after padding
	 * 
	 * @return padded string.
	 */
	public static String pad( String s, int length )
	{
		// Trim if longer...
		if( s.length() > length )
		{
			return s.substring( 0, length );
		}
		StringBuffer buffer = new StringBuffer(s);
		int spaces = length - s.length();
		while( spaces-- > 0 )
		{
			buffer.append(' ');
		}
		return buffer.toString();
	}
	
	/**
	 * Sorts the characters in the specified string.
	 * 
	 * @param s   input String to sort.
	 * 
	 * @return  output String, containing sorted characters.
	 */
	public static String sort( String s )
	{
		char[] chars = s.toCharArray();
		Arrays.sort( chars );
		return new String( chars );
	}

	  
	// ---------------------------------------------------------
	// String Matching
	// ---------------------------------------------------------
	
   /**
	 * Checks whether a String is whitespace, empty or null.
	 *
	 * @param s   the <tt>String</tt> to analyze.
	 * @return  otherwise.
	 */
	public static boolean isBlank( String s )
	{
		if (s == null)
		{
			return true;
		}
		int sLen = s.length();
		for (int i = 0; i < sLen; i++)
		{
			if (!Character.isWhitespace(s.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}
   
   /**
    * Checks whether a <tt>String</tt> is composed entirely of whitespace characters.
    *
    * @param s   the <tt>String</tt> to analyze.
    * @return  otherwise.
    */
	public static boolean isWhitespace( String s )
	{
		if( s == null )
		{
			return false;
		}
		int sLen = s.length();
		for (int i = 0; i < sLen; i++)
		{
			if (!Character.isWhitespace(s.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}
	
	// ---------------------------------------------------------
	// Search-related
	// ---------------------------------------------------------
   
	/**
	 * Counts the number of occurrences of a character in the specified <tt>String</tt>.
	 * 
	 * @param s   the <tt>String</tt> to analyze.
	 * @param c   the character to search for.
	 * 
	 * @return number of occurrences found.
	 */
	public static int countOccurrences( String s, char c )
	{
		int count = 0;
		int index = 0;
		while( true )
		{
			index = s.indexOf( c, index );
			if( index == -1 )
			{
				break;
			}
			count++;
		}
		return count;
	}
	
	/**
	 * Indicates whether the specified array of <tt>String</tt>s contains
	 * a given <tt>String</tt>.
	 *
	 * @param array the array
	 * @param s the s
	 * @return  otherwise.
	 */
	public static boolean isContained( String[] array, String s )
	{
		for (String string : array)
		{
			if( string.equals( s ) )
			{
				return true;
			}
		}
		return false;
	}
	
	// ---------------------------------------------------------
	// Array/Collection conversion
	// ---------------------------------------------------------
	
	/**
	 * Returns the index of the first occurrence of the specified <tt>String</tt>
	 * in an array of <tt>String</tt>s.
	 * 
	 * @param array  array of <tt>String</tt>s to search.
	 * @param s      the <tt>String</tt> to search for.
	 * 
	 * @return the index of the first occurrence of the argument in this list, 
	 *         or -1 if the string is not found.
	 */
	public static int indexOf( String[] array, String s )
	{
		for (int index = 0; index < array.length; index++)
		{
			if( s.equals( array[index] ) )
			{
				return index;
			}
		}
		return -1;
	}
	
	/**
	 * Creates a new <tt>ArrayList</tt> collection from the specified array of <tt>String</tt>s.
	 *
	 * @param array the array
	 * @return  newly created .
	 */
	public static ArrayList<String> toList( String[] array )
	{
		if( array == null )
		{
			return new ArrayList<String>( 0 );
		}
		ArrayList<String> list = new ArrayList<String>( array.length );
		for (String s : array)
		{
			list.add( s );
		}
		return list;
	}
	
	/**
	 * Creates a new <tt>Vector</tt> collection from the specified array of <tt>String</tt>s.
	 *
	 * @param array the array
	 * @return  newly created .
	 */
	public static Vector<String> toVector( String[] array )
	{
		if( array == null )
		{
			return new Vector<String>( 0 );
		}
		Vector<String> v = new Vector<String>( array.length );
		v.copyInto( array );
		return v;
	}
	
	/**
	 * Creates a new <tt>ArrayList</tt> collection from the specified <tt>Set</tt> of <tt>String</tt>s.
	 *
	 * @param set   a set of <tt>String</tt>s.
	 * @return newly created .
	 */
	public static ArrayList<String> toList( Set<String> set )
	{
		int n = set.size();
		ArrayList<String> list = new ArrayList<String>( n );
		for (String string : set)
		{
			list.add(string);
		}
		return list;
	}

    
}
