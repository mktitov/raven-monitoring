package org.raven.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.sql.rowset.CachedRowSet;

import org.raven.dbcp.ConnectionPool;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.rowset.CachedRowSetImpl;

public abstract class AbstractTablesManager {
	private static Logger logger = LoggerFactory.getLogger(AbstractTablesManager.class);
	public static final String DT_FORMAT = "yyyyMMdd";
	private static final String sDelTable = "drop table "; 
	private static final String sTableExists = "select count(*) from "; 
	private static final String _sLoadMeta = "select fd,name from @ order by fd asc"; 
	private static final String _sInsertToMeta = "insert into @(fd,name) values(?,?)"; 
	private static final String _sDeleteFromMeta = "delete from @ where fd=?"; 
	private static final String[] sCreateMetaTable = {"create table @ (fd date,name varchar(128))"};
	private int storeDays = 30;
	private boolean metaInited = false; 
	private HashMap<Long,String> metaTableRecords = null;
	public static final String TABLE_MARKER = "@";
	public static final String metaTableNamePostfix = "Tables";
	protected String metaTableNamePrefix;
	private String metaTableName;
	private String sLoadMeta; 
	private String sInsertToMeta; 
	private String sDeleteFromMeta; 
	private SimpleDateFormat dateFormat;
	
//	private static final String sLoadMeta = "select fd,name from "+metaTableName +" order by fd asc"; 
//	private static final String sInsertToMeta = "insert into "+metaTableName+"(fd,name) values(?,?)"; 
//	private static final String sDeleteFromMeta = "delete from "+metaTableName+" where fd=?"; 
//  private NodeLoggerNode nodeLoggerNode;

	private ConnectionPool pool = null;
	private Connection connection = null;
    
	protected abstract boolean createTable(String tableName); 

	public static void closeConnectionS(Connection con)
	{
		try { con.close();} catch(Exception e) {};
	}	

	protected boolean setConnection()
	{
		connection = pool.getConnection();
		if(connection==null)
		{
			logger.error("connection is null");
			return false;
		}	
		return true;
	}

	protected Connection getConnection()
	{
		return connection;
	}
	
	
	protected void closeConnection()
	{
		if(connection!=null)
		{
			closeConnectionS(connection);
			connection = null;
		}
	}	
	
	protected void commit()
	{
		if(connection!=null)
		{
			try { connection.commit();} 
			catch (SQLException e) { logger.error("on commit:",e);}
			finally {closeConnection();}
		}
	}
	
    protected void init()
    {
    	metaTableName = metaTableNamePrefix+metaTableNamePostfix;
    	sLoadMeta = _sLoadMeta.replaceFirst(TABLE_MARKER, metaTableName);
    	sInsertToMeta = _sInsertToMeta.replaceFirst(TABLE_MARKER, metaTableName);
    	sDeleteFromMeta = _sDeleteFromMeta.replaceFirst(TABLE_MARKER, metaTableName);
    	dateFormat = new SimpleDateFormat("'"+metaTableNamePrefix+"_'"+DT_FORMAT);
    }
    
	protected int executeUpdate(String sql, Object[] args, boolean hideError) 
	{
		if(sql==null || sql.length()==0)
		{
			logger.error("invalid ddl :\""+sql+"\""); 
			return -1;
		}
		
		Connection con;
		boolean solidCon = false;
		if(connection!=null)
		{
			solidCon = true;
			con = connection;
		} 
			else con = pool.getConnection();
		if(con==null) return -2;
		Statement st = null;
		int ret = -3;
		try {
			if(args==null)
			{
				st = con.createStatement();
				ret = st.executeUpdate(sql);
			}
			else
			{
				PreparedStatement ps = con.prepareStatement(sql);
				for(int i=0;i< args.length;i++)
					ps.setObject(i+1, args[i]);
				st = ps;
				ret = ps.executeUpdate();
			}	
			if(!solidCon) con.commit();
		}
		catch(SQLException e) 
			{ 
				if(!hideError)
					logger.error("on \""+sql+"\" : ", e); 
			}
		finally 
			{ 
				try { st.close();} catch(Exception e) {};
				if(!solidCon) closeConnectionS(con);
			}
		return ret;
	}	

