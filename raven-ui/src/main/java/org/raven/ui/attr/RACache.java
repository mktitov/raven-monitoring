package org.raven.ui.attr;

import java.util.Map;
import org.raven.cache.AbstractCache;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.ui.node.NodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RACache extends AbstractCache<NodeWrapper,Map<String,NodeAttribute>,Integer> 
{
	private static Logger logger = LoggerFactory.getLogger(RACache.class);
	
	public RACache()
	{
		super();
		setCheckInterval(1000*60*3);
		setDeleteAfter(1000*60*60*6);
		setUpdateTimeOnGet(true);
	}

	protected void afterRemove(Map<String, NodeAttribute> value) {}

	protected Integer getStoreKey(NodeWrapper key) 
	{
		return key.getNodeId();
	}

	protected Map<String, NodeAttribute> getValue(NodeWrapper key){return null;} 
	
	/*
	protected Map<String, NodeAttribute> getValue(NodeWrapper key) 
	{
		//logger.info("get RA for node="+nw.getNodePath());
		if ( !key.isViewable() ) return null;
		Viewable viewable = (Viewable) key.getNode();
		Map<String,NodeAttribute> ra = get(key.getNodeId());
		boolean found = false;
		if(ra!=null)
		{
			found = true;
			//logger.info("RA found in map: "+ra);
			//return ra;
			Map<String,NodeAttribute> rb = getRA(viewable);
			if(rb==null || rb.size()==0)
			{
				remove(nw);
				ra = null;
			}	
			else
			{
				for(String name : rb.keySet())
					if(!ra.containsKey(name)) 
						ra.put(name, rb.get(name));
				for(Iterator<String> it=ra.keySet().iterator();it.hasNext();)
					if(!rb.containsKey(it.next())) 
						it.remove();
			}	
		} else ra = getRA(viewable);
		if(!found) put(nw.getNodeId(), ra);
		return ra;
	}
*/
	private Map<String,NodeAttribute> getRA(Viewable viewable)
	{
		Map<String,NodeAttribute> ra = null;
		try { ra = viewable.getRefreshAttributes(); }
		catch (Exception e){logger.error("on load refresh attributes: ",e);}
		return ra;
	}	
	
}
