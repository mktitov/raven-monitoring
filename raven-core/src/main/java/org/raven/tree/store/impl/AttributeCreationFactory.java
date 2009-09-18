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

package org.raven.tree.store.impl;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreationFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;
import org.xml.sax.Attributes;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeCreationFactory implements ObjectCreationFactory
{
    public static final String PARENT_ATTRIBUTE = "parent";
    public static final String REQUIRED_ATTRIBUTE = "required";
    public static final String TEMPLATE_EXPRESSION_ATTRIBUTE = "templateExpression";
    @Service
    private static TypeConverter converter;

    public final static String NAME_ATTRIBUTE = "name";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String VALUE_HANDLER_ATTRIBUTE = "valueHandler";

    private Digester digester;

    public Object createObject(Attributes attributes) throws Exception
    {
        Node owner = (Node)digester.peek();
        String attrName = attributes.getValue(NAME_ATTRIBUTE);
        NodeAttribute attr = owner.getNodeAttribute(attrName);
        if (attr==null)
        {
            String type = attributes.getValue(TYPE_ATTRIBUTE);
            Class typeClass = Class.forName(type);
            attr = new NodeAttributeImpl(attrName, typeClass, null, null);
            attr.setOwner(owner);
            attr.setRequired(getBoolean(attributes, REQUIRED_ATTRIBUTE));
            attr.setParentAttribute(attributes.getValue(PARENT_ATTRIBUTE));
            attr.init();
            owner.addNodeAttribute(attr);
        }
        String valueHandler = attributes.getValue(VALUE_HANDLER_ATTRIBUTE);
        if (!ObjectUtils.equals(attr.getValueHandlerType(), valueHandler))
            attr.setValueHandlerType(valueHandler);
        boolean templateExpression = getBoolean(attributes, TEMPLATE_EXPRESSION_ATTRIBUTE);
        if (attr.isTemplateExpression()!=templateExpression)
            attr.setTemplateExpression(templateExpression);
        return attr;
    }

    private boolean getBoolean(Attributes attributes, String attrName)
    {
        Boolean res = converter.convert(Boolean.class, attributes.getValue(attrName), null);
        return res!=null && res;
    }

    public Digester getDigester()
    {
        return digester;
    }

    public void setDigester(Digester digester)
    {
        this.digester = digester;
    }
}
