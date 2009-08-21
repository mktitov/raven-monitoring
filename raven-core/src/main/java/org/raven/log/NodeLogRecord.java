package org.raven.log;

import java.util.Comparator;

import org.raven.store.IRecord;
import org.raven.util.Utl;

public class NodeLogRecord implements Comparator<NodeLogRecord>, IRecord
{
	public static final int MAX_MESSAGE_LENGTH   = 10240;
	public static final int MAX_NODE_PATH_LENGTH =   512;
	public static final int MAX_SHORT_MES_LENGTH = 200;
	public static final String SHORT_MES_TAIL = "...";
	public static final String FD = "fd";
	public static final String NODE_ID = "nodeId";
	public static final String NODE_PATH = "nodePath";
	public static final String LEVEL = "level";
	public static final String MESSAGE = "message";
	public static final String[] FIELDS = {FD,NODE_ID,NODE_PATH,LEVEL,MESSAGE};	

	public static final String[] sCreateLogTable = { 
		"create table @ ("+NodeLogRecord.FD+" timestamp not null,"+
		NodeLogRecord.NODE_ID+" int not null,"+
		NodeLogRecord.NODE_PATH+" varchar("+MAX_NODE_PATH_LENGTH+") ,"+
		NodeLogRecord.LEVEL+" int not null,"+
		NodeLogRecord.MESSAGE+" varchar("+NodeLogRecord.MAX_MESSAGE_LENGTH+") not null )",
		"create index @_"+NodeLogRecord.FD+" on @("+NodeLogRecord.FD+")",
		"create index @_"+NodeLogRecord.NODE_ID+" on @("+NodeLogRecord.NODE_ID+")",
		"create index @_"+NodeLogRecord.LEVEL+" on @("+NodeLogRecord.LEVEL+")"};
	
	private long fd;
	private int nodeId;
	private LogLevel level;
	private String message;
	private String nodePath;
	
	public NodeLogRecord(int nodeId,String nodePath,LogLevel level,String message)
	{
		fd = System.currentTimeMillis();
		this.nodeId = nodeId;
		this.level = level;
		this.message = message;
		this.nodePath = nodePath;
	}

	public NodeLogRecord()
	{
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(""+fd);
		sb.append("; "+nodeId);
		sb.append("; "+nodePath);
		sb.append("; "+level);
		sb.append("; "+message);
		return sb.toString();
	}
	
	public long getFd() {
		return fd;
	}

	public String getFdString() 
	{
		return Utl.formatDate(fd);
	}
	
	public void setFd(long time) {
		this.fd = time;
	}
	public int getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	public LogLevel getLevel() {
		return level;
	}
	public void setLevel(LogLevel level) {
		this.level = level;
	}

	public String getMessage() {
		return message;
	}
	
	public String getShortMessage() 
	{
		if(message==null) return null;
		int len = message.length();
		if(len>MAX_SHORT_MES_LENGTH)
			return message.substring(0, MAX_SHORT_MES_LENGTH)+SHORT_MES_TAIL;
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}

	public String getNodePath() {
		return nodePath;
	}

	public int compare(NodeLogRecord o1, NodeLogRecord o2) {
		if(o1.fd > o2.fd) return -1;
		if(o1.fd < o2.fd) return 1;
		return 0;
	}

	public Object[] getDataForInsert() 
	{
		String mes = getMessage();
		mes = mes.substring(0, Math.min(MAX_MESSAGE_LENGTH,mes.length()));
		
		Object[] x = new Object[]{
				new java.sql.Timestamp(getFd()),
				new Integer(getNodeId()),
				getNodePath(),
				new Integer(getLevel().ordinal()),
				mes
		};		
		return x;
	}
	
	
}
