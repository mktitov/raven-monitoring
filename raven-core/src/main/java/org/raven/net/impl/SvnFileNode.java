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
import org.raven.tree.impl.BaseNode;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 *
 * @author Mikhail Titov
 */
public class SvnFileNode extends BaseNode
{
    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        SvnFileContentNode content = (SvnFileContentNode) getChildren(SvnFileContentNode.NAME);
        if (content==null)
        {
            content = new SvnFileContentNode();
            addAndSaveChildren(content);
            content.start();
        }

        SvnFileRevisionsNode revisions =
                (SvnFileRevisionsNode) getChildren(SvnFileRevisionsNode.NAME);
        if (revisions==null)
        {
            revisions = new SvnFileRevisionsNode();
            addAndSaveChildren(revisions);
            revisions.start();
        }
    }

    public File getFile()
    {
        StringBuilder path = new StringBuilder();
        Node node = this;
        while (!(node instanceof SvnBrowserNode))
        {
            if (!(node.getParent() instanceof SvnBrowserNode))
                path.insert(0, File.separator);
            path.insert(0, node.getName());
            node = node.getParent();
        }
        return new File(((SvnBrowserNode)node).getBaseDir(), path.toString());
    }

    public SVNClientManager getSvnClient()
    {
        Node node = getParent();
        while (!(node instanceof SvnBrowserNode))
            node = node.getParent();
        return ((SvnBrowserNode)node).getSvnClient();
    }
}
