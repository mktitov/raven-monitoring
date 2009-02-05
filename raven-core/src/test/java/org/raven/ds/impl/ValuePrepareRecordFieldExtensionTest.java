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

import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class ValuePrepareRecordFieldExtensionTest extends RavenCoreTestCase
{
    private ValuePrepareRecordFieldExtension valuePrepare;

    @Before
    public void prepare()
    {
        valuePrepare = new ValuePrepareRecordFieldExtension();
        valuePrepare.setName("valuePrepare");
        tree.getRootNode().addAndSaveChildren(valuePrepare);
        valuePrepare.start();
        assertEquals(Status.STARTED, valuePrepare.getStatus());
    }

    @Test
    public void convertToTypeTest() throws Exception
    {
        assertEquals("test", valuePrepare.prepareValue("test"));
        assertEquals(1, valuePrepare.prepareValue(1));

        valuePrepare.setConvertToType(Integer.class);
        assertEquals(1, valuePrepare.prepareValue("1"));
    }

    @Test
    public void useExpressionTest() throws Exception
    {
        valuePrepare.setExpression("value+1");
        valuePrepare.setUseExpression(true);

        assertEquals(2, valuePrepare.prepareValue(1));
    }
}