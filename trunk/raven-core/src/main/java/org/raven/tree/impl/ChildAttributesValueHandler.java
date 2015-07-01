/*
 * Copyright 2013 Mikhail Titov.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ChildAttributesValueHandler extends AbstractAttributeValueHandler {

    public ChildAttributesValueHandler(NodeAttribute attribute) {
        super(attribute);
    }

    public void setData(String value) throws Exception {
    }

    public String getData() {
        return null;
    }

    public Object handleData() {
        List<String> names = new ArrayList<String>(10);
        for (NodeAttribute attr: attribute.getOwner().getAttrs())
            if (attribute.getName().equals(attr.getParentAttribute())) 
                names.add(attr.getName());
        if (names.isEmpty()) return null;
        else {
            Collections.sort(names);
            StringBuilder buf = new StringBuilder();
            for (String name: names) {
                if (buf.length()>0) buf.append("; ");
                buf.append(name).append("=").append(attribute.getOwner().getAttr(name).getValue());
            }
            return buf.toString();
        }
    }

    public void close() {
    }

    public boolean isReferenceValuesSupported() {
        return false;
    }

    public boolean isExpressionSupported() {
        return false;
    }

    public boolean isExpressionValid() {
        return true;
    }

    public void validateExpression() throws Exception {
    }
}
