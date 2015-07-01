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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SvnBrowserNode extends SvnDirectoryNode implements Schedulable
{
    @Parameter @NotNull
    private String repositoryUrl;
    @Parameter @NotNull
    private String workDirectory;
    @Parameter @NotNull
    private String username;
    @Parameter @NotNull
    private String password;
    @Parameter
    private String initialPath;
    @Parameter
    private Long childrenExpirationInterval;
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler expirationScheduler;

    private SVNURL repUrl;
    private File workDir;
    private File baseDir;
    private SVNClientManager svnClient;
    private SVNRepository repository;

    public SvnBrowserNode()
    {
        setStartAfterChildrens(true);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        setupSubversion();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        svnClient.dispose();
        removeChildrens();
    }

    @Override
    public SVNClientManager getSvnClient() {
        return svnClient;
    }

    public Long getChildrenExpirationInterval() {
        return childrenExpirationInterval;
    }

    public void setChildrenExpirationInterval(Long childrenExpirationInterval) {
        this.childrenExpirationInterval = childrenExpirationInterval;
    }

    public Scheduler getExpirationScheduler() {
        return expirationScheduler;
    }

    public void setExpirationScheduler(Scheduler expirationScheduler) {
        this.expirationScheduler = expirationScheduler;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public String getInitialPath() {
        return initialPath;
    }

    public void setInitialPath(String initialPath) {
        this.initialPath = initialPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    private void setupSubversion() throws Exception
    {
        repUrl = SVNURL.parseURIEncoded(repositoryUrl);
        if ("file".equals(repUrl.getProtocol()))
            FSRepositoryFactory.setup();
        workDir = new File(workDirectory);
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        svnClient = SVNClientManager.newInstance(options, username, password);
        try
        {
            createWorkDir(svnClient, workDir);
        }
        catch (Exception e)
        {
            svnClient.dispose();
            throw e;
        }
    }

    private void createWorkDir(SVNClientManager svnClient, File workDir) throws Exception
    {
        if (!workDir.exists() || !(new File(workDir, ".svn").exists()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Initializing work directory (%s)", workDir.getPath()));
            if (!workDir.exists())
                if (!workDir.mkdirs())
                    throw new Exception(String.format(
                            "Error creating working directory (%s)", workDir.getAbsolutePath()));
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Checkouting to the work directory");
            svnClient.getUpdateClient().doCheckout(
                    repUrl, workDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        }
        String _initialPath = initialPath;
        if (_initialPath==null || _initialPath.isEmpty())
            baseDir = workDir;
        else
            baseDir = new File(workDir, _initialPath);
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        if (getStatus().equals(Status.STARTED))
        {
            Long _expirationInterval = childrenExpirationInterval;
            if (_expirationInterval!=null)
                removeChildrensIfExpired(childrenExpirationInterval*1000);
            else if (isLogLevelEnabled(LogLevel.WARN))
                warn("Can not delete expired nodes because of value for the " +
                        "(childrenExpirationInterval) attribute not setted");
        }
    }
}
