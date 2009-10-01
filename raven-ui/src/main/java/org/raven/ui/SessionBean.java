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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
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
import org.raven.audit.Action;
import org.raven.audit.ActionType;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.conf.Configurator;
import org.raven.conf.impl.AccessControl;
import org.raven.conf.impl.UserAcl;
import org.raven.ui.util.Messages;
import org.raven.util.Utl;
import org.raven.template.impl.TemplateNode;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.component.core.data.CoreTree;
import org.apache.myfaces.trinidad.model.ChildPropertyTreeModel;
import org.apache.myfaces.trinidad.model.TreeModel;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;
import org.apache.tapestry5.ioc.Registry;
import org.raven.tree.InvalidPathException;
import javax.faces.component.UIComponent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.myfaces.trinidad.event.PollEvent;

public class SessionBean 
{
	public static final String BEAN_NAME = "sBean";
	public static final String SELECT_NODE_PARAM = "nodePath";
    private Logger logger = LoggerFactory.getLogger(SessionBean.class);
    public static final String disabledInNames = "[^:;\"\\~\\"+Node.NODE_SEPARATOR+"\\"+Node.ATTRIBUTE_SEPARATOR+"]+";
    public static final String LEFT_FRAME = "parent.frames.frame1";
    public static final String RIGHT_FRAME = "parent.frames.frame2";

    @org.weda.internal.annotations.Service
    private Auditor auditor;
    
    public static final SelectItem[] auditATSI = makeSI(ActionType.values(),true);

    public static final SelectItem[] auditASI = makeSI(Action.values(),true);
/*    
    { 
    	new SelectItem(null,"-----"),
		new SelectItem(Action.ATTR_CREATE),	
		new SelectItem(Action.ATTR_DEL),	
		new SelectItem(Action.ATTR_RENAME),	
		new SelectItem(Action.ATTR_CH_VALUE),	
		new SelectItem(Action.ATTR_CH_TYPE),	
		new SelectItem(Action.ATTR_CH_SUBTYPE),	
		new SelectItem(Action.ATTR_CH_DSC),	
		new SelectItem(Action.ATTR_CH_VALUE),
		new SelectItem(Action.NODE_CREATE),
		new SelectItem(Action.NODE_DEL),
		new SelectItem(Action.NODE_COPY),
		new SelectItem(Action.NODE_MOVE),
		new SelectItem(Action.NODE_RENAME),
		new SelectItem(Action.NODE_CH_INDEX),
		new SelectItem(Action.NODE_START),
		new SelectItem(Action.NODE_STOP),
		new SelectItem(Action.NODE_START_RECURSIVE),
		new SelectItem(Action.SESSION_START),
		new SelectItem(Action.SESSION_STOP)
	};
*/    
	private UserAcl userAcl = null;
	private Tree tree = null;
	private RavenTreeModel treeModel = null;   
	//private Node currentNode = null;
//	private Configurator configurator;
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
	private String remoteIp = null;
	private String auditViewFd = "now-7d";
	private String auditViewTd = "now";
	private ActionType auditActionType = null;
	private Action auditAction = null;
	private String auditLogin = "";
	private String auditNodeId = "";
	private String auditNodePath = "";
	private List<AuditRecord> auditData = new ArrayList<AuditRecord>();
	private TreeModel resourcesTreeModel;
	private CoreTable coreTable; 

	@SuppressWarnings("unchecked")
	public static SelectItem[] makeSI(Enum[] values, boolean needNull)
	{
		ArrayList<SelectItem> si = new ArrayList<SelectItem>();
		if(needNull) 
			si.add(new SelectItem(null,"-----"));
		for(Enum x : values)
			si.add(new SelectItem(x));
		return si.toArray(new SelectItem[]{});
	}

