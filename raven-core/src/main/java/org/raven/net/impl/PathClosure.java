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
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;

/**
 *
 * @author Mikhail Titov
 */
public class PathClosure extends Closure {
    private final String appPath;
    private final NodePathResolver pathResolver;
    private final Node sriRootNode;

    public PathClosure(Object owner, String appPath, NodePathResolver pathResolver, Node sriRootNode) {
        super(owner);
        this.appPath = appPath + "/sri";
        this.pathResolver = pathResolver;
        this.sriRootNode = sriRootNode;
    }
    
    public String doCall(String subpath) {
        return appPath+"/"+subpath;
    }
    
    public String doCall(Node node) {
        return appPath + "/" + pathResolver.getRelativePath(sriRootNode, node).replace("\"", "");        
    }
}
