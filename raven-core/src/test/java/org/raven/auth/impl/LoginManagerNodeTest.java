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

import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class LoginManagerNodeTest extends RavenCoreTestCase {
    
    @Test
    public void structureTest() {
        LoginManagerNode manager = new LoginManagerNode();
        tree.getRootNode().addAndSaveChildren(manager);
        assertTrue(manager.start());
        Node systemService = manager.getNode(SystemLoginService.NAME);
        assertNotNull(systemService);
        assertTrue(systemService instanceof SystemLoginService);
        assertStarted(systemService);
    }
}
