package org.raven.audit;

import java.util.Date;
import java.util.List;

import org.raven.audit.impl.AuditorNode;
import org.raven.tree.Node;

public interface Auditor 
{
    
    AuditorNode getAuditorNode();

    void setAuditorNode(AuditorNode auditorNode);

    public void write(AuditRecord aRec);
    
    public void write(Node node, String login, Action action, String message);

    public void write(Node node, String login, Action action, String message, String arg0);

    public void write(Node node, String login, Action action, String message, String arg0, String arg1);

    public void write(Node node, String login, Action action, String message, String arg0, String arg1, String arg2);
    
    public AuditRecord prepare(Node node, String login, Action action, String message);

    public AuditRecord prepare(Node node, String login, Action action, String message, String arg0);

    public AuditRecord prepare(Node node, String login, Action action, String message, String arg0, String arg1);

    public AuditRecord prepare(Node node, String login, Action action, String message, String arg0, String arg1, String arg2);
    
    public List<AuditRecord> getRecords(Date from,Date to, Object nodesId, Action action);
	
}
