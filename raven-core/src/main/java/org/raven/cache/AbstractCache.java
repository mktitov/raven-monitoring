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
	 * Определяет период времени, по истечении которого объект 
	 * считается устаревшим и удаляется из кеша.
	 * Если <=0 , то автоматической чистки кеша не будет.
	 */
	private long deleteAfter = 0;
	/**
	 * Время последнего поиска устаревших объектов.  
	 */
	private long lastCheck = 0;
	/**
	 * Как часто искать устаревшие объекты.
	 */
	private long checkInterval = 1000*60*5;
	/**
	 * Обновлять ли время размещения в кеше при обрашении к объекту, 
	 * который найден в кеше. 
	 */
	private boolean updateTimeOnGet = false;
	
	private boolean valueInserted = false;
		
	/**
	 * Отвечает за получение объекта, отсутствующего в кеше.
	 * @param key ключ объекта.
	 * @return найденный объект. 
	 */
	protected abstract V getValue(K key);
	
	protected abstract SK getStoreKey(K key);

	protected abstract void afterRemove(V value);
	
	/**
	 * Размещает объект в кеше.
	 * @param key ключ объекта.
	 * @param value объект.
	 * @return предыдущий объект, хранившийся в кеше и соответствующий данному ключу.
	 */
	public V put(K key,V value)
	{
		return put(key,value, 0);
	}

	/**
	 * Размещает объект в кеше.
	 * @param key ключ объекта.
	 * @param value объект.
	 * @return предыдущий объект, хранившийся в кеше и соответствующий данному ключу.
	 */
	public V put(K key,V value,long lifeTime)
	{
		if(value==null) return null;
		SK sk = getStoreKey(key);
		CacheValueContainer<V> vco = getVC(sk); 
		if(vco!=null)
			removeSK(sk);
		CacheValueContainer<V> v = new CacheValueContainer<V>(value);
		if(lifeTime>0) v.setLifeTime(lifeTime);
		map.put(sk, v);
		if(vco==null) return null;
		return vco.getValue();
	}
	
	/**
	 * @param key ключ объекта.
	 * @return CacheValueContainer, соответствующий ключу, без изменения времени занесения объекта в кеш.
	 */
	public CacheValueContainer<V> getVC(SK key)
	{
		return map.get(key);
	}	

	/**
	 * @param key ключ объекта.
	 * @return CacheValueContainer, соответствующий ключу.
	 */
	public CacheValueContainer<V> getValueContainer(SK key)
	{
		CacheValueContainer<V> vc = getVC(key);
		if( isUpdateTimeOnGet() && vc!=null ) vc.updateTime();
		return vc;
	}	

	/**
	 * Ищет объект в кеше.    
	 * @param key ключ объекта.
	 * @return найденный объект.
	 */
	public V getFromCacheOnly(K key)
	{
		checkOld();
		SK sk = getStoreKey(key);
		CacheValueContainer<V> vc = getValueContainer(sk);
		if(vc==null)
		{
	//		logger.info("not found in cache K: '{}', SK: '{}'",key,sk);
			return null;
		}
	//	logger.info("found in cache K: '{}', SK: '{}'",key,sk);
		return vc.getValue();
	}	

	/**
	 * Запрашивает объект через getValue() и кеширует.   
	 * @param key ключ объекта.
	 * @return найденный объект.
	 */
	protected V getValueX(K key)
	{
		V v = getValue(key);
		if(!isValueInserted())
		{
			put(key,v);
			return v;
		}	
		setValueInserted(false);
		return getFromCacheOnly(key);
	}
	
	/**
	 * Ищет объект в кеше. Если не находит - запрашивает 
	 * его через getValueX().   
	 * @param key ключ объекта.
	 * @return найденный объект.
	 */
	public V get(K key)
	{
		V v = getFromCacheOnly(key);
		if(v!=null) return v;
		return getValueX(key);
	}	
	
	/**
	 * Удаляет (если есть) объект из кеша, после чего запрашивает 
	 * его через getValueX().   
	 * @param key ключ объекта.
	 * @return найденный объект.
	 */
	public V reload(K key)
	{
		remove(key);
		return getValueX(key);
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
		Iterator<SK> it = map.keySet().iterator();
		ArrayList<SK> killList = new ArrayList<SK>();
		while(it.hasNext())
			killList.add(it.next());
		for(it = killList.iterator();it.hasNext();)
			removeSK(it.next());
	//	logger.info("cache cleared");
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
//		logger.info("removed SK: '{}'",storeKey);
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
			if(vc==null) continue;
			long lt = vc.getLifeTime();
			if( lt>0 )
			{
				if(ct-vc.getTime()<=lt ) continue;
			}
			else 
				if( ! isOld(vc.getTime(), ct) ) continue;
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

		public void setValueInserted(boolean valueInserted) {
			this.valueInserted = valueInserted;
		}

		public boolean isValueInserted() {
			return valueInserted;
		}
	
	
}
