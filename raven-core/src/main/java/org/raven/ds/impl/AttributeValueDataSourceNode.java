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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.raven.Helper;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class AttributeValueDataSourceNode extends BaseNode implements DataSource
{
    @Parameter
    private String requiredAttributes;

    @Parameter
    private String value;

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        if (!getStatus().equals(Status.STARTED))
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s). Data source not started"
                        , dataConsumer.getPath()));
            return false;
        }
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
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Skiping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }
            
        try
        {
            dataConsumer.setData(this, value);
            return true;
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s)", dataConsumer.getPath()));
            return false;
        }
    }

    protected boolean checkDataConsumer(
            DataConsumer consumer, Map<String, NodeAttribute> attributes)
    {
        return  !(consumer instanceof Node) || ((Node)consumer).getStatus()==Status.STARTED
                && Helper.checkAttributes(this, generateAttributes(), consumer, attributes);
    }


    public Collection<NodeAttribute> generateAttributes()
    {
        ArrayList<NodeAttribute> consumerAttrs = null;
        Collection<NodeAttribute> attrs = getNodeAttributes();
        if (attrs!=null && !attrs.isEmpty())
            for (NodeAttribute attr: attrs)
                if (attr.getValueHandlerType().equals(
                        DataConsumerAttributeValueHandlerFactory.TYPE))
                {
                    if (consumerAttrs==null)
                        consumerAttrs = new ArrayList<NodeAttribute>();
                    consumerAttrs.add(attr);
                }
            
        return consumerAttrs;
    }
}
