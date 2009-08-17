package org.raven.audit;

import java.util.Date;
import java.util.List;

import org.raven.audit.impl.AuditorNode;
import org.raven.tree.Node;

public interface Auditor 
{
    
    AuditorNode getAuditorNode();

    void setAuditorNode(AuditorNode auditorNode);
    
    public void write(Node node, String login, Action action, String message);
    
    public List<AuditRecord> getRecords(Date from,Date to, Object nodesId, Action action);
	
}
