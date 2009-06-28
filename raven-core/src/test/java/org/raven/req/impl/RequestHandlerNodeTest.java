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

package org.raven.req.impl;

import org.raven.conv.impl.ConversationNode;
import org.raven.conv.impl.ConversationPointNode;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.expr.impl.IfNode;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class RequestHandlerNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        ConversationNode requestHandler = new ConversationNode();
        requestHandler.setName("requestHandler");
        tree.getRootNode().addAndSaveChildren(requestHandler);
        assertTrue(requestHandler.start());

        BaseNode hello = new BaseNode("hello");
        requestHandler.addAndSaveChildren(hello);
        assertTrue(hello.start());

        ConversationPointNode helloEntry = new ConversationPointNode();
        helloEntry.setName("helloEntry");
        requestHandler.addAndSaveChildren(helloEntry);
        assertTrue(hello.start());

        IfNode if1 = new IfNode();
        if1.setName("dtmf==1");
        helloEntry.addAndSaveChildren(if1);
        if1.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("dtmf==1");
        if1.setUsedInTemplate(false);
        assertTrue(if1.start());

        BaseNode balance = new BaseNode("your balance is zero");
//        if
    }
}