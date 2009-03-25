package org.raven.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public abstract class AbstractCache<K,V,SK> 
{
//	private static Logger logger = LoggerFactory.getLogger(AbstractCache.class);
	
	private ConcurrentMap<SK, CacheValueContainer<V>> map = new ConcurrentHashMap<SK, CacheValueContainer<V>>();
	/**
	 * ���������� ������ �������, �� ��������� �������� ������ 
	 * ��������� ���������� � ��������� �� ����.
	 * ���� <=0 , �� �������������� ������ ���� �� �����.
	 */
	private long deleteAfter = 0;
	/**
	 * ����� ���������� ������ ���������� ��������.  
	 */
	private long lastCheck = 0;
	/**
	 * ��� ����� ������ ���������� �������.
	 */
	private long checkInterval = 1000*60*5;
	/**
	 * ��������� �� ����� ���������� � ���� ��� ��������� � �������, 
	 * ������� ������ � ����. 
	 */
	private boolean updateTimeOnGet = false;
		
	/**
	 * �������� �� ��������� �������, �������������� � ����.
	 * @param key ���� �������.
	 * @return ��������� ������. 
	 */
	protected abstract V getValue(K key);
	
	protected abstract SK getStoreKey(K key);

	protected abstract void afterRemove(V value);
	
	/**
	 * ��������� ������ � ����.
	 * @param key ���� �������.
	 * @param value ������.
	 * @return ���������� ������, ����������� � ���� � ��������������� ������� �����.
	 */
	public V put(K key,V value)
	{
		if(value==null) return null;
		SK sk = getStoreKey(key);
		CacheValueContainer<V> vco = getVC(sk); 
		if(vco!=null)
			removeSK(sk);
		map.put(sk, new CacheValueContainer<V>(value));
		if(vco==null) return null;
		return vco.getValue();
	}
		
	/**
	 * @param key ���� �������.
	 * @return CacheValueContainer, ��������������� �����, ��� ��������� ������� ��������� ������� � ���.
	 */
	public CacheValueContainer<V> getVC(SK key)
	{
		return map.get(key);
	}	

	/**
	 * @param key ���� �������.
	 * @return CacheValueContainer, ��������������� �����.
	 */
	public CacheValueContainer<V> getValueContainer(SK key)
	{
		CacheValueContainer<V> vc = getVC(key);
		if( isUpdateTimeOnGet() && vc!=null ) vc.updateTime();
		return vc;
	}	

	/**
	 * ���� ������ � ����.    
	 * @param key ���� �������.
	 * @return ��������� ������.
	 */
	public V getFromCacheOnly(K key)
	{
		checkOld();
		CacheValueContainer<V> vc = getValueContainer(getStoreKey(key));
		if(vc==null) return null;
		return vc.getValue();
	}	
	
	/**
	 * ���� ������ � ����. ���� �� ������� - ����������� 
	 * ��� ����� getValue() � ��������.   
	 * @param key ���� �������.
	 * @return ��������� ������.
	 */
	public V get(K key)
	{
		V v = getFromCacheOnly(key);
		if(v!=null) return v;
		v = getValue(key);
		put(key,v);
		return v;
	}	
	
	/**
	 * ������� (���� ����) ������ �� ����, ����� ���� ����������� 
	 * ��� ����� getValue() � ��������.   
	 * @param key ���� �������.
	 * @return ��������� ������.
	 */
	public V reload(K key)
	{
		remove(key);
		V v = getValue(key);
		put(key,v);
		return v;
	}	

	public V get(K key, boolean reload)
	{
		if(reload) return reload(key);
		return get(key);	
	}	
	
	/**
	 * Clears chache.
	 */
	public void clear()
	{
		map.clear();
	}

	/**
	 * Removes object from chache by the storeKey.
	 */
	protected void removeSK(SK storeKey)
	{
		CacheValueContainer<V> vc = map.remove(storeKey);
		if(vc==null) return;
		V v = vc.getValue();
		if(v==null) return;
		afterRemove(v);
	}

	
	/**
	 * Removes object from chache by the key.
	 */
	public void remove(K key)
	{
		removeSK(getStoreKey(key));
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
	
	/**
	 * Checks all cache value containers and removes outdated objects.
	 */
	public void checkOld()
	{
		long ct = System.currentTimeMillis();
		if(deleteAfter<=0 || ct-getLastCheck()< getCheckInterval()) return;
		setLastCheck(ct);
		Iterator<SK> it = map.keySet().iterator();
		ArrayList<SK> killList = new ArrayList<SK>();
		while(it.hasNext())
		{
			SK key = it.next();
			CacheValueContainer<V> vc = getVC(key);
			if( vc!=null && isOld(vc.getTime(), ct) )
				killList.add(key);
		}
		for(it = killList.iterator();it.hasNext();)
			removeSK(it.next());

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
