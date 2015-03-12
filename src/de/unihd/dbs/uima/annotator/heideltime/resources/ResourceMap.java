package de.unihd.dbs.uima.annotator.heideltime.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;

public class ResourceMap implements Map<String, String> {
	HashMap<String, File> outerFiles = new HashMap<String, File>();
	HashMap<String, String> innerFiles = new HashMap<String, String>();

	@Override
	public void clear() {
		outerFiles.clear();
		innerFiles.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return outerFiles.containsKey(key) || innerFiles.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return outerFiles.containsValue(value) || innerFiles.containsValue(value);
	}

	@Override
	@Deprecated
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return null;
	}

	@Override
	@Deprecated
	public String get(Object key) {
		return null;
	}
	
	public InputStream getInputStream(String key) {
		InputStream is = null;
		
		if(outerFiles.containsKey(key)) {
			try {
				is = new FileInputStream(outerFiles.get(key));
			} catch(FileNotFoundException e) {
				Logger.printError("File " + key + " disppeared while loading resources.");
			}
		} else if(innerFiles.containsKey(key)) {
			is = this.getClass().getClassLoader().getResourceAsStream(key);
		}
		
		return is;
	}

	@Override
	public boolean isEmpty() {
		return outerFiles.isEmpty() && innerFiles.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		Set<String> set = outerFiles.keySet();
		set.addAll(innerFiles.keySet());
		
		return set;
	}

	@Override
	@Deprecated
	public String put(String key, String value) {
		return null;
	}
	
	public String putInnerFile(String key, String value) {
		return innerFiles.put(key, value);
	}
	
	public File putOuterFile(String key, File value) {
		return outerFiles.put(key, value);
	}

	@Override
	@Deprecated
	public void putAll(Map<? extends String, ? extends String> m) {
		
	}

	@Override
	public String remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<String> values() {
		// TODO Auto-generated method stub
		return null;
	}

}
