package org.raven.ui;

import java.util.HashMap;
import java.util.Map;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshAttributesStorage 
{
	protected Logger logger = LoggerFactory.getLogger(RefreshAttributesStorage.class);
	HashMap<Integer, Map<String,NodeAttribute>> storage = 
						new HashMap<Integer, Map<String,NodeAttribute>>();
	
	private void put(Integer id,Map<String,NodeAttribute> ra)
	{
		if(ra!=null)
		{
			storage.put(id, ra);
			logger.info("put RA id="+id+" ra="+ra);
		}	
	}
	
	private Map<String,NodeAttribute> get(int id)
	{
		Map<String,NodeAttribute> ra = storage.get(id);
		return ra;
	}

	private void remove(int id)
	{
		storage.remove(id);
		logger.info("remove RA id="+id);
	}

	public void remove(NodeWrapper nw)
	{
		remove(nw.getNodeId());
	}
	
	public Map<String,NodeAttribute> get(NodeWrapper nw)
	{
		logger.info("get RA for node="+nw.getNodePath());
		if ( !nw.isViewable() ) return null; 
		Viewable viewable = (Viewable) nw.getNode();
		Map<String,NodeAttribute> ra = get(nw.getNodeId());
		if(ra!=null)
		{
			logger.info("RA found in map: "+ra);
			return ra;
		}	
		try { ra = viewable.getRefreshAttributes(); }
		catch (Exception e) 
		{
			logger.error("on load refresh attributes: ",e);
			return null;
		}
		put(nw.getNodeId(), ra);
		return ra;
	}

	
}
