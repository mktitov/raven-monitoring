package org.raven.ui.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.raven.log.NodeLogRecord;

public class LogsByNodes 
{
	private HashMap<String,LogByNode> map = new HashMap<String, LogByNode>();
	
	public LogsByNodes(List<NodeLogRecord> recs)
	{
		putAll(recs);
	}
	
	private void put(NodeLogRecord rec)
	{
		String path = rec.getNodePath();
		LogByNode log = map.get(path);
		if(log==null) 
		{
			log = new LogByNode(path);
			map.put(path, log);
		}
		log.addRecord(rec);
	}
	
	private void putAll(List<NodeLogRecord> recs)
	{
		for(NodeLogRecord rec:  recs)
			put(rec);
	}

	public List<LogByNode> getAll()
	{
		return new ArrayList<LogByNode>(map.values());
	}
	
}
