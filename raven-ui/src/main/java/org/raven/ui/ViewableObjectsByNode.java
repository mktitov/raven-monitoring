package org.raven.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewableObjectsByNode 
{
	protected Logger logger = LoggerFactory.getLogger(ViewableObjectsByNode.class);
	private HashMap<Integer,List<ViewableObjectWrapper>> vomap = 
					new HashMap<Integer, List<ViewableObjectWrapper>>();
	
	
	private List<ViewableObjectWrapper> getObjectsByNode(NodeWrapper nw,boolean reload)
	{
		if(!nw.isViewable()) 
			return new ArrayList<ViewableObjectWrapper>(); 
		List<ViewableObjectWrapper> no = null;
		no = vomap.get(nw.getNodeId());
		if(reload) 
		{
			remove(nw);
			no = null;
		}	
		if(no==null) 
		{
			no = loadObjects(nw);
			vomap.put(nw.getNodeId(), no);
			logger.info("vomap put id="+nw.getNodeId()+" "+nw.getNodePath());
		}
		return no;
	}

	public List<ViewableObjectWrapper> getObjects(NodeWrapper nw)
	{
		logger.info("getObjects for"+nw.getNodePath());
		boolean reload = nw.isNeedRefreshVO();
		nw.setNeedRefreshVO(false);
//		if( ! vomap.containsKey(nw.getNodeId()) ) 
//			needReloadChildren = true;
		List<ViewableObjectWrapper> lst = getObjectsByNode(nw,reload);
		if(!nw.isChildViewable()) //.isNeedShowRefreshAttributes())
			return lst;
		//if(nw.isViewable())
		List<NodeWrapper> c = nw.getViewableChilddren();
		if(c==null)
			return lst;
		Iterator<NodeWrapper> it = c.iterator();
		while(it.hasNext())
		{
			NodeWrapper x = it.next();
			lst.addAll(getObjectsByNode(x,reload));
		}
		logger.info("getObjects found "+lst.size());
		return lst;
	}
	
	public void remove(NodeWrapper nw)
	{
		logger.info("remove "+nw.getNodePath());
		ViewableObjectsStorage vos = SessionBean.getInstance().getViewableObjectsStorage();
		List<ViewableObjectWrapper> wrList = vomap.remove(nw.getNodeId());
		if(wrList==null) return;
		for(ViewableObjectWrapper wr :  wrList)
			if(wr.isImage())
				vos.remove(wr.getId());
	}

	/**
	 * Loads viewable objects for the node, saves images in ViewableObjectsStorage.
	 * @return list of viewable objects 
	 */
	private List<ViewableObjectWrapper> loadObjects(NodeWrapper nw)
	{
		logger.info("loading objects for "+nw.getNodePath());
		Viewable viewable;
		List<ViewableObjectWrapper> vowl = new ArrayList<ViewableObjectWrapper>();
		vowl.add(0, new ViewableObjectWrapper(nw.getNode()) );
		if ( ! nw.isViewable() )
			return vowl;
		viewable = (Viewable) nw.getNode();
		List<ViewableObject> vol = null;
		try { vol = viewable.getViewableObjects(nw.getRefreshAttributesMap());}
			catch (Exception e) { logger.error("on load viewable objects: ",e);}
		if(vol==null)
			return vowl;
		ViewableObjectsStorage vos = SessionBean.getInstance().getViewableObjectsStorage();
		for(ViewableObject vo : vol)
		{
			ViewableObjectWrapper wr = new ViewableObjectWrapper(vo);
			if(wr.isImage()) 
				vos.put(wr);
			vowl.add(wr);
		}
		logger.info("loading objects: "+vowl.size());
		return vowl;
	}

}
