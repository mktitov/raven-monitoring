package org.raven.log;

import java.util.Comparator;

import org.raven.util.Utl;

public class NodeLogRecord implements Comparator<NodeLogRecord	>
{
	public static final int MAX_SHORT_MES_LENGTH = 200;
	public static final String SHORT_MES_TAIL = "...";
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

	
}
