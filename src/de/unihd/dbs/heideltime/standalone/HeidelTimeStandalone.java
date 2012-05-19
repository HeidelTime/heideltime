/*
 * HeidelTimeStandalone.java
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;



import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.heideltime.standalone.components.impl.JCasFactoryImpl;
import de.unihd.dbs.heideltime.standalone.components.impl.TimeMLResultFormatter;
import de.unihd.dbs.heideltime.standalone.components.impl.TreeTaggerWrapper;
import de.unihd.dbs.heideltime.standalone.components.impl.UimaContextImpl;
import de.unihd.dbs.heideltime.standalone.components.impl.XMIResultFormatter;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.types.heideltime.Dct;

/**
 * Execution class for UIMA-Component HeidelTime. Singleton-Pattern
 * 
 * @author Andreas Fay, Jannik Strötgen, Heidelberg Universtiy
 * @version 1.01
 */
public class HeidelTimeStandalone {

	/**
	 * Used document type
	 */
	private DocumentType documentType;

	/**
	 * HeidelTime instance
	 */
	private HeidelTime heidelTime;

	/**
	 * Type system description of HeidelTime
	 */
	private JCasFactory jcasFactory;

	/**
	 * Used language
	 */
	private Language language;

	/**
	 * Logging engine
	 */
	private Logger logger;

	/**
	 * Constructor
	 * 
	 * @param language
	 * @param typeToProcess
	 * @param outputType
	 */
	public HeidelTimeStandalone(Language language, DocumentType typeToProcess, OutputType outputType) {
		this.language = language;
		this.documentType = typeToProcess;

		// Initialize logger -------------------
		logger = Logger.getLogger("HeidelTimeStandalone");
		logger.log(Level.INFO, "HeidelTimeStandalone initialized with language "+this.language.toString());

		// Initialize config -------------------
		logger.log(Level.FINE, "Initializing config...");
		try {
//			if (!Config.isInitialized()) {
				InputStream configStream = this.getClass().getClassLoader()
						.getResourceAsStream("config.props");

				Properties props = new Properties();
				props.load(configStream);

				Config.setProps(props);
				logger.log(Level.INFO, "Config initialized");
//			} else {
//				logger.log(Level.INFO, "Config already initialized");
//			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Config could not be initialized");
		}

		// Initialize HeidelTime ---------------
		logger.log(Level.FINE, "Initializing HeidelTime(" + language.toString()
				+ ")...");
		try {
			heidelTime = new HeidelTime();
			heidelTime.initialize(new UimaContextImpl(language, typeToProcess));
			logger.log(Level.INFO, "HeidelTime initialized");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "HeidelTime could not be initialized");
		}

