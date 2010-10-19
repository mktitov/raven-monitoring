/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.api.impl;

import org.junit.Test;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeAccessImplTest extends RavenCoreTestCase
{
    @Test
    public void getValueWithParams() throws Exception
    {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        NodeAttributeImpl attr1 = new NodeAttributeImpl("attr1", Integer.class, null, null);
        attr1.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr1.setValue("arg1+1");
        attr1.setOwner(node);
        attr1.init();
        node.addNodeAttribute(attr1);

        NodeAttributeImpl attr2 = new NodeAttributeImpl("attr2", Integer.class, null, null);
        attr2.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr2.setValue("node['attr1'].getValue([arg1:1])");
        attr2.setOwner(node);
        attr2.init();
        node.addNodeAttribute(attr2);

        assertEquals(2, attr2.getRealValue());
    }
}