	protected int executeUpdate(String sql, Object[] args) 
	{
		return executeUpdate(sql, args ,false);
	}
	
	protected int executeUpdate(String sql)
	{
		return executeUpdate(sql, null ,false);
	}

	private boolean deleteTable(String name, boolean hideError)  
	{
		if(name==null || name.length()==0)
		{
			logger.error("invalid table name :\""+name+"\""); 
			return false;
		}	
		if( executeUpdate(sDelTable+name,null,hideError)<0 ) return false;
		return true;
	}	

	private boolean deleteTable(String name)
	{
		return deleteTable(name, false);
	}
	
	protected boolean createTable(String[] tableDDL, String name) 
	{
		if(tableDDL==null || tableDDL.length==0)
		{
			logger.error("invalid tableDDL "); 
			return false;
		}	
		if(name==null || name.length()==0)
		{
			logger.error("invalid table name :\""+name+"\""); 
			return false;
		}	
		boolean ok = false;
		for(String sqx: tableDDL)
		{
			String sq = sqx.replaceAll(TABLE_MARKER, name);
			if(executeUpdate(sq) < 0) 
			{
				deleteTable(name,true);
				ok = false;
				break;
			}
			ok = true;
		}
		return ok;
	}

	private boolean tableExists(String name)  
	{
		if(name==null || name.length()==0) return false;
		String sql = sTableExists+name;
		Connection con = pool.getConnection();
		Statement st = null;
		boolean ok = false;
		try {
			st = con.createStatement(); 
			st.execute(sql);
			ok = true;
		}
		catch(SQLException e) 
		{
			//logger.error("on \""+sql+"\" : ", e);
		}
		finally 
		{
			try { st.close();} catch(Exception e) {};
			try { con.close();} catch(Exception e) {};
		}	
		return ok;
	}
	
	protected CachedRowSet select(String sql, Object[] args) 
	{
		boolean ok = false;
		Connection con = pool.getConnection();
		if(con==null)
		{
			logger.error("connection is null"); 
			return null;
		}	
		Statement st = null;
		ResultSet rs = null;
		CachedRowSet crs = null; 
		try {
			if(args==null)
			{
				st = con.createStatement();
				rs = st.executeQuery(sql);
			}	
			else
			{
				PreparedStatement ps = con.prepareStatement(sql);
				for(int i=0;i< args.length;i++)
					ps.setObject(i+1, args[i]);
				st = ps;
				rs = ps.executeQuery();
			}	
			crs = new CachedRowSetImpl();
			crs.populate(rs);
			ok = true;
		}
		catch(SQLException e) 
			{ logger.error("on \""+sql+"\" : ", e);	}
		finally 
			{ 
				try { rs.close();} catch(Exception e) {};
				try { st.close();} catch(Exception e) {};
				try { con.close();} catch(Exception e) {};
			}
		if(!ok) return null;
		return crs;
	}
	
	private boolean loadMetaTable()
	{
		boolean ok = false;
		HashMap<Long,String> mtrMap = new HashMap<Long, String>(); 
		CachedRowSet crs = select(sLoadMeta, null);
        if(crs==null) return false;
        try {
        	while(crs.next())
        	{ 
        		Date fd = crs.getDate(1);
        		String mes = crs.getString(2);
        		mtrMap.put(fd.getTime(), mes);
        	}
        	metaTableRecords = mtrMap;
        	ok = true;
        } catch(SQLException e) {logger.error("on loadMetaTable:",e);}
        return ok;
	}
	
	private boolean createMetaTable() 
	{
		if(tableExists(metaTableName)) return true;
		return createTable(sCreateMetaTable, metaTableName);
	}
	
	protected List<String> getTablesNames(long fdx, long td)
	{
		List<String> ret = new ArrayList<String>();
		long fd = truncTime(fdx);
		if(!isMetaInited() || fdx>=td) return ret;
		List<Long> lst = new ArrayList<Long>();
		//Set<Long> set = metaTableRecords.keySet();
		Iterator<Long> it = metaTableRecords.keySet().iterator();
		while(it.hasNext())
		{
			long x = it.next();
			if(x>=fd && x<=td)
				lst.add(x);
		}	
		Collections.sort(lst);
		for(int i=lst.size()-1; i>=0; i--)
		{
			String x = metaTableRecords.get(lst.get(i));
			if(x!=null) 
				ret.add(x);
		}
		return ret;	
	}
	
