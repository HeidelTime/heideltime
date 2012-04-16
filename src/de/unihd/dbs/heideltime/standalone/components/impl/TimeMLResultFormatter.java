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

		// iterate over Timex3 Annotations and start with the last one in the document
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer,Timex3> hmTimexEndTimex = new TreeMap<Integer,Timex3>();
		while (iterTimex.hasNext()){
			Timex3 t = (Timex3) iterTimex.next();
			hmTimexEndTimex.put(t.getEnd(), t);
		}
		
		for (Integer end : hmTimexEndTimex.descendingKeySet()){
			Timex3 t = hmTimexEndTimex.get(end);
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
