package org.raven.ui.vo;

import java.util.List;
import org.raven.table.Table;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.Node;
import org.raven.ui.node.NodeWrapper;
import org.raven.util.Utl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewableObjectWrapper 
{
	private static final Logger logger = LoggerFactory.getLogger(ViewableObjectWrapper.class);
    public static final String NAVIGATE_TO = "navigateToNode";
	public static final String NODE_URL = "nodeUrl";
	public static final String RAVEN_TABLE_GR = "ravenTable";
	public static final String IMAGE = "image";
	public static final String UID_DELIM = "@"; 
	private ViewableObject viewableObject = null;
	private Node node = null;
	private long fd = 0;
//	private String htmlTable = null;
	private byte[] image = null;
	private VOTableWrapper tableWrapper = null;
	private int uid;
	private int nodeId;
//	private List<TableItemWrapper[]> tableData  = null;
//	private String[] tableColumnNames  = null;
//	private boolean[] valid = null;
	
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

	public String getIdVO()
	{
		if(viewableObject==null) return "";
		return viewableObject.toString(); 
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
		if(isViewable() && getMimeGroup().equals(IMAGE)) return true;
		return false;
	}

	public boolean isFile()
	{
		if(!isViewable()) return false;
		if(isImage() || isTable()) return false;
		return true;
	}
	
	public boolean isTable()
	{
		if(tableWrapper!=null) return true;
		if(!isViewable() || 
				!viewableObject.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE))
			return false;
		Object x = viewableObject.getData();
		if(x==null) return false;
		if (x instanceof Table) 
		{
			tableWrapper = new VOTableWrapper((Table) x);
			return true;
		}
		return false;
	}

	public String getFromDate()
	{
		return Utl.formatDate(fd);
	}
	
	public List<TableItemWrapper[]> getTableData()
	{
		if(!isTable())
		{
			logger.error("VO isn't table !");
			return null;
		}
		return tableWrapper;
	}
	
	public String[] getTableColumnNames()
	{
		if(!isTable())
		{
			logger.error("VO isn't table !!");
			return null;
		}
		return tableWrapper.getColumnNames();
	}

    public boolean[] getValid()
    {
		if(!isTable())
		{
			logger.error("valid - VO isn't table");
			return null;
		}
		return tableWrapper.getValid();
    }
    
	public String getMimeGroup()
	{
		if(!isViewable()) return NODE_URL;
		if( isTable() ) return RAVEN_TABLE_GR;
		String mtype = viewableObject.getMimeType();
		String[] sa = mtype.split("/");
		//if(IMAGE.equals(sa[0])) return IMAGE;
		//return mtype;
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

	public String getNavigateTo()
	{
		if(!isViewable()) return NodeWrapper.getNodeByAttr(node,NAVIGATE_TO).getPath();
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

	public void setUid(int uid) 
	{
	//	logger.info("setUid():"+uid);
		this.uid = uid;
	}

	public int getUid() 
	{
	//	logger.info("getUid():"+uid);
		return uid;
	}

	public void setNodeId(int nodeId) 
	{
	//	logger.info("setNodeId():"+nodeId);
		this.nodeId = nodeId;
	}

	public int getNodeId() 
	{
	//	logger.info("getNodeId():"+nodeId);
		return nodeId;
	}
	
	public String getComplexUid() {
		return ""+getNodeId()+UID_DELIM+getUid();
	}
	
}
