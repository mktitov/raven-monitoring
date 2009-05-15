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
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.raven.ui.attr.RefreshAttributesCache;
import org.raven.ui.attr.RefreshIntervalCache;
import org.raven.ui.filter.AuthFilter;
import org.raven.ui.log.LogViewAttributesCache;
import org.raven.ui.log.LogsCache;
import org.raven.ui.node.CopyMoveNodeBean;
import org.raven.ui.node.NewNodeFromTemplate;
import org.raven.ui.node.NodeWrapper;
import org.raven.ui.util.RavenImageRenderer;
import org.raven.ui.util.RavenRegistry;
import org.raven.ui.util.RavenViewableImageRenderer;
import org.raven.ui.vo.VObyNode;
import org.raven.ui.vo.ImagesStorage;
import org.raven.conf.Configurator;
import org.raven.conf.impl.UserAcl;
import org.raven.template.impl.TemplateNode;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;
import org.apache.myfaces.trinidad.component.core.data.CoreTree;
import org.apache.myfaces.trinidad.model.TreeModel;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;
import org.apache.tapestry5.ioc.Registry;
import org.raven.tree.InvalidPathException;
import javax.faces.component.UIComponent;
import org.apache.myfaces.trinidad.event.PollEvent;

public class SessionBean 
{
	public static final String BEAN_NAME = "sBean";
	public static final String SELECT_NODE_PARAM = "nodePath";
    private Logger logger = LoggerFactory.getLogger(SessionBean.class);	
	private UserAcl userAcl = null;
	private Tree tree = null;
	private RavenTreeModel treeModel = null;   
	//private Node currentNode = null;
	private Configurator configurator;
	private NodeWrapper wrapper = null;
	private String title = "RAVEN";
//	private ClassDescriptorRegistry classDsc = null;
	private boolean refreshTree = true;

	private String newNodeType = null;
	private String newNodeName = null;
	
	private CoreTree coreTree = null;
//	private TemplateNode templateNode = null; 
	private NewNodeFromTemplate template;
	private RefreshAttributesCache refreshAttributesCache;
	private ImagesStorage imagesStorage;
	private VObyNode viewableObjectsCache;
	private RefreshIntervalCache refreshIntervalCache;
	private LogViewAttributesCache logViewAttributesCache;
	private LogsCache logsCache; 
	private boolean collapsed = false;
	
	//public String getSelectNodeParam() { return SELECT_NODE_PARAM; }
	
	public String getNodeNamePattern()
	{
		//return "[^\\Q~"+Node.NODE_SEPARATOR+Node.ATTRIBUTE_SEPARATOR+"\\E]+";
		return "[^:;\"\\~\\"+Node.NODE_SEPARATOR+"\\"+Node.ATTRIBUTE_SEPARATOR+"]+";
	}
	
	public String getTitle()
	{
		return title;
	}

	public SessionBean() 
	{
		//FacesContext fc = FacesContext.getCurrentInstance();
		//fc.getExternalContext().getRequestLocale()
	    wrapper = (NodeWrapper) getElValue(NodeWrapper.BEAN_NAME);
	    initNodeWrapper(wrapper);
		
		userAcl = getUserAcl();
		tree = getTree();
//		classDsc = getClassDscRegistry();
		configurator = getConfigurator();
		
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(tree.getRootNode());
		wrapper.setNode(tree.getRootNode());
		
		treeModel = new RavenTreeModel(nodes, "childrenList");
		treeModel.setUserAcl(userAcl);
		
		wrapper.createNewAttribute();
		template = new NewNodeFromTemplate();
		
		CopyMoveNodeBean cmnb = (CopyMoveNodeBean) getElValue(CopyMoveNodeBean.BEAN_NAME);
		cmnb.getTreeModel().toString();
		setRefreshAttributesCache(new RefreshAttributesCache());
		setImagesStorage(new ImagesStorage());
		setViewableObjectsCache(new VObyNode());
		setRefreshIntervalCache(new RefreshIntervalCache());
		setLogViewAttributesCache(new LogViewAttributesCache());
		setLogsCache(new LogsCache(getLogViewAttributesCache()));
		viewableObjectsCache.setImagesStorage(getImagesStorage());
	}

	public void reloadLeftFrame()
	{
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		 service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
		 logger.info("reloadLeftFrame");
	}
	
