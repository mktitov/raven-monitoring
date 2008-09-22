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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.core.layout.CoreShowDetailItem;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.Viewable;
import org.raven.tree.impl.NodeAttributeImpl;
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
    protected Logger logger = LoggerFactory.getLogger(NodeWrapper.class);	
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
	private boolean hasUnsavedChanges = false;
	private boolean needRefreshVO = false;
	//private int refreshViewInteval = 0;
		
	public NodeWrapper() 
	{
		super();
		SessionBean.initNodeWrapper(this);
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
		  return isViewable() || isChildViewable();
	}
	
	public boolean isNodeAndChildViewable()
	{
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
		Collection<Node> c = getNode().getSortedChildrens();
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
		String ret = (String) event.getReturnValue();
		if(ret!=null && ret.length()>0)
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
		RefreshAttributesStorage rs = sb.getRefreshAttributesStorage();
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
		List<ViewableObjectWrapper> x = SessionBean.getInstance().getViewableObjectsHash().getObjects(this);
		logger.info("+++ ");
		for(ViewableObjectWrapper v : x)
		{
			logger.info(" id="+v.getId()+"  group="+v.getMimeGroup());
		}
		logger.info("--- ");
		return x;
	}
	
	public void removeVOFromHash()
	{
		SessionBean.getInstance().getViewableObjectsHash().remove(this);
	}
	
	public void removeRAFromHash()
	{
		SessionBean.getInstance().getRefreshAttributesStorage().remove(this);
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
		editingAttrs = new ArrayList<Attr>();
		readOnlyAttributes = new ArrayList<NodeAttribute>();
		savedAttrs = new ArrayList<NodeAttribute>(); 
		for(NodeAttribute na : attrList)
		{	
			if(na.isReadonly()) readOnlyAttributes.add(na);
			else
			{
				savedAttrs.add(na);
				try {
					editingAttrs.add(new Attr(na));
				} catch (TooManyReferenceValuesException e) {
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
			  if( !at.isExpressionSupported() &&  !val.equals(at.getValue()) ) save |=1;
			  if( !dsc.equals(at.getDescription()) ) save |=2;
			  if( !na.getName().equals(at.getName()) ) save |=4;
			  if( at.isExpressionSupported() &&  ! ObjectUtils.equals(na.getRawValue(), at.getExpression()) ) save |=8;
              if( !ObjectUtils.equals(na.getValueHandlerType(), at.getValueHandlerType())) save |=16;
              if(at.isTemplateExpression() != na.isTemplateExpression()) save |=32;
              if(isHasUnsavedChanges()) save |= 4096; 
			  
			  if(save==0) continue;
			  try 
			  { 
				  if( (save&1) !=0 ) na.setValue(at.getValue());
				  if( (save&2) !=0 ) na.setDescription(at.getDescription());
				  if( (save&4) !=0 ) na.setName(at.getName());
				  if( (save&8) !=0 ) na.setValue(at.getExpression());
                  if( (save&16) !=0) na.setValueHandlerType(at.getValueHandlerType());
                  if( (save&32) !=0) na.setTemplateExpression(at.isTemplateExpression());
				  
				  if(write)
				  {
					  getConfigurator().getTreeStore().saveNodeAttribute(na);
					  setHasUnsavedChanges(false);
				  }	  
				  	else setHasUnsavedChanges(true);
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
			  catch(Throwable e) 
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
					  catch(Throwable e) 
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
		  setHasUnsavedChanges(false);
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
		showTab.setDisclosed(false);
		nodeEditTab.setDisclosed(true);
		treeEditTab.setDisclosed(true);
		setNode(n);
		return "";
	}
	
	public String onRefresh()
	{
		setNeedRefreshVO();
		SessionBean.getInstance().reloadRightFrame();
		return null; 
	}
	
	public void pollRefresh(PollEvent event)
	{
		onRefresh();
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

	public void setHasUnsavedChanges(boolean hasUnsavedChanges) {
		this.hasUnsavedChanges = hasUnsavedChanges;
	}

	public boolean isHasUnsavedChanges() {
		return hasUnsavedChanges;
	}

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

	public void setRefreshViewInteval(long refreshViewInteval) 
	{
		RefreshIntervalStorage s = SessionBean.getInstance().getRefreshIntervalStorage();
		s.setInterval(this, refreshViewInteval);
	}

	public long getRefreshViewInteval() 
	{
		RefreshIntervalStorage s = SessionBean.getInstance().getRefreshIntervalStorage();
		return s.getInterval(this);
	}

	public long getRefreshViewIntevalMS() 
	{
		return getRefreshViewInteval()*1000;
	}
	
	
}
