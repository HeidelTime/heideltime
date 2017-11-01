package de.unihd.dbs.heideltime.standalone.components.impl;


import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.jcas.JCas;
import org.apache.uima.json.JsonCasSerializer;

import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;



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
			l.log(Level.FINEST, "Preparing to serialize the results in JSON");
			JsonCasSerializer serializer = new JsonCasSerializer();
			serializer.setPrettyPrint(true);
			serializer.serialize(jcas.getCas(), output);
			outText = output.toString();
			l.log(Level.FINEST, "JSON-serialization finished.");
		}
		finally {
			output.close();
		}
		return outText;
	}

}