		// Initialize JCas factory -------------
		logger.log(Level.FINE, "Initializing JCas factory...");
		try {
			TypeSystemDescription[] descriptions = new TypeSystemDescription[] {
					UIMAFramework
							.getXMLParser()
							.parseTypeSystemDescription(
									new XMLInputSource(
											this.getClass()
													.getClassLoader()
													.getResource(
															Config.get(Config.TYPESYSTEMHOME)))),
					UIMAFramework
							.getXMLParser()
							.parseTypeSystemDescription(
									new XMLInputSource(
											this.getClass()
													.getClassLoader()
													.getResource(
															Config.get(Config.TYPESYSTEMHOME_DKPRO)))) };
			jcasFactory = new JCasFactoryImpl(descriptions);
			logger.log(Level.INFO, "JCas factory initialized");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "JCas factory could not be initialized");
		}
	}

	/**
	 * Provides jcas object with document creation time if
	 * <code>documentCreationTime</code> is not null.
	 * 
	 * @param jcas
	 * @param documentCreationTime
	 * @throws DocumentCreationTimeMissingException
	 *             If document creation time is missing when processing a
	 *             document of type {@link DocumentType#NEWS}.
	 */
	private void provideDocumentCreationTime(JCas jcas,
			Date documentCreationTime)
			throws DocumentCreationTimeMissingException {
		if (documentCreationTime == null) {
			// Document creation time is missing
			if (documentType == DocumentType.NEWS) {
				// But should be provided in case of news-document
				throw new DocumentCreationTimeMissingException();
			}
			if (documentType == DocumentType.COLLOQUIAL) {
				// But should be provided in case of colloquial-document
				throw new DocumentCreationTimeMissingException();
			}
		} else {
			// Document creation time provided
			// Translate it to expected string format
			SimpleDateFormat dateFormatter = new SimpleDateFormat(
					"yyyy.MM.dd'T'HH:mm");
			String formattedDCT = dateFormatter.format(documentCreationTime);

			// Create dct object for jcas
			Dct dct = new Dct(jcas);
			dct.setValue(formattedDCT);

			dct.addToIndexes();
		}
	}

	/**
	 * Establishes preconditions for jcas to be processed by HeidelTime
	 * 
	 * @param jcas
	 */
	private void establishHeidelTimePreconditions(JCas jcas) {
		// Token information & sentence structure
		establishPartOfSpeechInformation(jcas);
	}

	/**
	 * Establishes part of speech information for cas object.
	 * 
	 * @param jcas
	 */
	private void establishPartOfSpeechInformation(JCas jcas) {
		logger.log(Level.FINEST, "Establishing part of speech information...");

		PartOfSpeechTagger partOfSpeechTagger = new TreeTaggerWrapper();
		partOfSpeechTagger.process(jcas, language);

		logger.log(Level.FINEST, "Part of speech information established");
	}

	/**
	 * Processes document with HeidelTime
	 * 
	 * @param document
	 * @return Annotated document
	 * @throws DocumentCreationTimeMissingException
	 *             If document creation time is missing when processing a
	 *             document of type {@link DocumentType#NEWS}. Use
	 *             {@link #process(String, Date)} instead to provide document
	 *             creation time!
	 */
	public String process(String document, ResultFormatter resultFormatter)
			throws DocumentCreationTimeMissingException {
		return process(document, null, resultFormatter);
	}

	/**
	 * Processes document with HeidelTime
	 * 
	 * @param document
	 * @param documentCreationTime
	 *            Date when document was created - especially important if
	 *            document is of type {@link DocumentType#NEWS}
	 * @return Annotated document
	 * @throws DocumentCreationTimeMissingException
	 *             If document creation time is missing when processing a
	 *             document of type {@link DocumentType#NEWS}
	 */
	public String process(String document, Date documentCreationTime, ResultFormatter resultFormatter)
			throws DocumentCreationTimeMissingException {
		logger.log(Level.INFO, "Processing started");

		// Generate jcas object ----------
		logger.log(Level.FINE, "Generate CAS object");
		JCas jcas = null;
		try {
			jcas = jcasFactory.createJCas();
			jcas.setDocumentText(document);
			logger.log(Level.FINE, "CAS object generated");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Cas object could not be generated");
		}

		// Process jcas object -----------
		try {
			logger.log(Level.FINER, "Establishing preconditions...");
			provideDocumentCreationTime(jcas, documentCreationTime);
			establishHeidelTimePreconditions(jcas);
			logger.log(Level.FINER, "Preconditions established");

			heidelTime.process(jcas);

			logger.log(Level.INFO, "Processing finished");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Processing aborted due to errors");
		}

		// Process results ---------------
		logger.log(Level.FINE, "Formatting result...");
		// PrintAnnotations.printAnnotations(jcas.getCas(), System.out);
		String result = null;
		try {
			result = resultFormatter.format(jcas);
			logger.log(Level.INFO, "Result formatted");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Result could not be formatted");
		}

		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Parse command line parameters
		System.err.println("Parameters recognized:");

		// Check input encoding
		System.err.print("Encoding '-e': ");
		String encodingType = "UTF-8";
		try{
			encodingType = getCommandLineParameter(args, "-e");
			System.err.println(encodingType.toString());
		} catch (Exception e){
			// Encoding type not found
			System.err.println("NOT FOUND OR RECOGNIZED; set to 'UTF-8'");
			encodingType = "UTF-8";
		}
		
		// Check output format
		System.err.print("Output '-o': ");
		OutputType outputType = null;
		try{
			outputType = OutputType.valueOf(getCommandLineParameter(args, "-o"));
			System.err.println(outputType.toString());
		} catch (Exception e){
			// Output type not found
			System.err.println("NOT FOUND OR RECOGNIZED; set to 'TIMEML'");
			outputType = OutputType.TIMEML;
		}
		
		// Check language
		System.err.print("Language '-l': ");
		Language language = null;
		try {
			language = Language.valueOf(getCommandLineParameter(args, "-l"));
			System.err.println(language.toString());
		} catch (Exception e) {
			// Language not found
			System.err.println("NOT FOUND OR RECOGNIZED; set to 'ENGLISH'");
			language = Language.ENGLISH;
		}

		// Check type
		System.err.print("Type '-t': ");
		DocumentType type = null;
		try {
			type = DocumentType.valueOf(getCommandLineParameter(args, "-t"));
			System.err.println(type.toString());
		} catch (Exception e) {
			// Type not found
			System.err.println("NOT FOUND OR RECOGNIZED; set to 'NARRATIVES'");
			type = DocumentType.NARRATIVES;
		}

		// Check document creation time
		System.err.print("Document Creation Time '-dct': ");
		Date dct = null;
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			dct = formatter.parse(getCommandLineParameter(args, "-dct"));
			System.err.println(dct.toString());
		} catch (Exception e) {
			// Dct not found
			if ((type == DocumentType.NEWS) ||
					(type == DocumentType.COLLOQUIAL)) {
				// Dct needed
				dct = new Date();
				System.out.println("NOT FOUND OR RECOGNIZED; set to today ("
						+ dct.toString() + ")");
			} else {
				System.err.println("NOT FOUND OR RECOGNIZED; skipped");
			}
		}

		// Run HeidelTime
		try {
			// Get document
			String docPath = null;
			if (args.length > 0 && args.length % 2 == 1) {
				if (!args[0].startsWith("-")) {
					// First parameter considered file
					docPath = args[0];
				} else {
					// Last parameter considered file
					docPath = args[args.length - 1];
				}
			}
			
			if (docPath == null) {
				System.err.println("No input file given; aborting.");
				return;
			}
			
			System.err.println("Reading document using charset: " + encodingType);
			
			BufferedReader fileReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(docPath), encodingType));
			
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = fileReader.readLine()) != null) {
				sb.append("\n"); // ADDED to keep the newlines?
				sb.append(line);
			}
			String input = sb.toString();
			// should not be necessary, but without this, it's not running on Windows (?)
			input = new String(input.getBytes("UTF-8"), "UTF-8");
			
			HeidelTimeStandalone standalone = new HeidelTimeStandalone(
					language, type, outputType);
			String out = "";
			if (outputType.toString().equals("xmi")){
				ResultFormatter resultFormatter = new XMIResultFormatter();
				out = standalone.process(input, dct, resultFormatter);	
			}
			else{
				ResultFormatter resultFormatter = new TimeMLResultFormatter();
				out = standalone.process(input, dct, resultFormatter);
			}
			

			// Print output always as UTF-8
			PrintWriter pwOut = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
			pwOut.println(out);
			pwOut.close();
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtains parameter from command line
	 * 
	 * @param args
	 *            Command line arguments
	 * @param name
	 *            Name of argument
	 * @return
	 */
	private static String getCommandLineParameter(String[] args, String name) {
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals(name)) {
				// Parameter found
				String value = args[i + 1].toUpperCase();

				if (value.startsWith("-")) {
					// Invalid value
					return null;
				} else {
					return value;
				}
			}
		}

		// Parameter not found
		return null;
	}

}
