/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.io.File;
import org.raven.tree.Node;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 *
 * @author Mikhail Titov
 */
public class SvnNodeHelper
{
    private SvnNodeHelper()
    {
    }

    public final static File getSvnFile(Node node)
    {
        StringBuilder path = new StringBuilder();
        while (!(node instanceof SvnBrowserNode))
        {
            path.insert(0, node.getName());
            if (!(node.getParent() instanceof SvnBrowserNode))
                path.insert(0, File.separator);
            node = node.getParent();
        }
        SvnBrowserNode browserNode = (SvnBrowserNode) node;
        if (browserNode.getStatus().equals(Node.Status.STARTED))
            return new File(browserNode.getBaseDir(), path.toString());
        else
            return null;
    }

    public final static SVNClientManager getSvnClient(Node node)
    {
        while (!(node instanceof SvnBrowserNode))
            node = node.getParent();
        return node.getStatus().equals(Node.Status.STARTED)? 
                ((SvnBrowserNode)node).getSvnClient() : null;
    }
}
