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

public class RowExplorer 
{
	private NodeWrapper current = null;
	private String rowName = "row";
//	private String id;
	
	public String getRefresh()
	{
	    current = (NodeWrapper) SessionBean.getElValue(rowName);
		return current.getNode().getPath(); 
	}
	
//	public String toString() { 	return getRefresh(); }
//	public Object getRow() { return current; }

}
