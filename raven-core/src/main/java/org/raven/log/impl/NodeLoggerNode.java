/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.log.impl;

import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.log.NodeLogger;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class NodeLoggerNode extends BaseNode
{
    public static String NAME = "Node logger";

    @Service
    private static NodeLogger nodeLogger;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private ConnectionPool connectionPool;

    public NodeLoggerNode()
    {
        super(NAME);
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        nodeLogger.setNodeLoggerNode(this);
    }

    public ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }

    @Override
    protected boolean includeLogLevel()
    {
        return false;
    }
}
