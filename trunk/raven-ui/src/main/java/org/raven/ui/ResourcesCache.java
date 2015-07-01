package org.raven.ui;

import java.io.InputStream;

import org.raven.cache.SimpleAbstractCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesCache extends SimpleAbstractCache<String, IconResource> 
{
    private static final Logger logger = LoggerFactory.getLogger(ResourcesCache.class);
	public static final String[] iconExt = {"gif","png"};
	private static ResourcesCache instance = null;
	
	private ResourcesCache()
	{
		super();
		setDeleteAfter(0);
	}
	
	public static synchronized ResourcesCache getInstance()
	{
		if(instance==null)
			instance = new ResourcesCache();
		return instance;
	}

	private static byte[] readBytes(InputStream is)
	{
	    byte res[] = new byte[0];
	    byte buf[] = new byte[65535];
	    try {
			for(int i = 0; (i = is.read(buf)) > 0;)  
			 {
			        int tmp = res.length + i;
			        byte[] tempBuf = new byte[tmp];
			        System.arraycopy(res, 0, tempBuf, 0, res.length);
			        System.arraycopy(buf, 0, tempBuf, res.length, i);
			        res = tempBuf;
			      }
		} catch (Exception e) {
			logger.error("on readBytes:",e);
			res = null;
		}
		finally 
		{
			if(is!=null)
				try {is.close();} catch(Exception e) {}
		}
		return res;
	}	
	
	/*
	public static String classNameToResource(String cName)
	{
		if(cName==null) return null;
		return cName.replaceAll("\\.", "/");
	}	
*/
	@SuppressWarnings("unchecked")
	public static String classToResource(Class cls)
	{
		return cls.getName().replaceAll("\\.", "/");
	}	
	
	@SuppressWarnings("unchecked")
	public static IconResource getIconResourceForClass(Class cls)
	{
		String x = classToResource(cls);
		ClassLoader cLoader = cls.getClassLoader();
		InputStream is = null;
		for(String a : iconExt)
		{
			is = cLoader.getResourceAsStream(x+"."+a);
			if(is!=null)
				return new IconResource(a,readBytes(is),x);
		}
		return new IconResource(null,null,x);
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
