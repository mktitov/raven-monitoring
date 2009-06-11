package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.List;
import org.raven.cache.AbstractCache;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.ui.node.NodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VObyNode extends AbstractCache<NodeWrapper, List<ViewableObjectWrapper>, Integer> 
{
	private static final Logger logger = LoggerFactory.getLogger(VObyNode.class);
	private ImagesStorage imagesStorage;
	
	public VObyNode()
	{
		setDeleteAfter(1000*60*60*2);
		setUpdateTimeOnGet(false);
	}
	
	/**
	 * Loads viewable objects for the node, saves images in ViewableObjectsStorage.
	 * @return list of viewable objects 
	 */
	protected List<ViewableObjectWrapper> getValue(NodeWrapper nwx)
	{
		logger.info("loading objects for "+nwx.getNodePath());
		Viewable viewable;
		int uid = 1;
		List<ViewableObjectWrapper> vowl = new ArrayList<ViewableObjectWrapper>();
		//if ( ! nw.isViewable() )
		//			return vowl;
		ViewableObjectWrapper wrp = new ViewableObjectWrapper(nwx.getNode());
		wrp.setUid(uid++);
		wrp.setNodeId(nwx.getNodeId());
		vowl.add(0, wrp );
		NodeWrapper nw = new NodeWrapper(nwx.getVoSource());
		if ( ! nw.isViewable() ) return vowl;
		viewable = (Viewable) nw.getNode();
		List<ViewableObject> vol = null;
		try { vol = viewable.getViewableObjects(nw.getRefreshAttributesMap());}
			catch (Exception e) { logger.error("on load viewable objects: ",e);}
		if(vol==null)
		{
			//vowl.clear();
			return vowl;
		}	
		for(ViewableObject vo : vol)
		{
			ViewableObjectWrapper wr = new ViewableObjectWrapper(vo);
			wr.setUid(uid++);
			wr.setNodeId(nw.getNodeId());
			if(wr.isImage()) 
				imagesStorage.put(wr);
			vowl.add(wr);
		}
		logger.info("loading objects: "+vowl.size());
		return vowl;
	}
	
	protected Integer getStoreKey(NodeWrapper key)
	{
		return key.getNodeId(); 
	}
	
	protected void afterRemove(List<ViewableObjectWrapper> value)
	{
		if(value==null) return;
		for(ViewableObjectWrapper wr :  value)
			if(wr.isImage())
				imagesStorage.remove(wr.getId());
	}
	
	public List<ViewableObjectWrapper> getObjects(NodeWrapper nw)
	{
		logger.info("getObjects for "+nw.getNodePath());
		boolean reload = nw.isNeedRefreshVO();
		nw.setNeedRefreshVO(false);
		List<ViewableObjectWrapper> lst = new ArrayList<ViewableObjectWrapper>();
		if(!nw.isShowVO())
		{
			List<ViewableObjectWrapper> no = getFromCacheOnly(nw);
			if(no != null) lst.addAll(no);
			return lst;
		}	
		
		List<ViewableObjectWrapper> no = get(nw,reload);
		if(no != null)
		{
			logger.info("from {} loaded {} VO ",nw.getNodePath(),no.size());
			lst.addAll(no);
		}
		
	//	if(!nw.isChildViewable()) 
	//	{
	//		logger.info("Node {} has not viewable children",nw.getNodePath());
	//		return lst;
	//	}	
	//	List<NodeWrapper> c = nw.getViewableChilddren();
		List<NodeWrapper> c = nw.getChildrenList();
		if(c==null) return lst;
		//logger.info("Node {} has {} viewable children",nw.getNodePath(),c.size());
		boolean arFlag = false;
		for(NodeWrapper x : c)
		{
			if(!nw.isRefreshPressed() && !x.isAutoRefresh()) arFlag = true; //continue;
			List<ViewableObjectWrapper> zz = get(x,reload);
			int cnt = 0;
			for(ViewableObjectWrapper wr: zz)
			{
				if(arFlag && !wr.isNodeUrl()) continue;
				lst.add(wr);
				cnt++;
			}
			logger.info("from {} loaded2 {} VO ",x.getNodePath(),cnt);
		}
		logger.info("getObjects for {} found: {} ",nw.getNodePath(),lst.size());
		return lst;
	}

	public void setImagesStorage(ImagesStorage imagesStorage) {
		this.imagesStorage = imagesStorage;
	}

	public ImagesStorage getImagesStorage() {
		return imagesStorage;
	}

}
