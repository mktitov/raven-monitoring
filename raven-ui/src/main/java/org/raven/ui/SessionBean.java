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
import org.raven.template.TemplateNode;
import org.raven.tree.Node;
//import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;
import org.apache.myfaces.trinidad.component.core.data.CoreTree;
//import org.apache.myfaces.trinidad.event.FocusListener;
//import org.apache.myfaces.trinidad.event.FocusEvent;
import org.apache.myfaces.trinidad.event.PollEvent;
//import org.apache.myfaces.trinidad.model.RowKeySetImpl;
//import org.apache.myfaces.trinidad.model.RowKeySetTreeImpl;
import org.apache.myfaces.trinidad.model.TreeModel;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;
import org.apache.tapestry.ioc.Registry;
import org.raven.tree.InvalidPathException;

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
	private boolean refreshTree = true;

	private String newNodeType = null;
	private String newNodeName = null;
	
	private CoreTree coreTree = null;
//	private TemplateNode templateNode = null; 
	private NewNodeFromTemplate template;
	
	public String getNodeNamePattern()
	{
		//return "[^\\Q~"+Node.NODE_SEPARATOR+Node.ATTRIBUTE_SEPARATOR+"\\E]+";
		return "[^\\~\\"+Node.NODE_SEPARATOR+"\\"+Node.ATTRIBUTE_SEPARATOR+"]+";
	}
	
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
		template = new NewNodeFromTemplate();
	}

	public void reloadLeftFrame()
	{
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		 service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
	}
	
	 public String reloadBothFrames()
	  {
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		// service.addScript(facesContext, "parent.frames.frame1.document.treeform.reftree.focus();");
		 service.addScript(facesContext, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
		 service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
		 return ("success");
	  }

	  //public void al(ActionEvent event)  { send(); }
	 
	 public static Object getElValue(String name)
	 {
	    FacesContext context = FacesContext.getCurrentInstance();
	    return context.getELContext().getELResolver().getValue(context.getELContext(), null, name);
	 }
	  
	  public void show(ActionEvent event)
	  {
		  FacesContext context = FacesContext.getCurrentInstance();
		  Node n = (Node) context.getELContext().getELResolver().getValue(context.getELContext(), null, "node");
		  context.getExternalContext().log("Node="+n.getPath());
		  setCurrentNode(n);
	  }

	  public void onTreePoll(PollEvent event)
	  {
		  //treeModel.setRowIndex(-1);
		  
		  //treeModel.setRowKey(null);
		  //FacesContext context = FacesContext.getCurrentInstance();
		  //CoreTree tr = (CoreTree) context.getELContext().getELResolver().getValue(context.getELContext(), null, "ctree");
		  //RowKeySetTreeImpl rowKeys = new RowKeySetTreeImpl();
		  //RowKeySetImpl rk = new RowKeySetImpl();
		  //rowKeys.
		  //rowKeys.clear();
		  //coreTree.setSelectedRowKeys(null);
		  /*
		  FocusListener[] fl =  tr.getFocusListeners();
		  for(FocusListener f: fl)
		  {
			//  FocusEvent fe = new FocusEvent(); 
		  } */
	//	  if(coreTree!=null)
	//	  {
		 // coreTree.setFocusRowKey(null);
		 // coreTree.setClientRowKey(null);
		//	  coreTree.setClientRowKey(null)
		  //coreTree.markInitialState();
	//	  }
	  }
	  
	  public String getFocusRowKey()
	  {
		  /*
		  if(coreTree==null) return "isNull";
		  org.apache.myfaces.trinidad.model.RowKeySet rk =  coreTree.getSelectedRowKeys();
		  String z="";
		  if(rk!=null)
		  {
		  Iterator it = rk.iterator();
		  while(it.hasNext())
		  {
			  z = z + it.next()+";";
		  }
		  }
		  return "focus: "+coreTree.getFocusRowKey()+"  selected:"+z;
		  */
		  return "";
	  }
	
	public TreeModel getTreeModel() { return treeModel; }
	public void setTreeModel(RavenTreeModel treeModel) { this.treeModel = treeModel; }

	public Node getCurrentNode() { return wrapper.getNode(); }
	public void setCurrentNode(Node currentNode) 
	{
		if(getCurrentNode()==null || !getCurrentNode().equals(currentNode))
		{
			wrapper.setNode(currentNode);
			reloadBothFrames();
		} else reloadLeftFrame();
	}
	
	public String createTemplate()
	{
        try
        {
            Node n = tree.getNode(newNodeType);
            if (n instanceof TemplateNode)            
            {
                template.init((TemplateNode) n, wrapper.getNode(), getNewNodeName());
                return "dialog:templateAttrEdit";
            }
            return "err";
        } catch (InvalidPathException invalidPathException)
        {
            return null;
        }
	}
	
	public String createNode()
	{
		if(!wrapper.isAllowCreateSubNode())
		{
			logger.warn("not AllowCreateSubNode");
			return "err";
		}
		if(newNodeName==null || newNodeName.length()==0)
		{
			logger.warn("no newNodeName");
			return "err";
		}	
		if(newNodeType==null || newNodeType.length()==0)
		{
			logger.warn("no newNodeType");
			return "err";
		}	
		if(newNodeType.startsWith(""+Node.NODE_SEPARATOR))
		{
			return createTemplate();
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
		if(n.isAutoStart()) n.start();
		logger.warn("Added new node name={}",getNewNodeName());
		wrapper.goToEditNewAttribute(n);
		return "ok";
	}

	public int deleteNode(NodeWrapper node)
	{
		Node n = node.getNode();
		if(n.getDependentNodes()!=null) return -1;
		tree.remove(n);
		logger.warn("removed node: {}",n.getName());
		//FacesContext.getCurrentInstance().getExternalContext().log("removed node: "+n.getName());
		return 0;
	}
	
	public void afterDeleteNodes() { wrapper.onSetNode(); }
	
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

	public boolean isRefreshTree() { return refreshTree; }
 	public void setRefreshTree(boolean refreshTree) { this.refreshTree = refreshTree; }
 	
 	public int getRefreshTreeInterval()
 	{
 		if(isRefreshTree()) return 10000;
 		return 100000000;
 	}

	public CoreTree getCoreTree() { return coreTree; }
	public void setCoreTree(CoreTree coreTree) { this.coreTree = coreTree; }

	public NewNodeFromTemplate getTemplate() {
		return template;
	}

	public void setTemplate(NewNodeFromTemplate template) {
		this.template = template;
	}
	
}
