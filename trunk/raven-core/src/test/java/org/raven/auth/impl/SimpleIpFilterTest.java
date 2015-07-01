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

/**
 *
 * @author MIkhail Titov
 */
public class SimpleIpFilterTest extends RavenCoreTestCase {
    @Test
    public void test() throws Exception {
        SimpleIpFilter filter = new SimpleIpFilter();
        filter.setName("1.1.1.1");
        testsNode.addAndSaveChildren(filter);
        assertTrue(filter.start());
        assertTrue(filter.isIpAllowed("1.1.1.1"));
        assertFalse(filter.isIpAllowed("2.1.1.1"));
    }
}
