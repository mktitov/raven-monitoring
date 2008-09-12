package org.raven.ui;

import java.util.HashMap;

public class ViewableObjectsStorage 
{
	HashMap<String, ViewableObjectWrapper> storage = new HashMap<String, ViewableObjectWrapper>();
	
	public void put(ViewableObjectWrapper vo)
	{
		if(vo!=null)
			storage.put(vo.toString(), vo);
	}
	
	public ViewableObjectWrapper get(String id)
	{
		return storage.get(id);
	}

	public void remove(String id)
	{
		storage.remove(id);
	}
	
/*
	private class StorageUnit
	{
		long fd;
		ViewableObjectWrapper vobject;

		private StorageUnit(ViewableObjectWrapper o)
		{
			fd = System.currentTimeMillis();
			vobject = o;
		}
	}
*/
}
