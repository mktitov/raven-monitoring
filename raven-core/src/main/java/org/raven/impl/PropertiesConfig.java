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

package org.raven.impl;

import org.raven.Config;
import java.util.Properties;
import java.io.FileInputStream;

public class PropertiesConfig implements Config {
	public static final String CONFIG_PROPERTY_NAME = "raven.properties"; 
	public static final String CONFIG_PROPERTY_NAME_DEFAULT = "raven.cfg";
	public static final String TRUE_VALUE = "true";
	public static final String FALSE_VALUE = "false";
	private static PropertiesConfig instance = null;
	private Properties properties;
	
	protected PropertiesConfig() throws java.io.IOException 
	{
		String fileName = System.getProperty(CONFIG_PROPERTY_NAME);
		if(fileName==null) fileName = CONFIG_PROPERTY_NAME_DEFAULT;
		Properties properties = new Properties();
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream(fileName);
			properties.load(propsFile);
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


	public Boolean getBooleanProperty(String property, Boolean defaultValue) 
	{
		String t = properties.getProperty(property);
		if( t==null || t.length()==0 ) return defaultValue;
		if( t.equalsIgnoreCase(TRUE_VALUE) ) return Boolean.TRUE;
		if( t.equalsIgnoreCase(FALSE_VALUE) ) return Boolean.FALSE;
		return defaultValue;
	}

	public Integer getIntegerProperty(String property, Integer defaultValue) 
	{
		String t = properties.getProperty(property);
		if( t==null || t.length()==0 ) return defaultValue;
		Integer i;
		try { i = Integer.parseInt(t); }
		catch(java.lang.NumberFormatException e) { return defaultValue; }
		return i;
	}

	public String getStringProperty(String property, String defaultValue)
	{
		return properties.getProperty(property, defaultValue);
	}

}
