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
import java.util.HashMap;
import java.util.Map;
import org.raven.Helper;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataPipe extends AbstractDataConsumer implements DataSource
{
    private Collection<NodeAttribute> consumerAttributes;

    @Override
    protected void initFields()
    {
        super.initFields();
        consumerAttributes = new ArrayList<NodeAttribute>();
        fillConsumerAttributes(consumerAttributes);
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes) 
    {
        Map<String, NodeAttribute> attributes = new HashMap<String, NodeAttribute>();
        Collection<NodeAttribute> nodeAttributes = 
                dataConsumer instanceof Node? ((Node)dataConsumer).getNodeAttributes() : null;
        if (nodeAttributes!=null)
            for (NodeAttribute attr: nodeAttributes)
                attributes.put(attr.getName(), attr);
        if (sessionAttributes!=null)
            for (NodeAttribute attr: sessionAttributes)
                attributes.put(attr.getName(), attr);

        if (!checkDataConsumer(dataConsumer, attributes))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Skiping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }
        try
        {
            return gatherDataForConsumer(dataConsumer, attributes);
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
    
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        return getDataSource().getDataImmediate(this, null);
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

    protected void sendDataToConsumers(Object data)
    {
        Collection<Node> deps = getDependentNodes();
        if (deps!=null && deps.size()>0)
            for (Node dep: deps)
                if (dep.getStatus()==Status.STARTED && dep instanceof DataConsumer)
                    ((DataConsumer)dep).setData(this, data);
    }

}
