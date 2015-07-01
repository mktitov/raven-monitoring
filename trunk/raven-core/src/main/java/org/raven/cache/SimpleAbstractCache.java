package org.raven.cache;

public abstract class SimpleAbstractCache<K,V> extends AbstractCache<K,V,K>
{
	protected K getStoreKey(K key)
	{
		return key;
	}
	
	protected void afterRemove(V value) 
	{
		
	}
	
}
