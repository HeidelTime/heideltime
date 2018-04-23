package de.unihd.dbs.heideltime.standalone.components.impl;

import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.impl.ConfigurationManager_impl;
import org.apache.uima.resource.impl.ResourceManager_impl;

/**
 * UIMA context with manually set configuration manager.
 */
public class StandaloneConfigContext extends RootUimaContext_impl {
	private ConfigurationManager mConfigManager;

	public StandaloneConfigContext() {
		super();
		mConfigManager = new ConfigurationManager_impl();
		this.initializeRoot(null, new ResourceManager_impl(), mConfigManager);
		mConfigManager.setSession(this.getSession());
	}

	public void setConfigParameterValue(String key, Object val) {
		mConfigManager.setConfigParameterValue(makeQualifiedName(key), val);
	}
	
	@Override
	public ConfigurationManager getConfigurationManager() {
		return mConfigManager;
	}
}
