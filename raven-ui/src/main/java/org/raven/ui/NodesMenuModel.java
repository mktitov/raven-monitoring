package org.raven.ui;

//import org.apache.myfaces.trinidad.model.MenuModel;
import java.util.List;

import org.apache.myfaces.trinidad.model.BaseMenuModel;
import org.raven.tree.Node;

public class NodesMenuModel extends BaseMenuModel
 {
	
	public NodesMenuModel(List<Node> lst )
	{
		super.setWrappedData(lst);
	}

	public Object getFocusRowKey() {
		return super.getRowKey();
	}


}
