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
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceFieldValueGeneratorTest extends RavenCoreTestCase {
    private PushOnDemandDataSource ds;
    
    @Before
    public void prepare() {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());
        
    }
    
    @Test
    public void test() {
        DataSourceFieldValueGenerator fieldValue = new DataSourceFieldValueGenerator();
        fieldValue.setName("fieldValue");
        testsNode.addAndSaveChildren(fieldValue);
        fieldValue.setDataSource(ds);
        assertTrue(fieldValue.start());

        ds.addDataPortion(1);
        assertEquals(1, fieldValue.getFieldValue(new DataContextImpl()));
    }

    @Test
    public void expressionTest() {
        DataSourceFieldValueGenerator fieldValue = new DataSourceFieldValueGenerator();
        fieldValue.setName("fieldValue");
        testsNode.addAndSaveChildren(fieldValue);
        fieldValue.setDataSource(ds);
        fieldValue.setExpression("data+1");
        fieldValue.setUseExpression(true);
        assertTrue(fieldValue.start());

        ds.addDataPortion(1);
        assertEquals(2, fieldValue.getFieldValue(new DataContextImpl()));
    }
    
    @Test
    public void preprocessTest() {
        DataSourceFieldValueGenerator fieldValue = new DataSourceFieldValueGenerator();
        fieldValue.setName("fieldValue");
        testsNode.addAndSaveChildren(fieldValue);
        fieldValue.setDataSource(ds);
        fieldValue.setUsePreProcess(Boolean.TRUE);
        fieldValue.setPreProcess("context.parameters.testValue");
        assertTrue(fieldValue.start());
        DataContext context = new DataContextImpl();
        context.putAt("testValue", 10);
        assertEquals(10, fieldValue.getFieldValue(context));
    }
}