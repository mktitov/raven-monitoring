package org.raven.ui.attr;

import org.raven.cache.SimpleAbstractCache;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class RefreshIntervalCache extends SimpleAbstractCache<Integer, Long> 
{
//	protected static Logger logger = LoggerFactory.getLogger(RefreshIntervalStorage.class);
	public RefreshIntervalCache()
	{
		super();
		setCheckInterval(1000*60*3);
		setDeleteAfter(1000*60*60*6);
		setUpdateTimeOnGet(true);
	}
	
	protected Long getValue(Integer key) 
	{
		return new Long(0);
	}
	
	public Long put(Integer key, Long value)
	{
		if(value<0) value = 0L;
		return super.put(key, value);
	}
}
