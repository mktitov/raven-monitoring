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

package org.raven.table;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenUtils;
import org.raven.ds.AggregateFunctionType;
import org.raven.ds.impl.SafeDataConsumer;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TableSummaryNodeTest extends RavenCoreTestCase
{
    private TableSummaryNode tablesum;
    private PushOnDemandDataSource ds;
    private SafeDataConsumer consumer;

    @Before
    public void prepare()
    {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        tablesum = new TableSummaryNode();
        tablesum.setName("table summary");
        tree.getRootNode().addAndSaveChildren(tablesum);
        tablesum.setDataSource(ds);
        assertTrue(tablesum.start());

        consumer = new SafeDataConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addAndSaveChildren(consumer);
        consumer.setDataSource(tablesum);
        assertTrue(consumer.start());
    }

    @Test
    public void columnAggregationTest() throws Exception
    {
        createAggregation("colAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.COLUMN);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table) dataList.get(0));
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[]{1, 2}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3}, rows.get(1));
        assertArrayEquals(new Object[]{3., 5.}, rows.get(2));
    }

    private void createAggregation(
            String name, String title, String selectorExpression, AggregateFunctionType aggType
            , AggregationDirection dir)
        throws Exception
    {
        TableValuesAggregatorNode aggDef = new TableValuesAggregatorNode();
        aggDef.setName(name);
        tablesum.addAndSaveChildren(aggDef);
        aggDef.setTitle(title);
        if (selectorExpression!=null)
            aggDef.getNodeAttribute(TableValuesAggregatorNode.SELECTOR_ATTR).setValue(selectorExpression);
        aggDef.setAggregateFunction(aggType);
        aggDef.setAggregationDirection(dir);
        assertTrue(aggDef.start());
    }
}