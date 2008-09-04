package org.raven.ui;

import org.raven.tree.ViewableObject;
import org.raven.tree.Node;

public class ViewableObjectWrapper 
{
	public static final String NODE_URL = "nodeUrl";
	private ViewableObject viewableObject = null;
	private Node node = null;
	
	public ViewableObjectWrapper(ViewableObject vo)
	{
		viewableObject = vo;
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
	
	
	public String getMimeGroup()
	{
		if(!isViewable()) return NODE_URL;
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
	
}
