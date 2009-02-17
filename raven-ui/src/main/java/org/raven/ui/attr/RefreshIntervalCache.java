package org.raven.ui.attr;

import org.raven.cache.AbstractCache;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class RefreshIntervalCache extends AbstractCache<Integer, Long> 
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
	/*
	protected Logger logger = LoggerFactory.getLogger(RefreshIntervalStorage.class);
	private static final int tryRemoveOldAfter = 100;
	private static final long howOld = 1000*60*60*24*2;
	private long accessCount = 0;
	private HashMap<Integer, StorageUnit> storage = 
						new HashMap<Integer, StorageUnit>();
	
	private void put(Integer id, StorageUnit su )
	{
		if(su!=null)
			storage.put(id, su );
	}

	private void remove(int id)
	{
		storage.remove(id);
	}
	
	private void removeOld()
	{
		if(++accessCount < tryRemoveOldAfter) return;
		accessCount = 0;
		Iterator<Integer> it =  storage.keySet().iterator();
		ArrayList<Integer> killList = new ArrayList<Integer>(); 
		while(it.hasNext())
		{
			Integer i = it.next();
			if(System.currentTimeMillis() - storage.get(i).getLastAccess() > howOld)
				killList.add(i);
		}
		it =  killList.iterator();
		while(it.hasNext())
			remove(it.next());
	}
	
	public long getInterval(NodeWrapper nw)
	{
		removeOld();
		StorageUnit su = storage.get(nw.getNodeId());
		if(su!=null) 
		{
			return su.getInterval();
		}
		put(nw.getNodeId(), new StorageUnit());
		return 0;
	}

	public void setInterval(NodeWrapper nw, long val)
	{
		storage.put(nw.getNodeId(), new StorageUnit(val));
	}

	private class StorageUnit
	{
		private long interval = 0;
		private long lastAccess = 0;
		
		private StorageUnit()
		{
			setLastAccess();
		}

		private StorageUnit(long x)
		{
			this();
			interval = x;
		}
		
		
		public void setLastAccess() 
		{
			this.lastAccess = System.currentTimeMillis();
		}

		public long getLastAccess() 
		{
			return lastAccess;
		}

		public void setInterval(long interval) 
		{
			if(interval<0) interval = 0;
			setLastAccess();
			this.interval = interval;
		}

		public long getInterval() 
		{
			setLastAccess();
			return interval;
		}
		
	}
	
	*/
}
