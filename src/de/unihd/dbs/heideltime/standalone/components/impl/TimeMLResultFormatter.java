/*
 * TimeMLResultFormatter.java
 *
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, University of Heidelberg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License.
 *
 * authors: Andreas Fay, Jannik Strötgen
 * email:  fay@stud.uni-heidelberg.de, stroetgen@uni-hd.de
 *
 * HeidelTime is a multilingual, cross-domain temporal tagger.
 * For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
 */ 

package de.unihd.dbs.heideltime.standalone.components.impl;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.uima.types.heideltime.Timex3;

/**
 * Result formatter based on TimeML.
 * 
 * @see {@link org.apache.uima.examples.xmi.XmiWriterCasConsumer}
 * 
 * @author Andreas Fay, Jannik Strötgen Heidelberg University
 * @version 1.01
 */
public class TimeMLResultFormatter implements ResultFormatter {
	public String format(JCas jcas) throws Exception {
		ByteArrayOutputStream outStream = null;

		String documentText = jcas.getDocumentText();

		/* 
		 * loop through the timexes to create two treemaps:
		 * - one containing startingposition=>timex tuples for eradication of overlapping timexes
		 * - one containing endposition=>timex tuples for assembly of the XML file
		 */
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer, Timex3> forwardTimexes = new TreeMap<Integer, Timex3>(),
				backwardTimexes = new TreeMap<Integer, Timex3>();
		while(iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();
			forwardTimexes.put(t.getBegin(), t);
			backwardTimexes.put(t.getEnd(), t);
		}
		
		HashSet<Timex3> timexesToSkip = new HashSet<Timex3>();
		Timex3 prevT = null;
		Timex3 thisT = null;
		// iterate over timexes to find overlaps
		for(Integer begin : forwardTimexes.navigableKeySet()) {
			thisT = (Timex3) forwardTimexes.get(begin);
			
			// check for whether this and the previous timex overlap. ex: [early (friday] morning)
			if(prevT != null && prevT.getEnd() > thisT.getBegin()) {
				
				Timex3 removedT = null; // only for debug message
				// assuming longer value string means better granularity
				if(prevT.getTimexValue().length() > thisT.getTimexValue().length()) {
					timexesToSkip.add(thisT);
					removedT = thisT;
					/* prevT stays the same. */
				} else {
					timexesToSkip.add(prevT);
					removedT = prevT;
					prevT = thisT; // this iteration's prevT was removed; setting for new iteration 
				}
				
				// ask user to let us know about possibly incomplete rules
				Logger l = Logger.getLogger("TimeMLResultFormatter");
				l.log(Level.WARNING, "Two overlapping Timexes have been discovered:" + System.getProperty("line.separator")
						+ "Timex A: " + prevT.getCoveredText() + " [\"" + prevT.getTimexValue() + "\" / " + prevT.getBegin() + ":" + prevT.getEnd() + "]" 
						+ System.getProperty("line.separator")
						+ "Timex B: " + removedT.getCoveredText() + " [\"" + removedT.getTimexValue() + "\" / " + removedT.getBegin() + ":" + removedT.getEnd() + "]" 
						+ " [removed]" + System.getProperty("line.separator")
						+ "The writer chose, for granularity: " + prevT.getCoveredText() + System.getProperty("line.separator")
						+ "This usually happens with an incomplete ruleset. Please consider adding "
						+ "a new rule that covers the entire expression.");
			} else { // no overlap found? set current timex as next iteration's previous timex
				prevT = thisT;
			}
		}

		// iterate backwards over Timex3 Annotations and start with the last one in the document
		for (Integer end : backwardTimexes.descendingKeySet()){
			Timex3 t = backwardTimexes.get(end);
			if(timexesToSkip.contains(t)) continue; // skip this timex
			
			String timexStartTag = "<TIMEX3";
			if (!(t.getTimexId().equals(""))){
				timexStartTag += " tid=\""+t.getTimexId()+"\"";
			}
			if (!(t.getTimexType().equals(""))){
				timexStartTag += " type=\""+t.getTimexType()+"\"";
			}
			if (!(t.getTimexValue().equals(""))){
				timexStartTag += " value=\""+t.getTimexValue()+"\"";
			}
			if (!(t.getTimexQuant().equals(""))){
				timexStartTag += " quant=\""+t.getTimexQuant()+"\"";
			}
			if (!(t.getTimexFreq().equals(""))){
				timexStartTag += " freq=\""+t.getTimexFreq()+"\"";
			}
			if (!(t.getTimexMod().equals(""))){
				timexStartTag += " mod=\""+t.getTimexMod()+"\"";
			}
			timexStartTag += ">";
			documentText = documentText.substring(0, t.getBegin()) +
								timexStartTag + 
								documentText.substring(t.getBegin(), t.getEnd()) +
								"</TIMEX3>" +
								documentText.substring(t.getEnd());
		}
		
		// Add TimeML start and end tags		
		documentText = "<?xml version=\"1.0\"?>\n<!DOCTYPE TimeML SYSTEM \"TimeML.dtd\">\n<TimeML>\n" + documentText;
		documentText = documentText + "\n</TimeML>\n";
		
		try {
			// Write TimeML file
			return documentText;
		} finally {
			if (outStream != null) {
				outStream.close();
			}
		}
	}

}
