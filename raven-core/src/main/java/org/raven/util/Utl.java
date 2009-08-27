package org.raven.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jrobin.core.RrdException;
import org.jrobin.core.Util;

public class Utl 
{
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

	public static String formatDate(Date d)
	{
		return formatter.format(d);
	}

	public static String formatDate(long d)
	{
		return formatter.format(new Date(d));
	}
	
	public static Date parseDate(String s)
	{
		try { return formatter.parse(s); }
			catch (ParseException e) { return null; }
	}

	public static long getDefaultFdTime()
	{
		return System.currentTimeMillis()-1000*60*60*24;
	}

	public static long getDefaultTdTime()
	{
		return System.currentTimeMillis();
	}
	
	public static long convert(String dt)
	{
		try { return Util.getTimestamp(dt)*1000; }
		catch (RrdException e) 
		{
			Date d = Utl.parseDate(dt);
			if(d==null) return getDefaultFdTime();
			return d.getTime();
		}
	}

	public static String trim(String s)
	{
		if(s==null) return null;
		return s.trim();
	}

	public static String trim2Empty(String s)
	{
		if(s==null) return "";
		return s.trim();
	}

	public static String trim2Null(String s)
	{
		if(s==null) return null;
		String ss = s.trim();
		if(ss.length()==0) return null;
		return ss;
	}
	
}
