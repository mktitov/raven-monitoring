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
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceRecordFieldValueGeneratorTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        DataSourceRecordFieldValueGenerator fieldValue = new DataSourceRecordFieldValueGenerator();
        fieldValue.setName("fieldValue");
        tree.getRootNode().addAndSaveChildren(fieldValue);
        fieldValue.setDataSource(ds);
        assertTrue(fieldValue.start());

        ds.addDataPortion(1);
        assertEquals(1, fieldValue.getFieldValue(null));
    }
}