package org.raven.log;

public class NodeLogRecord 
{
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
	public void setMessage(String message) {
		this.message = message;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}

	public String getNodePath() {
		return nodePath;
	}

	
}
