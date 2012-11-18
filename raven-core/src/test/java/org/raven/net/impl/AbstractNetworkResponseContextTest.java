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

package org.raven.net.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.net.Authentication;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractNetworkResponseContextTest extends RavenCoreTestCase
{
    @Test
    public void authTest() throws Exception {
        TestNetworkResponseContext context = new TestNetworkResponseContext();
        context.setName("context");
        tree.getRootNode().addAndSaveChildren(context);
        assertTrue(context.start());
        assertFalse(context.getNeedsAuthentication());
        assertNull(context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR));
        assertNull(context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR));
        assertNull(context.getAuthentication());

        context.setNeedsAuthentication(true);
        NodeAttribute user = context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR);
        assertNotNull(user);
        NodeAttribute password =
                context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR);
        assertNotNull(password);

        user.setValue("user_name");
        password.setValue("user_password");

        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("user_name", auth.getUser());
        assertEquals("user_password", auth.getPassword());

        tree.reloadTree();

        context = (TestNetworkResponseContext) tree.getNode(context.getPath());
        assertNotNull(context);
        
        assertNotNull(context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR));
        assertNotNull(context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR));
        assertNotNull(context.getAuthentication());

        context.setNeedsAuthentication(false);
        assertNull(context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR));
        assertNull(context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR));
        assertNull(context.getAuthentication());
    }
}