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

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.store.INodeHasPool;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, anyChildTypes=true)
public class NodeLoggerNode extends BaseNode implements INodeHasPool, DataSource
{
    public static String NAME = "Node logger";

    @Service
    private static NodeLogger nodeLogger;
    private String staticPath;
    
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private ConnectionPool connectionPool;
    
    @NotNull @Parameter(defaultValue="ERROR")
    private LogLevel logLevelThreshold;

    public NodeLoggerNode() {
        super(NAME);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        staticPath = getPath();
        nodeLogger.setNodeLoggerNode(this);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        nodeLogger.setNodeLoggerNode(null);
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public LogLevel getLogLevelThreshold() {
        return logLevelThreshold;
    }

    public void setLogLevelThreshold(LogLevel logLevelThreshold) {
        this.logLevelThreshold = logLevelThreshold;
    }

    @Override
    protected boolean includeLogLevel() {
        return false;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this dataSource");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
    
    void pushLogRecordToHandlers(final NodeLogRecord rec) {
        if (!isStarted() || rec.getNodePath().startsWith(staticPath) || !logLevelThreshold.isLogLevelEnabled(rec.getLevel()))
            return;
        ExecutorService _executor = executor;
        if (_executor==null)
            return;
        _executor.executeQuietly(new AbstractTask(this, "Processing log message") {
            @Override public void doRun() throws Exception {
                DataContext context = new DataContextImpl();
                for (DataConsumer cons: NodeUtils.extractNodesOfType(getDependentNodes(), DataConsumer.class))
                    if (cons.getPath().startsWith(staticPath))
                        cons.setData(NodeLoggerNode.this, rec, context);
            }
        });
    }
}
