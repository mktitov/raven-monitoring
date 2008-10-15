package org.raven.ui.cache;

import java.util.List;

import org.raven.cache.AbstractCache;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.weda.internal.annotations.Service;

public class LogsCache extends AbstractCache<Long,List<NodeLogRecord>>
{
	@Service
	private NodeLogger nodeLogger;
	
	@Override
	protected List<NodeLogRecord> getValue(Long key) 
	{
		//nodeLogger.
		//this.p
		return null;
	}
	

}