	private boolean insertToMetaTable(long date, String name)
	{
		int rc = executeUpdate(sInsertToMeta, new Object[]{ new java.sql.Date(date) , name});
		if(rc>0) return true;
		return false;
	}

	private boolean deleteFromMetaTable(long date)
	{
		int rc = executeUpdate(sDeleteFromMeta, new Object[]{ new java.sql.Date(date) });
		if(rc>0) return true;
		return false;
	}

	protected boolean addToMeta(long date, String tableName)
	{
		if(!insertToMetaTable(date, tableName)) return false;
		metaTableRecords.put(date, tableName);
		return true;
	}

	protected boolean deleteFromMeta(long date)
	{
		if(!deleteFromMetaTable(date)) return false;
		metaTableRecords.remove(date);
		return true;
	}
	
	private boolean deleteOld()
	{
		long x = System.currentTimeMillis();
		x = truncTime(x);
		x = addDays(x, -storeDays);
		Iterator<Long> it = metaTableRecords.keySet().iterator();
		ArrayList<Long> al = new ArrayList<Long>();
		while(it.hasNext())
		{
			Long dt = it.next();
			if(dt<x) al.add(dt);
		}	
		it = al.iterator();
		while(it.hasNext())
		{
			long fd = it.next();
			String name  = metaTableRecords.get(fd);
			if(deleteTable(name))
				deleteFromMeta(fd);
		}

		return true;
	}
	
	public static long truncTime(long date)
	{
		Calendar cl = GregorianCalendar.getInstance();
		cl.setTimeInMillis(date);
		cl.set(Calendar.MILLISECOND,0);
		cl.set(Calendar.SECOND,0);
		cl.set(Calendar.MINUTE,0);
		cl.set(Calendar.HOUR_OF_DAY,0);
		return cl.getTimeInMillis();
	}

	public static long addDays(long date, int dayCount)
	{
		Calendar cl = GregorianCalendar.getInstance();
		cl.setTimeInMillis(date);
		cl.add(Calendar.DAY_OF_MONTH, dayCount);
		return cl.getTimeInMillis();
	}
	
	public String makeTableName(long date)
	{
		return dateFormat.format(date);
	}
	
	protected String getTableName(IRecord rec)
	{
		if( !isMetaInited() ) return null;
		long trDate = truncTime(rec.getFd());
		String tableName = metaTableRecords.get(trDate);
		if(tableName!=null) return tableName;
		deleteOld();
		tableName = makeTableName(trDate);
		if(!createTable(tableName)) return null;
		if(!addToMeta(trDate, tableName)) return null;
	    return tableName;
	}
    
	public synchronized void setPool(ConnectionPool pool) {
		this.pool = pool;
	}

	public synchronized ConnectionPool getPool() {
		return pool;
	}

	public synchronized boolean isMetaInited() 
	{
//		if(getPool()==null || getPool().getConnection()==null)
   		if(getPool()==null || getPool().getStatus()!=Node.Status.STARTED)
   		{
//   			logger.info("getPool is null or not started");
			return false;
   		}		
//   		if(getPool()==null)
//   			setPool(nodeLoggerNode.getConnectionPool());
		if(metaInited)
		{
//   			logger.info("meta is already inited");
			return true;
		}	
//		logger.info("meta init...");
		if(createMetaTable() && loadMetaTable())
		{
//			logger.info("meta init ok");
				metaInited = true;
		}		
			else
			{
				logger.warn("meta info init error");
				metaInited = false;
			}
		return metaInited;
	}

	protected int getStoreDays() {
		return storeDays;
	}

	protected void setStoreDays(int storeDays) {
		this.storeDays = storeDays;
	}

	protected String getMetaTableName() {
		return metaTableName;
	}

	protected String getMetaTableNamePrefix() {
		return metaTableNamePrefix;
	}

	protected void setMetaTableNamePrefix(String metaTableNamePrefix) {
		this.metaTableNamePrefix = metaTableNamePrefix;
	}

//	public synchronized void setMetaInited(boolean inited) {
//		this.metaInited = inited;
//	}

}