	 public String reloadBothFrames()
	  {
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		// service.addScript(facesContext, "parent.frames.frame1.document.treeform.reftree.focus();");
		 service.addScript(facesContext, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
		 service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
		 logger.info("reloadBothFrame");
		 return ("success");
	  }

	 public String reloadRightFrame()
	  {
		 FacesContext fc = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 Service.getRenderKitService(fc, ExtendedRenderKitService.class);
		 logger.info("reloadRightFrame");
		 service.addScript(fc, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
		 return ("success");
	  }
	 
	  //public void al(ActionEvent event)  { send(); }
	 
	 public static void initNodeWrapper(NodeWrapper nw)
	 {
		 nw.setUserAcl(getUserAcl());
		 nw.setTree(getTree());
		 nw.setClassDesc(getClassDscRegistry());
		 nw.setConfigurator(getConfigurator());
	 }
	 
	 public static Object getElValue(String name)
	 {
	    FacesContext context = FacesContext.getCurrentInstance();
	    return context.getELContext().getELResolver().getValue(context.getELContext(), null, name);
	 }

	 public static UserAcl getUserAcl()
	 {
		FacesContext fc = FacesContext.getCurrentInstance();
		Map<String,Object> session = fc.getExternalContext().getSessionMap();
		return (UserAcl)session.get(AuthFilter.USER_ACL);
	 }	

	 public static Tree getTree()
	 {
		Registry registry = RavenRegistry.getRegistry();
		return registry.getService(Tree.class);
	 }	 

	 private static ClassDescriptorRegistry getClassDscRegistry()
	 {
		Registry registry = RavenRegistry.getRegistry();
		return registry.getService(ClassDescriptorRegistry.class);
	 }	 

	 private static Configurator getConfigurator()
	 {
		Registry registry = RavenRegistry.getRegistry();
		return registry.getService(Configurator.class);
	 }	 
	 
	  public void show(ActionEvent event)
	  {
		  Node n = (Node) SessionBean.getElValue("node");
		  setCurrentNode(n);
	  }

	  public void pollReloadRightFrame(PollEvent event)
	  {
		  reloadRightFrame();
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
	  
	  public void selectNode(ActionEvent event)
	  {
		  UIComponent component=event.getComponent();
		  if(component==null)
		  {
			  logger.info("component==null");
			  return;
		  }	  
		  Map<String, Object> params = component.getAttributes();
		  String nodePath =  (String) params.get(SELECT_NODE_PARAM);
		  logger.info("select Node: "+nodePath);
		  if(nodePath==null || nodePath.length()==0) return;
		  try {
			Node n = tree.getNode(nodePath);
			setCurrentNode(n);
		  } catch (InvalidPathException e) { logger.error("InvalidPath ["+nodePath+"]",e); }
	  }
	
	public TreeModel getTreeModel() { return treeModel; }
	public void setTreeModel(RavenTreeModel treeModel) { this.treeModel = treeModel; }

	public Node getCurrentNode() { return wrapper.getNode(); }
	public void setCurrentNode(Node currentNode) 
	{
		if(getCurrentNode()==null || !getCurrentNode().equals(currentNode))
		{
			wrapper.setNode(currentNode);
			clearNewNode();
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
		boolean isTemplate = newNodeType.startsWith(""+Node.NODE_SEPARATOR);
		if(newNodeName==null || newNodeName.length()==0)
		{
			logger.warn("no newNodeName");
			return "err";
		}	
		if(newNodeType==null || newNodeType.length()==0)
		{
			newNodeType=null;
			if(!isTemplate)
			{
				logger.warn("no newNodeType");
				return "err";
			}
		}	
		if(isTemplate)
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
		try {
			wrapper.getNode().addChildren(n);
		} catch(NodeError e) 
		{
			logger.error("",e);
		}
		configurator.getTreeStore().saveNode(n);
		n.init();
		if(n.isAutoStart()) n.start();
		logger.warn("Added new node name={}",getNewNodeName());
		clearNewNode();
		return wrapper.goToEditNewAttribute(n);
		//return "ok";
	}

	public static SessionBean getInstance()
	{
		return (SessionBean) SessionBean.getElValue(BEAN_NAME);
	}
	
	public static NodeWrapper getNodeWrapper()
	{
		return (NodeWrapper) SessionBean.getElValue(NodeWrapper.BEAN_NAME);
	}
	
	public int deleteNode(NodeWrapper node)
	{
		return deleteNode(node.getNode());
	}

	public int deleteNode(Node n)
	{
		if(n.getDependentNodes()!=null && !n.getDependentNodes().isEmpty()) return -1;
		tree.remove(n);
		logger.warn("removed node: {}",n.getName());
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
			if(n.getDependentNodes()!=null && !n.getDependentNodes().isEmpty())
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
	
	public void clearNewNode()
	{
		setNewNodeType("");
		setNewNodeName("");
	}
/*
	public void exportToExcel(ActionEvent actionEvent) 
	{
		UIComponent uic = actionEvent.getComponent();
		try {
			CoreTable ct = (CoreTable)uic.getParent().getParent();
			logger.warn(ct.getId());
			VOTableWrapper lst = (VOTableWrapper) ct.getValue();
			 String contentType = "application/vnd.ms-excel";
			    FacesContext fc = FacesContext.getCurrentInstance();
//			    String filename = fc.getExternalContext().getUserPrincipal().getName() + "-" + System.currentTimeMillis()+ ".xls";
			    String filename =  "table-" + System.currentTimeMillis()+ ".xls";
			    HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
			    response.setHeader("Content-disposition", "attachment; filename=" + filename);
			    response.setContentType(contentType);

			    PrintWriter out = null;
				try 
				{
					out = response.getWriter(); 
			    	out.print(lst.makeHtmlTable().toString());
				}
				catch (IOException e) { logger.error("",e); }
				finally { try {out.close();} catch(Exception e) {}}
			    fc.responseComplete(); 			
			
			//ct.g
		}
		catch(ClassCastException e)
		{
			logger.error("!!! ",e);
		}
    }

	public void exportToCSV(ActionEvent actionEvent) 
	{
		UIComponent uic = actionEvent.getComponent();
		try {
			CoreTable ct = (CoreTable)uic.getParent().getParent();
			logger.warn(ct.getId());
			VOTableWrapper lst = (VOTableWrapper) ct.getValue();
			 String contentType = "text/csv";
			    FacesContext fc = FacesContext.getCurrentInstance();
//			    String filename = fc.getExternalContext().getUserPrincipal().getName() + "-" + System.currentTimeMillis()+ ".xls";
			    String filename =  "table-" + System.currentTimeMillis()+ ".csv";
			    HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
			    response.setHeader("Content-disposition", "attachment; filename=" + filename);
			    response.setContentType(contentType);

			    PrintWriter out = null;
				try 
				{
					out = response.getWriter(); 
			    	out.print(lst.makeCSV());
				}
				catch (IOException e) { logger.error("",e); }
				finally { try {out.close();} catch(Exception e) {}}
			    fc.responseComplete(); 			
		}
		catch(ClassCastException e)
		{
			logger.error("!!! ",e);
		}
    }
 
 */
	
	@SuppressWarnings("unchecked")
	public Class getRavenImageRenderer() { return RavenImageRenderer.class; }

	@SuppressWarnings("unchecked")
	public Class getRavenViewableImageRenderer() { return RavenViewableImageRenderer.class; }
	
	public void setNewNodeType(String o) { newNodeType = o; }
	public String getNewNodeType() { return newNodeType; }

	public String getNewNodeName() { return newNodeName; }
	public void setNewNodeName(String newName) 
	{
		newNodeName = newName;
		if(newNodeName!=null) 
			newNodeName = newNodeName.trim();
	}

	public boolean isRefreshTree() { return refreshTree; }
 	public void setRefreshTree(boolean refreshTree) { this.refreshTree = refreshTree; }
 	
 	public int getRefreshTreeInterval()
 	{
 		if(isRefreshTree()) return 10000;
 		return 100000000;
 	}

	public CoreTree getCoreTree() { return coreTree; }
	public void setCoreTree(CoreTree coreTree) { this.coreTree = coreTree; }

	public NewNodeFromTemplate getTemplate() { return template;	}
	public void setTemplate(NewNodeFromTemplate template) { this.template = template; }

	public void setRefreshAttributesCache(RefreshAttributesCache refreshAttributesStorage) {
		this.refreshAttributesCache = refreshAttributesStorage;
	}

	public RefreshAttributesCache getRefreshAttributesCache() {
		return refreshAttributesCache;
	}

	public void setImagesStorage(ImagesStorage imagesStorage) {
		this.imagesStorage = imagesStorage;
	}

	public ImagesStorage getImagesStorage() {
		return imagesStorage;
	}

	public void setViewableObjectsCache(VObyNode voCache) {
		viewableObjectsCache = voCache;
	}

	public VObyNode getViewableObjectsCache() {
		return viewableObjectsCache;
	}

	public void setRefreshIntervalCache(RefreshIntervalCache refreshIntervalStorage) {
		this.refreshIntervalCache = refreshIntervalStorage;
	}

	public RefreshIntervalCache getRefreshIntervalCache() {
		return refreshIntervalCache;
	}

	public void setLogViewAttributesCache(LogViewAttributesCache logViewAttributesStorage) {
		this.logViewAttributesCache = logViewAttributesStorage;
	}

	public LogViewAttributesCache getLogViewAttributesCache() {
		return logViewAttributesCache;
	}

	public void setLogsCache(LogsCache logsCache) {
		this.logsCache = logsCache;
	}

	public LogsCache getLogsCache() {
		return logsCache;
	}

	private void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed; 
	}

	public String switchCollapsed() 
	{
		if(isCollapsed()) setCollapsed(false);
			else setCollapsed(true);
		return null;
	}
	
	public boolean isCollapsed() {
		return collapsed;
	}
	
	
	
}
