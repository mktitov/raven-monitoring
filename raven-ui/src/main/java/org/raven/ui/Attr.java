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

import java.util.List;
import java.util.ArrayList;
import javax.faces.model.SelectItem;

import org.apache.tapestry.ioc.Registry;
import org.raven.RavenRegistry;
import org.raven.tree.NodeAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;
//import org.raven.tree.Tree;

public class Attr 
{
    protected Logger logger = LoggerFactory.getLogger(Attr.class);
    private NodeAttribute attribute;
	private int id;
	private String name;
	private String value;
	private String description;
	private String classDisplayName;
	private List<SelectItem> selectItems = null;
	private boolean allowDelete = false;
	private boolean edit = false;
	private boolean reference = false;
	private String refPath = "";

	public Attr(NodeAttribute na)
	{
		attribute = na;
		this.name = na.getName();
		this.value = na.getValue();
		this.description = na.getDescription();
		this.id = na.getId();
		Registry registry = RavenRegistry.getRegistry();
		ClassDescriptorRegistry classDsc = registry.getService(ClassDescriptorRegistry.class);
		classDisplayName = classDsc.getClassDescriptor(na.getType()).getDisplayName();
		if(na.getParentAttribute()==null && na.getParameterName()==null) allowDelete = true;
		List<String> lst = na.getReferenceValues();
		reference = na.isAttributeReference();
		if(reference) refPath = na.getRawValue();  
		if(lst==null)
		{
			logger.warn("NodeAttribute '{}' has not reference values",name);
			return;
		}	
		logger.warn("NodeAttribute '{}' has reference values",name);
		selectItems = new ArrayList<SelectItem>();
		for(String val: lst)
			selectItems.add( new SelectItem(val,val) );
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getValue() { return value; }
	public void setValue(String value) 
	{ 
		if(value!=null && value.length()==0) this.value = null;
			else this.value = value; 
	}

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public int getId() { return id; }
	public void setId(int id) { this.id = id; }

	public List<SelectItem> getSelectItems() { return selectItems; }
	public void setSelectItems(List<SelectItem> selectItems) { this.selectItems = selectItems; }

	public String getClassDisplayName() { return classDisplayName; }
	public void setClassDisplayName(String type) { this.classDisplayName = type; }

	public boolean isAllowDelete() { return allowDelete; }
	public void setAllowDelete(boolean allowDelete) { this.allowDelete = allowDelete; }

	public boolean isEdit() { return edit; }
	public void setEdit(boolean edit) { this.edit = edit; }

	public boolean isReference() { return reference; }
	public void setReference(boolean reference) { this.reference = reference; }

	public String getRefPath() { return refPath; }
	public void setRefPath(String refPath) { this.refPath = refPath; }

	public NodeAttribute getAttribute() { return attribute; }
	public void setAttribute(NodeAttribute attribute) { this.attribute = attribute; }
}
