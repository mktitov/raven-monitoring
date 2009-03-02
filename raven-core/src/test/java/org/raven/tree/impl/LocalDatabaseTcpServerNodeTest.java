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

import java.text.ParseException;
import org.junit.Test;
import org.raven.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class LocalDatabaseTcpServerNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws ParseException
    {
        LocalDatabaseTcpServerNode server = new LocalDatabaseTcpServerNode();
        tree.getRootNode().addAndSaveChildren(server);
        server.setPort(9093);
        assertTrue(server.start());
        server.stop();
    }
}