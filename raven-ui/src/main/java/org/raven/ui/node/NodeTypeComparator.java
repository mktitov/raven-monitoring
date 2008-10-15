package org.raven.ui.node;

import java.util.Comparator;


public class NodeTypeComparator implements Comparator<NodeType> 
{

	public int compare(NodeType o1, NodeType o2) 
	{
		return o1.getShortName().compareTo(o2.getShortName());
	}

}
