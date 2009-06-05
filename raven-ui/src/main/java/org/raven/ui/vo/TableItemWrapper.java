package org.raven.ui.vo;

import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

public class TableItemWrapper 
{
	private static final int NONE = 0;
	private static final int VO_TABLE = 1;
	private static final int VO_IMAGE = 2;
	private static final int VO_OTHER = 3;
	private static final int OTHER = 4;
	private Object item;
	private ViewableObjectWrapper wrapper = null;
	private boolean wrpInited = false;
	
	public TableItemWrapper(Object x)
	{
		item = x;
	}
	
	public String getString()
	{
		return item.toString();
	}
	
	public Object getItem()
	{
		return item;
	}
	
	public boolean isTable()
	{
		if(getItemType()==VO_TABLE) return true;
		return false; 
	}

	public boolean isOther()
	{
		if(getItemType()==OTHER) return true;
		return false; 
	}

	public int getItemType()
	{
		if(item==null) return NONE;
		if (item instanceof ViewableObject) 
		{
			ViewableObject x = (ViewableObject) item;
			if(x.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE)) 
				return VO_TABLE;
			//if(x.getMimeType().startsWith(ViewableObjectWrapper.IMAGE)) 
			//	return VO_IMAGE;
			return VO_OTHER;
		}
		return OTHER;
	}
	
	public boolean isFile()
	{
		if(getItemType()==VO_OTHER) return true;
		return false; 
	}
	
	public ViewableObjectWrapper getWrapper()
	{
		if(!wrpInited)
		{
			if (item!=null && (item instanceof ViewableObject)) 
				wrapper = new ViewableObjectWrapper((ViewableObject) item);
			wrpInited = true;
		}
		return wrapper;
	}
	
}
