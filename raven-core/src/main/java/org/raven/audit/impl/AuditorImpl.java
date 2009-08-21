package org.raven.audit.impl;

import java.util.Date;
import java.util.List;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.store.AbstractDbWorker;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class AuditorImpl extends AbstractDbWorker implements Auditor 
{
	private static Logger logger = LoggerFactory.getLogger(AuditorImpl.class);
	
    public AuditorImpl()
    {
    	setMetaTableNamePrefix("audit");
    	setName("auditor");
    	setStoreDays(360);
    	super.init();
    }

	public List<AuditRecord> getRecords(Date from, Date to, Object nodesId,
			Action action) {
		// TODO Auto-generated method stub
		return null;
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
    
	public AuditRecord prepare(Node node, String login, Action action, String message,
			String arg0) {
		String mes = MessageFormatter.format(message,arg0);
		return prepare(node,login,action,mes);
	}

	public AuditRecord prepare(Node node, String login, Action action, String message,
			String arg0, String arg1) {
		String mes = MessageFormatter.format(message,arg0,arg1);
		return prepare(node,login,action,mes);
	}

	public AuditRecord prepare(Node node, String login, Action action, String message,
			String arg0, String arg1, String arg2) {
		String mes = MessageFormatter.format(message,new Object[] {arg0,arg1,arg2});
		return prepare(node,login,action,mes);
	}

	public void write(AuditRecord rec) 
	{
		writeToQueue(rec);
	}
	
	
}
