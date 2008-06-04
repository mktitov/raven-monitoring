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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.raven.conf.impl.AccessControl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.constraints.ConstraintException;
import org.weda.converter.TypeConverterException;

//import org.raven.DynamicImageNode;
//import java.util.Set;
//import org.raven.tree.Node;
//import java.util.Collection;
//import org.weda.services.ClassDescriptorRegistry;
//import org.raven.tree.Tree;
//import org.raven.conf.Configurator;
//import org.raven.conf.impl.AccessControl;
//import org.raven.conf.impl.UserAcl;
//import javax.faces.context.FacesContext;
//import javax.faces.event.ActionEvent;

public class NodeWrapper extends AbstractNodeWrapper
{
	public static final String BEAN_NAME = "cNode";
    protected Logger logger = LoggerFactory.getLogger(NodeWrapper.class);	
	private List<NodeAttribute> savedAttrs = null;
	private List<Attr> editingAttrs = null;
//	private String newAttributeType = null;
//	private String newAttributeName = null;
//	private String newAttributeDsc = "";
	private NewAttribute newAttribute = null; 
	
	public NodeWrapper() 
	{
	}

	public NodeWrapper(Node node) 
	{
		super();
		this.setNode(node);
	}

	public void createNewAttribute()
	{
		if(getClassDesc()!=null && getTree()!=null)
			newAttribute = new NewAttribute(getTree().getNodeAttributesTypes(),getClassDesc());
		
	}
	
	public String nodeStart()
	{
		if( ! isAllowControl() ) return "err";
		if(!isCanNodeStart()) return "err";
		getNode().start();
		return "ok";
	}

	public String nodeStop()
	{
		if( ! isAllowControl() ) return "err";
		if(!isCanNodeStop()) return "err";
		getNode().stop();
		return "ok";
	}
	
	public List<Attr> getAttributes()
	{
		savedAttrs = getNodeAttributes();
		editingAttrs = new ArrayList<Attr>();
		for(NodeAttribute na : savedAttrs)
		{
			editingAttrs.add(new Attr(na));
		}
		return editingAttrs;
	}

//	  public String delAttr()  {   return "";  }
	
	  public String save()
	  {
		  int save = 0;
		  StringBuffer ret = new StringBuffer();
		  if( ! isAllowNodeEdit() ) return "err";
		  Iterator<NodeAttribute> itn = savedAttrs.iterator();
		  Iterator<Attr> ita = editingAttrs.iterator();
		  NodeAttribute na = null; 
		  while(itn.hasNext())
		  {
			  if(!ita.hasNext()) break;
			  save = 0;
			  na = itn.next();
			  Attr at = ita.next();
			  if(na.getId()!=at.getId()) continue;
			  String val = getNotNull(na.getValue());
			  //String dsc = getNotNull(na.getDescription());
			  if( ! val.equals(at.getValue()) ) save |=1;
			  //if( ! dsc.equals(at.getDescription()) ) save |=2;
			  if( ! na.getName().equals(at.getName()) ) save |=4;
			  if(save==0) continue;
			  try 
			  { 
				  if( (save&1) !=0 )na.setValue(at.getValue());
				  //if( (save&2) !=0 )na.s.setValue(at.getValue());
				  
				  getConfigurator().getTreeStore().saveNodeAttribute(na);
			  }
			  catch(ConstraintException e) 
			  {
				  String t = Messages.getString("org.raven.ui.messages", "attribute",new Object[] {});
				  if(ret.length()!=0) ret.append(". ");
				  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
				  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
			  }
			  catch(TypeConverterException e) 
			  {
				  String t = Messages.getString("org.raven.ui.messages", "attribute",new Object[] {});
				  if(ret.length()!=0) ret.append(". ");
				  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
				  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
			  }
			  finally 
			  { 
				  at.setValue(na.getValue());
				  at.setDescription(na.getDescription());
				  at.setName(na.getName());
			  }
		  }
		  if(ret.length()==0) return null;
		  return ret.toString();
	  }
	
	  public String cancel() //ActionEvent event
	  {
		  getAttributes();
		  return "";
	  }
	  
