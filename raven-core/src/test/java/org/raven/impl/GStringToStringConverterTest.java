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
package org.raven.impl;

import org.junit.*;
import static org.junit.Assert.*;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class GStringToStringConverterTest extends RavenCoreTestCase {
    
    @Test
    public void test() throws Exception {
        BaseNode node = new BaseNode("test");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        
        NodeAttributeImpl attr = new NodeAttributeImpl("expr", String.class, "\"test\"", null);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);
        
        assertEquals("test", attr.getRealValue());
    }
    
    @Test
    public void test2() throws Exception {
        NodeWithExpr node = new NodeWithExpr("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        
        node.setExpr("\"test\"");
        assertEquals("test", node.getExpr());
    }
}
