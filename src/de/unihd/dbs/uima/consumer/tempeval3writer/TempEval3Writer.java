package de.unihd.dbs.uima.consumer.tempeval3writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Timex3;

public class TempEval3Writer extends CasConsumer_ImplBase {
	private Class<?> component = this.getClass();

	private static final String PARAM_OUTPUTDIR = "OutputDir";

	private File mOutputDir;

	public void initialize() throws ResourceInitializationException {
		mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
		
		if (!mOutputDir.exists()) {
			if(!mOutputDir.mkdirs()) {
				Logger.printError(component, "Couldn't create non-existant folder "+mOutputDir.getAbsolutePath());
				throw new ResourceInitializationException();
			}
		}
		
		if(!mOutputDir.canWrite()) {
			Logger.printError(component, "Folder "+mOutputDir.getAbsolutePath()+" is not writable.");
			throw new ResourceInitializationException();
		}
	}

	public void processCas(CAS aCAS) throws ResourceProcessException {
		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}
		
		// get the DCT
		Dct dct = (Dct) jcas.getAnnotationIndex(Dct.type).iterator().next();

		// assemble an XML document
		Document xmlDoc = buildTimeMLDocument(jcas, dct);
		
		// write the document to file
		writeTimeMLDocument(xmlDoc, dct.getFilename());
	}

	
	private Document buildTimeMLDocument(JCas jcas, Dct dct) {
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		
		try {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			Logger.printError(component, "XML Builder could not be instantiated for "+dct.getFilename());
		}
		
		// create the TimeML root element
		Element rootEl = doc.createElement("TimeML");
		rootEl.setAttributeNS("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
		rootEl.setAttributeNS("xsi", "noNamespaceSchemaLocation", "http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd");
		doc.appendChild(rootEl);
		
		// create DOCID tag
		Element docidEl = doc.createElement("DOCID");
		docidEl.appendChild(doc.createTextNode(dct.getFilename()));
		rootEl.appendChild(docidEl);
		
		// create DCT tag
		Element dctEl = doc.createElement("DCT");
		dctEl.appendChild(doc.createTextNode(dct.getValue()));
		rootEl.appendChild(dctEl);
		
		// create and fill the TEXT tag
		Integer offset = 0;
		Element textEl = doc.createElement("TEXT");
		rootEl.appendChild(textEl);
		
		FSIterator it = jcas.getAnnotationIndex(Timex3.type).iterator();
		// if there are no timexes, just add one text node as a child. otherwise, iterate through timexes
		String docText = jcas.getDocumentText();
		if(!it.hasNext()) {
			textEl.appendChild(doc.createTextNode(docText));
		} else {
			while(it.hasNext()) {
				Timex3 t = (Timex3) it.next();
				if(t.getBegin() > offset) { 
					// add a textnode that contains the text before the timex
					textEl.appendChild(doc.createTextNode(docText.substring(offset, t.getBegin())));
				}
				// create the TIMEX3 element
				Element newTimex = doc.createElement("TIMEX3");
				// set its required attributes
				newTimex.setAttribute("tid", t.getTimexId());
				newTimex.setAttribute("type", t.getTimexType());
				newTimex.setAttribute("value", t.getTimexValue());
				// set its optional attributes
				if(!t.getTimexMod().equals(""))
					newTimex.setAttribute("mod", t.getTimexMod());
				if(!t.getTimexQuant().equals(""))
					newTimex.setAttribute("quant", t.getTimexQuant());
				if(!t.getTimexFreq().equals(""))
					newTimex.setAttribute("freq", t.getTimexFreq());
				
				// set text
				newTimex.appendChild(doc.createTextNode(t.getCoveredText()));
				
				// add the timex tag to the text tag
				textEl.appendChild(newTimex);
				
				// set cursor to the end of the timex
				offset = t.getEnd();
			}
			
			// append the rest of the document text
			if(offset < docText.length())
				textEl.appendChild(doc.createTextNode(docText.substring(offset)));
		}
		
		return doc;
	}

	
	private void writeTimeMLDocument(Document xmlDoc, String filename) {
		// create output file handle
		File outFile = new File(mOutputDir, filename+".tml"); 
		
		BufferedWriter bw = null;
		try {
			// create a buffered writer for the output file
			bw = new BufferedWriter(new FileWriter(outFile));
			
			// prepare the transformer to convert from the xml doc to output text
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			// some pretty printing
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(xmlDoc);
			StreamResult result = new StreamResult(bw);
			
			// transform
			transformer.transform(source, result);
		} catch (IOException e) { // something went wrong with the bufferedwriter
			e.printStackTrace();
			Logger.printError(component, "File "+outFile.getAbsolutePath()+" could not be written.");
		} catch (TransformerException e) { // the transformer malfunctioned (call optimus prime)
			e.printStackTrace();
			Logger.printError(component, "XML transformer could not be properly initialized.");
		} finally { // clean up for the bufferedwriter
			try {
				bw.close();
			} catch(IOException e) {
				e.printStackTrace();
				Logger.printError(component, "File "+outFile.getAbsolutePath()+" could not be closed.");
			}
		}
	}
	

}
