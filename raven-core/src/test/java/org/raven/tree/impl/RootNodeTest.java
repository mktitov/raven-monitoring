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

package org.raven.tree.impl;

import org.raven.expr.impl.ExpressionAttributeValueHandler;
import javax.script.Bindings;
import org.junit.Assert;
import org.junit.Test;
import org.raven.expr.BindingSupport;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class RootNodeTest extends Assert
{
    @Test
    public void test()
    {
        Bindings bindings = createMock(Bindings.class);
        BindingSupport s1 = createMock("s1", BindingSupport.class);
//        BindingSupport s1 = createMock("s1", BindingSupport.class);
        s1.addTo(bindings);
        s1.setForceDisableScriptExcecution(true);
//        expect(s1.remove(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING)).andReturn(null);
        replay(s1, bindings);

        RootNode root = new RootNode();
        root.formExpressionBindings(bindings);
        root.addBindingSupport("s1", s1);
        root.formExpressionBindings(bindings);
        root.removeBindingSupport("s1");
        
        verify(s1, bindings);
    }
}