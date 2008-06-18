/*
 *  Copyright 2008 Sergey Pinevskiy.
 * 
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

package org.raven.ui;

//import java.util.ArrayList;
//import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;

//import javax.faces.event.ActionEvent;
//import org.raven.tree.NodeAttribute;
//import org.apache.myfaces.trinidad.component.UIXCollection;
//import org.apache.myfaces.trinidad.model.RowKeySetImpl;

public class AttributesTableBean 
{
	public static final String BEAN_NAME = "cNodeAttrs";
	private UIComponent table = null;
//	private List<Attr> selected;
	private CoreMessage message = null;

	@SuppressWarnings("unchecked")
	public AttributesTableBean() 
	{ 
		//selected = Collections.EMPTY_LIST;
	}
		  
	@SuppressWarnings("unchecked")
	public String deleteAttributes() //ActionEvent action
	{
		UIXTable tbl = (UIXTable) table;
		RowKeySet state;
		state = tbl.getSelectedRowKeys();
		Iterator it = state.iterator();
		Object oldKey = tbl.getRowKey();
		StringBuffer retb = new StringBuffer();
		NodeWrapper nw = (NodeWrapper) SessionBean.getElValue(NodeWrapper.BEAN_NAME);
		Attr attr = null;
		boolean deleted = false;
		while (it.hasNext())
		{
			tbl.setRowKey(it.next());
			attr = (Attr)tbl.getRowData();
			int t = nw.deleteAttrubute(attr);
			if(t==0) { it.remove(); deleted = true; }
				else 
					if(t==-1) 
					{
						retb.append(Messages.getUiMessage("attributeNotFound"));
						break;
					}
					else
					{
						if(retb.length()==0) retb.append(Messages.getUiMessage("attributesCantDeleted"));
						retb.append(" "+attr.getName());
					}
		}
		tbl.setRowKey(oldKey);
		
		if(attr == null) retb.append(Messages.getUiMessage("noSelectedValues"));
		else if(deleted)
		{
			state.clear();
			nw.afterDeleteAttrubutes();
		}	
		//tbl.setSelectedRowKeys(state);
		

		if(message!=null) message.setMessage(retb.toString());
		//else message.setMessage("");
		return "";
	}
		  
	public String tryDelete(List<Attr> attrs)
	{
		FacesContext context = FacesContext.getCurrentInstance();
		NodeWrapper nw = (NodeWrapper) context.getELContext().getELResolver().getValue(context.getELContext(), null, NodeWrapper.BEAN_NAME);
		String ret = nw.deleteAttrubutes(attrs);
/*		if(ret==null)
		{
			UIXTable tbl = (UIXTable) table;
			tbl.setSelectedRowKeys(null);
		}
*/		return ret;
	}
	
	public String saveAttributes()
	{
		FacesContext context = FacesContext.getCurrentInstance();
		NodeWrapper nw = (NodeWrapper) context.getELContext().getELResolver().getValue(context.getELContext(), null, NodeWrapper.BEAN_NAME);
		String ret = nw.save();
		if(ret!=null && message!=null) message.setMessage(ret);
		else message.setMessage("");
		return "";
	}
	
	public CoreMessage getMessage() { return message; }
	public void setMessage(CoreMessage message) 
	{ 
		this.message = message; 
		//this.message.setMessage("");
	}
	//public void setMessageText(String message) { this.message.setMessage(message); }
	
	public UIComponent getTable() { return table; }
	public void setTable(UIComponent table) { this.table = table; }
		
}
