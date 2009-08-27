package org.raven.audit.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.raven.audit.Action;
import org.raven.audit.ActionType;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.log.NodeLogRecord;
import org.raven.store.AbstractDbWorker;
import org.raven.store.SqlQuery;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class AuditorImpl extends AbstractDbWorker<AuditRecord> implements Auditor 
{
	private static Logger logger = LoggerFactory.getLogger(AuditorImpl.class);
	
//	private static final String NODES_MARKER = "#";

	private static final String orderBy = "order by "+NodeLogRecord.FD+" desc";
	private static final String sMainSelect = "select "+getFieldsList(AuditRecord.FIELDS)+" from @ where ";
	//AuditRecord.FD+" between ? and ? ";
//			"//"+AuditRecord.LEVEL+" >= ? ";
	
	
    public AuditorImpl()
    {
    	setMetaTableNamePrefix("audit");
    	setName("auditor");
    	setStoreDays(360);
    	super.init();
    }

	public List<AuditRecord> getRecords(Date from, Date to, Object nodeId,
			Object nodePath, String login, ActionType type, Action action) 
	{
		List<AuditRecord> empty = new ArrayList<AuditRecord>();
		if(from==null || to==null) return empty;
		SqlQuery sql = new SqlQuery(sMainSelect);
		
		sql.appendBetween(AuditRecord.FD, from, to);
		sql.appendInteger(AuditRecord.NODE_ID,nodeId);
		sql.appendLike(AuditRecord.LOGIN, login);
		if(type!=null)
			sql.appendInteger(AuditRecord.ACTION_TYPE ,type.ordinal());
		if(action!=null)
			sql.appendInteger(AuditRecord.ACTION ,action.ordinal());
		
		if (nodePath!=null && nodePath instanceof String) 
		{
			sql.appendLike(AuditRecord.NODE_PATH, (String) nodePath);
		}
		
		sql.appendString(orderBy);
		List<String> names = getTablesNames(from.getTime(), to.getTime());
		return selectObjectsMT(names, sql.toString(), sql.getValues());
	}
  
	public AuditRecord getObjectFromRecord(ResultSet rs) throws SQLException
	{
		return AuditRecord.getObjectFromRecord(rs);
	}
	
	public void write(Node node, String login, Action action, String message) 
	{
		AuditRecord ar = new AuditRecord(node,login,action,message);
		write(ar);
	}
    
	public void write(Node node, String login, Action action, String message,
			String arg0) {
		String mes = MessageFormatter.format(message,arg0);
		write(node,login,action,mes);
	}

	public void write(Node node, String login, Action action, String message,
			String arg0, String arg1) {
		String mes = MessageFormatter.format(message,arg0,arg1);
		write(node,login,action,mes);
	}

	public void write(Node node, String login, Action action, String message,
			String arg0, String arg1, String arg2) {
		String mes = MessageFormatter.format(message,new Object[] {arg0,arg1,arg2});
		write(node,login,action,mes);
	}

	public AuditRecord prepare(Node node, String login, Action action, String message) 
	{
		return new AuditRecord(node,login,action,message);
	}
    
	public AuditRecord prepare(Node node, String login, Action action, 
			String message, String arg0) 
	{
		String mes = MessageFormatter.format(message,arg0);
		return prepare(node,login,action,mes);
	}

	public AuditRecord prepare(Node node, String login, Action action, 
			String message,	String arg0, String arg1) 
	{
		String mes = MessageFormatter.format(message,arg0,arg1);
		return prepare(node,login,action,mes);
	}

	public AuditRecord prepare(Node node, String login, Action action, 
			String message, String arg0, String arg1, String arg2) 
	{
		String mes = MessageFormatter.arrayFormat(message,new Object[] {arg0,arg1,arg2});
		return prepare(node,login,action,mes);
	}

	protected String[] getFields() 
	{
		return AuditRecord.FIELDS;
	}

	protected String[] getStCreateTable() 
	{
		return AuditRecord.sCreateLogTable;
	}

	public synchronized void setAuditorNode(AuditorNode auditorNode) 
	{
		setNode(auditorNode);
	}

    public synchronized AuditorNode getAuditorNode()
    {
    	try { return (AuditorNode) getNode(); }
    	catch(Exception e) { logger.error("Xmm...",e); }
    	return null;
    }
	
}
