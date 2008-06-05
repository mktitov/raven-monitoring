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
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
//import java.util.Set;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.raven.RavenRegistry;
import org.raven.conf.Configurator;
import org.raven.conf.impl.UserAcl;
import org.raven.tree.Node;
//import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;
import org.apache.myfaces.trinidad.model.TreeModel;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;
import org.apache.tapestry.ioc.Registry;

public class SessionBean 
{
	public static final String BEAN_NAME = "sBean";
    protected Logger logger = LoggerFactory.getLogger(SessionBean.class);	
	private UserAcl userAcl = null;
	private Tree tree = null;
	private RavenTreeModel treeModel = null;   
	//private Node currentNode = null;
	private Configurator configurator;
	private NodeWrapper wrapper = null;
	private String title = "RAVEN";
	private ClassDescriptorRegistry classDsc = null;

	private String newNodeType = null;
	private String newNodeName = null;
	
	public String getTitle()
	{
		return title;
	}

	public SessionBean() 
	{
		FacesContext fc = FacesContext.getCurrentInstance();
	    wrapper = (NodeWrapper) fc.getELContext().getELResolver().getValue(fc.getELContext(), null, NodeWrapper.BEAN_NAME);
		
		Map<String,Object> session = fc.getExternalContext().getSessionMap();
		userAcl = (UserAcl)session.get(AuthFilter.USER_ACL);
		wrapper.setUserAcl(userAcl);

		Registry registry = RavenRegistry.getRegistry();
		tree = registry.getService(Tree.class);
		wrapper.setTree(tree);
		classDsc = registry.getService(ClassDescriptorRegistry.class);
		wrapper.setClassDesc(classDsc);
		
//		classDsc.getClassDescriptor(Class.class).
//		registry.getService(ClassDescriptorRegistry.class);
		
		configurator = registry.getService(Configurator.class);
		wrapper.setConfigurator(configurator);
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(tree.getRootNode());
		wrapper.setNode(tree.getRootNode());
		
		treeModel = new RavenTreeModel(nodes, "childrenList");
		treeModel.setUserAcl(userAcl);
		
		wrapper.createNewAttribute();
	}

	 public String send()
	  {
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		 service.addScript(facesContext, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
		 return ("success");
	  }

	  //public void al(ActionEvent event)  { send(); }  
	  
	  public void show(ActionEvent event)
	  {
		  FacesContext context = FacesContext.getCurrentInstance();
		  Node n = (Node) context.getELContext().getELResolver().getValue(context.getELContext(), null, "node");
		  context.getExternalContext().log("Node="+n.getPath());
		  setCurrentNode(n);
//		  currentNode = n; 
	  ///RequestContext requestContext = RequestContext.getCurrentInstance();
	//	  requestContext.getPageFlowScope().put("selectedNode", n);
//		  send();
	  }
	  
	
	public TreeModel getTreeModel() { return treeModel; }
	public void setTreeModel(RavenTreeModel treeModel) { this.treeModel = treeModel; }

	public Node getCurrentNode() { return wrapper.getNode(); }
	public void setCurrentNode(Node currentNode) 
	{
		if(getCurrentNode()==null || !getCurrentNode().equals(currentNode))
		{
			wrapper.setNode(currentNode);
			send();
		}
	}
	
	public String createNode()
	{
		if(!wrapper.isAllowCreateSubNode())
		{
			logger.warn("not AllowCreateSubNode");
			return "err";
		}	
		if(newNodeType==null || newNodeType.length()==0)
		{
			logger.warn("no newNodeType");
			return "err";
		}	
		if(newNodeName==null || newNodeName.length()==0)
		{
			logger.warn("no newNodeName");
			return "err";
		}	
		Object o = null;
		try { o = Class.forName(newNodeType).newInstance(); } 
		catch(Exception e) 	{
			logger.error("On newInstance for '"+newNodeType+"' ", e);
			return "err";
		}
		Node n = (Node) o;
		n.setName(getNewNodeName());
		wrapper.getNode().addChildren(n);
		configurator.getTreeStore().saveNode(n);
		n.init();
		logger.warn("Added new node name={}",getNewNodeName());
		wrapper.goToEditNewAttribute(n);
		return "ok";
	}
	
	public String deleteNodes(List<NodeWrapper> nodes)
	{
		StringBuffer ret = new StringBuffer();
		Iterator<NodeWrapper> it = nodes.iterator();
		while(it.hasNext())
		{
			Node n = it.next().getNode();
			if(n.getDependentNodes()!=null)
			{
				if(ret.length()==0) ret.append("This nodes have dependensies: ");
					else ret.append(", ");
				ret.append(n.getName());
				continue;
			}
			tree.remove(n);
			logger.warn("removed node: {}",n.getName());
			FacesContext.getCurrentInstance().getExternalContext().log("removed node: "+n.getName());
		}
		wrapper.onSetNode();
		if(ret.length()==0) return null;
		return ret.toString();
	}
	
	@SuppressWarnings("unchecked")
	public Class getRavenImageRenderer() { return RavenImageRenderer.class; }
	
	public void setNewNodeType(String o) { newNodeType = o; }
	public String getNewNodeType() { return newNodeType; }

	public String getNewNodeName() { return newNodeName; }
	public void setNewNodeName(String newNodeName) { this.newNodeName = newNodeName; }
	
}
