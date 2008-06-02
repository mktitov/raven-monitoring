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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
//import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.UIXCollection;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.component.core.output.CoreMessage;
//import org.raven.tree.NodeAttribute;

public class AttributesTableBean 
{
		  private UIComponent table = null;
		  private List<Attr> selected;
		  private CoreMessage message = null;

		  @SuppressWarnings("unchecked")
		  public AttributesTableBean() { selected = Collections.EMPTY_LIST; }
		  
		  @SuppressWarnings("unchecked")
		  public String deleteAttributes() //ActionEvent action
		  {
		    UIXCollection tbl = (UIXCollection) table;
		    final RowKeySet state;
		    state = ((UIXTable) tbl).getSelectedRowKeys();
		    Iterator it = state.iterator();
		    Object oldKey = tbl.getRowKey();
		    selected = new ArrayList<Attr>();
		    while (it.hasNext())
		    {
		      tbl.setRowKey(it.next());
		      selected.add((Attr)tbl.getRowData());
		    }
		    tbl.setRowKey(oldKey);
		    if(selected.size()==0)
		    {
		    	String t = Messages.getString("org.raven.ui.messages", "noSelectedValues",new Object[] {});
		    	message.setMessage(t);
		    	return "";
		    }	
		    String ret = tryDelete(selected);
		    if(ret!=null && message!=null) message.setMessage(ret);
		    else message.setMessage("");
		    return "";
		  }
		  
		  public String tryDelete(List<Attr> attrs)
		  {
		    FacesContext context = FacesContext.getCurrentInstance();
		    NodeWrapper nw = (NodeWrapper) context.getELContext().getELResolver().getValue(context.getELContext(), null, NodeWrapper.BEAN_NAME);
		    return nw.deleteAttrubutes(attrs);
		  }

		  public CoreMessage getMessage() { return message; }
		  public void setMessage(CoreMessage message) { this.message = message; }

		  public UIComponent getTable() { return table; }
		  public void setTable(UIComponent table) { this.table = table; }
	
}
