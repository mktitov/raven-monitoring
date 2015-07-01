package org.raven.ui.vo;

import java.util.LinkedList;
import org.raven.ui.SessionBean;

public class ObjectsQueue extends LinkedList<Object> 
{
	private static final long serialVersionUID = 3906211924608052043L;
	public static final String BEAN_NAME = "objectsQueue";
	
	public static ObjectsQueue getInstance()
	{
		return (ObjectsQueue) SessionBean.getElValue(BEAN_NAME);
	}

}
