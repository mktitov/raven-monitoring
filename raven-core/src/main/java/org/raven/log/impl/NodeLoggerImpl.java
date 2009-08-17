/*
 *  Copyright 2008 Sergey Pinevskiy.
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
import javax.sql.rowset.CachedRowSet;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.raven.store.AbstractDbWorker;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sergey Pinevskiy
 */
public class NodeLoggerImpl extends AbstractDbWorker implements NodeLogger, Runnable
{
	private static Logger logger = LoggerFactory.getLogger(NodeLoggerImpl.class);
	private static final String NODES_MARKER = "#";
	
	private static final String[] sCreateLogTable = { 
		"create table @ ("+NodeLogRecord.FD+" timestamp not null,"+
		NodeLogRecord.NODE_ID+" int not null,"+
		NodeLogRecord.NODE_PATH+" varchar("+MAX_NODE_PATH_LENGTH+") ,"+
		NodeLogRecord.LEVEL+" int not null,"+
		NodeLogRecord.MESSAGE+" varchar("+NodeLogRecord.MAX_MESSAGE_LENGTH+") not null )",
		"create index @_"+NodeLogRecord.FD+" on @("+NodeLogRecord.FD+")",
		"create index @_"+NodeLogRecord.NODE_ID+" on @("+NodeLogRecord.NODE_ID+")",
		"create index @_"+NodeLogRecord.LEVEL+" on @("+NodeLogRecord.LEVEL+")"};

	private static final String orderBy = "order by "+NodeLogRecord.FD+" desc";
	private static final String sMainSelect = "select "+getFieldsList(NodeLogRecord.FIELDS)+" from @ where "+
	NodeLogRecord.FD+" between ? and ? and "+NodeLogRecord.LEVEL+" >= ? ";
	private static final String sSelLogsFromSingleTable =  
										sMainSelect+orderBy;
	private static final String sSelLogsFromSingleTableN = sMainSelect+ " and "+
	NodeLogRecord.NODE_ID+" = ? "+orderBy;
	private static final String sSelLogsFromSingleTableNN = sMainSelect+ " and "+
	NodeLogRecord.NODE_ID+" in("+NODES_MARKER+") "+orderBy;
//	private static final String sInsertToLog = "insert into @("+FIELDS+") values(?,?,?,?,?)";
	
    private NodeLoggerNode nodeLoggerNode;
//    private Queue<NodeLogRecord> queue;
    
    public NodeLoggerImpl()
    {
    	setMetaTableNamePrefix("nodeLogger");
    	setName("log");
    	setStoreDays(30);
    	init();
    }
    
    protected synchronized boolean dbWorkAllowed()
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
		writeToQueue(new NodeLogRecord(node.getId(),node.getPath(),level,message));
	}
	
/*
	protected boolean createTable(String tableName) 
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
*/
	private List<NodeLogRecord> selectLogRecordsST(String sql,Object[] args)
	{
		CachedRowSet crs = select(sql, args);
		ArrayList<NodeLogRecord> recs = new ArrayList<NodeLogRecord>(); 
        if(crs==null) return recs;
        try {
        	while(crs.next())
        	{ 
        		NodeLogRecord nlr = new NodeLogRecord();
        		nlr.setFd(crs.getDate(NodeLogRecord.FD).getTime());
        		nlr.setNodeId(crs.getInt(NodeLogRecord.NODE_ID));
        		nlr.setNodePath(crs.getString(NodeLogRecord.NODE_PATH));
        		nlr.setLevel(LogLevel.values()[crs.getInt(NodeLogRecord.LEVEL)]);
        		nlr.setMessage(crs.getString(NodeLogRecord.MESSAGE));
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
	
	@SuppressWarnings("unchecked")
	public List<NodeLogRecord> getRecords(Date fd, Date td, Object nodesId,LogLevel level) 
	{
		String sql;
		Object[] args;
		List<String> names;
		dbWorkAllowed();
		if(nodesId ==null)
		{
			sql = sSelLogsFromSingleTable;
			args = new Object[]{fd,td,level.ordinal()};
		}
		else
			if (nodesId instanceof List) 
			{
				List<Integer> nodes = (List) nodesId;
				StringBuffer lst = new StringBuffer("");
				for(Integer id : nodes)
				{
					if(lst.length()>0) lst.append(",");
					lst.append(id);
				}	
				sql = sSelLogsFromSingleTableNN.replaceFirst(NODES_MARKER, lst.toString());
				args = new Object[]{fd,td,level.ordinal()};
			}
			else
			{
				sql = sSelLogsFromSingleTableN;
				args = new Object[]{fd,td,level.ordinal(),(Integer)nodesId};
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

	protected String[] getFields() {
		return NodeLogRecord.FIELDS;
	}

	protected String[] getStCreateTable() {
		return sCreateLogTable;
	}
	
}
