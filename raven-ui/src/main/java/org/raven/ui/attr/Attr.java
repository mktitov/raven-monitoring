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

package org.raven.ui.attr;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.apache.tapestry.ioc.Registry;
import org.raven.ui.SessionBean;
import org.raven.ui.node.NodeWrapper;
import org.raven.ui.node.SelectNodeBean;
import org.raven.ui.util.RavenRegistry;
import org.raven.tree.InvalidPathException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import javax.faces.event.ActionEvent;
//import org.apache.myfaces.trinidad.model.UploadedFile;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.raven.tree.DataFile;
import org.raven.tree.impl.DataFileValueHandlerFactory;
//import org.apache.myfaces.trinidad.component.core.nav.CoreCommandButton;

public class Attr implements Comparable<Attr>  
{
    @Service
    private static Tree tree;
    
    private Logger logger = LoggerFactory.getLogger(Attr.class);
    private NodeAttribute attribute;
	private int id;
	private String name;
	private String oldValue;
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
	private List<Attr> children = new ArrayList<Attr>();
	private boolean hasChildren = false;
	private boolean templateExpression = false;
	private boolean refreshAttribute = false;
    private UploadedFile file;

	@SuppressWarnings("unchecked")
	public Attr(NodeAttribute na) throws TooManyReferenceValuesException
	{
		attribute = na;
		this.name = na.getName();
		try { value = na.getValue(); }
		catch(Throwable e) {value = "";}
		oldValue = value;
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
            valueHandlerTypes = new ArrayList<SelectItem>(refValues.size()+1);
            valueHandlerTypes.add(new SelectItem(null, ""));
            for (ReferenceValue refValue: refValues)
                valueHandlerTypes.add(
                    new SelectItem(refValue.getValue(), refValue.getValueAsString()));
        } 
        else valueHandlerTypes = Collections.EMPTY_LIST;
        templateExpression = na.isTemplateExpression();
        
	}

	public Attr(NodeAttribute na, boolean ra) throws TooManyReferenceValuesException
	{
		this(na);
		setRefreshAttribute(ra);
	}
	
	public boolean isValueChanged()
	{
		if(oldValue==null) 
			if(value!=null) return true;
				else return false;
		return !oldValue.equals(value);
	}

    public boolean isFileAttribute()
    {
        return DataFile.class.isAssignableFrom(attribute.getType())
                && DataFileValueHandlerFactory.TYPE.equals(attribute.getValueHandlerType());
    }

    public UploadedFile getFile()
    {
        return file;
    }

    public void setFile(UploadedFile file)
    {
    	logger.info("setFile: '{}'",file.getName());
        this.file = file;
    }

    public void fileUploaded(ValueChangeEvent event)
    {
    	UploadedFile f = (UploadedFile) event.getNewValue();
    	setFile(f);
    }
    
    public boolean isEnableEditValue()
    {
    	if(expressionSupported || isFileAttribute() || getSelectItems() != null ) return false;
    	return true; 
    }

	private boolean isExpressionNoChoice()
	{
		if(expressionSupported && (getSelectItems() == null) ) 
				return true;
		return false;
	}

	public boolean isEnableExpressionChoice()
	{
		if(expressionSupported && (getSelectItems() != null) ) 
				return true;
		return false;
	}
	
	public boolean isEnableEditExpression()
	{
		if(isFileAttribute()) return false;
		if(isExpressionNoChoice() && !isSubTypeNodeReference() ) 
				return true;
		return false;
	}

	public boolean isEnableSelectNodeDialog()
	{
		if(isExpressionNoChoice() && isSubTypeNodeReference() ) 
				return true;
		return false;
	}
	
	public void setNode(ActionEvent event)
	{
		if( !isEnableSelectNodeDialog() ) return;
		if(expression==null || expression.length()==0) return;
		SelectNodeBean nb = (SelectNodeBean) SessionBean.getElValue(SelectNodeBean.BEAN_NAME);
		Tree tree = SessionBean.getTree();
		Node n = null;
		try {
			n = tree.getNode(expression);
		} catch (InvalidPathException e) {
			logger.error("on set current node in dialog : ",e);
			return;
		}
		nb.setDstNode(n);
	}
	
	public void selectNodeHandleReturn(ReturnEvent event)
	{
		expression = (String) event.getReturnValue();
		//SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
		//sb.reloadBothFrames();
	}
	
	public boolean isSubTypeNodeReference()
	{
		if("NodeReference".equals(valueHandlerType)) return true;
		return false;
	}
	
	public void applySubType(ValueChangeEvent vce)
	{
		valueHandlerType = (String) vce.getNewValue();
		NodeWrapper nw = SessionBean.getNodeWrapper();
		if(isRefreshAttribute()) 
			nw.saveRefreshAttributes();
		else
		{
			nw.saveWithoutWrite();
			nw.loadAttributes();
		}
	}
	
	public void applyTemplateExpression(ValueChangeEvent vce)
	{
		Boolean b = (Boolean) vce.getNewValue();
		templateExpression = b.booleanValue();
		NodeWrapper nw = SessionBean.getNodeWrapper();
		if(isRefreshAttribute())
			nw.saveRefreshAttributes();
		else
		{
			nw.saveWithoutWrite();
			nw.loadAttributes();
		}	
	}
	
	public void addChild(Attr a) 
	{ 
		children.add(a);
		setHasChildren(true);
	}
	
	public List<Attr> getChildAttributes()
	{
		return children;
	}
	
	public void findChildren(List<Attr> l)
	{
		for(Attr a: l)
		{
			String parent = a.getAttribute().getParentAttribute();
			if(parent!=null && this.name.equals(parent)) addChild(a);
		}
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

    public String getDisplayName() { return attribute.getDisplayName(); }

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

	public List<SelectItem> getSelectItems()  
    { 
		List<ReferenceValue> lst = null;
		try {
			lst = attribute.getReferenceValues();
		} catch (TooManyReferenceValuesException e) {
			logger.error("xmm: ",e);
		}
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

    public String getValueHandlerType() { return valueHandlerType; }
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

	public boolean isHasChildren() 
	{ 
		return hasChildren; 
	}
	
	public void setHasChildren(boolean hasChildren) 
	{ 
		this.hasChildren = hasChildren; 
	}

	public boolean isTemplateExpression() 
	{ 
		return templateExpression; 
	}
	
	public void setTemplateExpression(boolean templateExpression) 
	{ 
		this.templateExpression = templateExpression; 
	}

	public void setRefreshAttribute(boolean refreshAttribute) {
		this.refreshAttribute = refreshAttribute;
	}

	public boolean isRefreshAttribute() {
		return refreshAttribute;
	}

	public int compareTo(Attr o) {
		return getName().compareTo(o.getName());
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getOldValue() {
		return oldValue;
	}
	
}
