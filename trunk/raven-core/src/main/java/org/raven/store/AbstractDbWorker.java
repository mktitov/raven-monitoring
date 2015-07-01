package org.raven.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import javax.sql.rowset.CachedRowSet;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDbWorker<T extends IRecord> extends AbstractTablesManager implements Runnable 
{
	private static Logger logger = LoggerFactory.getLogger(AbstractDbWorker.class);
//	public static final int MAX_NODE_PATH_LENGTH =   512;
	
	public static final String FIELDS_MARKER = "$FIELDS$";
	public static final String VALUES_MARKER = "$VALUES$";
	public static final String STARTED = "{} started !";
	public static final String STOPPED = "{} stopped !";
	public static final String _sInsertToTable = "insert into @("+FIELDS_MARKER+") values("+VALUES_MARKER+")";

	private String sInsertToTable;
    private Queue<T> queue;
    private String name;
    private INodeHasPool node;
    private boolean stopFlag = false;
    private boolean running = false;
    
    protected abstract String[] getFields();
	
	protected abstract String[] getStCreateTable();
	
	protected abstract T getObjectFromRecord(ResultSet rs) throws SQLException; 

	public static String getFieldsList(String[] fields, boolean queryMark)
	{
    	StringBuffer sb = new StringBuffer();
    	for(String s : fields)
    	{
    		if(sb.length()>0) sb.append(",");
    		if(queryMark) sb.append("?");
    			else sb.append(s);
    	}
		return sb.toString();
	}

	public static String getFieldsList(String[] fields)
	{
		return getFieldsList(fields, false);
	}
	
	protected List<T> selectObjects(String sql,Object[] args)
	{
		ArrayList<T> recs = new ArrayList<T>();
		CachedRowSet crs = select(sql, args);
        if(crs==null) return recs;
        try {
        	while(crs.next())
        		recs.add(getObjectFromRecord(crs));
        } catch(SQLException e) {logger.error("on selectObjects:",e);}
        return recs;
	}
	
	protected List<T> selectObjectsMT(List<String> tables, String sql, Object[] args)
	{
		ArrayList<T> recs = new ArrayList<T>();
		for(String tableName : tables)
		{
			String sqlx = sql.replaceAll(TABLE_MARKER, tableName);
			recs.addAll(selectObjects(sqlx, args));
		}
		return recs;
	}
	
	private void go()
	{
		if(isRunning()) return;
    	Thread x = new Thread(this);
    	if(name!=null && name.trim().length()>0)
    		x.setName(name);
    	x.setDaemon(true);
    	setStopFlag(false);
    	x.start();
    	setRunning(true);
    	logger.warn(STARTED,name);
	}

    public void init()
    {
//    	metaTableNamePrefix = "log";
//    	setStoreDays(30);
    	
    	String fList = getFieldsList(getFields());
    	String qmList = getFieldsList(getFields(),true);

    	sInsertToTable = _sInsertToTable.replaceFirst(Pattern.quote(FIELDS_MARKER), fList);
    	sInsertToTable = sInsertToTable.replaceFirst(Pattern.quote(VALUES_MARKER), qmList);
    	
    	super.init();
    	queue = new ConcurrentLinkedQueue<T>();
    	go();
    }
    
    protected synchronized boolean dbWorkAllowed()
    {
    	if(node==null || node.getStatus()!=Node.Status.STARTED)
    	{
    		setPool(null);
    		return false;
    	}
   		if(getPool()==null)
   			setPool(node.getConnectionPool());
   		if(getPool()==null || getPool().getStatus()!=Node.Status.STARTED)
   			return false;
    	return true;
    }

    public void write(T rec) 
	{
		if(dbWorkAllowed())
		{
			queue.offer(rec);
			go();
		}	
	}

	protected boolean createTable(String tableName) 
	{
		return createTable(getStCreateTable(), tableName);
	}
	
	//abstract
	protected boolean insert(T rec)
	{
		String tname = getTableName(rec);
		String sql = sInsertToTable.replaceAll(TABLE_MARKER, tname);
		Object[] x = rec.getDataForInsert();
		if( executeUpdate(sql, x)<0 ) return false;
		return true;
	}
	
	private boolean writeMessagesFromQueue()
	{
		T rec;
		int cnt = 0;
		if(queue.isEmpty()) return false;
		if(!setConnection()) return false;
		while( (rec=queue.poll())!=null )
		{	
			if(insert(rec)) 
				cnt++;
		}	
		if(cnt>0) commit();
		closeConnection();
		if(cnt==0) return false;
		return true;
	}
	
	public void run()
	{
		while(!isStopFlag())
		{
			writeMessagesFromQueue();
			try { Thread.sleep(200); } catch (InterruptedException e) { }
		}
		setRunning(false);
		logger.warn(STOPPED,name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	protected INodeHasPool getNode() {
		return node;
	}

	protected void setNode(INodeHasPool node) {
		this.node = node;
	}

	protected synchronized boolean isRunning() {
		return running;
	}

	protected synchronized void setRunning(boolean running) {
		this.running = running;
	}

	protected synchronized boolean isStopFlag() {
		return stopFlag;
	}

	protected synchronized void setStopFlag(boolean stopFlag) {
		this.stopFlag = stopFlag;
	}
    
}
