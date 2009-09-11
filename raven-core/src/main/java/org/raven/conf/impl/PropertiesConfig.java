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
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.SystemNode;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;

public class PropertiesConfig implements Config 
{
    protected Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);	
	public static final String CONFIG_PROPERTY_NAME = "raven.properties"; 
	public static final String CONFIG_PROPERTY_NAME_DEFAULT = "c:/raven.cfg";
	public static final String TRUE_VALUE = "true";
	public static final String FALSE_VALUE = "false";
	public static final int MIN_INTERVAL = 2000;
	public static final String ANPATH = Node.NODE_SEPARATOR+SystemNode.NAME+
							Node.NODE_SEPARATOR+AuthorizationNode.NODE_NAME;

	private static PropertiesConfig instance = null;
	private Properties properties = null;
	private Properties fileProperties = null;
	private Properties treeProperties = null;
	private File confFile;
	private long lastFileUpdate = 0;
    private long lastFileCheck = 0;
	private String fileName;
	/**
	 * Interval for check configuration change (milliseconds).
	 */
	private long checkInterval = 60000;

	@Service
	private static Tree tree = null;
	
	private long lastTreeUpdate = 0;
	private long lastTreeCheck = 0;
//	boolean treeEnabled = false;
	
	protected PropertiesConfig() throws IOException 
	{
		fileName = System.getProperty(CONFIG_PROPERTY_NAME);
		if(fileName==null) fileName = CONFIG_PROPERTY_NAME_DEFAULT;
		confFile = new File(fileName);
		loadCfg();
		lastFileCheck = System.currentTimeMillis();
	}

    public synchronized Properties getProperties()
    {
        return properties;
    }

	/**
	 * Loads data from xml properties file.
	 * @throws java.io.IOException
	 * @return <code>true</code> if config loaded
	 */
	private boolean loadXML() throws IOException
	{
		boolean ok = false;
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream(fileName);
			Properties props = new Properties();
			props.loadFromXML(propsFile);
			fileProperties = props;
			lastFileUpdate = confFile.lastModified();
			ok = true;
		}
		catch(InvalidPropertiesFormatException e) {}
		finally
		{
			if(propsFile!=null) 
			{
				propsFile.close(); 
				if(ok) logger.info("xml config loaded");
			}	
		}	
		return ok;
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
			fileProperties = props;
			lastFileUpdate = confFile.lastModified();
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
	 * Loads data from tree properties.
	 */
	private void loadTreeAC(String s) 
	{
		boolean ok = false;
		Reader r = null;
		try {
			r = new StringReader(s);
			Properties props = new Properties();
			props.load(r);
			treeProperties = props;
			setLastTreeCheck(getLastTreeUpdate());
			ok = true;
		} catch (Exception e) {
			logger.info("on tree config loading :",e);
		} finally
		{
			if(ok == true)
				logger.info("tree config loaded");
		}
	}
	
	private String getTreeAC()
	{
		AuthorizationNode n = null;
		try 
		{ 
			n = (AuthorizationNode) tree.getNode(ANPATH);
			return n.getAuthorizationData();
		}
		catch (Exception e) 
			{logger.error("on load node:"+ANPATH,e); }
		return "";
	}
	
	private void loadCfg() throws IOException
	{
		if(!loadXML()) 
			load();
		Properties props = new Properties();
		if(fileProperties!=null)
			props.putAll(fileProperties);
		if( getLastTreeUpdate() > getLastTreeCheck() )
		{
			loadTreeAC(getTreeAC());
			if(treeProperties!=null)
				props.putAll(treeProperties);
		}
		properties = props;
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

    private boolean isNeedCheckFile()
    {
    	long dt = System.currentTimeMillis();
    	if(dt-lastFileCheck < checkInterval) return false;
    	lastFileCheck = dt;
    	if(lastFileUpdate != confFile.lastModified()) return true;
    	return false;
    }

    private boolean isNeedCheckTree()
    {
    	long u = getLastTreeUpdate();
    	long c = getLastTreeCheck();
    	if(u > c)
    	{	
    		logger.info("TREE: CHECK:{} UPDATE:{}",c,u);
    		return true;
    	}	
    	return false;
    }
    
    /**
     *  Если прошло более <code>checkInterval</code> миллисекунд, и 
     *  файл был изменён, то заново грузим <code>properties</code>.
     */
    private void checkUpdate()
    {
    	if( checkInterval < MIN_INTERVAL ) return;
    	if(isNeedCheckFile() || isNeedCheckTree())
    		try 
    		{ 
    			logger.info("reloading config");
    			loadCfg(); 
    		}
    		catch(IOException e) { 
    			logger.error("on reload config : ",e); 
    		}
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
		return Math.max(lastFileUpdate,lastTreeUpdate); 
	}

	public synchronized long getCheckInterval() { return checkInterval; }
	public synchronized void setCheckInterval(long checkInterval) { this.checkInterval = checkInterval; }

	public synchronized void setAuthorizationTreeUpdated() 
	{
		setLastTreeUpdate(System.currentTimeMillis());
	}

	protected synchronized void setLastTreeUpdate(long l) 
	{
		logger.info("SET LAST_TREE_UPD");
		this.lastTreeUpdate = l;
	}

	protected synchronized long getLastTreeUpdate() {
		return lastTreeUpdate;
	}

	public synchronized void setLastTreeCheck(long l) 
	{
		logger.info("SET AUTH_TREE_CHECK");		
		this.lastTreeCheck = l;
	}

	public synchronized long getLastTreeCheck() {
		return lastTreeCheck;
	}
/*
	public synchronized boolean isTreeEnabled() {
		return treeEnabled;
	}

	public synchronized void setTreeEnabled(boolean x) {
		//if(treeEnabled!=x)
		setAuthorizationTreeUpdated();
		treeEnabled=x;
	}
*/	
}