		public String createAttribute()
		{
			
			if( !isAllowNodeEdit() ) return "err";
		/*	
			if(newNodeType==null || newNodeType.length()==0)
			{
				logger.warn("no newNodeType");
				return "err";
			}
		*/		
			if(newAttribute.getName()==null || newAttribute.getName().length()==0)
			{
				logger.warn("no newAttributeName");
				return "err";
			}	
			NodeAttribute na = null;
			na = new NodeAttributeImpl(newAttribute.getName(),newAttribute.getAttrClass(),"",newAttribute.getDescription());
		//	na.setType(String.class);
			na.setOwner(getNode());
			getNode().addNodeAttribute(na);
		//	try { na.setValue(""); }
		//	catch(ConstraintException e) { }
			getConfigurator().getTreeStore().saveNodeAttribute(na);
			logger.warn("Added new attribute='{}' for node='{}'",na.getName(),getNode().getName());
			return "ok";
		}
	  
	  public String deleteAttrubutes(List<Attr> attrs)
	  {
		  StringBuffer ret = new StringBuffer();
		  Iterator<Attr> it = attrs.iterator();
		  while(it.hasNext())
		  {
			  Attr a = it.next();
			  NodeAttribute na = getNode().getNodeAttribute(a.getName());
			  if(na==null)
			  {
				  String t = Messages.getString("org.raven.ui.messages", "attributeNotFound",new Object[] {});
				  ret.append(t + a.getName());
				  break;
			  }	
			  if(na.getParentAttribute()!=null || na.getParameterName()!=null)
			  {
				  String t = Messages.getString("org.raven.ui.messages", "attributesCantDeleted",new Object[] {});
				  if(ret.length()==0) ret.append(t); else ret.append(", ");
				  ret.append(na.getName());
				  continue;
			  }
			  getNode().removeNodeAttribute(na.getName());
			  getConfigurator().getTreeStore().removeNodeAttribute(na.getId());
			  logger.warn("removed attrubute: {}",na.getName());
		  }
		  if(ret.length()==0) return null;
		  return ret.toString();
	  }
/*	  
	  public List<Node> getIndexedNodes()
	  {
		  ArrayList<Node> al = new ArrayList<Node>();
		  if(isGraphNode()) al.add(getNode());
		  Collection<Node> c = getNode().getSortedChildrens();
		  if(c!=null)
		  {
			  Iterator<Node> it = c.iterator();
			  while(it.hasNext())
			  {
				  Node n = it.next();
				  if(getUserAcl().getAccessForNode(n) > AccessControl.TRANSIT) al.add(n);
			  }	  
		  }
		  return al;
	  }
*/
	  public List<NodeWrapper> getChildrenList()
	  {
		  ArrayList<NodeWrapper> al = new ArrayList<NodeWrapper>();
		  List<Node> lst = getNode().getChildrenList();
		  if(lst==null || lst.size()==0) return al;
		  Iterator<Node> it = lst.iterator();
		  while(it.hasNext())
		  {
			  Node n = it.next();
			  if( (getUserAcl().getAccessForNode(n) & AccessControl.TREE_EDIT) ==0 ) continue;
			  NodeWrapper nw = new NodeWrapper(n);
			  nw.setClassDesc(getClassDesc());
			  al.add(nw);
		  }	  
		  return al;
	  }
	  
	  public List<NodeWrapper> getIndexedNodes()
	  {
		  ArrayList<NodeWrapper> al = new ArrayList<NodeWrapper>();
		  if(getNodeShowType()!=null) al.add(this);
		  logger.warn("getNodeShowType()='{}'",getNodeShowType());
		  logger.warn("NodeClass='{}'",getNode().getClass());
		  logger.warn("NodeName='{}'",getNode().getName());
		  Collection<Node> c = getNode().getSortedChildrens();
		  if(c!=null)
		  {
			  Iterator<Node> it = c.iterator();
			  while(it.hasNext())
			  {
				  Node n = it.next();
				  if(getUserAcl().getAccessForNode(n) <= AccessControl.TRANSIT) continue;
				  NodeWrapper nw = new NodeWrapper();
				  nw.setNode(n);
				  if(nw.getNodeShowType()!=null) al.add(nw);
			  }	  
		  }
		  return al;
	  }
	  
	  public String getNodeShowType()
	  {
		if(isGraphNode()) return "GraphNode";
		return null;
	  }

	public NewAttribute getNewAttribute() { return newAttribute; }
	public void setNewAttribute(NewAttribute newAttribute) { this.newAttribute = newAttribute; }

	public static String getNotNull(String x)
	{
		  if(x==null) return "";
		  return x;
	}

}
