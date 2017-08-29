package de.unihd.dbs.heideltime.standalone.components.impl;


import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.json.JsonCasSerializer;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;



/**
 * Result formatter based on TimeML.
 * 
 * @see {@link org.apache.uima.examples.xmi.XmiWriterCasConsumer}
 * 
 * @author Lise Rebout, CRIM
 * @version 1.01
 */
public class JSONResultFormatter implements ResultFormatter {

	@Override
	public String format(JCas jcas) throws Exception {
		Logger l = Logger.getLogger("JSONResultFormatter");
		StringWriter output = new StringWriter();
		String outText = "";
		
		try {
			// TODO : filtrer les résultats pour ne garder que les annotations Timex3 
			l.log(Level.INFO, "Preparing to serialize the results in JSON");
			JsonCasSerializer serializer = new JsonCasSerializer();

			serializer.serialize(jcas.getCas(), output);
			outText = output.toString();
			l.log(Level.INFO, "JSON-serialization finished.");
		}
		finally {
			output.close();
		}
		return outText;
	}

}
