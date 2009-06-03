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
import java.util.Collection;
import org.raven.tree.Node;
import org.raven.tree.impl.AbstractDynamicNode;

/**
 *
 * @author Mikhail Titov
 */
public class SvnDirectoryNode extends AbstractDynamicNode
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
        File dir = path.length()==0?
            browser.getBaseDir() : new File(browser.getBaseDir(), path.toString());
        return null;
    }
}
