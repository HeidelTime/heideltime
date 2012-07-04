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
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;

import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
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
	private static Logger logger;

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

		logger.log(Level.INFO, "HeidelTimeStandalone initialized with language "+this.language.toString());

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

		TreeTaggerWrapper partOfSpeechTagger = new TreeTaggerWrapper();
		partOfSpeechTagger.setLogger(logger);
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
		// create instance of the logger
		logger = Logger.getLogger("HeidelTimeStandalone");

		/////// Parse command line parameters ///////
		String docPath = null;
		for(int i = 0; i < args.length; i++) { // iterate over cli parameter tokens
			if(args[i].startsWith("-")) { // assume we found a switch
				// get the relevant enum
				CLISwitch sw = CLISwitch.getEnumFromSwitch(args[i]);
				if(sw == null) { // unsupported CLI switch
					logger.log(Level.WARNING, "Unsupported switch: "+args[i]+". Quitting.");
					System.exit(-1);
				}
				
				if(sw.getHasFollowingValue()) { // handle values for switches
					if(args.length > i+1 && !args[i+1].startsWith("-")) { // we still have an array index after this one and it's not a switch
						sw.setValue(args[++i]);
					} else { // value is missing or malformed
						logger.log(Level.WARNING, "Invalid or missing parameter after "+args[i]+". Quitting.");
						System.exit(-1);
					}
				} else { // activate the value-less switches
					sw.setValue(null);
				}
			} else { // assume we found the document's path/name
				docPath = args[i];
			}
		}
		
		
		// start off with the verbosity recognition -- lots of the other 
		// stuff can be skipped if this is set too high
		if(CLISwitch.VERBOSITY2.getIsActive()) {
			logger.setLevel(Level.ALL);
			logger.log(Level.INFO, "Verbosity: '-vv'; Logging level set to ALL.");
		} else if(CLISwitch.VERBOSITY.getIsActive()) {
			logger.setLevel(Level.INFO);
			logger.log(Level.INFO, "Verbosity: '-v'; Logging level set to INFO and above.");
		} else {
			logger.setLevel(Level.WARNING);
			logger.log(Level.INFO, "Verbosity -v/-vv NOT FOUND OR RECOGNIZED; Logging level set to WARNING and above.");
		}

		// Check input encoding
		String encodingType = null;
		if(CLISwitch.ENCODING.getIsActive()) {
			encodingType = CLISwitch.ENCODING.getValue().toString();
			logger.log(Level.INFO, "Encoding '-e': "+encodingType);
		} else {
			// Encoding type not found
			encodingType = CLISwitch.ENCODING.getValue().toString();
			logger.log(Level.INFO, "Encoding '-e': NOT FOUND OR RECOGNIZED; set to 'UTF-8'");
		}
		
		// Check output format
		OutputType outputType = null;
		if(CLISwitch.OUTPUTTYPE.getIsActive()) {
			outputType = OutputType.valueOf(CLISwitch.OUTPUTTYPE.getValue().toString().toUpperCase());
			logger.log(Level.INFO, "Output '-o': "+outputType.toString().toUpperCase());
		} else {
			// Output type not found
			outputType = (OutputType) CLISwitch.OUTPUTTYPE.getValue();
			logger.log(Level.INFO, "Output '-o': NOT FOUND OR RECOGNIZED; set to "+outputType.toString().toUpperCase());
		}
		
		// Check language
		Language language = null;
		if(CLISwitch.LANGUAGE.getIsActive()) {
			language = Language.valueOf(CLISwitch.LANGUAGE.getValue().toString().toUpperCase());
			logger.log(Level.INFO, "Language '-l': "+language.toString().toUpperCase());
		} else {
			// Language not found
			language = (Language) CLISwitch.LANGUAGE.getValue();
			logger.log(Level.INFO, "Language '-l': NOT FOUND OR RECOGNIZED; set to "+language.toString().toUpperCase());
		}

		// Check type
		DocumentType type = null;
		if(CLISwitch.DOCTYPE.getIsActive()) {
			type = DocumentType.valueOf(CLISwitch.DOCTYPE.getValue().toString().toUpperCase());
			logger.log(Level.INFO, "Type '-t': "+type.toString().toUpperCase());
		} else {
			// Type not found
			type = (DocumentType) CLISwitch.DOCTYPE.getValue();
			logger.log(Level.INFO, "Type '-t': NOT FOUND OR RECOGNIZED; set to "+type.toString().toUpperCase());
		}

		// Check document creation time
		Date dct = null;
		if(CLISwitch.DCT.getIsActive()) {
			try {
				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				dct = formatter.parse(CLISwitch.DCT.getValue().toString());
				logger.log(Level.INFO, "Document Creation Time '-dct': "+dct.toString());
			} catch (Exception e) {
				// DCT was not parseable
				logger.log(Level.WARNING, "Document Creation Time '-dct': NOT RECOGNIZED. Quitting.");
				System.exit(-1);
			}
		} else {
			if ((type == DocumentType.NEWS) || (type == DocumentType.COLLOQUIAL)) {
				// Dct needed
				dct = (Date) CLISwitch.DCT.getValue();
				logger.log(Level.INFO, "Document Creation Time '-dct': NOT FOUND; set to local date ("
						+ dct.toString() + ").");
			} else {
				logger.log(Level.INFO, "Document Creation Time '-dct': NOT FOUND; skipping.");
			}
		}
		
		// Handle locale switch
		String locale = (String) CLISwitch.LOCALE.getValue();
		Locale myLocale = null;
		if(CLISwitch.LOCALE.getIsActive()) {
			// check if the requested locale is available
			for(Locale l : Locale.getAvailableLocales()) {
				if(l.toString().toLowerCase().equals(locale.toLowerCase()))
					myLocale = l;
			}
			
			try {
				Locale.setDefault(myLocale); // try to set the locale
				logger.log(Level.INFO, "Locale '-locale': "+myLocale.toString());
			} catch(Exception e) { // if the above fails, spit out error message and available locales
				logger.log(Level.WARNING, "Supplied locale parameter couldn't be resolved to a working locale. Try one of these:");
				logger.log(Level.WARNING, Arrays.asList(Locale.getAvailableLocales()).toString()); // list available locales
				System.exit(-1);
			}
		} else {
			// no -locale parameter supplied: just show default locale
			logger.log(Level.INFO, "Locale '-locale': NOT FOUND, set to environment locale: "+Locale.getDefault().toString());
		}
		
		// Read configuration from file
		String configPath = CLISwitch.CONFIGFILE.getValue().toString();
		try {
			logger.log(Level.INFO, "Configuration path '-c': "+configPath);
			
			InputStream configStream = new FileInputStream(configPath);
			
			Properties props = new Properties();
			props.load(configStream);

			Config.setProps(props);
			
			configStream.close();
			logger.log(Level.FINE, "Config initialized");
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.WARNING, "Config could not be initialized! Please supply the -c switch or "
					+ "put a config.props into this directory.");
			System.exit(-1);
		}
		
		// make sure we have a document path
		if (docPath == null) {
			logger.log(Level.WARNING, "No input file given; aborting.");
			System.exit(-1);
		}
		
		

		// Run HeidelTime
		try {
			
			logger.log(Level.INFO, "Reading document using charset: " + encodingType);
			
			BufferedReader fileReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(docPath), encodingType));
			
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = fileReader.readLine()) != null) {
				sb.append(System.getProperty("line.separator")+line);
			}
			String input = sb.toString();
			// should not be necessary, but without this, it's not running on Windows (?)
			input = new String(input.getBytes("UTF-8"), "UTF-8");
			
			HeidelTimeStandalone standalone = new HeidelTimeStandalone(language, type, outputType);
			String out = new String();
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

}