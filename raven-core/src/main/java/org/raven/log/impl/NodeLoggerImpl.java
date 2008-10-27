/*
 *  Copyright 2008 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.log.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sql.rowset.CachedRowSet;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class NodeLoggerImpl extends LogTablesManager implements NodeLogger, Runnable
{
	private static Logger logger = LoggerFactory.getLogger(NodeLoggerImpl.class);
//	public static final String DATE_FORMAT = "yyyyMMdd";
//	private static SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
	private static final int MAX_MESSAGE_LENGTH = 2048;
	private static final int MAX_NODE_PATH_LENGTH = 256;
	private static final String FD = "fd";
	private static final String NODE_ID = "nodeId";
	private static final String NODE_PATH = "nodePath";
	private static final String LEVEL = "level";
	private static final String MESSAGE = "message";
	private static final String FIELDS = FD+","+NODE_ID+","+NODE_PATH+","+LEVEL+","+MESSAGE;
	
	private static final String[] sCreateLogTable = { 
		"create table @ ("+FD+" timestamp not null,"+
			NODE_ID+" int not null,"+
			NODE_PATH+" varchar("+MAX_NODE_PATH_LENGTH+") ,"+
			LEVEL+" int not null,"+
			MESSAGE+" varchar("+MAX_MESSAGE_LENGTH+") not null )",
		"create index @_"+FD+" on @("+FD+")",
		"create index @_"+NODE_ID+" on @("+NODE_ID+")",
		"create index @_"+LEVEL+" on @("+LEVEL+")"};

	private static final String orderBy = "order by "+FD+" desc";
	private static final String sMainSelect = "select "+FIELDS+" from @ where "+
												FD+" between ? and ? and "+LEVEL+" >= ? ";
	private static final String sSelLogsFromSingleTable =  
										sMainSelect+orderBy;
	private static final String sSelLogsFromSingleTableN = sMainSelect+ " and "+
										NODE_ID+" = ? "+orderBy;
	private static final String sInsertToLog = "insert into @("+FIELDS+") values(?,?,?,?,?)";
	
    private NodeLoggerNode nodeLoggerNode;
    private Queue<NodeLogRecord> queue;
    
    public NodeLoggerImpl()
    {
    	queue = new ConcurrentLinkedQueue<NodeLogRecord>();
    	Thread x = new Thread(this);
    	x.start();
    	logger.warn("nodeLogger started !");
    }
    
    private synchronized boolean logAllowed()
    {
    	if(nodeLoggerNode==null || nodeLoggerNode.getStatus()!=Node.Status.STARTED)
    	{
    		setPool(null);
    		return false;
    	}
   		if(getPool()==null)
   			setPool(nodeLoggerNode.getConnectionPool());
   		if(getPool()==null || getPool().getStatus()!=Node.Status.STARTED)
   			return false;
    	return true;
    }

	public void write(Node node, LogLevel level, String message) 
	{
		if(logAllowed())
			queue.offer(new NodeLogRecord(node.getId(),node.getPath(),level,message));
	}

	protected boolean createLogTable(String tableName) 
	{
		return createTable(sCreateLogTable, tableName);
	}
	
	private boolean insert(NodeLogRecord rec)
	{
		String mes = rec.getMessage();
		mes = mes.substring(0, Math.min(MAX_MESSAGE_LENGTH,mes.length()));
		String tname = getTableName(rec);
		String sql = sInsertToLog.replaceAll(TABLE_MARKER, tname);
		
		Object[] x = new Object[]{
				new java.sql.Timestamp(rec.getFd()),
				new Integer(rec.getNodeId()),
				rec.getNodePath(),
				new Integer(rec.getLevel().ordinal()),
				mes
		}; 
		if( executeUpdate(sql, x)<0 ) return false;
		return true;
	}
	
	private boolean writeMessagesFromQueue()
	{
		NodeLogRecord rec;
		while( (rec=queue.poll())!=null )
		{
			insert(rec);
		}
		return true;
	}
	
	public void run()
	{
		while(true)
		{
			writeMessagesFromQueue();
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		}
	}

	private List<NodeLogRecord> selectLogRecordsST(String sql,Object[] args)
	{
		CachedRowSet crs = select(sql, args);
		ArrayList<NodeLogRecord> recs = new ArrayList<NodeLogRecord>(); 
        if(crs==null) return recs;
        try {
        	while(crs.next())
        	{ 
        		NodeLogRecord nlr = new NodeLogRecord();
        		nlr.setFd(crs.getDate(FD).getTime());
        		nlr.setNodeId(crs.getInt(NODE_ID));
        		nlr.setNodePath(crs.getString(NODE_PATH));
        		nlr.setLevel(LogLevel.values()[crs.getInt(LEVEL)]);
        		nlr.setMessage(crs.getString(MESSAGE));
        		recs.add(nlr);
        	}
        } catch(SQLException e) {logger.error("on selectLogRecordsST:",e);}
        return recs;
	}
	
	private List<NodeLogRecord> selectLogRecordsMT(List<String> tables, String sql, Object[] args)
	{
		ArrayList<NodeLogRecord> recs = new ArrayList<NodeLogRecord>();
		for(String tableName : tables)
		{
			String sqlx = sql.replaceAll(TABLE_MARKER, tableName);
			recs.addAll(selectLogRecordsST(sqlx, args));
		}
		return recs;
	}
	
	public List<NodeLogRecord> getRecords(Date fd, Date td, Integer nodeId,LogLevel level) 
	{
		String sql;
		Object[] args;
		List<String> names;
		logAllowed();
		if(nodeId ==null)
		{
			sql = sSelLogsFromSingleTable;
			args = new Object[]{fd,td,level.ordinal()};
		}
		else
		{
			sql = sSelLogsFromSingleTableN;
			args = new Object[]{fd,td,level.ordinal(),nodeId};
		}
		names = getTablesNames(fd.getTime(), td.getTime());
		return selectLogRecordsMT(names, sql, args);
	}
	
    public synchronized void setNodeLoggerNode(NodeLoggerNode nodeLoggerNode)
    {
        this.nodeLoggerNode = nodeLoggerNode;
    }

    public synchronized NodeLoggerNode getNodeLoggerNode()
    {
        return nodeLoggerNode;
    }
	
}
