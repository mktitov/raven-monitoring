package org.raven.store;

import java.util.ArrayList;

public class SqlQuery 
{
	public static final String AND = " and ";
	public static final String LIKE = " like ? ";
	public static final String EQUALS = " = ? ";
	public static final String BETWEEN = " between ? and ? ";
	private StringBuffer sql;
	private ArrayList<Object> values;
	private boolean ok = false;
	
	public SqlQuery(String x)
	{
		sql = new StringBuffer(x);
		values = new ArrayList<Object>();
	}

	private boolean append(String str)
	{
		if(!ok) ok = true;
		else sql.append(AND);
		sql.append(str);
		return true;
	}
	
	public boolean appendLike(String recName,String str)
	{
		if(str==null) return false;
		str = str.trim();
		if(str.length()>0)
		{
			append(recName + LIKE);
			values.add(str);
			return true;
		}
		return false;
	}

	public boolean appendBetween(String recName,Object o1, Object o2)
	{
		if(o1==null || o2==null) return false;
		append(recName + BETWEEN);
		values.add(o1);
		values.add(o2);
		return true;
	}
	
	
	public boolean appendInteger(String recName,Integer x)
	{
		if(x==null) return false;
		append(recName + EQUALS);
		values.add(x);
		return true;
	}

	public boolean appendInteger(String recName,Object x)
	{
		if(x==null) return false;
		if(x instanceof Integer)
		{
			return appendInteger(recName,(Integer) x);
		}
		return false;
	}
	
	public boolean appendString(String str)
	{
		sql.append(str);
		return true;
	}
	
	public String toString()
	{
		return sql.toString();
	}
	
	public Object[] getValues()
	{
		return values.toArray();
	}

	public boolean isOk() {
		return ok;
	}
}
