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
import java.util.ArrayList;
import java.util.Collection;
import org.raven.tree.Node;
import org.raven.tree.impl.AbstractDynamicNode;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 *
 * @author Mikhail Titov
 */
public class SvnDirectoryNode extends AbstractDynamicNode implements SvnNode
{
    @Override
    protected Collection<Node> doGetChildrens()
    {
        StringBuilder path = new StringBuilder();
        Node node = this;
        while (!(node instanceof SvnBrowserNode))
        {
            path.insert(0, node.getName()+File.separator);
            node = node.getParent();
        }
        SvnBrowserNode browser = (SvnBrowserNode) node;
        
        if (!browser.getStatus().equals(Status.STARTED))
            return null;

        File dir = path.length()==0?
            browser.getBaseDir() : new File(browser.getBaseDir(), path.toString());

        Collection<Node> nodes = new ArrayList<Node>();
        if (this==browser)
        {
            SvnFileRevisionsNode revisionsNode = new SvnFileRevisionsNode();
            revisionsNode.setRevisionsForFile(false);
            nodes.add(revisionsNode);
        }

        if (dir.exists())
        {
            File[] files = dir.listFiles();
            if (files!=null && files.length>0)
                for (File file: files)
                {
                    if (file.getName().startsWith(".svn"))
                        continue;
                    node = null;
                    if (file.isDirectory())
                        node = new SvnDirectoryNode();
                    else
                        node = new SvnFileNode();
                    if (node!=null)
                    {
                        node.setName(file.getName());
                        nodes.add(node);
                    }
                }
        }
        return nodes;
    }

    public File getSvnFile()
    {
        return SvnNodeHelper.getSvnFile(this);
    }

    public SVNClientManager getSvnClient()
    {
        return SvnNodeHelper.getSvnClient(this);
    }
}
