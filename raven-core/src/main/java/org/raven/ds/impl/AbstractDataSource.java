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
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataSource extends BaseNode implements DataSource
{
    public final static String DATASOURCE_ATTRIBUTE = "dataSource";
    
    private Collection<NodeAttribute> consumerAttributes;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean stopProcessingOnError;

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
        context.addSessionAttributes(
                dataConsumer instanceof Node? ((Node)dataConsumer).getNodeAttributes() : null
                , false);

        if (!checkDataConsumer(dataConsumer, context.getSessionAttributes()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Skipping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }
        try
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Received pull data request from consumer (%s)"
                        , dataConsumer.getPath()));
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
        return consumerAttributes;
    }
    
    public abstract boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context)
            throws Exception;
    
    /**
     * Use this method to add attributes that consumers must have and set. The filled collection
     * will be returned by the {@link org.raven.tree.AttributesGenerator#generateAttributes()}
     * method.
     * @param consumerAttributes the collection 
     */
    public abstract void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes);

    public Boolean getStopProcessingOnError() {
        return stopProcessingOnError;
    }

    public void setStopProcessingOnError(Boolean stopProcessingOnError) {
        this.stopProcessingOnError = stopProcessingOnError;
    }
    
    
    protected boolean checkDataConsumer(
            DataConsumer consumer, Map<String, NodeAttribute> attributes)
    {
        return  !(consumer instanceof Node) || ((Node)consumer).getStatus()==Status.STARTED
                && Helper.checkAttributes(this, consumerAttributes, consumer, attributes);
    }
    
    protected void sendDataToConsumers(Object data, DataContext context) {
        DataSourceHelper.sendDataToConsumers(this, data, context);
    }
}
