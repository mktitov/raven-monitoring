package org.raven.ui.log;

import java.util.ArrayList;
import java.util.List;
import org.raven.log.NodeLogRecord;

public class LogByNode 
{
	private String nodePath = null;
	private List<NodeLogRecord> records = new ArrayList<NodeLogRecord>();
	
	public LogByNode(String nodePath)
	{
		this.nodePath = nodePath;
	}
	
	public void addRecord(NodeLogRecord rec)
	{
		records.add(rec);
	}
	
	public String getNodePath() {
		return nodePath;
	}
	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}
	public List<NodeLogRecord> getRecords() {
		return records;
	}
	public void setRecords(List<NodeLogRecord> records) {
		this.records = records;
	}
	
}