	public String clearAuditData()
	{
		Date from = new Date(Utl.convert(auditViewFd));
		Date to = new Date(Utl.convert(auditViewTd));
		Integer nodeId;
		auditNodeId = Utl.trim2Empty(auditNodeId);
		try { nodeId = new Integer(auditNodeId); }
		catch(Exception e) {nodeId = null;}
		String nodePath = Utl.trim2Null(auditNodePath);
		String login = Utl.trim2Null(auditLogin);
		if( auditAction!=null && auditActionType!=null && !auditAction.getActionType().equals(auditActionType) ) auditAction = null;
		auditData = auditor.getRecords(from, to, nodeId, nodePath, login, auditActionType, auditAction);
		if(auditData==null) auditData = new ArrayList<AuditRecord>();
		return null;
	}
	
	/**
	 * @return name of parameter, using for node link
	 */
	public String getNodePathParName() 
	{ 
		return SELECT_NODE_PARAM; 
	}
	
	public String getNodeNamePattern()
	{
		//return "[^\\Q~"+Node.NODE_SEPARATOR+Node.ATTRIBUTE_SEPARATOR+"\\E]+";
		return disabledInNames;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public SessionBean() 
	{
		
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		remoteIp = request.getRemoteAddr();
	    wrapper = (NodeWrapper) getElValue(NodeWrapper.BEAN_NAME);
	    initNodeWrapper(wrapper);
		
		userAcl = getUserAcl();
		tree = getTree();
//		classDsc = getClassDscRegistry();
//		configurator = getConfigurator();
		
		Node x = tree.getRootNode();
		while(1==1)
		{
			int ac = userAcl.getAccessForNode(x);
			if(ac>AccessControl.TRANSIT) break;
			if(ac==AccessControl.TRANSIT)
			{
				int cnt = 0;
				Node z = null;
				for(Node y : x.getChildrens())
				{
					if(userAcl.getAccessForNode(y) > AccessControl.NONE)
					{
						if(++cnt>1) break;
						z = y;
					}	
				}	
				if(cnt>1) break;
				x = z;
			}
		}		
	//	while (en.hasMoreElements()) {
	//		type type = (type) en.nextElement();
	//	}
		
		resourcesTreeModel = new ChildPropertyTreeModel(getResources(), "childrenList");
		
		//List<Node> nodes = new ArrayList<Node>();
		List<NodeWrapper> nodes = new ArrayList<NodeWrapper>();
		////nodes.add(tree.getRootNode());
		nodes.add(new NodeWrapper(x));
		wrapper.setNode(x);
		
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
		onSessionStart();
	}

	public void onSessionStart()
	{
		writeAuditRecord(null, Action.SESSION_START, remoteIp);
		logger.info("session started, login:{} ip:{}",userAcl.getAccountName(),remoteIp);
	}

	public void onSessionStop()
	{
		writeAuditRecord(null, Action.SESSION_STOP, remoteIp);
		logger.info("session stopped, login:{} ip:{}",userAcl.getAccountName(),remoteIp);
	}
	
	/**
	 * Detects language code of request.
	 * @param fc FacesContext
	 * @return language code, or null, if it's default application language code
	 */
	public static String getPageLanguage(FacesContext fc)
	{
		ExternalContext ec = fc.getExternalContext();
		String defLang = fc.getApplication().getDefaultLocale().getLanguage();
		String lang = ec.getRequestLocale().getLanguage();
		if(defLang.equals(lang)) return null;
		Iterator<Locale> iloc = fc.getApplication().getSupportedLocales();
		String ret = null;
		while(iloc.hasNext() && ret==null)
			if(iloc.next().getLanguage().equals(lang))
				ret = lang;
		return ret;
	}
	
	public static String getOutcomeWithLang(FacesContext fc,String outcome)
	{
		String lang = getPageLanguage(fc);
		if(lang==null) return outcome; 
		return outcome+"_"+lang;
	}
	
	public String logout()
	{
		String ret = "logout"; 
		logger.info("logout, user:'{}'",userAcl.getAccountName());
		FacesContext fc = FacesContext.getCurrentInstance();
		ret = getOutcomeWithLang(fc,ret);
	    HttpSession s = (HttpSession) fc.getExternalContext().getSession(false);
		s.invalidate();
		return ret;
	}
	
	public boolean isSuperUser()
	{
		return userAcl.isSuperUser();
	}
	
	public void reloadLeftFrame()
	{
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 //org.apache.myfaces.trinidad.util.
		 Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		 //service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
//		 service.addScript(facesContext, LEFT_FRAME+".location.reload()");
		 service.addScript(facesContext, LEFT_FRAME+".location.href="+LEFT_FRAME+".location.href");
		 logger.info("reloadLeftFrame");
	}
	
	 public String reloadBothFrames()
	  {
		 FacesContext facesContext = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 org.apache.myfaces.trinidad.util.Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		// service.addScript(facesContext, "parent.frames.frame1.document.treeform.reftree.focus();");
//		 service.addScript(facesContext, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
//		 service.addScript(facesContext, "parent.frames.frame1.location.href=parent.frames.frame1.location.href");
//		 service.addScript(facesContext, RIGHT_FRAME+".location.reload()");
//		 service.addScript(facesContext, LEFT_FRAME+".location.reload()");
		 service.addScript(facesContext, RIGHT_FRAME+".location.href="+RIGHT_FRAME+".location.href");
		 service.addScript(facesContext, LEFT_FRAME+".location.href="+LEFT_FRAME+".location.href");
		 logger.info("reloadBothFrame");
		 return ("success");
	  }

	 public String reloadRightFrame()
	  {
		 FacesContext fc = FacesContext.getCurrentInstance();
		 ExtendedRenderKitService service = (ExtendedRenderKitService)
		 org.apache.myfaces.trinidad.util.Service.getRenderKitService(fc, ExtendedRenderKitService.class);
		 logger.info("reloadRightFrame");
//		 service.addScript(fc, "parent.frames.frame2.location.href=parent.frames.frame2.location.href");
//		 service.addScript(fc, RIGHT_FRAME+".location.reload()");
		 service.addScript(fc, RIGHT_FRAME+".location.href="+RIGHT_FRAME+".location.href");
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

	 public Object getElValueRow()
	 {
		 Object z = getElValue("row");
		 return z; //getElValue("row"); 
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
		  NodeWrapper n = (NodeWrapper) SessionBean.getElValue("node");
		  setCurrentNode(n.getNode());
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
	  
	  public List<String[]> getResourcesList()
	  {
		  HashMap<String,String> rl = userAcl.getResourcesList(tree);
		  List<String[]> ret = new ArrayList<String[]>();
		  if(rl==null)
		  {
			  logger.error("ResourcesList is null !");
			  return ret;
		  }
		  List<String> keys = new ArrayList<String>();
		  keys.addAll(rl.keySet());
		  Collections.sort(keys);
		  for(String key: keys)
		  { 
			  String nodePath = rl.get(key);
			  ret.add( new String[] {key,nodePath} );
		  }  
		  return ret;
	  }
	  
	  public List<ResourceInfo> getResources()
	  {
		  HashMap<String,String> rl = userAcl.getResourcesList(tree);
		  List<ResourceInfo> ret = new ArrayList<ResourceInfo>();
		  if(rl==null)
		  {
			  logger.error("ResourcesList is null !");
			  return ret;
		  }
		  List<String> keys = new ArrayList<String>();
		  keys.addAll(rl.keySet());
		  Collections.sort(keys);
		  for(String key: keys)
		  { 
			  String nodePath = rl.get(key);
			  ret.add( new ResourceInfo(key,nodePath) );
		  }  
		  ResourceInfo.makeListX(ret);
		  return ret;
	  }
	  
	  
	  public void selectNodeAndMaximize(ActionEvent event)
	  {
		  switchCollapsed();
		  selectNodeT(event);
	  }
	  
	  public void selectNodeT(ActionEvent event)
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
			setCurrentNode(n,false);
		  } catch (InvalidPathException e) { logger.error("InvalidPath ["+nodePath+"]",e); }
	  }
	  
	  
	public TreeModel getTreeModel() { return treeModel; }
	public void setTreeModel(RavenTreeModel treeModel) { this.treeModel = treeModel; }

	public Node getCurrentNode() { return wrapper.getNode(); }

	public void setCurrentNode(Node currentNode,boolean rFlag) 
	{
		if(getCurrentNode()==null || !getCurrentNode().equals(currentNode))
		{
			wrapper.setNode(currentNode);
			clearNewNode();
			//if(rFlag) reloadBothFrames();
			//else 
			reloadRightFrame();
		}// else reloadLeftFrame();
	}
	
	public void setCurrentNode(Node currentNode) 
	{
		if(getCurrentNode()==null || !getCurrentNode().equals(currentNode))
		{
			wrapper.setNode(currentNode);
			clearNewNode();
			//reloadBothFrames();
			reloadRightFrame();
		} //else reloadLeftFrame();
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
		tree.saveNode(n);
		n.init();
		if(n.isAutoStart()) n.start();
		logger.warn("Added new node name={}",getNewNodeName());
        auditor.write(n, getUserAcl().getAccountName(), Action.NODE_CREATE, "class: "+newNodeType);            	  
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
        auditor.write(n, getUserAcl().getAccountName(), Action.NODE_DEL, null);            	  
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
				if(ret.length()==0) ret.append(Messages.getUiMessage(Messages.NODES_HAVE_DEPEND)+" ");
					else ret.append(", ");
				ret.append(n.getName());
				continue;
			}
			tree.remove(n);
			logger.warn("removed node: {}",n.getName());
	        auditor.write(n, getUserAcl().getAccountName(), Action.NODE_DEL, null);            	  
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
 		if(isRefreshTree()) return 20000;
 		return 2000000000;
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

	public Auditor getAuditor() 
	{
		return auditor;
	}

	public String getAccountName()
	{
		return userAcl.getAccountName();
	}		
	
	public static String getAccountNameS()
	{
		return SessionBean.getUserAcl().getAccountName();
	}		
	
	public void writeAuditRecord(Node n,Action a,String mes) 
	{
		auditor.write(n, userAcl.getAccountName(), a, mes);
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public String getAuditViewFd() {
		return auditViewFd;
	}

	public void setAuditViewFd(String autitViewFrom) {
		this.auditViewFd = autitViewFrom;
	}

	public String getAuditViewTd() {
		return auditViewTd;
	}

	public void setAuditViewTd(String autitViewTo) {
		this.auditViewTd = autitViewTo;
	}
	
	public SelectItem[] getAuditActionSelectItems()  
    { 
		return auditASI; 
    }

	public SelectItem[] getAuditActionTypeSelectItems()  
    { 
		return auditATSI; 
    }


	public void setAuditAction(Action auditAction) {
		this.auditAction = auditAction;
	}


	public Action getAuditAction() {
		return auditAction;
	}


	public void setAuditActionType(ActionType auditActionType) {
		this.auditActionType = auditActionType;
	}


	public ActionType getAuditActionType() {
		return auditActionType;
	}


	public void setAuditLogin(String auditLogin) {
		this.auditLogin = auditLogin;
	}


	public String getAuditLogin() {
		return auditLogin;
	}


	public void setAuditNodeId(String auditNodeId) {
		this.auditNodeId = auditNodeId;
	}


	public String getAuditNodeId() {
		return auditNodeId;
	}


	public void setAuditNodePath(String auditNodePath) {
		this.auditNodePath = auditNodePath;
	}


	public String getAuditNodePath() {
		return auditNodePath;
	}

	public List<AuditRecord> getAuditData() {
		return auditData;
	}

	public TreeModel getResourcesTreeModel() {
		return resourcesTreeModel;
	}

	public void setCoreTable(CoreTable coreTable) {
		logger.info("setCoreTable:");
		this.coreTable = coreTable;
	}

	public CoreTable getCoreTable() {
		logger.info("getCoreTable:");
		return coreTable;
	}
	
	public Object getCoreTableRowData() {
		logger.info("getCoreTableRowData:");
		Object o = coreTable.getRowData();
		return o;
	}
	
}
