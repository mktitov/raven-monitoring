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
import java.util.List;
import java.util.Map;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 *
 * @author Mikhail Titov
 */
public class SvnFileRevisionsNode extends BaseNode implements Viewable
{
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        SvnFileNode fileNode = (SvnFileNode) getParent();
        SVNClientManager svnClient = fileNode.getSvnClient();
        File file = fileNode.getFile();
        
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    private class LogHandler implements ISVNLogEntryHandler
    {
        private Collection<SVNLogEntry> logEntries = new ArrayList<SVNLogEntry>();

        public Collection<SVNLogEntry> getLogEntries()
        {
            return logEntries;
        }

        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException
        {
            logEntries.add(logEntry);
        }
    }
}
