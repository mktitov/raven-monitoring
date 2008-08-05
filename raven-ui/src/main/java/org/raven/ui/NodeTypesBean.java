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

import java.util.Iterator;
import org.apache.myfaces.trinidad.component.UIXTable;
import org.apache.myfaces.trinidad.context.RequestContext;

public class NodeTypesBean 
{
	private UIXTable table;

	public UIXTable getTable() { return table; }
	public void setTable(UIXTable table) { this.table = table; }

	public String cancel()
	{
	    RequestContext.getCurrentInstance().returnFromDialog(null, null);
	    return null;
	}

	public String select()
	{
	    Iterator<Object> iterator = table.getSelectedRowKeys().iterator();
	    if( !iterator.hasNext() ) return null;
	    Object rowKey = iterator.next();
	    Object oldRowKey = table.getRowKey();
	    table.setRowKey(rowKey);
	    NodeType n = (NodeType) SessionBean.getElValue("row");
	    RequestContext.getCurrentInstance().returnFromDialog(n.getClassName(), null);
	    table.setRowKey(oldRowKey);
	    return null;
	  }

}
