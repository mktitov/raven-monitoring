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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import org.apache.myfaces.trinidad.component.core.layout.CoreShowDetailItem;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.event.ReturnEvent;
//import org.apache.myfaces.trinidad.model.UploadedFile;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.Viewable;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.ui.SessionBean;
import org.raven.ui.attr.Attr;
import org.raven.ui.node.AbstractNodeWrapper;
import org.raven.ui.attr.NewAttribute;
import org.raven.ui.attr.RefreshAttributesCache;
import org.raven.ui.attr.RefreshIntervalCache;
import org.raven.ui.log.LogByNode;
import org.raven.ui.log.LogViewAttributes;
import org.raven.ui.log.LogViewAttributesCache;
import org.raven.ui.log.LogsByNodes;
import org.raven.ui.log.LogsCache;
import org.raven.ui.util.Messages;
import org.raven.ui.vo.ViewableObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ConstraintException;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.converter.TypeConverterException;

public class NodeWrapper extends AbstractNodeWrapper 
implements Comparator<NodeAttribute>
{
	public static final String BEAN_NAME = "cNode";
    protected static Logger logger = LoggerFactory.getLogger(NodeWrapper.class);

    private static final SelectItem[] logsSI = { new SelectItem(LogLevel.TRACE),
		new SelectItem(LogLevel.DEBUG),	
		new SelectItem(LogLevel.INFO),	
		new SelectItem(LogLevel.WARN),	
		new SelectItem(LogLevel.ERROR)	
		};
    private static final int ALL_NODES = -1;
    
	private List<NodeAttribute> savedAttrs = null;
	private List<Attr> editingAttrs = null;
	//private List<NodeAttribute> savedRefreshAttrs = null;
	private Map<String,Attr> editingRefreshAttrs = null;
	private List<NodeAttribute> readOnlyAttributes = null;
	private NewAttribute newAttribute = null;
	private CoreShowDetailItem showTab; 
	private CoreShowDetailItem nodeEditTab; 
	private CoreShowDetailItem treeEditTab; 
	private CoreShowDetailItem selNodeTab; 
	private CoreShowDetailItem selTemplateTab;
	private boolean useChildAttributesView = true;
//	private boolean hasUnsavedChanges = false;
	private boolean needRefreshVO = false;
	//private int refreshViewInteval = 0;
	private boolean refreshPressed = false;
	private List<Integer> unsavedChanges = new ArrayList<Integer>();
		
	public NodeWrapper() 
	{
		super();
		SessionBean.initNodeWrapper(this);
	}

	public SelectItem[] getLogLevelSelectItems()  
    { 
		return logsSI; 
    }
	
	
	public NodeWrapper(Node node) 
	{
		this();
		this.setNode(node);
	}

	public String getRefreshAttributesTitle()
	{
		String mesName = null;
		if( isNodeAndChildViewable() ) mesName = "refreshAttributesOfNodeAndChildren";
		else if( isNodeViewableOnly() ) mesName = "refreshAttributesOfNode";
				else if( isChildViewableOnly() ) mesName = "refreshAttributesOfChildren";
						else return "";
		return Messages.getUiMessage(mesName);
	}

	public boolean isNeedShowRefreshAttributes()
	{
		if(!isAllowNodeRead()) return false;		
		return isViewable() || isChildViewable();
	}
	
	public boolean isNodeAndChildViewable()
	{
		if(!isAllowNodeRead()) return false;
		return isViewable() && isChildViewable();
	}

	public boolean isNodeViewableOnly()
	{
		return isViewable() && !isChildViewable();
	}

	public boolean isChildViewableOnly()
	{
		return !isViewable() && isChildViewable();
	}
	
	public boolean isChildViewable()
	{
		if(!isAllowNodeRead()) return false;
		Collection<Node> c = getNode().getSortedChildrens();
		if(c!=null) 
		{
			Iterator<Node> it = c.iterator();
			while(it.hasNext())
			{
				NodeWrapper nw = new NodeWrapper(it.next());
				if(!nw.isAllowNodeRead()) continue;
				if(nw.isViewable()) return true;
				//Map<String,NodeAttribute> lst = nw.getRefreshAttributesMap();
			}	  
		}
		return false;
	}

	public List<NodeWrapper> getViewableChilddren()
	{
		List<NodeWrapper> ret = new ArrayList<NodeWrapper>();
		//Collection<Node> c = getNode().getSortedChildrens();
		Collection<Node> c = getNode().getEffectiveChildrens();
		if(c!=null) 
		{
			Iterator<Node> it = c.iterator();
			while(it.hasNext())
			{
				NodeWrapper nw = new NodeWrapper(it.next());
				if(!nw.isAllowNodeRead()) continue;
				if(nw.isViewable()) ret.add(nw);
			}	  
		}
		return ret;
	}
	
	public int getNodeId()
	{
		return getNode().getId();
	}
	
	public boolean isViewable()
	{
		if(!isAllowNodeRead()) return false;
		if( getNode() instanceof Viewable ) 
			return true;
		return false;
	}
	
	public void setNameInDialog(ActionEvent event)
	{
		RenameNodeBean b = RenameNodeBean.getInstance();
		b.setName(getNode().getName());
	}
	
	public void renameNodeHandleReturn(ReturnEvent event)
	{
		if(!isAllowNodeRename()) return;
		String ret = (String) event.getReturnValue();
		if(ret==null) return;
		ret = ret.trim();
		if(ret.length()>0)
		{
			Node n = getNode();
			n.setName(ret);
			n.save();
			SessionBean.getInstance().reloadBothFrames();
		}
	}
	
	public void onSetNode()
	{
		editingAttrs = null;
		createNewAttribute();
		editingRefreshAttrs = null;
		refreshPressed = false;
		clearAllUnsavedChanges();
		//loadRefreshAttributes();
//		AttributesTableBean atb = (AttributesTableBean) context.getELContext().getELResolver().getValue(context.getELContext(), null, AttributesTableBean.BEAN_NAME);
//		if(atb != null && atb.getMessage() !=null) atb.getMessage().setMessage("");
	}

	public void createNewAttribute()
	{
		if(getClassDesc()!=null && getTree()!=null)
			newAttribute = new NewAttribute(getTree().getNodeAttributesTypes(getNode()),getClassDesc());
	}
	
	public String nodeStart()
	{
		if( ! isAllowControl() ) return "err";
		if(!isCanNodeStart()) return "err";
		try { getNode().start(); }
		catch (NodeError e)
		{
			logger.error("on start "+getNodeName()+" : "+e.getMessage());
		}
		return "ok";
	}

	public String nodeStop()
	{
		if( ! isAllowControl() ) return "err";
		if(!isCanNodeStop()) return "err";
		try { getNode().stop(); }
		catch (NodeError e)
		{
			logger.error("on stop "+getNodeName()+" : "+e.getMessage());
		}
		return "ok";
	}
	
	public List<Attr> getAttributes() 
	{
		if(editingAttrs==null) loadAttributes();
		if(!useChildAttributesView) return editingAttrs;
		List<Attr> parentAttrs = new ArrayList<Attr>();
		for(Attr a : editingAttrs)
			if(a.getAttribute().getParentAttribute()==null) parentAttrs.add(a);
		return parentAttrs;
	}
	
	
	public Map<String,NodeAttribute> getRefreshAttributesMap()
	{
		SessionBean sb = SessionBean.getInstance();
		RefreshAttributesCache rs = sb.getRefreshAttributesCache();
		return rs.get(this);
	}
	
	public List<NodeAttribute> getRefreshAttributes()
	{
		List<NodeAttribute> nal = new ArrayList<NodeAttribute>();
		Map<String,NodeAttribute> map = getRefreshAttributesMap();
		if(map!=null)
			nal.addAll(map.values());
		return nal;
	}
		
	public List<ViewableObjectWrapper> getViewableObjects()
	{
		if(!isAllowNodeRead()) return new ArrayList<ViewableObjectWrapper>();
		List<ViewableObjectWrapper> x = SessionBean.getInstance().getViewableObjectsCache().getObjects(this);
		logger.info("+++ ");
		for(ViewableObjectWrapper v : x)
		{
			logger.info(" id="+v.getId()+"  group="+v.getMimeGroup()+"  nodeId="+v.getNodeId()+"  uid="+v.getUid());
		}
		logger.info("--- ");
		return x;
	}
	
	public void removeVOFromHash()
	{
		SessionBean.getInstance().getViewableObjectsCache().remove(this);
	}
	
	public void removeRAFromHash()
	{
		SessionBean.getInstance().getRefreshAttributesCache().remove(this);
	}
	
	public List<NodeWrapper> getViewableNodes()
	{
	  //Collection<Node> c = getNode().getSortedChildrens();
	  Collection<Node> c = getNode().getEffectiveChildrens();
	  ArrayList<NodeWrapper> wrappers = new ArrayList<NodeWrapper>();
	  if(c!=null) 
	  {
		  ArrayList<Node> nodes = new ArrayList<Node>();
		  nodes.addAll(c);
		  Iterator<Node> it = nodes.iterator();
		  while(it.hasNext())
		  {
			  NodeWrapper nw = new NodeWrapper(it.next());
			  if(!nw.isAllowNodeRead()) continue;
			  wrappers.add( nw );
		  }	  
	  }
	  if ( isViewable() ) 
		  wrappers.add(0, this );
	  return wrappers;
}
	
	public void loadRefreshAttributes() 
	{
		List<NodeWrapper> wrappers = getViewableNodes();
		if(wrappers.size()>1)
		{
			NodeWrapper tmp = wrappers.remove(0);
			wrappers.add(tmp);
		}
		if(editingRefreshAttrs!=null) editingRefreshAttrs.clear();
		editingRefreshAttrs = new HashMap<String,Attr>();
		for(NodeWrapper nw : wrappers)
		{
			List<NodeAttribute> nal = nw.getRefreshAttributes();
			for(NodeAttribute na : nal)
				try {
					editingRefreshAttrs.put(na.getName(),new Attr(na,true));
				} catch (TooManyReferenceValuesException e) {
					logger.error("on load refresh attributes : ",e);
				}
		}
	}
	
	public void  loadAttributes() 
	{
		loadRefreshAttributes();
		List<NodeAttribute> attrList = getNodeAttributes();
		Collections.sort(attrList, this);
		//if(editingAttrs!=null) editingAttrs.clear();
		//if(readOnlyAttributes!=null) readOnlyAttributes.clear();
		//if(savedAttrs!=null) savedAttrs.clear();
		
		editingAttrs = new ArrayList<Attr>();
		readOnlyAttributes = new ArrayList<NodeAttribute>();
		savedAttrs = new ArrayList<NodeAttribute>(); 
		for(NodeAttribute na : attrList)
		{	
			if(na.isReadonly()) readOnlyAttributes.add(na);
			else
			{
				savedAttrs.add(na);
				try { editingAttrs.add(new Attr(na)); }
				catch (TooManyReferenceValuesException e) {
					logger.error("on load attributes: ",e);
					return;
				}
			}	
		}	
		for(Attr a : editingAttrs)
			a.findChildren(editingAttrs);
		//logger.info("loadAttributes()");
	}
	
//	  public String delAttr()  {   return "";  }

	  public String saveWithoutWrite()
	  {
		  return save(false);
	  }
	
	  public String save(boolean write)
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
			  String dsc = getNotNull(na.getDescription());
//			  if( !at.isExpressionSupported() &&  !val.equals(at.getValue()) )
			  if( !at.isExpressionSupported() &&  at.isValueChanged() )				  
			  {
				  save |=1;
				  logger.info("old value: '{}', new value: '{}'",val,at.getValue());
			  }	  
			  if( !dsc.equals(at.getDescription()) ) save |=2;
			  if( !na.getName().equals(at.getName()) ) save |=4;
			  if( at.isExpressionSupported() &&  ! ObjectUtils.equals(na.getRawValue(), at.getExpression()) ) save |=8;
              if( !ObjectUtils.equals(na.getValueHandlerType(), at.getValueHandlerType())) save |=16;
              if(at.isTemplateExpression() != na.isTemplateExpression()) save |=32;
              if(at.isFileAttribute() && at.getFile()!=null) save |=64;
              if(hasUnsavedChanges(na.getId())) save |= 4096; 
			  
			  if(save==0) continue;
			  try 
			  { 
				  if( (save&1) !=0 ) na.setValue(at.getValue());
				  if( (save&2) !=0 ) na.setDescription(at.getDescription());
				  if( (save&4) !=0 ) na.setName(at.getName());
				  if( (save&8) !=0 ) na.setValue(at.getExpression());
                  if( (save&16) !=0) na.setValueHandlerType(at.getValueHandlerType());
                  if( (save&32) !=0) na.setTemplateExpression(at.isTemplateExpression());
				  if( (save&64) !=0)
                  {
                      UploadedFile file = at.getFile();
                      DataFile dataFile = na.getRealValue();
                      dataFile.setFilename(file.getName());
                      dataFile.setMimeType(file.getContentType());
                      dataFile.setDataStream(file.getInputStream());
                      logger.info("Uploaded: '{}'; size:{} ",file.getName(),file.getSize());                      
                      //file.dispose();
                  }
				  if(write)
				  {
					  getConfigurator().getTreeStore().saveNodeAttribute(na);
					  clearUnsavedChanges(na.getId());
					  this.afterWriteAttrubutes();
				  }	  
				  	else addUnsavedChanges(na.getId());
			  }
			  catch(ConstraintException e) 
			  {
				  String t = Messages.getUiMessage("attribute");
				  if(ret.length()!=0) ret.append(". ");
				  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
				  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
			  }
			  catch(TypeConverterException e) 
			  {
				  String t = Messages.getUiMessage("attribute");
				  if(ret.length()!=0) ret.append(". ");
				  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
				  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
			  }
			  catch(Throwable e) 
			  {
				  String t = Messages.getUiMessage("attribute");
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
		  if(ret.length()!=0) ret.toString();
		  loadAttributes(); 
		  return null;
	  }

	  public String saveRefreshAttributes()
	  {
		  int save = 0;
		  StringBuffer ret = new StringBuffer();
		  
		  Iterator<String> keys = editingRefreshAttrs.keySet().iterator();
		  while(keys.hasNext())
		  {
			  String key = keys.next();
			  Attr at = editingRefreshAttrs.get(key);
			  List<NodeWrapper> nwlist = getViewableNodes();
			  for(NodeWrapper nw : nwlist)
			  {
				  Map<String,NodeAttribute> map = nw.getRefreshAttributesMap();
				  if(map==null) continue;
				  if(map.containsKey(key))
				  {
					  NodeAttribute na = map.get(key);
					  String val = getNotNull(na.getValue());
					  if( !at.isExpressionSupported() &&  !val.equals(at.getValue()) ) save |=1;
					  if( at.isExpressionSupported() &&  ! ObjectUtils.equals(na.getRawValue(), at.getExpression()) ) save |=8;
		              if( !ObjectUtils.equals(na.getValueHandlerType(), at.getValueHandlerType())) save |=16;
		              if(at.isTemplateExpression() != na.isTemplateExpression()) save |=32;

					  if(save==0) continue;
		              
					  try 
					  { 
						  if( (save&1) !=0 ) na.setValue(at.getValue());
						  if( (save&8) !=0 ) na.setValue(at.getExpression());
		                  if( (save&16) !=0) na.setValueHandlerType(at.getValueHandlerType());
		                  if( (save&32) !=0) na.setTemplateExpression(at.isTemplateExpression());
					  }
					  catch(ConstraintException e) 
					  {
						  String t = Messages.getUiMessage("attribute");
						  if(ret.length()!=0) ret.append(". ");
						  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
						  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
					  }
					  catch(TypeConverterException e) 
					  {
						  String t = Messages.getUiMessage("attribute");
						  if(ret.length()!=0) ret.append(". ");
						  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
						  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
					  }
					  catch(Throwable e) 
					  {
						  String t = Messages.getUiMessage("attribute");
						  if(ret.length()!=0) ret.append(". ");
						  ret.append(t+" '"+at.getName()+"' : "+e.getMessage());
						  logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
					  }
					  finally 
					  { 
						  at.setValue(na.getValue());
						  at.setDescription(na.getDescription());
						  at.setName(na.getName());
		                  at.setValueHandlerType(na.getValueHandlerType());
		                  at.setTemplateExpression(na.isTemplateExpression());
					  }
				  }
			  }
			  
		  }
		  if(ret.length()!=0) ret.toString();
		  loadRefreshAttributes();
		  return null;
	  }
	  
	  
	  public String cancel() //ActionEvent event
	  {
		  getAttributes();
		  clearAllUnsavedChanges();
		  return "";
	  }

	  public String cancelRefreshAttributes() //ActionEvent event
	  {
		  loadRefreshAttributes();
		  return "";
	  }
	  
	  public String createAttribute() throws Exception
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
		  na = new NodeAttributeImpl(newAttribute.getName(),newAttribute.getAttrClass(),null,newAttribute.getDescription());
		  //	na.setType(String.class);
			na.setOwner(getNode());
			getNode().addNodeAttribute(na);
			na.setRequired(newAttribute.isRequired());
		//	try { na.setValue(""); }
		//	catch(ConstraintException e) { }
            na.init();
            na.save();
			getConfigurator().getTreeStore().saveNodeAttribute(na);
			logger.warn("Added new attribute='{}' for node='{}'",na.getName(),getNode().getName());
			onSetNode();
			return "ok";
	  }

		public int deleteAttrubute(Attr attr)
		{
			NodeAttribute na = getNode().getNodeAttribute(attr.getName());
			if(na==null) return -1;
			if(!attr.isAllowDelete()) return -2;
			getNode().removeNodeAttribute(na.getName());
			getConfigurator().getTreeStore().removeNodeAttribute(na.getId());
			logger.warn("removed attrubute: {}",na.getName());
			return 0;
		}
		
		public void afterDeleteAttrubutes()
		{
			onSetNode();
			restartNode();
		}
		
		public void afterWriteAttrubutes()
		{
			restartNode();
		}
		
		public void restartNode()
		{
			if(getNode().getStatus() == Node.Status.STARTED)
			{
				getNode().stop();
				getNode().start();
			}
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
			  if(!a.isAllowDelete())
			  //if(na.getParentAttribute()!=null || na.getParameterName()!=null)
			  {
				  String t = Messages.getString("org.raven.ui.messages", "attributesCantDeleted",new Object[] {});
				  if(ret.length()==0) ret.append(t); else ret.append(", ");
				  ret.append(na.getName());
				  continue;
			  }
			  getNode().removeNodeAttribute(na.getName());
			  getConfigurator().getTreeStore().removeNodeAttribute(na.getId());
			  logger.warn("removed attrubute: {}",na.getName());
			  onSetNode();
		  }
		  if(ret.length()==0) return null;
		  return ret.toString();
	  }

	public boolean equals(Object x)
	{
		if (x!=null && x instanceof NodeWrapper) {
			NodeWrapper xx = (NodeWrapper) x;
			return(this.getNode().equals(xx.getNode()));
		}
		return false;
	}
		
	/*
	 * 	
	 */
	  public List<NodeWrapper> getChildrenList()
	  {
		  ArrayList<NodeWrapper> al = new ArrayList<NodeWrapper>();
		  List<Node> lst = getNode().getChildrenList();
		  if(lst==null || lst.size()==0) return al;
		  Iterator<Node> it = lst.iterator();
		  while(it.hasNext())
		  {
			  NodeWrapper nw = new NodeWrapper(it.next());
			  if(!nw.isAllowTreeEdit()) continue;
			  al.add(nw);
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
	
	public String goToEditNewAttribute(Node n)
	{
		/*
		showTab.setDisclosed(false);
		nodeEditTab.setDisclosed(true);
		treeEditTab.setDisclosed(true);
		*/
		setNode(n);
		return "tabNodeEdit";
	}
	
	public String onRefresh()
	{
		refreshPressed = true;
		return onRefreshX(); 
	}

	public String onRefreshX()
	{
		setNeedRefreshVO();
		SessionBean.getInstance().reloadRightFrame();
		return null; 
	}
	
	public void pollRefresh(PollEvent event)
	{
		onRefreshX();
	}
	
	public CoreShowDetailItem getShowTab() { return showTab; }
	public void setShowTab(CoreShowDetailItem showTab) { this.showTab = showTab; }

	public CoreShowDetailItem getNodeEditTab() { return nodeEditTab; }
	public void setNodeEditTab(CoreShowDetailItem nodeEditTab) { this.nodeEditTab = nodeEditTab; }

	public CoreShowDetailItem getTreeEditTab() { return treeEditTab; }
	public void setTreeEditTab(CoreShowDetailItem treeEditTab) { this.treeEditTab = treeEditTab; }

	public CoreShowDetailItem getSelNodeTab() { return selNodeTab; }
	public void setSelNodeTab(CoreShowDetailItem selNodeTab) { 	this.selNodeTab = selNodeTab; }

	public CoreShowDetailItem getSelTemplateTab() { return selTemplateTab; }
	public void setSelTemplateTab(CoreShowDetailItem selTemplateTab) { this.selTemplateTab = selTemplateTab; }

	public boolean isUseChildAttributesView() {
		return useChildAttributesView;
	}

	public void setUseChildAttributesView(boolean useChildAttributesView) {
		this.useChildAttributesView = useChildAttributesView;
	}

//	public void setHasUnsavedChanges(boolean hasUnsavedChanges) {
//		this.hasUnsavedChanges = hasUnsavedChanges;
//	}

	
	public boolean hasUnsavedChanges() 
	{
	 if(unsavedChanges.size()>0) return true;
	 return false;
	}

	public boolean hasUnsavedChanges(int attrId) 
	{
	 if(unsavedChanges.contains(attrId)) return true;
	 return false;
	}

	public void clearUnsavedChanges(int attrId) 
	{
	 unsavedChanges.remove(new Integer(attrId));
	}

	public void clearAllUnsavedChanges() 
	{
	 unsavedChanges.clear();
	}
	
	public void addUnsavedChanges(int attrId) 
	{
	 unsavedChanges.add(attrId);
	}
	
//	public boolean isHasUnsavedChanges() {
//		return hasUnsavedChanges;
//	}

	public List<NodeAttribute> getReadOnlyAttributes() throws TooManyReferenceValuesException 
	{
		if(readOnlyAttributes==null) loadAttributes();
		return readOnlyAttributes;
	}

	public void setReadOnlyAttributes(List<NodeAttribute> readOnlyAttributes) {
		this.readOnlyAttributes = readOnlyAttributes;
	}

	public List<Attr> getEditingRefreshAttrs() {
		if(editingRefreshAttrs==null )
				loadRefreshAttributes();
		
		List<Attr> la =  new ArrayList<Attr>(editingRefreshAttrs.values());
		Collections.sort(la);
		return la;
	}

	public void setNeedRefreshVO() {
		this.needRefreshVO = true;
	}
	
	public void setNeedRefreshVO(boolean needRefreshVO) {
		this.needRefreshVO = needRefreshVO;
	}

	public boolean isNeedRefreshVO() {
		return needRefreshVO;
	}

	public int compare(NodeAttribute o1, NodeAttribute o2) {
		return o1.getName().compareTo(o2.getName());
	}

	/**
	 * 
	 * @param refreshViewInteval - the interval (seconds)
	 */
	public void setRefreshViewInteval(long refreshViewInteval) 
	{
		RefreshIntervalCache s = SessionBean.getInstance().getRefreshIntervalCache();
		s.put(this.getNodeId(), refreshViewInteval*1000);
	}

	/**
	 * 
	 * @return - the interval (seconds)
	 */
	public long getRefreshViewInteval() 
	{
		return getRefreshViewIntevalMS()/1000;
	}
	/**
	 * 
	 * @return - the interval (milliseconds)
	 */
	public long getRefreshViewIntevalMS() 
	{
		RefreshIntervalCache s = SessionBean.getInstance().getRefreshIntervalCache();
		return s.get(this.getNodeId());
	}
	
	public String getLogViewFd()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		return lvac.get(getNodeId()).getFd();
	}

	public void setLogViewFd(String fd)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(getNodeId());
		lva.setFd(fd);
	}

	public String getAllLogViewFd()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		return lvac.get(ALL_NODES).getFd();
	}

	public void setAllLogViewFd(String fd)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		lva.setFd(fd);
	}
	
	
	public String getLogViewTd()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		return lvac.get(getNodeId()).getTd();
	}

	public void setLogViewTd(String td)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(getNodeId());
		lva.setTd(td);
	}

	public String getAllLogViewTd()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		return lvac.get(ALL_NODES).getTd();
	}

	public void setAllLogViewTd(String td)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		lva.setTd(td);
	}
	
	public LogLevel getLogLevel()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(getNodeId());
		return lva.getLevel();
	}

	public LogLevel getAllLogLevel()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		return lva.getLevel();
	}
	
	public void setLogLevel(LogLevel level)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(getNodeId());
		lva.setLevel(level);
	}

	public void setAllLogLevel(LogLevel level)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		lva.setLevel(level);
	}
	
	public List<NodeLogRecord> getLogsForNode()
	{
		if(getNode().getParent()!=null)
		{
			LogsCache lvac = SessionBean.getInstance().getLogsCache();
			List<NodeLogRecord> ret = lvac.get(getNodeId());
			if(ret!=null) return ret;
		}
		return new ArrayList<NodeLogRecord>();
	}

	public List<NodeLogRecord> getLogsForAllNodes()
	{
		if(getNode().getParent()!=null)
		{
			LogsCache lvac = SessionBean.getInstance().getLogsCache();
			List<NodeLogRecord> ret = lvac.get(ALL_NODES);
			if(ret!=null) return ret;
		}
		return new ArrayList<NodeLogRecord>();
	}

	public List<LogByNode> getLogsGroupedByNodes()
	{
		return (new LogsByNodes(getLogsForAllNodes())).getAll();
	}
	
	public String clearLogForNode()
	{
		LogsCache lvac = SessionBean.getInstance().getLogsCache();
		lvac.remove(getNodeId());
		return null;
	}

	public String clearLogForAllNodes()
	{
		LogsCache lvac = SessionBean.getInstance().getLogsCache();
		lvac.remove(ALL_NODES);
		return null;
	}
	
	public void setGroupByNodes(boolean val)
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		lva.setGroupByNodes(val);
	}

	public boolean isGroupByNodes()
	{
		LogViewAttributesCache lvac = SessionBean.getInstance().getLogViewAttributesCache();
		LogViewAttributes lva = lvac.get(ALL_NODES);
		return lva.isGroupByNodes();
	}

	public static boolean isAutoRefresh(Node node)
	{
		if(node instanceof Viewable) 
		{
			Viewable v = (Viewable) node;
			Boolean b = v.getAutoRefresh();
			if(b!=null && b==false) return false;
		}
		return true;
	}
	
	public boolean isAutoRefresh()
	{
		return isAutoRefresh(getNode());
	}
	
	public boolean isShowVO() 
	{
		if(isRefreshPressed()) return true;
		return isAutoRefresh();
	}

	public boolean isRefreshPressed() {
		return refreshPressed;
	}

//	public void setRefreshPressed(boolean refreshPressed) {
//		this.refreshPressed = refreshPressed;
//	}
	
}
