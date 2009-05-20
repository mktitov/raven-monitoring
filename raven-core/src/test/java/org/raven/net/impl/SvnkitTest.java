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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 *
 * @author Mikhail Titov
 */
public class SvnkitTest
{
    File importDir = new File("/home/tim/tmp/svnimp");

    @Test
    public void test() throws SVNException, IOException
    {
        SVNURL repUrl = SVNRepositoryFactory.createLocalRepository(
                new File("/home/tim/tmp/svnrep"), true, true);
        FSRepositoryFactory.setup();
//        SVNURL repUrl = SVNURL.parseURIEncoded("file:///./home/tim/tmp/svnrep");
        SVNURL url = repUrl.appendPath("MyRepos", false);
        String myWorkingCopyPath = "/home/tim/tmp/wc";
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager ourClientManager = SVNClientManager.newInstance(options, "test", "test");
        long rev = ourClientManager.getCommitClient().doMkDir(
                new SVNURL[]{url}, "create dir message").getNewRevision();
        
//        createLocalDirs();
//        ourClientManager.getCommitClient().doImport(
//                importDir, url, "initial import", null, true, true, SVNDepth.INFINITY);
        File wcDir = new File(myWorkingCopyPath);
        wcDir.mkdirs();
        ourClientManager.getUpdateClient().doCheckout(repUrl, wcDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
    }

    private void createLocalDirs() throws IOException
    {
        importDir.mkdirs();
        File dir1 = new File(importDir, "test_dir");
        dir1.mkdirs();
        File file = new File(dir1, "test_file");
        FileUtils.writeStringToFile(file, "file content");
    }
}
