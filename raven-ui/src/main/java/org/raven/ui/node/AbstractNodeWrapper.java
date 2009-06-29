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

package org.raven.ui.node;

//import org.raven.template.TemplateWizard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.raven.conf.Configurator;
import org.raven.conf.impl.AccessControl;
import org.raven.conf.impl.UserAcl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;

public abstract class AbstractNodeWrapper
{
    protected Logger logger = LoggerFactory.getLogger(AbstractNodeWrapper.class);	
	private UserAcl userAcl = null;
	private Tree tree = null;
	private Node node = null;
	private ClassDescriptorRegistry classDesc = null;
//	private List<NodeAttribute> savedAttrs = null;
//	private List<Attr> editingAttrs = null;
	private Configurator configurator;
	
	public boolean isGraphNode()
	{
		if (node instanceof org.raven.DynamicImageNode) return true;
		return false;
	}
	
	public boolean isAllowCreateSubNode() 
	{
		return node.isContainer() && isAllowTreeEdit(); 
	}
	
	public boolean isNodeStopped()
	{
		if(getNode().getStatus()==Node.Status.INITIALIZED) return true;
		return false;
	}

	public boolean isNodeStarted()
	{
		if(getNode().getStatus()==Node.Status.STARTED) return true;
		return false;
	}

	public boolean isNodeCreated()
	{
		if(getNode().getStatus()==Node.Status.CREATED) return true;
		return false;
	}
/*
	public String getStatusImage()
	{
		if(getNode().getStatus()==Node.Status.INITIALIZED) return "Inited";
		if(getNode().getStatus()==Node.Status.CREATED) return "Created";
		if(getNode().getStatus()==Node.Status.STARTED) return "Started";
		return "unknown";
	}
*/	
	public boolean isAllowControl()
	{
		if( (userAcl.getAccessForNode(node) & AccessControl.CONTROL) ==0) return false;
		return true;
	}

	public boolean isAllowNodeRename()
	{
		if(getNode().getParent()==null) return false;
		return isAllowTreeEdit();
	}
	
	public boolean isAllowTreeEdit()
	{
		if( (userAcl.getAccessForNode(node) & AccessControl.TREE_EDIT) ==0 ) return false;
		return true;
	}

	public boolean isAllowNodeEdit()
	{
		if( (userAcl.getAccessForNode(node) & AccessControl.WRITE) ==0 ) return false;
		return true;
	}

	public boolean isAllowNodeRead()
	{
		int acc = userAcl.getAccessForNode(node);
		if( ( acc & AccessControl.READ) ==0 ) return false;
		return true;
	}

	public boolean isAnyAccess()
	{
		int acc = userAcl.getAccessForNode(node);
		if( acc > AccessControl.NONE ) return true;
		return false;
	}
	
	public boolean isCanNodeStop()
	{
		if( ! isAllowControl() ) return false;
		if(isNodeCreated()) return false;
		return isNodeStarted();
	}

	public boolean isCanNodeStart()
	{
		if( ! isAllowControl() ) return false;
		if(isNodeCreated()) return false;
		return isNodeStopped();
	}
	
	public String getNodeStatusText()
	{
		if(isNodeCreated()) return "Created";
		if(isNodeStarted()) return "Started";
		if(isNodeStopped()) return "Stopped";
		return "Unknown";
	}
	
	@SuppressWarnings("unchecked")
	public List<NodeType> getValidSubNodeTypesList()
	{
		List<Class> cls = getNode().getChildNodeTypes();
		ArrayList<NodeType> al = new ArrayList<NodeType>();
		if(cls==null) return al;
		for(Class c: cls)
		{
			String simpleName = classDesc.getClassDescriptor(c).getType().getSimpleName();
			String dsc = classDesc.getClassDescriptor(c).getDescription();
			String sdsc = classDesc.getClassDescriptor(c).getDisplayName();
			al.add(new NodeType(c.getCanonicalName(),simpleName, dsc, sdsc));
		}
		Collections.sort(al, new NodeTypeComparator());
		return al;
	}

	public List<NodeType> getValidSubNodeTemplatesList()
	{
		List<Node> templates = tree.getTempltateNodes();
		ArrayList<NodeType> al = new ArrayList<NodeType>();
		if(templates==null) return al;
		for(Node n: templates)
		{
			//String dispName = classDesc.getClassDescriptor(n.getClass()).getDisplayName();
			//String dsc = classDesc.getClassDescriptor(n.getClass()).getDescription();
			al.add(new NodeType(n.getPath(),n.getName(), "", ""));
		}
		Collections.sort(al, new NodeTypeComparator());
		return al;
	}
	
	  public String getNodeName()
	  {
		 Node n = getNode();
		 if(n==null) return "isNull";
		 return n.getName();
	  }

	  public String getNodePath()
	  {
		 Node n = getNode();
		 if(n==null) return "isNull";
		 return n.getPath();
	  }
	
	  public List<Node> getDependencies()
	  {
		  Set<Node> s = getNode().getDependentNodes();
		  ArrayList<Node> al = new ArrayList<Node>();
		  if(s!=null) al.addAll(s);
		  return al;
	  }
		
	  public List<NodeAttribute> getNodeAttributes()
	  {
		  Collection<NodeAttribute> c = getNode().getNodeAttributes();
		  ArrayList<NodeAttribute> al = new ArrayList<NodeAttribute>();
		  if(c!=null) al.addAll(c);
		  return al;
	  }
	  
	  public String getClassSimpleName()
	  {
		  return classDesc.getClassDescriptor(getNode().getClass()).getType().getSimpleName();
	  }

	  public String getClassShortDescription()
	  {
		  return classDesc.getClassDescriptor(getNode().getClass()).getDisplayName();
	  }
	  
	  public String getClassDescription()
	  {
		  return classDesc.getClassDescriptor(getNode().getClass()).getDescription();
	  }
	  
	  public abstract void onSetNode(); 
	  
	  public Node getNode() { return node; }
	  public void setNode(Node node) 
	  { 
		  this.node = node;
		  onSetNode();
	  }

	  public UserAcl getUserAcl() { return userAcl; }
	  public void setUserAcl(UserAcl userAcl) { this.userAcl = userAcl; }

	  public ClassDescriptorRegistry getClassDesc() { return classDesc; }
	  public void setClassDesc(ClassDescriptorRegistry classDesc) { this.classDesc = classDesc; }

	  public Tree getTree() { return tree; }
	  public void setTree(Tree tree) { this.tree = tree; }

	  public Configurator getConfigurator() { return configurator; }
	  public void setConfigurator(Configurator configurator) { this.configurator = configurator; }

}
