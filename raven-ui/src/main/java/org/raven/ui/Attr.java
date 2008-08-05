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
import java.util.Collections;
import javax.faces.model.SelectItem;

import org.apache.tapestry.ioc.Registry;
import org.raven.RavenRegistry;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
//import org.raven.tree.Tree;

public class Attr 
{
    @Service
    private static Tree tree;
    
    protected Logger logger = LoggerFactory.getLogger(Attr.class);
    private NodeAttribute attribute;
	private int id;
	private String name;
	private String value;
    private String valueHandlerType;
	private String description;
	private String classDisplayName;
	private List<SelectItem> selectItems = null;
    private List<SelectItem> valueHandlerTypes = null;
	private boolean allowDelete = false;
	private boolean edit = false;
	private boolean expressionSupported = false;
	private String expression = "";

	@SuppressWarnings("unchecked")
	public Attr(NodeAttribute na) throws TooManyReferenceValuesException
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
        
		expressionSupported = na.isExpression();
		if(expressionSupported) expression = na.getRawValue();  
        
        valueHandlerType = na.getValueHandlerType();
        List<ReferenceValue> refValues = tree.getAttributeValueHandlerTypes(na);
        if (refValues!=null)
        {
            valueHandlerTypes = new ArrayList<SelectItem>(refValues.size());
            for (ReferenceValue refValue: refValues)
                valueHandlerTypes.add(
                    new SelectItem(refValue.getValue(), refValue.getValueAsString()));
        } 
        else
        {
            valueHandlerTypes = Collections.EMPTY_LIST;
        }
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

	public List<SelectItem> getSelectItems() throws TooManyReferenceValuesException 
    { 
		List<ReferenceValue> lst = attribute.getReferenceValues();
		if(lst!=null)
		{
            selectItems = new ArrayList<SelectItem>();
            for(ReferenceValue val: lst)
                selectItems.add( new SelectItem(val.getValue(), val.getValueAsString()));
		}	
        
        return selectItems; 
    }
	public void setSelectItems(List<SelectItem> selectItems) throws TooManyReferenceValuesException 
    {
    }

	public String getClassDisplayName() { return classDisplayName; }
	public void setClassDisplayName(String type) { this.classDisplayName = type; }

	public boolean isAllowDelete() { return allowDelete; }
	public void setAllowDelete(boolean allowDelete) { this.allowDelete = allowDelete; }

	public boolean isEdit() { return edit; }
	public void setEdit(boolean edit) { this.edit = edit; }

	public boolean isExpressionSupported() { return attribute.isExpression(); }
//	public void setReference(boolean reference) { this.expression = reference; }

	public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

	public NodeAttribute getAttribute() { return attribute; }
	public void setAttribute(NodeAttribute attribute) { this.attribute = attribute; }

    public String getValueHandlerType()
    {
        return valueHandlerType;
    }

    public void setValueHandlerType(String valueHandlerType)
    {        
        if (valueHandlerType!=null && valueHandlerType.length()==0)
            this.valueHandlerType = null;
        else
            this.valueHandlerType = valueHandlerType;
    }

    public List<SelectItem> getValueHandlerTypes()
    {
        return valueHandlerTypes;
    }

    public void setValueHandlerTypes(List<SelectItem> valueHandlerTypes)
    {
        this.valueHandlerTypes = valueHandlerTypes;
    }
    
}
