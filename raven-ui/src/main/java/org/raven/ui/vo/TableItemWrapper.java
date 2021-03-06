/* 
 *  Copyright 2008 Sergey Pinevskiy.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.raven.ui.vo;

import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
*
* @author Sergey Pinevskiy
*/
public class TableItemWrapper 
{
	public static final int NONE = 0;
	public static final int VO_TABLE = 1;
	public static final int VO_IMAGE = 2;
	public static final int VO_NODE = 3;
	public static final int VO_TEXT = 4;
	public static final int VO_ACTION = 5;
	public static final int VO_OTHER = 6;
	public static final int OTHER = 7;
    private final boolean selectedRow;
	private Object item;
	private ViewableObjectWrapper wrapper = null;
//	private boolean wrpInited = false;
	
	public TableItemWrapper(Object x, boolean selectedRow)
	{
        this.selectedRow = selectedRow;
		item = x;
		if (x instanceof ViewableObject) {
			wrapper = new ViewableObjectWrapper((ViewableObject) x);
		}
	}
	
	public String getString()
	{
		if(item==null) return "";
		return item.toString();
	}
	
	
	public Object getItem()
	{
		return item;
	}

    public String getStyle()
    {
        return selectedRow? "background-color: #FFF297" : null;
    }

	public ViewableObjectWrapper getItemWrapper()
	{
		return wrapper;
	}
	
	public String getIdVO()
	{
		return wrapper.getIdVO(); 
	}
	
	public boolean isTable()
	{
		if(getItemType()==VO_TABLE) return true;
		return false; 
	}

	public boolean isNode()
	{
		if(getItemType()==VO_NODE) return true;
		return false; 
	}
	
	public boolean isOther()
	{
		if(getItemType()==OTHER) return true;
		return false; 
	}

	public boolean isText()
	{
		if(getItemType()==VO_TEXT) return true;
		return false; 
	}
	
	public int getItemType()
	{
		if(item==null) return NONE;
		if (item instanceof ViewableObject) 
		{
			ViewableObject x = (ViewableObject) item;
			String mt = x.getMimeType();
			if(Viewable.RAVEN_ACTION_MIMETYPE.equals(mt)) 
				return VO_ACTION;
			if(Viewable.RAVEN_TABLE_MIMETYPE.equals(mt)) 
				return VO_TABLE;
			if(Viewable.RAVEN_NODE_MIMETYPE.equals(mt)) 
				return VO_NODE;
			if(Viewable.RAVEN_TEXT_MIMETYPE.equals(mt)) 
				return VO_TEXT;
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

	public boolean isAction()
	{
		if(getItemType()==VO_ACTION) return true;
		return false; 
	}
	
	
/*	
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
*/	
}
