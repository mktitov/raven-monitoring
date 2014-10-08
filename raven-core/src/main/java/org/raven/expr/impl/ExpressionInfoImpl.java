/*
 * Copyright 2014 Mikhail Titov.
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
package org.raven.expr.impl;

import org.raven.expr.ExpressionInfo;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionInfoImpl implements ExpressionInfo {
    private final String attrName;
    private final Node node;
    private final String source;

    public ExpressionInfoImpl(String attrName, Node node, String source) {
        this.attrName = attrName;
        this.node = node;
        this.source = source;
    }

    public Node getNode() {
        return node;
    }

    public String getAttrName() {
        return attrName;
    }

    public String getSource() {
        return source;
    }
    
}
