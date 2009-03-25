package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.ui.node.NodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewableObjectsByNode 
{
	private static Logger logger = LoggerFactory.getLogger(ViewableObjectsByNode.class);
	private static final int tryRemoveOldAfter = 100;
	private static final long howOld = 1000*60*60*2;
	private long accessCount = 0;
	private HashMap<Integer,List<ViewableObjectWrapper>> vomap = 
					new HashMap<Integer, List<ViewableObjectWrapper>>();
	private ImagesStorage viewableObjectsStorage;
	
	private List<ViewableObjectWrapper> getObjectsByNode(NodeWrapper nw,boolean reload)
	{
		if(!nw.isViewable()) 
			return new ArrayList<ViewableObjectWrapper>();
		removeOld();
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
		logger.info("getObjects for "+nw.getNodePath());
		boolean reload = nw.isNeedRefreshVO();
		nw.setNeedRefreshVO(false);
//		if( ! vomap.containsKey(nw.getNodeId()) ) 
//			needReloadChildren = true;
		List<ViewableObjectWrapper> lst = new ArrayList<ViewableObjectWrapper>(); 
		List<ViewableObjectWrapper> no = getObjectsByNode(nw,reload);
		if(no != null)
		{
			logger.info("from {} loaded {} VO ",nw.getNodePath(),no.size());
			lst.addAll(no);
		}
		if(!nw.isChildViewable()) //.isNeedShowRefreshAttributes())
		{
			logger.info("Node {} has not viewable children",nw.getNodePath());
			return lst;
		}	
		//if(nw.isViewable())
		List<NodeWrapper> c = nw.getViewableChilddren();
		if(c==null)
			return lst;
		logger.info("Node {} has {} viewable children",nw.getNodePath(),c.size());
		Iterator<NodeWrapper> it = c.iterator();
		while(it.hasNext())
		{
			NodeWrapper x = it.next();
//			lst.addAll(getObjectsByNode(x,reload));
			List<ViewableObjectWrapper> zz = getObjectsByNode(x,reload);
			logger.info("from {} loaded2 {} VO ",x.getNodePath(),zz.size());
			lst.addAll(zz);
			
		}
		logger.info("getObjects found "+lst.size());
		logger.info("end getObjects for "+nw.getNodePath());
		return lst;
	}

	public void remove(int id)
	{
		logger.info("remove by id="+id);
		//ViewableObjectsStorage vos = SessionBean.getInstance().getViewableObjectsStorage();
		List<ViewableObjectWrapper> wrList = vomap.remove(id);
		if(wrList==null) return;
		for(ViewableObjectWrapper wr :  wrList)
			if(wr.isImage())
				viewableObjectsStorage.remove(wr.getId());
	}
	
	public void remove(NodeWrapper nw)
	{
		remove(nw.getNodeId());
	}

	private void removeOld()
	{
		if(++accessCount < tryRemoveOldAfter) return;
		accessCount = 0;
		Iterator<Integer> it =  vomap.keySet().iterator();
		ArrayList<Integer> killList = new ArrayList<Integer>(); 
		while(it.hasNext())
		{
			Integer i = it.next();
			ViewableObjectWrapper w = vomap.get(i).get(0);
			if(w==null || System.currentTimeMillis() - w.getFd() > howOld)
				killList.add(i);
		}
		it =  killList.iterator();
		while(it.hasNext())
			remove(it.next());
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
		//ViewableObjectsStorage vos = SessionBean.getInstance().getViewableObjectsStorage();
		for(ViewableObject vo : vol)
		{
			ViewableObjectWrapper wr = new ViewableObjectWrapper(vo);
			if(wr.isImage()) 
				viewableObjectsStorage.put(wr);
			vowl.add(wr);
		}
		logger.info("loading objects: "+vowl.size());
		return vowl;
	}

	public void setViewableObjectsStorage(ImagesStorage viewableObjectsStorage) {
		this.viewableObjectsStorage = viewableObjectsStorage;
	}

	public ImagesStorage getViewableObjectsStorage() {
		return viewableObjectsStorage;
	}

}
