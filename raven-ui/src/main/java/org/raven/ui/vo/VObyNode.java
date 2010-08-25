/* 
 *  Copyright 2008 Sergey Pinevskiy.
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
package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.List;
import org.raven.cache.AbstractCache;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.ui.node.NodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
* @author Sergey Pinevskiy
*/
public class VObyNode extends AbstractCache<NodeWrapper, List<ViewableObjectWrapper>, Integer> 
{
	private static final Logger log = LoggerFactory.getLogger(VObyNode.class);
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
		log.info("loading objects for "+nwx.getNodePath());
		Viewable viewable;
		int uid = 2;
		List<ViewableObjectWrapper> vowl = new ArrayList<ViewableObjectWrapper>();
		//if ( ! nw.isViewable() )
		//			return vowl;
		//ViewableObjectWrapper wrp = new ViewableObjectWrapper(nwx.getNode());
		//NodeWrapper nw = nwx.getVoSourceNW();
		if ( ! nwx.isViewable() || ! nwx.isAllowNodeRead()) return vowl;
		if(!nwx.isAutoRefresh() && !nwx.isRefreshPressed()) 
				return vowl;
		viewable = (Viewable) nwx.getNode();
		List<ViewableObject> vol = null;
		
		try { vol = viewable.getViewableObjects(nwx.getRefreshAttributesMap());}
			catch (Exception e) { log.error("on load viewable objects: ",e);}
		if(vol==null)
		{
			//vowl.clear();
			return vowl;
		}	
		for(ViewableObject vo : vol)
		{
			ViewableObjectWrapper wr = new ViewableObjectWrapper(vo);
			wr.setUid(uid++);
			wr.setNodeId(nwx.getNodeId());
			if(wr.isImage()) 
				imagesStorage.put(wr);
			vowl.add(wr);
		}
		log.info("loading objects: "+vowl.size());
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
		log.info("getObjects for "+nw.getNodePath());
		boolean reload = nw.isNeedRefreshVO();
		boolean hideNN = nw.isHideNodeName();
		nw.setNeedRefreshVO(false);
		List<ViewableObjectWrapper> lst = new ArrayList<ViewableObjectWrapper>();
		
		if(!hideNN) {
			ViewableObjectWrapper wrp = new ViewableObjectWrapper(nw.getNode());
			wrp.setUid(1);
			wrp.setNodeId(nw.getNodeId());
			lst.add(0, wrp );
		}
		
		NodeWrapper nwx = nw.getVoSourceNW();
		
		List<ViewableObjectWrapper> no = nw.isShowVO()? get(nwx,reload) : getFromCacheOnly(nwx);
		if(no != null)	{
			log.info("from {} loaded {} VO ",nwx.getNodePath(),no.size());
			lst.addAll(no);
		}
		
		List<NodeWrapper> c = nwx.getChildrenList();
		if(c==null) return lst;
		
		for(NodeWrapper z : c)
		{
			if(!hideNN) {
				ViewableObjectWrapper wr = new ViewableObjectWrapper(z.getNode());
				wr.setUid(1);
				wr.setNodeId(z.getNodeId());
				lst.add( wr );
			}	
			NodeWrapper x = z.getVoSourceNW();
			List<ViewableObjectWrapper> zz = nw.isShowVO()? get(x,reload) : getFromCacheOnly(x);
			if(zz!=null) {
				lst.addAll(zz);
				log.info("from {} loaded2 {} VO ",x.getNodePath(),zz.size());
			}
		}
		log.info("getObjects for {} found: {} ",nw.getNodePath(),lst.size());
		return lst;
	}

	public void remove(NodeWrapper nw)
	{
		super.remove(nw.getVoSourceNW());
	}
	
	public void setImagesStorage(ImagesStorage imagesStorage) {
		this.imagesStorage = imagesStorage;
	}

	public ImagesStorage getImagesStorage() {
		return imagesStorage;
	}

}
