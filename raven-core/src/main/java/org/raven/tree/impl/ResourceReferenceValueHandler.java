/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.tree.impl;

import java.util.Locale;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ResourceManager;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceReferenceValueHandler extends AbstractAttributeValueHandler {
    
    public static final String RESOURCE_LOCALE_ATTR = "resourceLocale";
    
    @Service
    private static ResourceManager resourceManager;
    
    private String data;

    public ResourceReferenceValueHandler(NodeAttribute attribute) {
        super(attribute);
        this.data = attribute.getRawValue();
    }
    
    public void setData(String value) throws Exception {
        String oldData = data;
        if (!ObjectUtils.equals(data, value)) {
            Node oldResource = resolveResource(data);
            Node newResource = resolveResource(value);
            attribute.save();
            if (!ObjectUtils.equals(oldResource, newResource)) {
                fireValueChangedEvent(oldResource, newResource);
                this.data = value;
            }
        }
    }

    public String getData() {
        return data;
    }

    public Object handleData() {
        return resolveResource(data);
    }
    
    private Node resolveResource(String key) {
        return key==null || key.isEmpty()? null : resourceManager.getResource(key, getResourceLocale());
    }
    
    private Locale getResourceLocale() {
        NodeAttribute localeAttr = attribute.getOwner().getNodeAttribute(RESOURCE_LOCALE_ATTR);
        if (localeAttr==null || !Locale.class.equals(localeAttr.getType()))
            return null;
        else
            return localeAttr.getRealValue();
    }

    public void close() { }

    public boolean isReferenceValuesSupported() {
        return false;
    }

    public boolean isExpressionSupported() {
        return true;
    }

    public boolean isExpressionValid() {
        return true;
    }

    public void validateExpression() throws Exception { }
    
}
