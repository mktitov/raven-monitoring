/*
 *  Copyright 2008 Sergey Pinevskiy.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.ui.node;

//import org.raven.template.TemplateWizard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessControl;
import org.raven.auth.impl.UserAcl;
import org.raven.conf.Configurator;
import org.raven.prj.impl.ProjectNode;
import org.raven.template.impl.TemplatesNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.util.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.services.ClassDescriptorRegistry;

public abstract class AbstractNodeWrapper
{
    protected Logger logger = LoggerFactory.getLogger(AbstractNodeWrapper.class);	
	private UserContext user = null;
	private Tree tree = null;
	private Node node = null;
	private ClassDescriptorRegistry classDesc = null;
//	private List<NodeAttribute> savedAttrs = null;
//	private List<Attr> editingAttrs = null;
	private Configurator configurator;
	
	public boolean isGraphNode()
	{
		if (node instanceof org.raven.DynamicImageNode) return true;
		return false;
	}
	
	public boolean isAllowCreateSubNode() 
	{
		return node.isContainer() && isAllowTreeEdit(); 
	}
	
	public boolean isNodeStopped()
	{
		if(getNode().getStatus()==Node.Status.INITIALIZED) return true;
		return false;
	}

	public boolean isNodeStarted()
	{
		if(getNode().getStatus()==Node.Status.STARTED) return true;
		return false;
	}

	public boolean isNodeCreated()
	{
		if(getNode().getStatus()==Node.Status.CREATED) return true;
		return false;
	}
/*
	public String getStatusImage()
	{
		if(getNode().getStatus()==Node.Status.INITIALIZED) return "Inited";
		if(getNode().getStatus()==Node.Status.CREATED) return "Created";
		if(getNode().getStatus()==Node.Status.STARTED) return "Started";
		return "unknown";
	}
*/
	
	public boolean isAllowViewAudit()
	{
		return user.isAdmin();
	}
	
	public boolean isAllowControl()
	{
		if( (user.getAccessForNode(node) & AccessControl.CONTROL) ==0) return false;
		return true;
	}

	public boolean isAllowNodeRename()
	{
		if(getNode().getParent()==null) return false;
		return isAllowTreeEdit();
	}
	
	public boolean isAllowTreeEdit()
	{
		if( (user.getAccessForNode(node) & AccessControl.TREE_EDIT) ==0 ) return false;
		return true;
	}

	public boolean isAllowNodeEdit()
	{
		if( (user.getAccessForNode(node) & AccessControl.WRITE) ==0 ) return false;
		return true;
	}

	public boolean isAllowNodeRead()
	{
		int acc = user.getAccessForNode(node);
		if( ( acc & AccessControl.READ) ==0 ) return false;
		return true;
	}

	public boolean isAnyAccess()
	{
		int acc = user.getAccessForNode(node);
		if( acc > AccessControl.NONE ) return true;
		return false;
	}
	
	public boolean isCanNodeStop()
	{
		if( ! isNodeAccessible() ) return false;
		return isNodeStarted();
	}

	public boolean isCanNodeStart()
	{
		if( ! isNodeAccessible() ) return false;
		return isNodeStopped();
	}

	public boolean isNodeAccessible()
	{
		if( ! isAllowControl() ) return false;
		if(isNodeCreated()) return false;
		return true;
	}
	
	public String getNodeStatusText()
	{
		if(isNodeCreated()) return "Created";
		if(isNodeStarted()) return "Started";
		if(isNodeStopped()) return "Stopped";
		return "Unknown";
	}
	
	@SuppressWarnings("unchecked")
	public List<NodeType> getValidSubNodeTypesList()
	{
		List<Class> cls = getNode().getChildNodeTypes();
		ArrayList<NodeType> al = new ArrayList<NodeType>();
		if(cls==null) return al;
		for(Class c: cls)
		{
			String simpleName = classDesc.getClassDescriptor(c).getType().getSimpleName();
			String dsc = classDesc.getClassDescriptor(c).getDescription();
			String sdsc = classDesc.getClassDescriptor(c).getDisplayName();
			al.add(new NodeType(c.getCanonicalName(),simpleName, dsc, sdsc));
		}
		Collections.sort(al, new NodeTypeComparator());
		return al;
	}

	public List<NodeType> getValidSubNodeTemplatesList() {
		ArrayList<NodeType> al = new ArrayList<NodeType>();
        //project templates
        if (node!=null) {
            ProjectNode project = NodeUtils.getParentOfType(node, ProjectNode.class, true);
            if (project!=null) {
                int pathLen = project.getNode(TemplatesNode.NAME).getPath().length();
                for (Node t: project.getTempltateNodes())
                    al.add(new NodeType(t.getPath(), "(Project) "+t.getPath().substring(pathLen), "", ""));
            }
        }
        //system templates
		List<Node> templates = tree.getTempltateNodes();
        int templatePathLen = tree.getRootNode().getNode(TemplatesNode.NAME).getPath().length();
		for(Node n: templates) 
			al.add(new NodeType(n.getPath(), n.getPath().substring(templatePathLen), "", ""));
		Collections.sort(al, new NodeTypeComparator());
		return al.isEmpty()? Collections.EMPTY_LIST : al;
	}
	
    public String getNodeName() {
        Node n = getNode();
        if (n == null) {
            return "isNull";
        }
        if (n.getParent() == null) {
            return "@";
        }
        return n.getName();
    }

    public String getNodePath() {
        Node n = getNode();
        if (n == null) {
            return "isNull";
        }
        return n.getPath();
    }

    public List<Node> getDependencies() {
        Set<Node> s = getNode().getDependentNodes();
        ArrayList<Node> al = new ArrayList<Node>();
        if (s != null) {
            al.addAll(s);
        }
        return al;
    }

    public List<NodeAttribute> getNodeAttributes() {
        Collection<NodeAttribute> c = getNode().getNodeAttributes();
        ArrayList<NodeAttribute> al = new ArrayList<NodeAttribute>();
        if (c != null) {
            al.addAll(c);
        }
        return al;
    }

    public String getClassSimpleName() {
        return classDesc.getClassDescriptor(getNode().getClass()).getType().getSimpleName();
    }

    public String getClassShortDescription() {
        return classDesc.getClassDescriptor(getNode().getClass()).getDisplayName();
    }

    public String getClassDescription() {
        return classDesc.getClassDescriptor(getNode().getClass()).getDescription();
    }

    public abstract void onSetNode();

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
        onSetNode();
    }

    public UserContext getUserAcl() {
        return user;
    }

    public void setUserAcl(UserContext user) {
        this.user = user;
    }

    public ClassDescriptorRegistry getClassDesc() {
        return classDesc;
    }

    public void setClassDesc(ClassDescriptorRegistry classDesc) {
        this.classDesc = classDesc;
    }

    public Tree getTree() {
        return tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Configurator getConfigurator() {
        return configurator;
    }

    public void setConfigurator(Configurator configurator) {
        this.configurator = configurator;
    }

}
