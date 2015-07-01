package org.raven.ui;

import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.raven.audit.Action;
import org.raven.audit.ActionType;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.ui.util.UIUtil;
import org.raven.util.Utl;

public class AuditView 
{
    public static final SelectItem[] auditATSI = UIUtil.makeSI(ActionType.values(),true);

    public static final SelectItem[] auditASI = UIUtil.makeSI(Action.values(),true);
	
	private String fd = "now-1d";
	
	private String td = "now";
	
	private ActionType actionType = null;
	
	private Action action = null;
	
	private String login = "";
	
	private String nodeId = "";
	
	private String nodePath = "";

	private AuditRecordTable data = new AuditRecordTable();
	
	/*    
    { 
    	new SelectItem(null,"-----"),
		new SelectItem(Action.ATTR_CREATE),	
		new SelectItem(Action.ATTR_DEL),	
		new SelectItem(Action.ATTR_RENAME),	
		new SelectItem(Action.ATTR_CH_VALUE),	
		new SelectItem(Action.ATTR_CH_TYPE),	
		new SelectItem(Action.ATTR_CH_SUBTYPE),	
		new SelectItem(Action.ATTR_CH_DSC),	
		new SelectItem(Action.ATTR_CH_VALUE),
		new SelectItem(Action.NODE_CREATE),
		new SelectItem(Action.NODE_DEL),
		new SelectItem(Action.NODE_COPY),
		new SelectItem(Action.NODE_MOVE),
		new SelectItem(Action.NODE_RENAME),
		new SelectItem(Action.NODE_CH_INDEX),
		new SelectItem(Action.NODE_START),
		new SelectItem(Action.NODE_STOP),
		new SelectItem(Action.NODE_START_RECURSIVE),
		new SelectItem(Action.SESSION_START),
		new SelectItem(Action.SESSION_STOP)
	};
*/    

	public SelectItem[] getActionSelectItems()  
    { 
		return auditASI; 
    }

	public SelectItem[] getActionTypeSelectItems()  
    { 
		return auditATSI; 
    }
	
	
	private void checkAction()
	{
		if( action!=null && actionType!=null && !action.getActionType().equals(actionType) ) 
			action = null;
	}
	
	public void loadData(Auditor auditor)
	{
		checkAction();
		List<AuditRecord> lst = auditor.getRecords(getFrom(), getTo(), getId(), nodePath, login, actionType, action);
		data = new AuditRecordTable();
		if(lst!=null) data.addAll(lst);
	}
	
	public String getFd() {
		return fd;
	}

	public void setFd(String auditViewFd) {
		this.fd = auditViewFd;
	}

	public String getTd() {
		return td;
	}

	public void setTd(String auditViewTd) {
		this.td = auditViewTd;
	}

	public Date getFrom() {
		return new Date(Utl.convert(fd));
	}

	public Date getTo() {
		return new Date(Utl.convert(td));
	}
	
	public ActionType getActionType() {
		return actionType;
	}

	public void setActionType(ActionType auditActionType) {
		this.actionType = auditActionType;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action auditAction) {
		this.action = auditAction;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String auditLogin) {
		this.login = Utl.trim2Null(auditLogin);
	}

	public String getNodeId() {
		return nodeId;
	}
	
	public void setNodeId(String auditNodeId) {
		this.nodeId = Utl.trim2Empty(auditNodeId);
	}

	public Integer getId() 
	{
		try { return new Integer(nodeId); }
		catch(Exception e) {return null;}
	}
	
	public String getNodePath() {
		return nodePath;
	}

	public void setNodePath(String auditNodePath) {
		this.nodePath = Utl.trim2Null(auditNodePath);
	}

	public AuditRecordTable getData() {
		return data;
	}

	public void setData(AuditRecordTable auditData) {
		this.data = auditData;
	}

}
