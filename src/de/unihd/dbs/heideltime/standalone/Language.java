/*
 * Language.java
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

package de.unihd.dbs.heideltime.standalone;

/**
 * Possible languages supported by HeidelTime
 * 
 * @author Andreas Fay, Jannik Strötgen, University of Heidelberg
 * @version 1.0
 */
public enum Language {
	ENGLISH {
		public String toString() {
			return "english";
		}
	},
	GERMAN {
		public String toString() {
			return "german";
		}
	},
	DUTCH {
		public String toString() {
			return "dutch";
		}
	},
	FRENCH {
		public String toString() {
			return "french";
		}
	},
	SPANISH {
		public String toString() {
			return "spanish";
		}
	},
	ITALIAN {
		public String toString() {
			return "italian";
		}
	},
}
