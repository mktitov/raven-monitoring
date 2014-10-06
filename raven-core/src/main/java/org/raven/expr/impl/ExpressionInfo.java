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

import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionInfo {
    public final String attrName;
    public final Node node;
    public final String source;

    public ExpressionInfo(String attrName, Node node, String source) {
        this.attrName = attrName;
        this.node = node;
        this.source = source;
    }
    
}
