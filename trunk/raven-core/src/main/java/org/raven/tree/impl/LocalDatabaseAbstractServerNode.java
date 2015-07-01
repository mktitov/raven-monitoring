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

package org.raven.tree.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.h2.tools.Server;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class LocalDatabaseAbstractServerNode extends BaseNode
{
    @Parameter(defaultValue="false")
    @NotNull
    private Boolean allowOthers;
    
    @Parameter()
    private Integer port;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useSsl;

    @Parameter(defaultValue="false")
    private Boolean autoStart;

    private final String argsPrefix;
    protected Server server;

    public LocalDatabaseAbstractServerNode(String argsPrefix)
    {
        this.argsPrefix = argsPrefix;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        Integer _port = port;
        List<String> argsList = new ArrayList<String>(3);
        if (_port!=null)
        {
            argsList.add("-"+argsPrefix+"Port");
            argsList.add(""+_port);
        }
        if (allowOthers)
            argsList.add("-"+argsPrefix+"AllowOthers");
        if (useSsl)
            argsList.add("-"+argsPrefix+"SSL");
        String[] args = null;
        if (argsList.size()>0)
        {
            args = new String[argsList.size()];
            args = argsList.toArray(args);
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Creating H2 %s server", argsPrefix));
        server = createServer(args);
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("H2 %s server created", argsPrefix));
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Starting H2 %s server", argsPrefix));
        server.start();
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("H2 %s server started", argsPrefix));
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        server.stop();
    }

    public Boolean getAutoStart()
    {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart)
    {
        this.autoStart = autoStart;
    }

    public Boolean getAllowOthers()
    {
        return allowOthers;
    }

    public void setAllowOthers(Boolean allowOthers)
    {
        this.allowOthers = allowOthers;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public Boolean getUseSsl()
    {
        return useSsl;
    }

    public void setUseSsl(Boolean useSsl)
    {
        this.useSsl = useSsl;
    }

    public abstract Server createServer(String[] args) throws SQLException;
}
