package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.raven.table.Table;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.Node;
import org.raven.util.Utl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewableObjectWrapper 
{
	protected Logger logger = LoggerFactory.getLogger(ViewableObjectWrapper.class);
	public static final String NODE_URL = "nodeUrl";
	public static final String RAVEN_TABLE_GR = "ravenTable";
	public static final int MAX_ARR_LEN = 30;
	private ViewableObject viewableObject = null;
	private Node node = null;
	private long fd = 0;
	private String htmlTable = null;
	private byte[] image = null;
	private List<TableItemWrapper[]> tableData  = null;
//	private String[] tableColumnNames  = null;
	private boolean[] valid = null;
	
	public ViewableObjectWrapper(ViewableObject vo)
	{
		viewableObject = vo;
		setFd();
	}
	
	public ViewableObjectWrapper(Node node)
	{
		setFd();
		this.node = node;
	}

	public String getId()
	{
		return toString();
	}
	
	public String getHeight() 
	{ 
		if(viewableObject==null) return "";
		return ""+viewableObject.getHeight(); 
	}
	
	public String getWidth() 
	{ 
		if(viewableObject==null) return "";
		return ""+viewableObject.getWidth(); 
	}
	
	public boolean isViewable()
	{
		if(viewableObject==null) return false;
		return true;
	}

	public boolean isImage()
	{
		if(isViewable() && getMimeGroup().equals("image")) return true;
		return false;
	}
	
	public boolean isTable()
	{
		if(isViewable() && 
				viewableObject.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE)) 
			return true;
		return false;
	}

	public String getFromDate()
	{
		return Utl.formatDate(fd);
	}
	
	private String makeHtmlTable()
	{
		StringBuffer sb = new StringBuffer();
		Table table = (Table) viewableObject.getData(); 
		sb.append("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" ><thead><tr>");
		int cols = table.getColumnNames().length;
        for (int i = 0; i < cols; i++) 
        {
    		sb.append("<th>");
        	sb.append(table.getColumnNames()[i]);
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
	        	sb.append(ar[i]);
	    		sb.append("</td>");
	        }
			sb.append("</tr>");
		}
		sb.append("</tbody></table>");
		logger.info("table created");
		return sb.toString();
	}
	
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
	
	public List<TableItemWrapper[]> getTableData()
	{
		if(!isTable())
		{
			logger.error("VO isn't table !");
			return null;
		}
		if(tableData==null)
		{
			tableData = new ArrayList<TableItemWrapper[]>();
			Table table = (Table) viewableObject.getData(); 
			for(Iterator<Object[]> it = table.getRowIterator();it.hasNext();)
			{
				Object[] a = it.next();
				TableItemWrapper[] b = new TableItemWrapper[a.length];
				for(int i=0; i<a.length; i++)
					b[i] = new TableItemWrapper(a[i]);
				tableData.add(b);
			}	
		}
		logger.info("getTableData(): "+tableData.size());
		return tableData;
	}
	
	public String[]
	   //    List
	       getTableColumnNames()
	{
		if(!isTable())
		{
			logger.error("VO isn't table !!");
			return null;
		}
		Table table = (Table) viewableObject.getData(); 
		logger.info("getTableColumnNames(): "+table.getColumnNames().length);
		
		//List ar = new ArrayList();
		//for(String x : table.getColumnNames()) ar.add(x);
		return table.getColumnNames();
		//return ar;
	}

    public boolean[] getValid()
    {
		if(!isTable())
		{
			logger.error("valid - VO isn't table !!");
			return null;
		}
		if(valid==null)
		{
			valid = new boolean[MAX_ARR_LEN];
			String[] x = getTableColumnNames();
			for(int i=0;i<valid.length && i<MAX_ARR_LEN-1;i++)
				if(i<x.length) valid[i]=true;
				else valid[i]=false;
		}
		return valid;
    }
    
	public String getMimeGroup()
	{
		if(!isViewable()) return NODE_URL;
		if( isTable() ) 
			return RAVEN_TABLE_GR;
		String[] sa = viewableObject.getMimeType().split("/");
		return sa[0];
	}
	
	public String getMimeType()
	{
		if(!isViewable()) return getMimeGroup();
		return viewableObject.getMimeType();
	}
	
	public Object getData()
	{
		logger.info("get data");
		if(!isViewable()) return node.getPath();
		if(isImage())
		{
			if(image==null) image = (byte[]) viewableObject.getData();
			return image;
		}
		return viewableObject.getData();
	}
	
	public String getNodePath()
	{
		if(!isViewable()) return node.getPath();
		return null;
	}
	
	public ViewableObject getViewableObject() {
		return viewableObject;
	}

	public Node getNode() 
	{
		return node;
	}

	private void setFd() 
	{
		fd = System.currentTimeMillis();
	}
	
	public long getFd() 
	{
		return fd;
	}
	
}
