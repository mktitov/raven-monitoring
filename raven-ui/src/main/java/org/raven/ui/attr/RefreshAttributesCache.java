package org.raven.ui.attr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.ui.node.NodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshAttributesCache 
{
	protected Logger logger = LoggerFactory.getLogger(RefreshAttributesCache.class);
	private static final int tryRemoveOldAfter = 100;
	private static final long howOld = 1000*60*60*24*2;
	private long accessCount = 0;
	private HashMap<Integer, StorageUnit> storage = 
						new HashMap<Integer, StorageUnit>();
	
	private void put(Integer id,Map<String,NodeAttribute> ra)
	{
		if(ra!=null)
		{
			storage.put(id, new StorageUnit(ra) );
			logger.info("put RA id="+id+" ra="+ra);
		}	
	}
	
	private Map<String,NodeAttribute> get(int id)
	{
		StorageUnit su = storage.get(id);
		if(su==null) return null;
		Map<String,NodeAttribute> ra = su.getMap();
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
	
	private void removeOld()
	{
		if(++accessCount < tryRemoveOldAfter) return;
		accessCount = 0;
		Iterator<Integer> it =  storage.keySet().iterator();
		ArrayList<Integer> killList = new ArrayList<Integer>(); 
		while(it.hasNext())
		{
			Integer i = it.next();
			if(System.currentTimeMillis() - storage.get(i).getLastAccess() > howOld)
			{
				logger.info("cur="+System.currentTimeMillis()+" la="+storage.get(i).getLastAccess());
				killList.add(i);
			}	
		}
		it =  killList.iterator();
		while(it.hasNext())
			remove(it.next());
	}

	private Map<String,NodeAttribute> getRA(Viewable viewable)
	{
		Map<String,NodeAttribute> ra = null;
		try { ra = viewable.getRefreshAttributes(); }
		catch (Exception e){logger.error("on load refresh attributes: ",e);}
		return ra;
	}
	
	public Map<String,NodeAttribute> get(NodeWrapper nw)
	{
		logger.info("get RA for node="+nw.getNodePath());
		if ( !nw.isViewable() ) return null;
		removeOld();
		Viewable viewable = (Viewable) nw.getNode();
		Map<String,NodeAttribute> ra = get(nw.getNodeId());
		boolean found = false;
		if(ra!=null)
		{
			found = true;
			logger.info("RA found in map: "+ra);
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
	

	private class StorageUnit
	{
		private Map<String,NodeAttribute> map = null;
		private long lastAccess = 0;
		
		private StorageUnit(Map<String,NodeAttribute> map)
		{
			this.map = map;
			setLastAccess();
		}
		
//		public void setMap(Map<String,NodeAttribute> map) 
//		{
//			this.map = map;
//		}
		public Map<String,NodeAttribute> getMap() 
		{
			setLastAccess();
			return map;
		}

		private void setLastAccess() 
		{
			this.lastAccess = System.currentTimeMillis();
		}

		public long getLastAccess() {
			return lastAccess;
		}
		
	}
	
}
