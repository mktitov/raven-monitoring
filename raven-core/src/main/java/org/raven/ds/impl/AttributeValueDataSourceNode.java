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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.Helper;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class AttributeValueDataSourceNode extends BaseNode implements DataSource, BindingNames
{
    public static final String VALUE_ATTR = "value";

    @Parameter
    private String requiredAttributes;

    @Parameter
    private String value;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public String getRequiredAttributes() {
        return requiredAttributes;
    }

    public void setRequiredAttributes(String requiredAttributes) {
        this.requiredAttributes = requiredAttributes;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        if (!getStatus().equals(Status.STARTED))
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s). Data source not started"
                        , dataConsumer.getPath()));
            return false;
        }
        context.addSessionAttributes(
                dataConsumer instanceof Node? ((Node)dataConsumer).getNodeAttributes() : null
                , false);

        Collection<NodeAttribute> consumerAttributes = generateAttributes();

        if (!checkDataConsumer(dataConsumer, context.getSessionAttributes(), consumerAttributes))
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Skiping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }

        Set<String> consumerAttrNames = new HashSet<String>();
        if (consumerAttributes!=null && !consumerAttributes.isEmpty())
            for (NodeAttribute attr: consumerAttributes)
                consumerAttrNames.add(attr.getName());

        try {
            Object val = null;
            try {
                bindingSupport.put(SESSIONATTRIBUTES_BINDING, context.getSessionAttributes());
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(DATA_STREAM_BINDING, new DataStreamImpl(this, context));
                if (!consumerAttrNames.isEmpty())
                    for (String name: consumerAttrNames)
                        bindingSupport.put(name, context.getSessionAttributes().get(name).getRealValue());
                val = getNodeAttribute(VALUE_ATTR).getRealValue();
            } finally {
                bindingSupport.reset();
            }
            dataConsumer.setData(this, val, context);
            return true;
        } catch(Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s)", dataConsumer.getPath()));
            return false;
        }
    }

    protected boolean checkDataConsumer(
            DataConsumer consumer, Map<String, NodeAttribute> attributes
            , Collection<NodeAttribute> consumerAttributes)
    {
        return  !(consumer instanceof Node) || ((Node)consumer).getStatus()==Status.STARTED
                && Helper.checkAttributes(this, consumerAttributes, consumer, attributes);
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }


    public Collection<NodeAttribute> generateAttributes()
    {
        Set<String> reqAttrs = new HashSet<String>();
        if (getStatus().equals(Status.STARTED))
        {
            String _requiredAttributes = requiredAttributes;
            if (_requiredAttributes!=null && !_requiredAttributes.isEmpty())
            {
                String[] names = _requiredAttributes.split("\\s*,\\s*");
                for (String name: names)
                    reqAttrs.add(name);
            }
        }
        ArrayList<NodeAttribute> consumerAttrs = null;
        Collection<NodeAttribute> attrs = getNodeAttributes();
        if (attrs!=null && !attrs.isEmpty())
            for (NodeAttribute attr: attrs)
                if (DataConsumerAttributeValueHandlerFactory.TYPE.equals(
                    attr.getValueHandlerType()))
                {
                    if (consumerAttrs==null)
                        consumerAttrs = new ArrayList<NodeAttribute>();
                    try {
                        NodeAttribute clone = (NodeAttribute) attr.clone();
                        clone.setValueHandlerType(null);
                        if (reqAttrs.contains(clone.getName()))
                            clone.setRequired(true);
                        consumerAttrs.add(clone);
                    } catch (Exception ex) {
                        error(String.format("Error cloning attribute (%s)", attr.getName()), ex);
                    }
                }
            
        return consumerAttrs;
    }
}
