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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.raven.Helper;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataPipe extends AbstractDataConsumer implements DataSource
{
    public final static String CONSUMER_PARAM = "consumer";

    protected Collection<NodeAttribute> consumerAttributes;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean forwardDataSourceAttributes;

    public Boolean getForwardDataSourceAttributes()
    {
        return forwardDataSourceAttributes;
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public void setForwardDataSourceAttributes(Boolean forwardDataSourceAttributes)
    {
        this.forwardDataSourceAttributes = forwardDataSourceAttributes;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        consumerAttributes = new ArrayList<NodeAttribute>();
        fillConsumerAttributes(consumerAttributes);
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, DataContext context)
    {
        context.putNodeParameter(this, CONSUMER_PARAM, dataConsumer);

        context.addSessionAttributes(
                dataConsumer instanceof Node? ((Node)dataConsumer).getNodeAttributes() : null
                , false);

        if (!checkDataConsumer(dataConsumer, context.getSessionAttributes()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Skiping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }
        try
        {
            return gatherDataForConsumer(dataConsumer, context);
        }
        catch (Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s). %s"
                        , dataConsumer.getPath(), e.getMessage()), e);
            return false;
        }
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        Boolean _forwardDataSourceAttributes = forwardDataSourceAttributes;
        DataSource _dataSource = getDataSource();
        Collection genAttrs = consumerAttributes;
        if (_forwardDataSourceAttributes!=null && _forwardDataSourceAttributes && _dataSource!=null)
        {
            Collection<NodeAttribute> dsAttrs = _dataSource.generateAttributes();
            if (dsAttrs!=null && !dsAttrs.isEmpty())
            {
                Collection<NodeAttribute> attrs = new ArrayList<NodeAttribute>(dsAttrs);
                attrs.addAll(consumerAttributes);

                genAttrs = attrs;
            }
        }
        return genAttrs;
    }
    
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context)
            throws Exception
    {
        return getDataSource().getDataImmediate(this, context);
    }

    @Override
    protected boolean allowAttributesGeneration(NodeAttribute attr)
    {
        if (   attr.getName().equals(DATASOURCE_ATTRIBUTE)
            && forwardDataSourceAttributes!=null
            && forwardDataSourceAttributes)
        {
            return false;
        }
        else
            return super.allowAttributesGeneration(attr);
    }
    
    /**
     * Use this method to add attributes that consumers must have and set. The filled collection
     * will be returned by the {@link org.raven.tree.AttributesGenerator#generateAttributes()}
     * method.
     * @param consumerAttributes the collection 
     */
    public abstract void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes);
    
    protected boolean checkDataConsumer(
            DataConsumer consumer, Map<String, NodeAttribute> attributes)
    {
        return  !(consumer instanceof Node) || ((Node)consumer).getStatus()==Status.STARTED
                && Helper.checkAttributes(this, consumerAttributes, consumer, attributes);
    }

    protected void sendDataToConsumers(Object data, DataContext context)
    {
        DataConsumer consumer = (DataConsumer) context.getNodeParameter(this, CONSUMER_PARAM);
        if (consumer!=null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Sending data to requester consumer (%s)", consumer.getPath()));
            consumer.setData(this, data, context);
        }
        else
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Sending data to all consumers");
            Collection<Node> deps = getDependentNodes();
            if (deps!=null && deps.size()>0)
                for (Node dep: deps)
                    if (dep.getStatus()==Status.STARTED && dep instanceof DataConsumer)
                        ((DataConsumer)dep).setData(this, data, context);
        }
    }
}
