/*
 * UimaContextImpl.java
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

import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.impl.ConfigurationManager_impl;
import org.apache.uima.resource.impl.ResourceManager_impl;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

/**
 * Implementation of UimaContext
 * 
 * @author Andreas Fay, University of Heidelberg
 * @version 1.0
 */
public class UimaContextImpl extends RootUimaContext_impl {
	private ConfigurationManager mConfigManager;

	/**
	 * Constructor
	 * 
	 * @param language
	 *            Language to process
	 * @param typeToProcess
	 *            Document type to process
	 */
	public UimaContextImpl(Language language, DocumentType typeToProcess, Boolean debug) {
		super();

		// Initialize config
		mConfigManager = new ConfigurationManager_impl();

		// Initialize context
		this.initializeRoot(null, new ResourceManager_impl(), mConfigManager);

		// Set session
		mConfigManager.setSession(this.getSession());

		// Set necessary variables
		mConfigManager.setConfigParameterValue(makeQualifiedName(Config.DEBUG), debug);
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_DATE)),
				Boolean.parseBoolean(Config.get(Config.CONSIDER_DATE)));
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_DURATION)),
				Boolean.parseBoolean(Config.get(Config.CONSIDER_DURATION)));
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_LANGUAGE)),
				language.getName());
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_SET)),
				Boolean.parseBoolean(Config.get(Config.CONSIDER_SET)));
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_TIME)),
				Boolean.parseBoolean(Config.get(Config.CONSIDER_TIME)));
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_TEMPONYM)),
				Boolean.parseBoolean(Config.get(Config.CONSIDER_TEMPONYM)));
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.get(Config.UIMAVAR_TYPETOPROCESS)),
				typeToProcess.toString());
		mConfigManager.setConfigParameterValue(
				makeQualifiedName(Config.UIMAVAR_CONVERTDURATIONS),
				new Boolean(true));

	}
	
	@Override
	public ConfigurationManager getConfigurationManager() {
		return mConfigManager;
	}

}
