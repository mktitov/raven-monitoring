package org.raven.cache;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public abstract class AbstractCache<K,V> 
{
	private ConcurrentMap<K, CacheValueContainer<V>> map = new ConcurrentHashMap<K, CacheValueContainer<V>>();
	private long deleteAfter = 0;
	private long lastCheck = 0;
	private long checkInterval = 1000*60*5;
	private boolean updateTimeOnGet = false;
		
	protected abstract V getValue(K key);
		
	public V put(K key,V value)
	{
		if(value==null) return null;
		CacheValueContainer<V> vc = map.put( key, new CacheValueContainer<V>(value));
			if(vc==null) return null;
			return vc.getValue();
	}
		
	public CacheValueContainer<V> getVC(K key)
	{
		return map.get(key);
	}	

	public CacheValueContainer<V> getValueContainer(K key)
	{
		CacheValueContainer<V> vc = getVC(key);
		if( isUpdateTimeOnGet() && vc!=null ) vc.updateTime();
		return vc;
	}	
		
	public V get(K key)
	{
		checkOld();
		CacheValueContainer<V> vc = map.get(key);
		if(vc==null)
		{
			V v = getValue(key);
			put(key,v);
			return v;
		}
		if(isUpdateTimeOnGet()) vc.updateTime();
		return vc.getValue();
	}	
		
	public void clear()
	{
		map.clear();
	}

	public void remove(K key)
	{
		map.remove(key);
	}

	protected boolean isOld(long time, long cur)
	{
		if(deleteAfter>0 && cur-time > deleteAfter)
				return true;
		return false;
	}
		
	protected boolean isOld(long time)
	{
		return isOld(time,System.currentTimeMillis());
	}
		
	public void checkOld()
	{
		long ct = System.currentTimeMillis();
		if(deleteAfter<=0 || ct-getLastCheck()< getCheckInterval()) return;
		setLastCheck(ct);
		Iterator<K> it = map.keySet().iterator();
		while(it.hasNext())
		{
			K key = it.next();
			CacheValueContainer<V> vc = getVC(key);
			if( vc!=null && isOld(vc.getTime(), ct) )
				it.remove();
		}
	}
		
	public void setDeleteAfter(long deleteAfter) 
	{
		if(deleteAfter < 0) deleteAfter = 0;
		this.deleteAfter = deleteAfter;
		}
	public long getDeleteAfter() {
		return deleteAfter;
	}

	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}
	public long getCheckInterval() {
		return checkInterval;
	}

		private long getLastCheck() {
			return lastCheck;
		}
		private void setLastCheck(long lastCheck) {
			this.lastCheck = lastCheck;
		}
		
		public void setUpdateTimeOnGet(boolean updateTimeOnGet) {
			this.updateTimeOnGet = updateTimeOnGet;
		}
		public boolean isUpdateTimeOnGet() {
			return updateTimeOnGet;
		}
	
	
}
