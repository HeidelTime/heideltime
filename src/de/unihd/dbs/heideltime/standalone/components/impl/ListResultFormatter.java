/*
 * ListResultFormatter.java
 *
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, University of Heidelberg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License.
 *
 */ 

package de.unihd.dbs.heideltime.standalone.components.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;

/**
 * List Results Formatter
 * 
 * 
 * @author Francesco Paolo Albano, Politecnico di Bari
 * @version 1.0
 */
public class ListResultFormatter implements ResultFormatter {
	public String format(JCas jcas) throws Exception {
		final String documentText = jcas.getDocumentText();
		String outText = new String();


		//Logger l = Logger.getLogger("ListResultFormatter");
		//l.log(Level.INFO, "Start List ResultFormatter");

		FSIterator iterIntervals = jcas.getAnnotationIndex(Timex3Interval.type).iterator();
		while(iterIntervals.hasNext()) {
			Timex3Interval t = (Timex3Interval) iterIntervals.next();

			Integer start = t.getBegin();
			Integer end = t.getEnd();
			outText += t.getTimexType()+"\t"+start+" "+end+"\t"+documentText.substring(start, end)+"\t"+t.getTimexValueEB()+"-"+t.getTimexValueLB() +"\t"+t.getTimexValueEE()+"-"+t.getTimexValueLE()+"\n";
		}


		//only for timex3
		//we want TYPE start end Text NormalizedValue
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		while(iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();


			Integer start = t.getBegin();
			Integer end = t.getEnd();
			outText += t.getTimexType()+"\t"+start+" "+end+"\t"+documentText.substring(start, end)+"\t"+t.getTimexValue()+"\n";
		}

		//l.log(Level.INFO, "End List ResultFormatter");
		return outText;
	}
}
