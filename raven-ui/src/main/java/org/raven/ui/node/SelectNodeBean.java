package org.raven.ui.node;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.myfaces.trinidad.component.core.data.CoreTree;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.model.TreeModel;
import org.raven.tree.Tree;
import org.raven.ui.RavenTreeModel;
import org.raven.ui.SessionBean;

public class SelectNodeBean {
	public static final String BEAN_NAME = "selectNode";
	private NodeWrapper dstNode = null;
	private Tree tree = null;
	private RavenTreeModel treeModel = null;
	private CoreTree coreTree = null;
	
	public SelectNodeBean()
	{
		tree = SessionBean.getTree();
		List<NodeWrapper> nodes = new ArrayList<NodeWrapper>();
		nodes.add(new NodeWrapper(tree.getRootNode()));
		
		treeModel = new RavenTreeModel(nodes, "childrenList");
		treeModel.setUserAcl(SessionBean.getUserContext());
	}
	
	  public void setNode(ActionEvent event)
	  {
		  NodeWrapper n = (NodeWrapper) SessionBean.getElValue("nodex");
		  setDstNode(n);
	  }
	  
		public String select()
		{
			if(dstNode==null) return cancel();
			RequestContext.getCurrentInstance().returnFromDialog(dstNode.getNodePath(), null);
			return null;
		}

		public String cancel()
		{
			RequestContext.getCurrentInstance().returnFromDialog(null, null);
			return null;
		}
	
//	private ArrayList<Node> nodes = new ArrayList<Node>();
/*	
	public String moveNode()
	{
        try
        {
            Node n = tree.getNode(newNodeType);
            if (n instanceof TemplateNode)            
            {
                template.init((TemplateNode) n, wrapper.getNode(), getNewNodeName());
                return "dialog:templateAttrEdit";
            }
            return "err";
        } catch (InvalidPathException invalidPathException)
        {
            return null;
        }
	}

*/

	public TreeModel getTreeModel() {
		return treeModel;
	}

	public void setTreeModel(RavenTreeModel treeModel) {
		this.treeModel = treeModel;
	}

	public void setCoreTree(CoreTree coreTree) {
		this.coreTree = coreTree;
	}

	public CoreTree getCoreTree() {
		return coreTree;
	}

	public void setDstNode(NodeWrapper dstNode) {
		this.dstNode = dstNode;
	}

	public NodeWrapper getDstNode() {
		return dstNode;
	}	
	

}
