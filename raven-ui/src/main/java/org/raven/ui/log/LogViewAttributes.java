package org.raven.ui.log;

import org.jrobin.core.RrdException;
import org.jrobin.core.Util;
import org.raven.log.LogLevel;
import org.raven.util.Utl;
import java.util.Date;

public class LogViewAttributes 
{
	private String fd;
	private String td;
	private LogLevel level;
	private boolean groupByNodes = false;

	public LogViewAttributes()
	{
		fd = "now-1d";
		td = "now";
		level = LogLevel.WARN;
	}

	public long getDefaultFdTime()
	{
		return System.currentTimeMillis()-1000*60*60*24;
	}

	public long getDefaultTdTime()
	{
		return System.currentTimeMillis();
	}

	public long convert(String dt)
	{
		try { return Util.getTimestamp(dt)*1000; }
		catch (RrdException e) 
		{
			Date d = Utl.parseDate(dt);
			if(d==null) return getDefaultFdTime();
			return d.getTime();
		}
	}
	
	public long getFdTime()
	{
		return convert(fd);
	}

	public long getTdTime()
	{
		return convert(td);
	}
	
	public String getFd() {
		return fd;
	}
	public void setFd(String fd) {
		this.fd = fd;
	}
	
	public String getTd() {
		return td;
	}
	public void setTd(String td) {
		this.td = td;
	}
	
	public LogLevel getLevel() {
		return level;
	}
	public void setLevel(LogLevel level) {
		this.level = level;
	}
	
	public boolean isLevelTrace()
	{
		return level == LogLevel.TRACE;
	}

	public boolean isLevelDebug()
	{
		return level == LogLevel.DEBUG;
	}

	public boolean isLevelInfo()
	{
		return level == LogLevel.INFO;
	}

	public boolean isLevelWarn()
	{
		return level == LogLevel.WARN;
	}

	public boolean isLevelError()
	{
		return level == LogLevel.ERROR;
	}

	public void setGroupByNodes(boolean groupByNodes) {
		this.groupByNodes = groupByNodes;
	}

	public boolean isGroupByNodes() {
		return groupByNodes;
	}
	
}
