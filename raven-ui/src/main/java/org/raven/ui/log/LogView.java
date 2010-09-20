package org.raven.ui.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.model.SelectItem;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.ui.SessionBean;
import org.raven.ui.node.INodeScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogView
{
    protected static final Logger log = LoggerFactory.getLogger(LogView.class);
    public static final int ALL_NODES = -1;

    public static final SelectItem[] logsSI = 
    { 
    	new SelectItem(LogLevel.TRACE),
		new SelectItem(LogLevel.DEBUG),	
		new SelectItem(LogLevel.INFO),	
		new SelectItem(LogLevel.WARN),	
		new SelectItem(LogLevel.ERROR)	
	};
    
	private LogViewAttributes attrs;
	private int nodeId;
	private INodeScanner scanner;
	private LogRecordTable logRecordTable = new LogRecordTable();
	private boolean reload = false;

	public LogView(LogViewAttributes lvAttrs, int nodeId, INodeScanner scan)
	{
		attrs = lvAttrs;
		this.nodeId = nodeId;
		scanner = scan;
	}
	
	public SelectItem[] getLogLevelSelectItems() { 
		return logsSI; 
    }

	private List<NodeLogRecord> getLogsForNodes(List<Integer> idList)
	{
		List<NodeLogRecord> ret = new ArrayList<NodeLogRecord>();
//		if(nodeId != ROOT_NODE) {
		LogsCache lc = SessionBean.getLogsCacheS();
		for(Integer id : idList) {
			List<NodeLogRecord> x = lc.get(id,reload);
			if(x!=null) ret.addAll(x);
		}
//		}
		if(ret.size()>0 && ret.get(0)!=null) Collections.sort(ret, ret.get(0));
		return ret;
	}
	
	public List<NodeLogRecord> getLogsForNode()
	{
		List<NodeLogRecord> res;
		List<Integer> idList = new ArrayList<Integer>();
		if(!isFindRecursive()) 
			idList.add(nodeId);
		 else
			 if(scanner!=null)
				 idList = scanner.scan();
		res = getLogsForNodes(idList);
		return filterLogRecords(res);
	}
	
	public String loadLogForNodes()
	{
		logRecordTable.clear();
		//LogsCache lc = SessionBean.getLogsCacheS();
		//lc.remove(nodeId);
		//List<NodeLogRecord> lst = lc.get(nodeId); 
		//if(lst!=null) 
		reload = true;
		logRecordTable.addAll( getLogsForNode() );
		reload = false;
		return null;
	}
	
	public List<LogByNode> getLogsGroupedByNodes()
	{
		return (new LogsByNodes(getLogsForNode())).getAll();
	}
	
	public String clearLogForNode()
	{
		SessionBean.getLogsCacheS().remove(nodeId);
		return null;
	}
	
	public String getFd() {
		return attrs.getFd();
	}
	public void setFd(String fd) {
		attrs.setFd(fd);
	}
	
	public String getTd() {
		return attrs.getTd();
	}
	public void setTd(String td) {
		attrs.setTd(td);
	}
	
	public LogLevel getLevel() { 
		return attrs.getLevel();
	}
	public void setLevel(LogLevel level) {
		attrs.setLevel(level);
	}

	public List<NodeLogRecord> filterByRegExp(List<NodeLogRecord> res, String regExp)
	{
		List<NodeLogRecord> ret = new ArrayList<NodeLogRecord>();
		Pattern p = Pattern.compile(regExp);
		for(NodeLogRecord r : res) 
		{
			Matcher m = p.matcher(r.getMessage());
			if(m.find())
				ret.add(r);
		}
		return ret;
	}

	public List<NodeLogRecord> filterByString(List<NodeLogRecord> res, String pattern)
	{
		List<NodeLogRecord> ret = new ArrayList<NodeLogRecord>();
		for(NodeLogRecord r : res)
			if(r.getMessage().indexOf(pattern)!=-1)
				ret.add(r);
		return ret;
	}
	
	public void setGroupByNodes(boolean val) {
		attrs.setGroupByNodes(val);
	}

	public boolean isGroupByNodes() {
		return attrs.isGroupByNodes();
	}

	public String getMessageFilter() {
		return attrs.getMesFilter();
	}

	public void setMessageFilter(String f) {
		attrs.setMesFilter(f);
	}

	public boolean isFilterRegExp() {
		return attrs.isRegExp();
	}

	public void setFilterRegExp(boolean f) {
		attrs.setRegExp(f);
	}

	public boolean isFindRecursive() { 
		return attrs.isFindRecursive();
	}

	public void setFindRecursive(boolean f) {
		attrs.setFindRecursive(f);
	}

	public boolean isFilterOn() {
		return attrs.isFilterOn();
	}

	public void setFilterOn(boolean f) {
		attrs.setFilterOn(f);
	}

	public String filterEnable()
	{
		setFilterOn(true);
		return null;
	}
	
	public String filterDisable()
	{
		setFilterOn(false);
		return null;
	}
	
	public List<NodeLogRecord> filterLogRecords(List<NodeLogRecord> res)
	{
		String mf = getMessageFilter().trim();
		if(isFilterOn() && !mf.isEmpty() )
		{
			if(isFilterRegExp()) res = filterByRegExp(res, mf);
			else res = filterByString(res, mf);
		}
		return res;
	}

	public LogRecordTable getLogRecordTable() {
		return logRecordTable;
	}
	
}
