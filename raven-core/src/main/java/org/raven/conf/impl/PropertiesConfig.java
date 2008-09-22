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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesConfig implements Config 
{
    protected Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);	
	public static final String CONFIG_PROPERTY_NAME = "raven.properties"; 
	public static final String CONFIG_PROPERTY_NAME_DEFAULT = "c:/raven.cfg";
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
	
	protected PropertiesConfig() throws IOException 
	{
		fileName = System.getProperty(CONFIG_PROPERTY_NAME);
		if(fileName==null) fileName = CONFIG_PROPERTY_NAME_DEFAULT;
		confFile = new File(fileName);
		load();
		lastCheck = System.currentTimeMillis();
	}

    public synchronized Properties getProperties()
    {
        return properties;
    }
	
	/**
	 * Loads data from properties file.
	 * @throws java.io.IOException
	 */
	private void load() throws IOException
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
			if(propsFile!=null)
			{
				propsFile.close();
				logger.info("config loaded");
			}	
		}
	}
	
	/**
	 * Returns a PropertiesConfig object.
	 * @throws java.io.IOException
	 */
    public static final synchronized PropertiesConfig getInstance() throws IOException 
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
    		try 
    		{ 
    			logger.info("reloading config");
    			load(); 
    		}
    		catch(IOException e) { logger.error("on reload config : ",e); }
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
	
	public synchronized long getLastUpdate() 
	{ 
		checkUpdate();
		return lastUpdate; 
	}

	public synchronized long getCheckInterval() { return checkInterval; }
	public synchronized void setCheckInterval(long checkInterval) { this.checkInterval = checkInterval; }
	
}
