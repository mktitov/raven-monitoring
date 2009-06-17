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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 *
 * @author Mikhail Titov
 */
public class SvnFileDiffViewableObject implements ViewableObject
{
    private final SVNClientManager svnClient;
    private final long revision;
    private final File file;
    private final Node owner;
    private final boolean wrapToHtml;

    public SvnFileDiffViewableObject(
            SVNClientManager svnClient, long revision, File file, Node owner, boolean wrapToHtml)
    {
        this.svnClient = svnClient;
        this.revision = revision;
        this.file = file;
        this.owner = owner;
        this.wrapToHtml = wrapToHtml;
    }

    public String getMimeType() {
        return wrapToHtml? "text/html" : "text/x-diff";
    }

    public Object getData()
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        try {
            svnClient.getDiffClient().doDiff(
                    file, SVNRevision.UNDEFINED, SVNRevision.create(revision), SVNRevision.WORKING
                    , SVNDepth.IMMEDIATES, false, buf, null);
            byte[] data = buf.toByteArray();
            if (wrapToHtml)
                data = wrapDiffToHtml(data);
            return data;
        } 
        catch (Exception ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(
                    String.format(
                        "Error generating diff for file (%s) from revision (%d)"
                        , file.getAbsolutePath(), revision)
                    , ex);
            return null;
        }
    }

    public boolean cacheData() {
        return false;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    @Override
    public String toString() {
        return file.getName()+"-R"+revision+"-HEAD.diff"+(wrapToHtml? ".html" : "");
    }

    private byte[] wrapDiffToHtml(byte[] diff)
    {
        if (diff==null || diff.length==0)
            return null;
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(diff));
        LineIterator it = IOUtils.lineIterator(reader);
        try
        {
            StringBuilder html = new StringBuilder("<html><body>");
            boolean isHeader=true;
            while (it.hasNext())
            {
                String line = it.nextLine();
                if (line.startsWith("@@") && line.endsWith("@@"))
                {
                    if (isHeader) isHeader = false;
                    html.append("<b><font color=\"blue\">"+line+"</font></b>");
                }
                else if (line.startsWith("+") && !isHeader)
                    html.append("<font color=\"green\">"+line+"</font>");
                else if (line.startsWith("-") && !isHeader)
                    html.append("<font color=\"red\">"+line+"</font>");
                else if (isHeader)
                    html.append("<b>"+line+"</b>");
                else
                    html.append(line);
                html.append("<br/>");
            }
            html.append("</body></html>");
            return html.toString().getBytes();
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }
    }
}
