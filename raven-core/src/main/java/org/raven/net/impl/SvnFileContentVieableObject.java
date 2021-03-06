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

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 *
 * @author Mikhail Titov
 */
public class SvnFileContentVieableObject implements ViewableObject
{
    private final SVNClientManager svnClient;
    private final File file;
    private final long revision;
    private final Node owner;
    private final MimeType mimeType;
    private final boolean wrapToHtml;

    public SvnFileContentVieableObject(
            SVNClientManager svnClient, File file, long revision, Node owner, boolean wrapToHtml)
    {
        this.svnClient = svnClient;
        this.file = file;
        this.revision = revision;
        this.owner = owner;
        this.mimeType = (MimeType) MimeUtil.getMimeTypes(file).iterator().next();
        this.wrapToHtml = wrapToHtml;
    }

    public String getMimeType() {
        return wrapToHtml? "text/html" : mimeType.toString();
    }

    public Object getData()
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        try {
            if (wrapToHtml)
                buf.write("<html><body><pre>".getBytes());
            svnClient.getWCClient().doGetFileContents(
                    file, SVNRevision.UNDEFINED, SVNRevision.create(revision), false, buf);
            if (wrapToHtml)
                buf.write("</pre></body></html>".getBytes());
            return buf.toByteArray();
        }
        catch (Exception ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(
                        String.format(
                            "Error reading content of the file (%s) of revision (%d)"
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
    public String toString()
    {
        return file.getName()+(wrapToHtml? ".html" : "");
    }

}
