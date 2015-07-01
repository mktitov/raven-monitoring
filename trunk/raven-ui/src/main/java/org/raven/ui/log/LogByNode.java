package org.raven.ui.log;

import org.raven.log.NodeLogRecord;

public class LogByNode 
{
	private String nodePath = null;
	private LogRecordTable records = new LogRecordTable();
	
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
	public LogRecordTable getRecords() {
		return records;
	}
//	public void setRecords(List<NodeLogRecord> records) {
//		this.records = records;
//	}
	
}
