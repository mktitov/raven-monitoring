package org.raven.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.raven.table.Table;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewableObjectWrapper 
{
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	protected Logger logger = LoggerFactory.getLogger(ViewableObjectWrapper.class);
	public static final String NODE_URL = "nodeUrl";
	public static final String RAVEN_TABLE_GR = "ravenTable";
	private ViewableObject viewableObject = null;
	private Node node = null;
	private long fd = 0;
	private String htmlTable = null;
	
	public ViewableObjectWrapper(ViewableObject vo)
	{
		viewableObject = vo;
		fd = System.currentTimeMillis();
	}
	
	public ViewableObjectWrapper(Node node)
	{
		this.node = node;
	}

	public String getId()
	{
		return toString();
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
		if(isViewable() && viewableObject.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE)) 
			return true;
		return false;
	}

	public String getFromDate()
	{
	    SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT);
		return f.format(new Date(fd));
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
		if(!isViewable()) return node.getPath();
		return viewableObject.getData();
	}
	
	public String getNodePath()
	{
		if(!isViewable()) return node.getPath();
		return null;
	}
	
	
//	public void setViewableObject(ViewableObject viewableObject) {
//		this.viewableObject = viewableObject;
//	}

	public ViewableObject getViewableObject() {
		return viewableObject;
	}

//	public void setNode(Node node) {
//		this.node = node;
//	}

	public Node getNode() {
		return node;
	}

//	public void setFd(long fd) {
//		this.fd = fd;
//	}

	public long getFd() {
		return fd;
	}
	
}
