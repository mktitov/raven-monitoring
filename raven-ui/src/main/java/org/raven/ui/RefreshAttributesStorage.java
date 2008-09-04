package org.raven.ui;

import java.util.HashMap;
import java.util.Map;
import org.raven.tree.NodeAttribute;

public class RefreshAttributesStorage 
{

	HashMap<String, Map<String,NodeAttribute>> storage = new HashMap<String, Map<String,NodeAttribute>>();
	
	public void put(String id,Map<String,NodeAttribute> ra)
	{
		if(ra!=null)
			storage.put(id, ra);
	}
	
	public Map<String,NodeAttribute> get(String id)
	{
		Map<String,NodeAttribute> ra = storage.get(id);
		return ra;
	}

	public void remove(String id)
	{
		storage.remove(id);
	}
	
}
