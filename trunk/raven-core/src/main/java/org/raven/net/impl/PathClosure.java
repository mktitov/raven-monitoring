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

package org.raven.net.impl;

import groovy.lang.Closure;
import org.raven.net.Request;
import org.raven.prj.Project;
import org.raven.prj.impl.WebInterfaceNode;
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class PathClosure extends Closure {
    private final String rootPath;
    private final NodePathResolver pathResolver;
    private final Node sriRootNode;
    
    public PathClosure(Object owner, String rootPath, NodePathResolver pathResolver, Node sriRootNode) {
        super(owner);
        this.rootPath = rootPath;
        this.pathResolver = pathResolver;
        this.sriRootNode = sriRootNode;
    }
    
    /**
     * Adding subpath to root path. 
     * <br><b style='color:red'>WARNING!</b> Subpath must starts with service name! So it must starts like 
     * <b>projects/...</b> or <b>sri/</b>
     * @return 
     */
    public String doCall(String subpath) {
        return rootPath+"/"+subpath;
    }
    
    public String doCall(Node node) {
        Node serviceNode;
        StringBuilder path = new StringBuilder(rootPath).append("/");
        WebInterfaceNode webi = NodeUtils.getParentOfType(node, WebInterfaceNode.class, true);
        if (webi != null) {
            path.append(Request.PROJECTS_SERVICE).append("/").append(webi.getParent().getName());
            serviceNode = webi;
        } else {
            serviceNode = sriRootNode;
            path.append(Request.SRI_SERVICE);
        }
        if (node != serviceNode) {
            String nodePath = pathResolver.getRelativePath(serviceNode, node).replace("\"", "");
            path.append("/").append(nodePath, 0, nodePath.length()-1);
        }
        return path.toString();
    }
}
