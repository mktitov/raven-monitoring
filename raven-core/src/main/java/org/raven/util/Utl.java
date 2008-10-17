package org.raven.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	
}
