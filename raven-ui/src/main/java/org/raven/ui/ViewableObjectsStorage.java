package org.raven.ui;

import java.util.HashMap;

public class ViewableObjectsStorage 
{
	HashMap<String, StorageUnit> storage = new HashMap<String, StorageUnit>();
	
	public void put(ViewableObjectWrapper vo)
	{
		if(vo!=null)
			storage.put(vo.toString(), new StorageUnit(vo));
	}
	
	public ViewableObjectWrapper get(String id)
	{
		StorageUnit su = storage.remove(id);
		if(su!=null)
		{
			return su.vobject;
		}	
		return null;
	}
	
	public String getId()
	{
		return toString();
	}
	
	// ???????
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

}
