package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.raven.table.Table;
import org.raven.ui.TableWithDate;

//ArrayList<TIWList> ArrayList<TableItemWrapper[]>
public class VOTableWrapper extends ArrayList<TIWList> 
{
	private static final long serialVersionUID = -1356513548995799683L;
	public static final boolean addCounter = true;
	public static final int MAX_COLUMNS = 50;
	private Table table = null;
	
	public VOTableWrapper(Table x)
	{
		super();
		table = x;
		//init();
	}
	
	public void init()
	{
		if(table==null) return;
		clear();
		int count = 0;
		for(Iterator<Object[]> it = table.getRowIterator();it.hasNext();)
		{
			Object[] a = it.next();
			//TableItemWrapper[] b = new TableItemWrapper[a.length];
			TIWList b = new TIWList();
		//	int k = 0; 
			
			if(addCounter)
				b.add(new TableItemWrapper(++count));
			for(int i=0; i<a.length; i++)
				//b[i] = new TableItemWrapper(a[i]);
				b.add(new TableItemWrapper(a[i]));
			add(b);
		}	
	}
	
	public String makeHtmlTable(String charset, boolean forXls)
	{
		if(charset==null || charset.length()==0)
			charset = "utf-8";
		StringBuffer sb = new StringBuffer();
		sb.append("<html><head>");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+charset+"\">");
		sb.append("</head>");
		sb.append("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" ><thead><tr>");
		int cols = table.getColumnNames().length;
		TableWithDate twd = null;
		boolean isTWD = false;
		if (table instanceof TableWithDate) {
			twd = (TableWithDate) table;
			isTWD = true;
		}
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
	    		{
	    			if (forXls && isTWD && twd.isDate(i)) 
	    				sb.append("'");
	    			sb.append(ar[i]);
	    		}	
	    		sb.append("</td>");
	        }
			sb.append("</tr>");
		}
		sb.append("</tbody></table></html>");
		//logger.info("table created");
		return sb.toString();
	}
	
	public String makeCSV(boolean header,boolean lf,String delim,boolean escape)
	{
		StringBuffer sb = new StringBuffer();
		int cols = table.getColumnNames().length;
		if(header)
		{
			for (int i = 0; i < cols; i++) 
			{
				if(i>0) sb.append(delim);
				if(escape)
					sb.append(StringEscapeUtils.escapeCsv(table.getColumnNames()[i]));
				else
					sb.append(table.getColumnNames()[i]);
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
				if(ar[i]==null) continue;
				if(escape)
					sb.append(StringEscapeUtils.escapeCsv(ar[i].toString()));
				else
					sb.append(ar[i].toString());
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
		if(VOTableWrapper.addCounter)
		{
			ArrayList<String> a = new ArrayList<String>();
			a.add("");
			for(String x : table.getColumnNames())
				a.add(x);
			return a.toArray(new String[] {});
		}
		return table.getColumnNames();
	}

	public int getColumnsCount()
	{
		return getColumnNames().length;
	}
/*	
    public boolean[] getValid()
    {
		if(valid==null)
		{
			valid = new boolean[MAX_COLUMNS];
			String[] x = getColumnNames();
			int z = x.length;
			if(VOTableWrapper.addCounter) z++;
			for(int i=0;i<valid.length && i<MAX_COLUMNS-1;i++)
				if(i < z) valid[i]=true;
					else valid[i]=false;
		}
		return valid;
    }
*/
}
