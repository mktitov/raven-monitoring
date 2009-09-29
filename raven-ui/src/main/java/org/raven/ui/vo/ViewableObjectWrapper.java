package org.raven.ui.vo;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.raven.table.Table;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.Node;
import org.raven.ui.SessionBean;
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
	public static final String RAVEN_TEXT = "ravenText";
	public static final String IMAGE = "image";
	public static final String ACTION = "action";
	public static final String UID_DELIM = "@"; 
	private ViewableObject viewableObject = null;
	private NodeWrapper nodeWrapper = null;
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
		if(Viewable.RAVEN_NODE_MIMETYPE.equals(vo.getMimeType()))
		{
			try { 
				Node n  = SessionBean.getTree().getNode((String)vo.getData());
				nodeWrapper = new NodeWrapper(n);
			} 
			catch (InvalidPathException e) 
			{  logger.error("Invalid node path '{}' ",vo.getData(),e); 	}
		}
		else viewableObject = vo;	
		setFd();
	}
	
	public ViewableObjectWrapper(Node node)
	{
		setFd();
		this.nodeWrapper = new NodeWrapper(node);
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

	public boolean isNodeUrl()
	{
		if(nodeWrapper==null) return false;
		return true;
	}

	public boolean isAction()
	{
		if(isViewable() && getMimeGroup().equals(ACTION)) return true;
		return false;
	}
	
	public boolean isImage()
	{
		if(isViewable() && getMimeGroup().equals(IMAGE)) return true;
		return false;
	}

	public boolean isText()
	{
		if(isViewable() && viewableObject.getMimeType().equals(Viewable.RAVEN_TEXT_MIMETYPE))
			return true;
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

	public String runAction()
	{
		if(isAction()) viewableObject.getData();
		return null;
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
		if(isNodeUrl()) return NODE_URL;
		if( isTable() ) return RAVEN_TABLE_GR;
		if(isText()) return RAVEN_TEXT;
		String mtype = viewableObject.getMimeType();
		String[] sa = mtype.split("/");
		if(sa.length>1 && ACTION.equals(sa[1]))
			return ACTION;
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
		//logger.info("get data");
		if(!isViewable()) return nodeWrapper.getPath();
		if(isImage())
		{
			if(image==null)
			{
				Object dt = viewableObject.getData();
				if (dt instanceof InputStream) 
				{
					InputStream is = (InputStream)dt;
					try { image = IOUtils.toByteArray(is);}
					catch(Exception e) { logger.error("getData:",e); }
					finally { try {is.close();} catch(Exception e) {}; }
				}
				else image = (byte[]) dt;
			}	
			return image;
		}
		return viewableObject.getData();
	}
	
	public String getNodePath()
	{
		if(!isViewable()) return nodeWrapper.getPath();
		return null;
	}

	public String getNavigateTo()
	{
		if(!isViewable())
		{
			Node x = NodeWrapper.getNodeByAttr(nodeWrapper.getNode(),NAVIGATE_TO);
			if(x==null) x = nodeWrapper.getNode();
			return x.getPath();
		}	
		return null;
	}
	
	public ViewableObject getViewableObject() {
		return viewableObject;
	}

	public Node getNode() 
	{
		return nodeWrapper.getNode();
	}

	public NodeWrapper getNodeWrapper() 
	{
		return nodeWrapper;
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
