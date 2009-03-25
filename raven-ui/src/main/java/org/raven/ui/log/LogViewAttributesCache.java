package org.raven.ui.log;

import org.raven.cache.SimpleAbstractCache;

public class LogViewAttributesCache extends SimpleAbstractCache<Integer,LogViewAttributes> 
{
	
	public LogViewAttributesCache()
	{
		super();
		setDeleteAfter(1000*60*60*12);
		setCheckInterval(1000*60*5);
		setUpdateTimeOnGet(true);
	}
	
	protected Integer getStoreKey(Integer key) 
	{
		return key;
	}
	
	protected LogViewAttributes getValue(Integer key) 
	{
		return new LogViewAttributes();
	}

}
