/*
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.prj.impl;

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.auth.impl.LoginManagerNode;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.prj.Project;
import org.raven.template.impl.TemplatesNode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.SchemasNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass (parentNode = ProjectsNode.class)
public class ProjectNode extends BaseNode implements Project {

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initNodes(true);
    }

    public WebInterfaceNode getWebInterface() {
        return (WebInterfaceNode) getNode(WebInterfaceNode.NAME);
    }

    public void initNodes(boolean start) {
        if (!checkNode(LoginManagerNode.NAME)) 
            addSaveAndStartNode(new ProjectLoginManagerNode(), start);
        if (!checkNode(ConnectionPoolsNode.NAME))
            addSaveAndStartNode(new ConnectionPoolsNode(), start);
        if (!checkNode(SchemasNode.NAME))
            addSaveAndStartNode(new SchemasNode(), start);
        if (!checkNode(TemplatesNode.NAME))
            addSaveAndStartNode(new TemplatesNode(), start);        
        if (!checkNode(ConfigurationNode.NAME))
            addSaveAndStartNode(new ConfigurationNode(), start);
        if (!checkNode(WebInterfaceNode.NAME))
            addSaveAndStartNode(new WebInterfaceNode(), start);
        if (!checkNode(UserInterfaceNode.NAME))
            addSaveAndStartNode(new UserInterfaceNode(), start);        
    }
    
	public Collection<Node> getTempltateNodes() {
        return getNode(TemplatesNode.NAME).getEffectiveNodes();
//        return result==null? Collections.EMPTY_LIST : new ArrayList<Node>(result);
    }
    
    private void addSaveAndStartNode(Node node, boolean start) {
        addAndSaveChildren(node);
        if (start && !node.isStarted() && node.isAutoStart())
            node.start();
    }
    
    private boolean checkNode(String name) {
        Node node = getNode(name);
        if (node!=null && !node.isStarted() && node.isAutoStart())
            node.start();
        return node!=null;
    }
}
