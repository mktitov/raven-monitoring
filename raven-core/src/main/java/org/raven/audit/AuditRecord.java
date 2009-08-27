package org.raven.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import org.raven.store.IRecord;
import org.raven.tree.Node;
import org.raven.util.Utl;

public class AuditRecord implements Comparator<AuditRecord>, IRecord
{
	public static final int MAX_MESSAGE_LENGTH   = 10240;
	public static final int MAX_NODE_PATH_LENGTH =   512;
	public static final int MAX_SHORT_MES_LENGTH = 30;
	public static final String SHORT_MES_TAIL = "...";
	public static final String FD = "fd";
	public static final String NODE_ID = "nodeId";
	public static final String NODE_PATH = "nodePath";
	public static final String LOGIN = "login";
	public static final String ACTION_TYPE = "actionType";
	public static final String ACTION = "action";
	public static final String MESSAGE = "message";
	
	public static final String[] FIELDS = {FD, NODE_ID, NODE_PATH, LOGIN,
		ACTION_TYPE, ACTION, MESSAGE};
	
	public static final String[] sCreateLogTable = { 
		"create table @ ("+FD+" timestamp not null,"+
		NODE_ID+" int ,"+
		NODE_PATH+" varchar("+MAX_NODE_PATH_LENGTH+") ,"+
		LOGIN+" varchar(64) not null,"+
		ACTION_TYPE+" int not null,"+
		ACTION+" int not null,"+
		MESSAGE+" varchar("+MAX_MESSAGE_LENGTH+") )",
		"create index @_"+FD+" on @("+FD+")",
		"create index @_"+NODE_ID+" on @("+NODE_ID+")",
		"create index @_"+LOGIN+" on @("+LOGIN+")",
		"create index @_"+ACTION_TYPE+" on @("+ACTION_TYPE+")"
		};
	
	private long fd;
	private Integer nodeId;
	private String message;
	private String nodePath;
	private String login;
	private Action action;
	
	//int nodeId,String nodePath
	public AuditRecord(Node node,String login,Action action,String message)
	{
		fd = System.currentTimeMillis();
		this.message = message;
		if(node!=null)
		{
			this.nodeId = node.getId();
			this.nodePath = node.getPath();
		}
		else
		{
			this.nodeId = null;
			this.nodePath = null;
		}	
		this.login = login;
		this.action = action;
	}

	public AuditRecord()
	{
	}

	public static AuditRecord getObjectFromRecord(ResultSet rs) throws SQLException 
	{
			AuditRecord rec = new AuditRecord();
        	rec.setFd(rs.getDate(AuditRecord.FD).getTime());
        	rec.setNodeId(rs.getInt(AuditRecord.NODE_ID));
        	rec.setNodePath(rs.getString(AuditRecord.NODE_PATH));
        	rec.setLogin(rs.getString(AuditRecord.LOGIN));
        	rec.setAction(Action.values()[rs.getInt(AuditRecord.ACTION)]);
        	rec.setMessage(rs.getString(AuditRecord.MESSAGE));
        	return rec;
	}
	
	public Object[] getDataForInsert() 
	{
		String mes = getMessage();
		if(mes!=null)
			mes = mes.substring(0, Math.min(MAX_MESSAGE_LENGTH,mes.length()));
		Object[] x = new Object[]{
				new java.sql.Timestamp(getFd()),
				getNodeId(),
				getNodePath(),
				getLogin(),
				new Integer(getActionType().ordinal()),
				new Integer(getAction().ordinal()),
				mes}; 
			return x;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(""+fd);
		sb.append("; "+nodeId);
		sb.append("; "+nodePath);
		sb.append("; "+login);
		sb.append("; "+action.getActionType());
		sb.append("; "+action);
		sb.append("; "+message);
		return sb.toString();
	}
	
	public long getFd() {
		return fd;
	}

	public String getFdString() 
	{
		return Utl.formatDate(fd);
	}
	
	public void setFd(long time) {
		this.fd = time;
	}
	public Integer getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public String getMessage() {
		return message;
	}
	
	public String getShortMessage() 
	{
		if(message==null) return null;
		int len = message.length();
		if(len>MAX_SHORT_MES_LENGTH)
			return message.substring(0, MAX_SHORT_MES_LENGTH)+SHORT_MES_TAIL;
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}

	public String getNodePath() {
		return nodePath;
	}

	public int compare(AuditRecord o1, AuditRecord o2) {
		if(o1.fd > o2.fd) return -1;
		if(o1.fd < o2.fd) return 1;
		return 0;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String user) {
		this.login = user;
	}

	public ActionType getActionType() {
		return action.getActionType();
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action a) {
		action = a;
	}
	
}
