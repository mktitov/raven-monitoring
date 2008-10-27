package org.raven.ui.log;

import org.raven.cache.AbstractCache;

public class LogViewAttributesCache extends AbstractCache<Integer,LogViewAttributes> 
{
	
	public LogViewAttributesCache()
	{
		super();
		setDeleteAfter(1000*60*60*12);
		setCheckInterval(1000*60*5);
		setUpdateTimeOnGet(true);
	}
	
	protected LogViewAttributes getValue(Integer key) 
	{
		return new LogViewAttributes();
	}

}
