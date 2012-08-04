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

	@Override
	public String format(JCas jcas) throws Exception {
		ByteArrayOutputStream outStream = null;

		String documentText = jcas.getDocumentText();

		/* 
		 * loop through the timexes to create two treemaps:
		 * - one containing startingposition=>timex tuples for eradication of overlapping timexes
		 * - one containing endposition=>timex tuples for assembly of the XML file
		 */
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer,Timex3> tmTimexStartTimex = new TreeMap<Integer,Timex3>();
		
		while (iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();
			tmTimexStartTimex.put(t.getBegin(), t);
		}
		
		// iterate forwards over Timexes to remove overlapping timexes
		TreeMap<Integer,Timex3> tmTimexEndTimex = new TreeMap<Integer,Timex3>();
		for(Integer begin : tmTimexStartTimex.navigableKeySet()) {
			// grab a timex from the start set
			Timex3 t = tmTimexStartTimex.get(begin);
			
			/* 
			 * check if for this timex t there exists a timex tExist in the target set of timexes that 
			 * overlaps with this one and if so, ignore it. otherwise, add it to the target set.
			 */
			Boolean overlap = false;
			for(Timex3 tExist : tmTimexEndTimex.values()) {
				// the beginning index of the current timex lies between the beginning and end of a previous one
				if(t.getBegin() >= tExist.getBegin() && t.getBegin() <= tExist.getEnd()) {
					overlap = true;
					
					// ask user to let us know about possibly incomplete rules
					Logger l = Logger.getLogger("TimeMLResultFormatter");
					l.log(Level.WARNING, "Omitting a timex due to overlap: \""+t.getCoveredText()+"\"["+t.getBegin()+","+t.getEnd()+"].");
					l.log(Level.WARNING, "Please consider opening an issue on http://code.google.com/p/heideltime " +
							"that includes the relevant part of the document you were tagging when this omission occurred; " +
							"we're always eager to improve our resources so that these omissions due to overlap no longer occur.");
					
					// break out of analysis
					break;
				}
			}
			
			if(!overlap) {
				tmTimexEndTimex.put(t.getEnd(), t);
			}
		}

		// iterate backwards over Timex3 Annotations and start with the last one in the document		
		for (Integer end : tmTimexEndTimex.descendingKeySet()){
			Timex3 t = tmTimexEndTimex.get(end);
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

		// Output is UTF-8 always.
		// One could change documentText encoding from UTF-8 to the input encoding if needed

		
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
