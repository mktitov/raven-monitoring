/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.ds.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class CsvReaderNodeTest extends RavenCoreTestCase {
    private CsvReaderNode csvReader;
    private PushDataSource ds;
    private DataCollector collector;
    
    
    @Before
    public void prepare() {
        ds = new PushDataSource();
        ds.setName("data source");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        csvReader = new CsvReaderNode();
        csvReader.setName("csv reader");
        testsNode.addAndSaveChildren(csvReader);
        csvReader.setDataSource(ds);
        assertTrue(csvReader.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(csvReader);
        assertTrue(collector.start());
    }
    
    @Test
    public void testDefault() {
        ds.pushData(new ByteArrayInputStream("v1.1,v1.2\nv2.1,\"v2.\n2\"".getBytes()));
        checkResults();
    }
    
    @Test
    public void testTDF() {
        csvReader.setFormat(CsvReaderNode.Format.TDF);
        ds.pushData(new ByteArrayInputStream("v1.1\tv1.2\nv2.1\t\"v2.\n2\"".getBytes()));
    }
    
    @Test
    public void testDelimiter() {
        csvReader.setDelimiter(":");
        ds.pushData(new ByteArrayInputStream("v1.1:v1.2\nv2.1:\"v2.\n2\"".getBytes()));
        checkResults();
    }
    
    @Test
    public void testQuoteChar() {
        csvReader.setQuoteChar("'");
        ds.pushData(new ByteArrayInputStream("v1.1,v1.2\nv2.1,'v2.\n2'".getBytes()));
        checkResults();
    }
    
    @Test
    public void testIgnoreEmptyLines() {
//        csvReader.setIgnoreEmptyLines(Boolean.TRUE);
        ds.pushData(new ByteArrayInputStream("v1.1,v1.2\n\nv2.1,\"v2.\n2\"".getBytes()));
        checkResults();
    }
    
    @Test
    public void testDataEncoding() throws Exception {
        csvReader.setDataEncoding(Charset.forName("utf-8"));
        ds.pushData(new ByteArrayInputStream("ç1,ç2".getBytes("utf-8")));
        assertEquals(2, collector.getDataListSize());
        assertArrayEquals(new Object[]{"ç1","ç2"}, (Object[]) collector.getDataList().get(0));
        assertNull(collector.getDataList().get(1));        
    }

    @Test
    public void testNullValue() throws Exception {
        ds.pushData(new ByteArrayInputStream(",v2".getBytes()));
        assertEquals(2, collector.getDataListSize());
        assertArrayEquals(new Object[]{null,"v2"}, (Object[]) collector.getDataList().get(0));
        assertNull(collector.getDataList().get(1));        
    }

    private void checkResults() {
        assertEquals(3,collector.getDataListSize());
        assertArrayEquals(new Object[]{"v1.1","v1.2"}, (Object[]) collector.getDataList().get(0));
        assertArrayEquals(new Object[]{"v2.1","v2.\n2"}, (Object[]) collector.getDataList().get(1));
        assertNull(collector.getDataList().get(2));
    }
}
