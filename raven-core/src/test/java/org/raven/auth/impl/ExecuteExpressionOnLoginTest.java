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
package org.raven.auth.impl;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.auth.UserContext;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ExecuteExpressionOnLoginTest extends RavenCoreTestCase {
    
    @Test
    public void test() {
        ExecuteExpressionOnLogin listener = new ExecuteExpressionOnLogin();
        listener.setName("login listener");
        testsNode.addAndSaveChildren(listener);
        listener.setOnUserLoggedIn("user.params.test='value'");
        assertTrue(listener.start());
        
        UserContext user = createMock(UserContext.class);
        Map<String, Object> params = createMock(Map.class);
        expect(user.getParams()).andReturn(params);
        expect(params.put("test", "value")).andReturn(null);
        replay(user, params);
        
        listener.userLoggedIn(user);
        
        verify(user, params);
    }
}
