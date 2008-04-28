/*
 *  Copyright 2008 Sergey Pinevskiy.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.conf.impl;

import org.raven.conf.Config;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PropertiesConfig implements Config {
	public static final String CONFIG_PROPERTY_NAME = "raven.properties"; 
	public static final String CONFIG_PROPERTY_NAME_DEFAULT = "raven.cfg";
	public static final String TRUE_VALUE = "true";
	public static final String FALSE_VALUE = "false";
	public static final int MIN_INTERVAL = 1000;
	private static PropertiesConfig instance = null;
	private Properties properties = null;
	private File confFile;
	private long lastUpdate = 0;
    private long lastCheck = 0;
	private String fileName;
	private long checkInterval = 60000;
	
	protected PropertiesConfig() throws java.io.IOException 
	{
		fileName = System.getProperty(CONFIG_PROPERTY_NAME);
		if(fileName==null) fileName = CONFIG_PROPERTY_NAME_DEFAULT;
		confFile = new File(fileName);
		load();
		lastCheck = System.currentTimeMillis();
	}
	
	/**
	 * Loads data from properties file.
	 * @throws java.io.IOException
	 */
	private void load() throws java.io.IOException
	{
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream(fileName);
			Properties props = new Properties();
			props.load(propsFile);
			properties = props;
			lastUpdate = confFile.lastModified();
		} finally
		{
			if(propsFile!=null) propsFile.close();
		}
	}
	
	/**
	 * Returns a PropertiesConfig object.
	 * @throws java.io.IOException
	 */
    public static final PropertiesConfig getInstance() throws java.io.IOException 
    {
        if (instance == null) instance = new PropertiesConfig();
        return instance;
    }

    /**
     *  Если прошло более <code>checkInterval</code> миллисекунд, и 
     *  файл был изменён, то заново грузим <code>properties</code>.
     */
    private void checkUpdate()
    {
    	if( checkInterval < MIN_INTERVAL ) return;
    	long dt = System.currentTimeMillis();
    	if(dt-lastCheck < checkInterval) return;
    	if(lastUpdate != confFile.lastModified())
    		try { load(); }
    		catch(IOException e) {
    			// TODO  write to log
    		}
    	lastCheck = dt; 
    }

	public synchronized Boolean getBooleanProperty(String property, Boolean defaultValue) 
	{
		checkUpdate();
		String t = properties.getProperty(property);
		if( t==null || t.length()==0 ) return defaultValue;
		if( t.equalsIgnoreCase(TRUE_VALUE) ) return Boolean.TRUE;
		if( t.equalsIgnoreCase(FALSE_VALUE) ) return Boolean.FALSE;
		return defaultValue;
	}

	public synchronized Integer getIntegerProperty(String property, Integer defaultValue) 
	{
		checkUpdate();
		String t = properties.getProperty(property);
		if( t==null || t.length()==0 ) return defaultValue;
		Integer i;
		try { i = Integer.parseInt(t); }
		catch(java.lang.NumberFormatException e) { return defaultValue; }
		return i;
	}

	public synchronized String getStringProperty(String property, String defaultValue)
	{
		checkUpdate();
		return properties.getProperty(property, defaultValue);
	}
	
	public synchronized long getLastUpdate() { return lastUpdate; }
	public synchronized void setLastUpdate(long newValue) { lastUpdate = newValue; }

	public synchronized long getCheckInterval() { return checkInterval; }
	public synchronized void setCheckInterval(long checkInterval) { this.checkInterval = checkInterval; }
	
}
