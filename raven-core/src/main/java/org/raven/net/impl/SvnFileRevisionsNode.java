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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
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
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.weda.beans.ObjectUtils;
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
    public final static String NAME = "Revisions";
    public static final int MAX_LOG_ENTRIES = 100;
    
    public static final String FROM_DATE_ATTR = "fromDate";
    public static final String FROM_REVISION_ATTR = "fromRevision";
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
    private static String fileHtmlColumnName;
    @Message
    private static String diffColumnName;
    @Message
    private static String diffHtmlColumnName;
    @Message
    private static String filesColumnName;

    private boolean revisionsForFile;
    private SVNRepository repository;

    public SvnFileRevisionsNode()
    {
        super(NAME);
    }

    public SVNRepository getRepository() {
        return repository;
    }

    public void setRepository(SVNRepository repository) {
        this.repository = repository;
    }

    public boolean isRevisionsForFile() {
        return revisionsForFile;
    }

    public void setRevisionsForFile(boolean revisionsForFile) {
        this.revisionsForFile = revisionsForFile;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        TreeMap<String, NodeAttribute> attrs = new TreeMap<String, NodeAttribute>();
        putAttribute(FROM_REVISION_ATTR, Long.class, attrs);
        putAttribute(TO_REVISION_ATTR, Long.class, attrs);
        putAttribute(FROM_DATE_ATTR, String.class, attrs);
        putAttribute(TO_DATE_ATTR, String.class, attrs);
        return attrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        SVNRevision fromRevision = getRevision(refreshAttributes, "from", dateFormat);
        SVNRevision toRevision = getRevision(refreshAttributes, "to", dateFormat);
        if (toRevision!=SVNRevision.UNDEFINED || fromRevision!=SVNRevision.UNDEFINED)
        {
            if (toRevision==SVNRevision.UNDEFINED)
                toRevision = SVNRevision.WORKING;
            if (fromRevision==SVNRevision.UNDEFINED)
                fromRevision = SVNRevision.create(1);
        }
        SvnNode fileNode = (SvnNode) getParent();
        Collection<SVNLogEntry> entries;
        SVNClientManager svnClient = fileNode.getSvnClient();
        if (svnClient==null)
            return null;
        File file = fileNode.getSvnFile();
        long lastRevision = 0;
        if (isRevisionsForFile())
        {
            SVNStatusClient fileStatus = svnClient.getStatusClient();
            lastRevision = fileStatus.doStatus(file, false).getCommittedRevision().getNumber();
            LogHandler logHandler = new LogHandler();
            svnClient.getLogClient().doLog(
                isRevisionsForFile()? new File[]{file} : new File[]{}
                , fromRevision, toRevision, true
                , !isRevisionsForFile(), MAX_LOG_ENTRIES,logHandler);
            entries = logHandler.getLogEntries();
        }
        else
            entries = getLogEntriesRecursive(svnClient, fromRevision, toRevision, fileNode);

        if (entries.isEmpty())
            return null;

        TableImpl table;
        if (isRevisionsForFile())
            table = new TableImpl(
                new String[]{
                    revisionsColumnName, dateColumnName,
                    fileColumnName, fileHtmlColumnName,
                    diffColumnName, diffHtmlColumnName});
        else
            table = new TableImpl(
                new String[]{revisionsColumnName, dateColumnName, filesColumnName});
        int rows = 0;
        for (SVNLogEntry entry: entries)
        {
            if (++rows>MAX_LOG_ENTRIES)
                break;
            long revision = entry.getRevision();
            String date = dateFormat.format(entry.getDate());
            if (isRevisionsForFile())
            {
                SvnFileContentVieableObject content =
                        new SvnFileContentVieableObject(svnClient, file, revision, this, false);
                SvnFileContentVieableObject contentHtml =
                        new SvnFileContentVieableObject(svnClient, file, revision, this, true);
                SvnFileDiffViewableObject diff, diffHtml;
                diff = diffHtml = null;
                if (lastRevision != entry.getRevision())
                {
                    diff = new SvnFileDiffViewableObject(svnClient, revision, file, this, false);
                    diffHtml = new SvnFileDiffViewableObject(svnClient, revision, file, this, true);
                }
                table.addRow(new Object[]{revision, date, content, contentHtml, diff, diffHtml});
            }
            else
            {
                StringBuilder paths = new StringBuilder();
                Map changedPaths = entry.getChangedPaths();
                if (changedPaths!=null && !changedPaths.isEmpty())
                {
                    boolean first=true;
                    for (Object pathObj: changedPaths.values())
                    {
                        SVNLogEntryPath path = (SVNLogEntryPath) pathObj;
                        String pathInfo = "("+path.getType()+") "+path.getPath();
                        if (!first)
                            paths.append("<br>"+pathInfo);
                        else
                        {
                            first = false;
                            paths.append(pathInfo);
                        }
                    }
                }
                table.addRow(new Object[]{revision, date, paths.toString()});
            }
        }

        ViewableObject obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        return Arrays.asList(obj);
    }

    public Boolean getAutoRefresh()
    {
        return false;
    }

    private SVNRevision getRevision(
            Map<String, NodeAttribute> attrs, String prefix, SimpleDateFormat dateFormat)
        throws ParseException
    {
        Object rev = getAttributeValue(attrs, prefix+"Revision");
        if (rev!=null)
            return SVNRevision.create((Long)rev);
        rev = getAttributeValue(attrs, prefix+"Date");
        if (rev!=null)
        {
            Date date = dateFormat.parse((String)rev);
            return SVNRevision.create(date);
        }
        
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
            throws Exception
    {
        Messages messages = messagesRegistry.getMessages(this.getClass());
        String desc = messages.get(attrName+"Desc");
        String displayName = messages.get(attrName+"AttrName");
        NodeAttributeImpl attr = new NodeAttributeImpl(attrName, type, null, desc);
        attr.setOwner(this);
        attr.setDisplayName(displayName);
        attr.init();
        attrs.put(attr.getName(), attr);
    }

    private Collection<SVNLogEntry> getLogEntriesRecursive(
            SVNClientManager svnClient, SVNRevision fromRevision, SVNRevision toRevision
            , SvnNode node)
        throws SVNException
    {
        File dir = node.getSvnFile();
        Collection<File> files = FileUtils.listFiles(
                dir
                , FileFilterUtils.trueFileFilter()
                , FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".svn")));
        files.add(dir);
        File[] filesArr = new File[files.size()];
        files.toArray(filesArr);
        LogHandler logHandler = new LogHandler();
        svnClient.getLogClient().doLog(
            filesArr
            , fromRevision, toRevision, true
            , true, MAX_LOG_ENTRIES,logHandler);
        return logHandler.getLogEntries();
    }

    private class LogHandler implements ISVNLogEntryHandler
    {
//        private Collection<SVNLogEntry> logEntries = new ArrayList<SVNLogEntry>();
        private Map<Long, SVNLogEntry> logEntries =
                new TreeMap<Long, SVNLogEntry>(new ReverseLongComparator());

        public Collection<SVNLogEntry> getLogEntries()
        {
            return logEntries.values();
        }

        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException
        {
            logEntries.put(logEntry.getRevision(), logEntry);
        }
    }

    private class ReverseLongComparator implements Comparator<Long>
    {
        public int compare(Long o1, Long o2)
        {
            return o2.compareTo(o1);
        }
    }
}
