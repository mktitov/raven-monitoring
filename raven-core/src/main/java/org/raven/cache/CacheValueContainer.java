package org.raven.cache;

public class CacheValueContainer<V> 
{
	/**
	 * ���������� ������.
	 */
	private V value;
	/**
	 * ����� ���������� ������� � ����.
	 */
	private Long time;
	/**
	 * ���������� ����� ����� ����������� ������� � ����.
	 * ���� <=0 , �� ������������, ������������ �������� <code>AbstractCache.deleteAfter</code>. 
	 */
	private Long lifeTime = 0L;
		
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

	public void setLifeTime(Long lifeTime) {
		this.lifeTime = lifeTime;
	}

	public Long getLifeTime() {
		return lifeTime;
	}
	
}
