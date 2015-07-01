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

package org.raven.auth.impl;

import java.util.Map;
import org.junit.Test;
import org.raven.auth.UserContext;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class UserContextConfiguratorNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        UserContext context = createMock(UserContext.class);
        Map params = createMock(Map.class);
        
        expect(context.getParams()).andReturn(params);
        expect(params.put(eq("test"), eq("value"))).andReturn(null);

        replay(context, params);

        UserContextConfiguratorNode contextConf = new UserContextConfiguratorNode();
        contextConf.setName("context configurator");
        tree.getRootNode().addAndSaveChildren(contextConf);
        contextConf.setExpression("userContext.params.test='value'");
        assertTrue(contextConf.start());

        contextConf.configure(context);

        verify(context, params);
    }
}