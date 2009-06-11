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

import java.util.Collection;
import javax.script.Bindings;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.util.SyslogUtility;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.SyslogMessageHandler;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SyslogReaderNode.class)
public class SyslogMessageHandlerNode extends BaseNode implements DataSource, SyslogMessageHandler
{
    public final static String ACCEPT_MESSAGE_EXPRESSION_ATTR = "acceptMessageExpression";

    @Parameter @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean acceptMessageExpression;

    @Parameter(readOnly=true)
    private OperationStatistic messagesStat;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
        messagesStat = new OperationStatistic();
    }

    public Boolean getAcceptMessageExpression() {
        return acceptMessageExpression;
    }

    public void setAcceptMessageExpression(Boolean acceptMessageExpression) {
        this.acceptMessageExpression = acceptMessageExpression;
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public OperationStatistic getMessagesStat() {
        return messagesStat;
    }

    public void handleEvent(SyslogServerEventIF event)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes==null || depNodes.isEmpty())
            return;
        try
        {
            try
            {
                bindingSupport.put(SyslogRecordSchemaNode.DATE_FIELD, event.getDate());
                bindingSupport.put(SyslogRecordSchemaNode.HOST_FIELD, event.getHost());
                bindingSupport.put(
                        SyslogRecordSchemaNode.FACILITY_FIELD
                        , SyslogUtility.getFacilityString(event.getFacility()));
                bindingSupport.put(
                        SyslogRecordSchemaNode.LEVEL_FIELD
                        , SyslogUtility.getLevelString(event.getLevel()));
                bindingSupport.put(SyslogRecordSchemaNode.MESSAGE_FIELD, event.getMessage());
                Boolean filter = acceptMessageExpression;
                if (filter!=null && filter==true)
                {
                    long time = messagesStat.markOperationProcessingStart();
                    try
                    {
                        Record rec = recordSchema.createRecord();
                        rec.setValue(SyslogRecordSchemaNode.DATE_FIELD, event.getDate());
                        rec.setValue(SyslogRecordSchemaNode.HOST_FIELD, event.getHost());
                        rec.setValue(
                                SyslogRecordSchemaNode.FACILITY_FIELD
                                , SyslogUtility.getFacilityString(event.getFacility()));
                        rec.setValue(
                                SyslogRecordSchemaNode.LEVEL_FIELD
                                , SyslogUtility.getLevelString(event.getLevel()));
                        rec.setValue(SyslogRecordSchemaNode.MESSAGE_FIELD, event.getMessage());
                        for (Node depNode: depNodes)
                            if (   depNode instanceof DataConsumer
                                && depNode.getStatus().equals(Status.STARTED))
                            {
                                ((DataConsumer)depNode).setData(this, rec);
                            }
                    }
                    finally
                    {
                        messagesStat.markOperationProcessingEnd(time);
                    }
                }
            }
            catch(Exception e)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error("Error handling syslog event", e);
            }
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }
}
