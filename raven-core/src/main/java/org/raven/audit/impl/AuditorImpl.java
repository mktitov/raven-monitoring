package org.raven.audit.impl;

import java.util.Date;
import java.util.List;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.store.AbstractDbWorker;
import org.raven.tree.Node;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class AuditorImpl extends AbstractDbWorker implements Auditor 
{
//	private static Logger logger = LoggerFactory.getLogger(AuditorImpl.class);
	
	private static final String[] sCreateLogTable = { 
		"create table @ ("+AuditRecord.FD+" timestamp not null,"+
		AuditRecord.NODE_ID+" int not null,"+
		AuditRecord.NODE_PATH+" varchar("+MAX_NODE_PATH_LENGTH+") ,"+
		AuditRecord.LOGIN+" varchar(64) not null,"+
		AuditRecord.ACTION_TYPE+" int not null,"+
		AuditRecord.ACTION+" int not null,"+
		AuditRecord.MESSAGE+" varchar("+AuditRecord.MAX_MESSAGE_LENGTH+") not null )",
		"create index @_"+AuditRecord.FD+" on @("+AuditRecord.FD+")",
		"create index @_"+AuditRecord.NODE_ID+" on @("+AuditRecord.NODE_ID+")",
		"create index @_"+AuditRecord.LOGIN+" on @("+AuditRecord.LOGIN+")",
		"create index @_"+AuditRecord.ACTION_TYPE+" on @("+AuditRecord.ACTION_TYPE+")"
		};
	
    private AuditorNode auditorNode;
    
    public AuditorImpl()
    {
    	setMetaTableNamePrefix("audit");
    	setName("audit");
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
		return sCreateLogTable;
	}

	protected synchronized boolean dbWorkAllowed() 
	{
    	if(auditorNode==null || auditorNode.getStatus()!=Node.Status.STARTED)
    	{
	    	setPool(null);
	    	return false;
	    }
    	if(getPool()==null)
	   		setPool(auditorNode.getConnectionPool());
	   	if(getPool()==null || getPool().getStatus()!=Node.Status.STARTED)
	   		return false;
	    return true;
	}

	public void write(Node node, String login, Action action, String message) 
	{
		AuditRecord ar = new AuditRecord(node.getId(),node.getPath(),login,action,message);
		writeToQueue(ar);
	}

	public synchronized void setAuditorNode(AuditorNode auditorNode) 
	{
		this.auditorNode = auditorNode;
	}

	public synchronized AuditorNode getAuditorNode() 
	{
		return auditorNode;
	}
	
}
