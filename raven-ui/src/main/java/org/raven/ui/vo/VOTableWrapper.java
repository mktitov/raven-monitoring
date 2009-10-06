package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.raven.table.Table;

public class VOTableWrapper extends ArrayList<TableItemWrapper[]>
{
	private static final long serialVersionUID = -1356513548995799683L;
	public static final int MAX_ARR_LEN = 30;
	private boolean[] valid = null;
	private Table table;
	
	public VOTableWrapper(Table x)
	{
		super();
		table = x;
		for(Iterator<Object[]> it = x.getRowIterator();it.hasNext();)
		{
			Object[] a = it.next();
			TableItemWrapper[] b = new TableItemWrapper[a.length];
			for(int i=0; i<a.length; i++)
				b[i] = new TableItemWrapper(a[i]);
			add(b);
		}	
	}
	
	public String makeHtmlTable(String charset)
	{
		if(charset==null || charset.length()==0)
			charset = "utf-8";
		StringBuffer sb = new StringBuffer();
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+charset+"\">");
		sb.append("</head>");
		sb.append("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" ><thead><tr>");
		int cols = table.getColumnNames().length;
        for (int i = 0; i < cols; i++) 
        {
    		sb.append("<th>");
        	sb.append(StringEscapeUtils.escapeHtml(table.getColumnNames()[i]));
    		sb.append("</th>");
        }
		sb.append("</tr></thead><tbody>");
		Iterator<Object[]> it = table.getRowIterator();
		while(it.hasNext())
		{
			Object[] ar = it.next();
			sb.append("<tr>");
			for(int i=0;i < cols; i++)
	        {
	    		sb.append("<td>");
	        	//sb.append(StringEscapeUtils.escapeHtml(ar[i].toString()));
	    		if(ar[i]!=null)
	    			sb.append(ar[i]);
	    		sb.append("</td>");
	        }
			sb.append("</tr>");
		}
		sb.append("</tbody></table></html>");
		//logger.info("table created");
		return sb.toString();
	}
	
	public String makeCSV(boolean header,boolean lf,String delim)
	{
		StringBuffer sb = new StringBuffer();
		int cols = table.getColumnNames().length;
		if(header)
		{
			for (int i = 0; i < cols; i++) 
			{
				if(i>0) sb.append(delim);
				sb.append(StringEscapeUtils.escapeCsv(table.getColumnNames()[i]));
			}
			if(lf) sb.append("\r");
			sb.append("\n");
		}
		Iterator<Object[]> it = table.getRowIterator();
		while(it.hasNext())
		{
			Object[] ar = it.next();
			for(int i=0;i < cols; i++)
	        {
				if(i>0) sb.append(delim);
				if(ar[i]!=null) 
					sb.append(StringEscapeUtils.escapeCsv(ar[i].toString()));
	        }
			if(lf) sb.append("\r");
			sb.append("\n");
		}
		return sb.toString();
	}
	
/*	
	public String getHtmlTable()
	{
		if(!isTable())
		{
			logger.error("VO is not table !");
			return null;
		}
		if(htmlTable==null)
			htmlTable = makeHtmlTable();
		return htmlTable;
	}
*/	
	public String[] getColumnNames()
	{
		return table.getColumnNames();
	}

    public boolean[] getValid()
    {
		if(valid==null)
		{
			valid = new boolean[MAX_ARR_LEN];
			String[] x = getColumnNames();
			for(int i=0;i<valid.length && i<MAX_ARR_LEN-1;i++)
				if(i<x.length) valid[i]=true;
				else valid[i]=false;
		}
		return valid;
    }
	

}
