package org.raven.ui;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;

public class NewAttribute 
{
    protected Logger logger = LoggerFactory.getLogger(NewAttribute.class);	
	
	private String name="";
	@SuppressWarnings("unchecked")
	private Class attrClass;
	private String description="";
	//private String refPath = "";
	private List<SelectItem> selectItems = null;
	
	@SuppressWarnings("unchecked")
	public NewAttribute(Class[] cls,ClassDescriptorRegistry classDesc)
	{
		selectItems = new ArrayList<SelectItem>();
		setAttrClass(cls[0]); 
		for(Class val: cls)
		{
			//logger.info("Attribute class:{}",val);
			//logger.info("Attribute class displName:{}",classDesc.getClassDescriptor(val).getDisplayName());
			//String cname = val.getCanonicalName();
			String sname = classDesc.getClassDescriptor(val).getDisplayName();
			selectItems.add( new SelectItem(val,sname) );
		}	
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	@SuppressWarnings("unchecked")
	public Class getAttrClass() { return attrClass; }
	@SuppressWarnings("unchecked")
	public void setAttrClass(Class attrClass) { this.attrClass = attrClass; }

	public List<SelectItem> getSelectItems() { 	return selectItems; }
	public void setSelectItems(List<SelectItem> selectItems) { this.selectItems = selectItems; }

//	public String getRefPath() { return refPath; }
//	public void setRefPath(String refPath) { this.refPath = refPath; }

}
