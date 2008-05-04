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

package org.raven.tree.store.impl;

import org.junit.Test;
import org.raven.tree.store.TreeStoreError;

/**
 *
 * @author Mikhail Titov
 */
public class H2TreeStoreTest 
{
    @Test
    public void test() throws TreeStoreError
    {
        H2TreeStore store = new H2TreeStore();
        store.init("jdbc:h2:tcp://localhost/~/test;TRACE_LEVEL_FILE=3", "sa", "");
    }
}
