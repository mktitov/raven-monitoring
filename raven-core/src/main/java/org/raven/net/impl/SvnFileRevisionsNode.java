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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.ViewableObjectImpl;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.weda.internal.Messages;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class SvnFileRevisionsNode extends BaseNode implements Viewable
{
    public static final String FROM_DATE_ATTR = "fromDate";
    public static final String FROM_REVISION_ATTR = "fromRevision";
    public static final int MAX_LOG_ENTRIES = 100;
    public final static String NAME = "revisions";
    public static final String TO_DATE_ATTR = "toDate";
    public static final String TO_REVISION_ATTR = "toRevision";

    @Service
    private static MessagesRegistry messagesRegistry;

    @Message
    private static String revisionsColumnName;
    @Message
    private static String dateColumnName;
    @Message
    private static String fileColumnName;
    @Message
    private static String diffColumnName;

    public SvnFileRevisionsNode()
    {
        super(NAME);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        TreeMap<String, NodeAttribute> attrs = new TreeMap<String, NodeAttribute>();
        putAttribute(FROM_REVISION_ATTR, Long.class, attrs);
        putAttribute(TO_REVISION_ATTR, Long.class, attrs);
        putAttribute(FROM_DATE_ATTR, Long.class, attrs);
        putAttribute(TO_DATE_ATTR, Long.class, attrs);
        return attrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        SVNRevision fromRevision = getRevision(refreshAttributes, "from");
        SVNRevision toRevision = getRevision(refreshAttributes, "to");
        SvnFileNode fileNode = (SvnFileNode) getParent();
        SVNClientManager svnClient = fileNode.getSvnClient();
        File file = fileNode.getFile();
        LogHandler logHandler = new LogHandler();
        svnClient.getLogClient().doLog(
            new File[]{file}, fromRevision, toRevision, true, false, MAX_LOG_ENTRIES,logHandler);
        Collection<SVNLogEntry> entries = logHandler.getLogEntries();

        if (entries.isEmpty())
            return null;

        TableImpl table = new TableImpl(
                new String[]{revisionsColumnName, dateColumnName, fileColumnName, diffColumnName});
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (SVNLogEntry entry: entries)
        {
            long revision = entry.getRevision();
            String date = dateFormat.format(entry.getDate());
            SvnFileContentVieableObject content =
                    new SvnFileContentVieableObject(svnClient, file, revision, this);
            SvnFileDiffViewableObject diff =
                    new SvnFileDiffViewableObject(svnClient, revision, file, this);
            table.addRow(new Object[]{revision, date, content, diff});
        }

        ViewableObject obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        return Arrays.asList(obj);
    }

    public Boolean getAutoRefresh()
    {
        return false;
    }

    private SVNRevision getRevision(Map<String, NodeAttribute> attrs, String prefix)
    {
        Object rev = getAttributeValue(attrs, prefix+"Revision");
        if (rev!=null)
            return SVNRevision.create((Long)rev);
        rev = getAttributeValue(attrs, prefix+"Date");
        if (rev!=null)
            return SVNRevision.create((Date)rev);
        
        return SVNRevision.UNDEFINED;
    }

    private Object getAttributeValue(Map<String, NodeAttribute> attrs, String attrName)
    {
        if (attrs==null)
            return null;
        NodeAttribute attr = attrs.get(attrName);
        if (attr==null)
            return null;
        else
            return attr.getRealValue();
    }

    private void putAttribute(String attrName, Class type, Map<String, NodeAttribute> attrs)
    {
        Messages messages = messagesRegistry.getMessages(this.getClass());
        String desc = messages.get(attrName+"Desc");
        String displayName = messages.get(attrName+"AttrName");
        NodeAttributeImpl attr = new NodeAttributeImpl(attrName, type, null, desc);
        attr.setOwner(this);
        attr.setDisplayName(displayName);
        attrs.put(attr.getName(), attr);
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
