package org.raven.ui.log;

import java.util.Date;
import java.util.List;
import org.raven.cache.AbstractCache;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.weda.internal.annotations.Service;

public class LogsCache extends AbstractCache<Integer,List<NodeLogRecord>>
{
	@Service
	private NodeLogger nodeLogger;
	private LogViewAttributesCache logViewAttributesStorage;  
	
	public LogsCache(LogViewAttributesCache as)
	{
		super();
		logViewAttributesStorage = as;
		setCheckInterval(1000*60*5);
		setDeleteAfter(1000*60*60*2);
	}
	
	protected List<NodeLogRecord> getValue(Integer key) 
	{
		LogViewAttributes lva = logViewAttributesStorage.get(key);
		Date fd = new Date( lva.getFdTime() );
		Date td = new Date( lva.getTdTime() );
		List<NodeLogRecord> ret;
		if(key.intValue()!=-1) ret = nodeLogger.getRecords(fd, td, key, lva.getLevel());
			else ret = nodeLogger.getRecords(fd, td, null, lva.getLevel());
		return ret;
	}

	public void setLogViewAttributesStorage(LogViewAttributesCache logViewAttributesStorage) {
		this.logViewAttributesStorage = logViewAttributesStorage;
	}

	public LogViewAttributesCache getLogViewAttributesStorage() {
		return logViewAttributesStorage;
	}

}
