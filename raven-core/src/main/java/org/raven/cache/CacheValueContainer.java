package org.raven.cache;

public class CacheValueContainer<V> 
{
	/**
	 * Кешируемый объект.
	 */
	private V value;
	/**
	 * Время размешения объекта в кеше.
	 */
	private Long time;
		
	public CacheValueContainer(V value)
	{
		setValue(value);
	}
		
	public void updateTime()
	{
		setTime(System.currentTimeMillis());
	}

	private void setValue(V value) 
	{
		this.value = value;
		updateTime();
	}

	public V getValue() {
		return value;
	}

	private void setTime(Long time) {
		this.time = time;
	}

	public Long getTime() {
		return time;
	}
	
}
