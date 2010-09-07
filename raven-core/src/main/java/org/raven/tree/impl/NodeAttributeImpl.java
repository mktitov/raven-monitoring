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

package org.raven.tree.impl;

import java.util.ArrayList;
import java.util.List;
import org.raven.conf.Configurator;
import org.raven.log.LogLevel;
import org.raven.tree.AttributeReference;
import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeError;
import org.raven.tree.NodeParameter;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImpl 
        implements NodeAttribute, Cloneable, AttributeValueHandlerListener//, NodeAttributeListener
{
    @Service
    private static TypeConverter converter;
    @Service
    private static Configurator configurator;
    @Service
    private static Tree tree;
    @Service
    private static NodePathResolver pathResolver;
    @Service
    private static MessagesRegistry messagesRegistry;

    private int id;
    private String name;
    private String displayName;
    private String parameterName;
    private MessageComposer descriptionContainer;
    private String parentAttribute;
    private Class type;
    private String value;
    private String valueHandlerType;
    private boolean templateExpression;
    
    private boolean required;
    private BaseNode owner;
    //TODO: remove fild parameter 
    private NodeParameter parameter;
    //TODO: remove fild attributeReference 
    private AttributeReference attributeReference;
    private ParentAttributeValueHandler valueHandler;
    
    private boolean initialized = false;
    private boolean fireEvents = true;
//    private MessageComposer description;

    public NodeAttributeImpl()
    {
    }

    public NodeAttributeImpl(String name, Class type, Object value, String description)
    {
        this.name = name;
        this.type = type;
        this.value = converter.convert(String.class, value, null);
        setDescription(description);
    }
    
    public void init() throws Exception
    {
        valueHandler = new ParentAttributeValueHandler(this);
        valueHandler.setWrappedHandlerType(valueHandlerType);
        valueHandler.addListener(this);
//        valueHandler.setData(value);
        initialized = true;
    }
    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setParameter(NodeParameter parameter)
    {
        this.parameter = parameter;
    }

    public String getName()
    {
        return name;
    }

    public String getParameterName()
    {
        return parameterName;
    }

    public String getDescription()
    {
        if (parameter!=null)
            return parameter.getDescription();
        else if (descriptionContainer!=null)
            return descriptionContainer.toString();
        else
            return null;
    }

    public MessageComposer getDescriptionContainer()
    {
        return descriptionContainer;
    }

    public void setDescriptionContainer(MessageComposer descriptionContainer)
    {
        this.descriptionContainer = descriptionContainer;
    }

    public boolean isRequired()
    {
        if (parameter==null)
            return required;
        else
            return parameter.isRequired();
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public void setFireEvents(boolean fireEvents) {
        this.fireEvents = fireEvents;
    }

    public <T> T getRealValue()
    {
        return isReadonly()? (T)parameter.getValue() : (T)valueHandler.handleData();
//        if (isAttributeReference())
//            return attributeReference == null ? 
//                null : (T)attributeReference.getAttribute().getRealValue();
//        else {
//            if (parameter!=null)
//            {
//                return (T)parameter.getData();
//            }
//            else
//            {
//                if (value==null)
//                    return (T) converter.convert(
//                            type, owner.getParentAttributeRealValue(name), null);
//                else
//                    return (T) converter.convert(type, value, null);
//            }
//        }
    }

    public String getValue()
    {
        try
        {
            if (isReadonly())
                return converter.convert(
                        String.class, parameter.getValue(), parameter.getPattern());
            else
            {
                if (templateExpression)
                    return null;
                else
                    return converter.convert(String.class, valueHandler.handleData(), null);
            }
        }
        catch(Throwable e)
        {
            if (owner.isLogLevelEnabled(LogLevel.WARN))
                owner.warn(
                        String.format("Error getting value for attribute (%s)", getName())
                        , e);
            return null;
        }
    }

    public String getRawValue()
    {
        if (isReadonly())
            return null;
        else
            return valueHandler==null || templateExpression? value : valueHandler.getData();
    }
    
    public void setRawValue(String rawValue)
    {
        this.value = rawValue;
    }
    
    public Class getType() 
    {
        return type;
    }

    public String getValueHandlerType()
    {
        return valueHandlerType;
    }

    public AttributeValueHandler getValueHandler() 
    {
        return initialized && valueHandlerType!=null? valueHandler.getWrappedHandler() : null;
    }
    
    public void setValueHandlerType(String valueHandlerType) throws Exception
    {
        this.valueHandlerType = valueHandlerType;
        if (initialized)
            valueHandler.setWrappedHandlerType(valueHandlerType);
    }
    
    public String getParentAttribute()
    {
        return parentAttribute;
    }

    public void setDescription(String description)
    {
        if (description==null)
            this.descriptionContainer = null;
        else
            this.descriptionContainer = new MessageComposer(messagesRegistry).append(description);
    }

    public void setName(String name)
    {
        if (name==null)
            throw new NodeAttributeError("The name of the attribute can not be empty");
        
        if (!ObjectUtils.equals(this.name, name))
        {
            if (   this.name!=null 
                && ObjectUtils.in(owner.getStatus(), Status.INITIALIZED, Status.STARTED))
            {
                if (owner.getNodeAttribute(name)!=null)
                    throw new NodeAttributeError(String.format(
                            "The attribute with name (%s) is already exists in the node (%s)"
                            , name, owner.getPath()));
                
                String oldValue = this.name;
                this.name = name;
                owner.fireAttributeNameChanged(this, oldValue, this.name);
            } else
                this.name = name;
        }
    }

    public String getDisplayName()
    {
        return displayName==null? name : messagesRegistry.getMessageOrString(displayName);
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public Node getOwner()
    {
        return owner;
    }

    public void setOwner(Node owner)
    {
        this.owner = (BaseNode) owner;
    }

    public void setParameterName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    public void setParentAttribute(String parentAttribute)
    {
        this.parentAttribute = parentAttribute;
    }

    public void setType(Class type)
    {
        this.type = type;
    }

    public void setValue(String value) throws Exception
    {
        if (isReadonly())
            throw new Exception(String.format(
                    "Attribute (%s) of the node (%s) is readonly", getName(), owner.getPath()));
        if (templateExpression || !initialized)
            this.value = value;
        else 
            valueHandler.setData(value);
    }

    public boolean isGeneratorType()
    {
        return type!=null && AttributesGenerator.class.isAssignableFrom(type);
    }

    public List<ReferenceValue> getReferenceValues() throws TooManyReferenceValuesException
    {
        if (!valueHandler.isReferenceValuesSupported())
            return null;
        
        List<ReferenceValue> result = null;
        if (parameter!=null)
        {
                List<ReferenceValue> parameterRefValues = parameter.getReferenceValues();
                if (parameterRefValues!=null)
                {
                    result = new ArrayList<ReferenceValue>(parameterRefValues.size());
                    for (ReferenceValue refValue: parameterRefValues)
                    {
                        ReferenceValue value = new ReferenceValueImpl(
                                converter.convert(String.class, refValue.getValue(), null)
                                , refValue.getValueAsString());
                        result.add(value);
                    }
                }
        }
        if (result==null)
            result = tree.getReferenceValuesForAttribute(this);
        
        return result;
    }

    public void save()
    {
        tree.saveNodeAttribute(this);
    }

    public void shutdown() 
    {
        if (valueHandler!=null)
            valueHandler.close();
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        NodeAttributeImpl clone = (NodeAttributeImpl) super.clone();
        clone.id = 0;
        clone.owner = null;
        clone.valueHandler = null;
        clone.initialized = false;
        clone.value = getRawValue();
        return clone;
    }

//    public boolean isAttributeReference()
//    {
//        return type==null? false : AttributeReference.class.isAssignableFrom(type);
//    }

    public boolean isExpression()
    {
        if (templateExpression)
            return true;
        else
            return valueHandler==null? false : valueHandler.isExpressionSupported();
    }

    public boolean isExpressionValid()
    {
        return valueHandler.isExpressionValid();
    }

    public void validateExpression() throws Exception
    {
        valueHandler.validateExpression();
    }

    public boolean isTemplateExpression() 
    {
        return templateExpression;
    }

    public void setTemplateExpression(boolean templateExpression) 
    {
        this.templateExpression = templateExpression;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final NodeAttributeImpl other = (NodeAttributeImpl) obj;
        if (this.id != other.id)
        {
            return false;
        }
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name)))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 79 * hash + this.id;
        return hash;
    }

    public void valueChanged(Object oldValue, Object newValue)
    {
        if (fireEvents)
            owner.fireAttributeValueChanged(this, oldValue, newValue);
    }

    public void expressionInvalidated(Object oldValue)
    {
        if (fireEvents)
            owner.fireAttributeValueChanged(this, oldValue, null);
    }

    public String getPath() 
    {
        return owner==null? null : pathResolver.getAbsolutePath(this);
    }

    public boolean isReadonly()
    {
        return parameter==null? false : parameter.isReadOnly();
    }
}
