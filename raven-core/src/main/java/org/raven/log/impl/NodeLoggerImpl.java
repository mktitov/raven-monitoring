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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
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
public class NodeLoggerImpl extends AbstractDbWorker<NodeLogRecord> implements NodeLogger, Runnable
{
	private static Logger logger = LoggerFactory.getLogger(NodeLoggerImpl.class);
	private static final String NODES_MARKER = "#";

	private static final String orderBy = "order by "+NodeLogRecord.FD+" desc";
	private static final String sMainSelect = "select "+getFieldsList(NodeLogRecord.FIELDS)+" from @ where "+
	NodeLogRecord.FD+" between ? and ? and "+NodeLogRecord.LEVEL+" >= ? ";
	private static final String sSelLogsFromSingleTable =  
										sMainSelect+orderBy;
	private static final String sSelLogsFromSingleTableN = sMainSelect+ " and "+
	NodeLogRecord.NODE_ID+" = ? "+orderBy;
	private static final String sSelLogsFromSingleTableNN = sMainSelect+ " and "+
	NodeLogRecord.NODE_ID+" in("+NODES_MARKER+") "+orderBy;
	
    public NodeLoggerImpl()
    {
    	setMetaTableNamePrefix("log");
    	setName("nodeLogger");
    	setStoreDays(30);
    	init();
    }
    
	protected NodeLogRecord getObjectFromRecord(ResultSet rs) throws SQLException 
	{
		return NodeLogRecord.getObjectFromRecord(rs);
	}

	public void write(Node node, LogLevel level, String message) 
	{
		write(new NodeLogRecord(node.getId(),node.getPath(),level,message));
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
		return selectObjectsMT(names, sql, args);
	}
	
    public synchronized void setNodeLoggerNode(NodeLoggerNode nodeLoggerNode)
    {
        setNode(nodeLoggerNode);
    }

    public synchronized NodeLoggerNode getNodeLoggerNode()
    {
    	try { return (NodeLoggerNode) getNode(); }
    	catch(Exception e) { logger.error("Xmm...",e); }
    	return null;
    }

	protected String[] getFields() {
		return NodeLogRecord.FIELDS;
	}

	protected String[] getStCreateTable() {
		return NodeLogRecord.sCreateLogTable;
	}
	
}
