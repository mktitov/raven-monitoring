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

package org.raven.ds.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractRecordFieldExtensionTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        AbstractRecordFieldExtension extension = new AbstractRecordFieldExtension();
        extension.setName("ext");

        assertEquals("1", extension.prepareValue("1", null));

        ValuePrepareRecordFieldExtension valuePrepare = new ValuePrepareRecordFieldExtension();
        valuePrepare.setName("prepare");
        extension.addAndSaveChildren(valuePrepare);
        valuePrepare.setConvertToType(Integer.class);
        valuePrepare.start();
        assertEquals(Status.STARTED, valuePrepare.getStatus());
        
        assertEquals(1, extension.prepareValue("1", null));
    }
}