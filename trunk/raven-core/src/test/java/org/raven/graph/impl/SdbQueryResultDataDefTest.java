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

package org.raven.graph.impl;

import java.util.Arrays;
import java.util.Map;
import org.jrobin.data.Plottable;
import org.junit.Test;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.graph.DataDefException;
import org.raven.graph.GraphData;
import org.raven.statdb.impl.KeyValuesImpl;
import org.raven.statdb.impl.QueryResultImpl;
import org.raven.statdb.impl.SdbQueryResultNode;
import org.raven.statdb.impl.StatisticsValuesImpl;
import org.raven.statdb.query.KeyValues;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryResultDataDefTest extends RavenCoreTestCase
{
    @Test
    public void test() throws DataDefException
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        KeyValuesImpl keyValues1 = new KeyValuesImpl("/test/");
        KeyValuesImpl keyValues2 = new KeyValuesImpl("/test1/");
        keyValues2.addStatisticsValues(new StatisticsValuesImpl("s1", new double[]{10, 20}));
        keyValues2.addStatisticsValues(new StatisticsValuesImpl("s2", new double[]{100, 200}));
        QueryResultImpl res = new QueryResultImpl(
                Arrays.asList((KeyValues)keyValues1, (KeyValues)keyValues2));
        res.setTimestamps(new long[]{0, 100});

        ds.addDataPortion(res);

        SdbQueryResultDataDef def = new SdbQueryResultDataDef();
        def.setName("def");
        tree.getRootNode().addAndSaveChildren(def);
        def.setDataSource(ds);
        def.setKey("/test1/");
        def.setStatisticsName("s1");
        assertTrue(def.start());

        GraphData data = def.getData(0l, 100l);
        assertNotNull(data);
        Plottable points = data.getPlottable();
        assertNotNull(points);

        assertEquals(10., points.getValue(0l), 0.);
        assertEquals(20., points.getValue(100l), 0.);

        Map<String, NodeAttribute> attrs = ds.getLastSessionAttributes();
        assertNotNull(attrs);
        checkTimeAttr(SdbQueryResultNode.STARTTIME_SESSION_ATTRIBUTE, "0L", attrs);
        checkTimeAttr(SdbQueryResultNode.ENDTIME_SESSION_ATTRIBUTE, "100L", attrs);
    }

    private void checkTimeAttr(String attrName, String attrValue, Map<String, NodeAttribute> attrs)
    {
        NodeAttribute attr = attrs.get(attrName);
        assertNotNull(attr);
        assertEquals(String.class, attr.getType());
        assertEquals(attrValue, attr.getValue());
    }
}