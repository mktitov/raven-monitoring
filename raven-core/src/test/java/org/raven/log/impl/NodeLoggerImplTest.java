/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.log.impl;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.log.NodeLogger;

/**
 *
 * @author Mikhail Titov
 */
public class NodeLoggerImplTest extends RavenCoreTestCase
{
    @Test
    public void serviceTest() throws Exception
    {
        NodeLogger nodeLogger = registry.getService(NodeLogger.class);
        assertNotNull(nodeLogger);
        assertNotNull(nodeLogger.getNodeLoggerNode());
    }
}