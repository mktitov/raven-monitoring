package org.raven.ui;

import org.raven.cache.SimpleAbstractCache;

public class ResourcesCache extends SimpleAbstractCache<String, IconResource> 
{
	private static ResourcesCache instance = null;
	
	private ResourcesCache()
	{
		super();
		setDeleteAfter(0);
	}
	
	public static synchronized ResourcesCache getInstance()
	{
		if(instance==null) instance = new ResourcesCache();
		return instance;
	}

	protected IconResource getValue(String key) 
	{
		return null;
	}
	
	public IconResource get(String key)
	{
		return getFromCacheOnly(key);
	}

}
