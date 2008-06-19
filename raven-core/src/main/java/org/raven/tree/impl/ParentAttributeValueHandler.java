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

import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerListener;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeListener;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class ParentAttributeValueHandler 
        extends AbstractAttributeValueHandler 
        implements AttributeValueHandlerListener, NodeAttributeListener
{
    private AttributeValueHandler wrappedHandler;
    private NodeAttribute parentAttribute;
    private String value;

    public ParentAttributeValueHandler(NodeAttribute attribute)
    {
        super(attribute);
    }

    public AttributeValueHandler getWrappedHandler()
    {
        return wrappedHandler;
    }

    public void setWrappedHandler(AttributeValueHandler wrappedHandler)
    {
        if (!ObjectUtils.equals(this.wrappedHandler, wrappedHandler))
        {
            if (this.wrappedHandler!=null)
                this.wrappedHandler.close();

            this.wrappedHandler = wrappedHandler;
            if (this.wrappedHandler!=null)
            {
                this.wrappedHandler.addListener(this);
                this.wrappedHandler.setValue(value);
            }
        }
    }

    public void setValue(String value)
    {
        this.value = value;
        if (wrappedHandler!=null)
            wrappedHandler.setValue(value);
    }

    public String getValue()
    {
        if (wrappedHandler==null)
            return value;
        else
        {
            String result = wrappedHandler.getValue();
            if (result!=null)
            {
                cleanupParentAttribute();
                return result;
            }
            else
                return getParentAttribute()==null? null : getParentAttribute().getValue();
        }
    }

    public Object handleValue()
    {
        Object result = wrappedHandler==null? null : wrappedHandler.handleValue();
        if (result!=null)
        {
            cleanupParentAttribute();
            return result;
        } 
        else
            return getParentAttribute()==null? null : getParentAttribute().getRealValue();
    }

    public void close()
    {
        cleanupParentAttribute();
        if (wrappedHandler!=null)
            wrappedHandler.close();
    }

    public boolean canHandleValue()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isReferenceValuesSupported()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isExpressionSupported()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void valueChanged(Object newValue)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void cleanupParentAttribute()
    {
        if (parentAttribute!=null)
        {
            parentAttribute.getOwner().removeNodeAttributeDependency(attribute.getName(), this);
            parentAttribute = null;
        }
    }

    private NodeAttribute getParentAttribute()
    {
        if (parentAttribute==null)
        {
            parentAttribute = attribute.getOwner().getParentAttribute(attribute.getName());
            if (parentAttribute!=null)
                parentAttribute.getOwner().addNodeAttributeDependency(attribute.getName(), this);
        }
        return parentAttribute;
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, String oldValue, String newValue)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
