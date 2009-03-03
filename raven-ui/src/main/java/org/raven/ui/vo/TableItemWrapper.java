package org.raven.ui.vo;

import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

public class TableItemWrapper 
{
	Object item;
	
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
		if(item==null) return false;
		if (item instanceof ViewableObject) 
		{
			ViewableObject x = (ViewableObject) item;
			if(x.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE)) 
				return true;
		}
		return false;
	}

}
