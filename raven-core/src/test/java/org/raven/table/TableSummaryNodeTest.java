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
import org.raven.ds.impl.ValuesAggregatorNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;

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
        createAggregation("colAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.COLUMN, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.setTitle("table title");
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertEquals("table title", resTable.getTitle());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[]{1, 2}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3}, rows.get(1));
        assertArrayEquals(new Object[]{3., 5.}, rows.get(2));

        assertTrue(resTable.containsRowTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getRowTags(0));
        assertNull(resTable.getRowTags(1));
    }

    @Test
    public void columnAggregationCustomFunctionTest() throws Exception
    {
        TableValuesAggregatorNode agg = createAggregation(
                "colAgg", null, "columnNumber==2", AggregateFunctionType.CUSTOM, AggregationDirection.COLUMN, null);
//        agg.setUseStartAggregationExpression(Boolean.TRUE);
//        agg.setUseFinishAggregationExpression(Boolean.TRUE);
        agg.setAggregationExpression("println \"value=$value nextValue=$nextValue row[0]=${row[0]}\"; (value?:0)+nextValue+row[0]");
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.setTitle("table title");
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertEquals("table title", resTable.getTitle());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[]{null, 8}, rows.get(2));

        assertTrue(resTable.containsRowTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
    }

    @Test
    public void columnAggregationTitleAndSelectorTest() throws Exception
    {
        createAggregation("colAgg", "sum", "columnNumber>1", AggregateFunctionType.SUM, AggregationDirection.COLUMN, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table) dataList.get(0));
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 2}, rows.get(0));
        assertArrayEquals(new Object[]{"sum", 2.}, rows.get(1));
    }

    @Test
    public void severalColumnAggregationsTest() throws Exception
    {
        createAggregation("colAgg1", null, null, AggregateFunctionType.SUM, AggregationDirection.COLUMN, null);
        createAggregation("colAgg2", null, null, AggregateFunctionType.COUNT, AggregationDirection.COLUMN, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        List<Object[]> rows = RavenUtils.tableAsList((Table) dataList.get(0));
        assertEquals(4, rows.size());
        assertArrayEquals(new Object[]{1, 2}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3}, rows.get(1));
        assertArrayEquals(new Object[]{3., 5.}, rows.get(2));
        assertArrayEquals(new Object[]{2, 2}, rows.get(3));

        Table resTable = (Table)dataList.get(0);
        assertNull(resTable.getRowTags(0));
        assertNull(resTable.getRowTags(1));
        assertTrue(resTable.containsRowTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertTrue(resTable.containsRowTag(3, TableSummaryNode.AGGREGATION_TAG_ID));

    }

    @Test
    public void rowAggregationTest() throws Exception
    {
        createAggregation("rowAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.ROW, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col2", null}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 2, 3.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3, 5.}, rows.get(1));

        assertTrue(resTable.containsColumnTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
    }

    @Test
    public void rowAggregationTitleTest() throws Exception
    {
        createAggregation("rowAgg", "sum", null, AggregateFunctionType.SUM, AggregationDirection.ROW, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col2", "sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 2, 3.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3, 5.}, rows.get(1));
    }

    @Test
    public void rowAggregationSelectorTest() throws Exception
    {
        createAggregation("rowAgg", "sum", "(rowNumber+row[0])==2", AggregateFunctionType.SUM, AggregationDirection.ROW, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col2", "sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 2, 3.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3, null}, rows.get(1));
    }

    @Test
    public void severalRowAggregationTest() throws Exception
    {
        createAggregation("rowAgg1", "sum", null, AggregateFunctionType.SUM, AggregationDirection.ROW, null);
        createAggregation("rowAgg2", "count", null, AggregateFunctionType.COUNT, AggregationDirection.ROW, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col2", "sum", "count"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 2, 3., 2}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3, 5., 2}, rows.get(1));

        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertTrue(resTable.containsColumnTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertTrue(resTable.containsColumnTag(3, TableSummaryNode.AGGREGATION_TAG_ID));
    }

    @Test
    public void rowAggregationWithGroupExpressionTest() throws Exception
    {
        createAggregation("rowAgg", "groupValue+' sum'", null, AggregateFunctionType.SUM, AggregationDirection.ROW, "columnName[0..3]");
        TableImpl table = new TableImpl(new String[]{"col1", "col11", "col2"});
        table.addRow(new Object[]{1, 10, 2});
        table.addRow(new Object[]{2, 11, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col11", "col1 sum", "col2", "col2 sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 10, 11., 2, 2.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 11, 13., 3, 3.}, rows.get(1));

        assertTrue(resTable.containsColumnTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertTrue(resTable.containsColumnTag(4, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertNull(resTable.getColumnTags(3));
    }

    @Test
    public void rowAggregationWithGroupExpressionTest2() throws Exception
    {
        createAggregation("rowAgg", "groupValue+' sum'", null, AggregateFunctionType.CUSTOM, AggregationDirection.ROW, "columnName[0..3]");
        tablesum.getChildren("rowAgg").getNodeAttribute(ValuesAggregatorNode.USE_FINISH_AGGREGATION_EXPRESSION_ATTR).setValue("true");
        tablesum.getChildren("rowAgg").getNodeAttribute(ValuesAggregatorNode.FINISH_AGGREGATION_EXPRESSION_ATTR).setValue("sum=0; row.each{sum+=it}; sum-value");
        tablesum.getChildren("rowAgg").getNodeAttribute(ValuesAggregatorNode.AGGREGATION_EXPRESSION_ATTR).setValue("val=(value?:0)+nextValue;println '!!! val='+val; val");
        TableImpl table = new TableImpl(new String[]{"col1", "col11", "col2"});
        table.addRow(new Object[]{1, 10, 2});
        table.addRow(new Object[]{2, 11, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col11", "col1 sum", "col2", "col2 sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 10, 2, 2, 11}, rows.get(0));
        assertArrayEquals(new Object[]{2, 11, 3, 3, 13}, rows.get(1));

        assertTrue(resTable.containsColumnTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertTrue(resTable.containsColumnTag(4, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertNull(resTable.getColumnTags(3));
    }

    @Test
    public void rowAggregationWithGroupExpressionAndGroupValidatorExpressionTest() throws Exception
    {
        TableValuesAggregatorNode aggDef = createAggregation(
                "rowAgg", "groupValue+' sum'", null, AggregateFunctionType.SUM, AggregationDirection.ROW, "columnName[0..3]");
        NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.GROUP_VALIDATOR_EXPRESSION_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("groupColumn!=3 && groupFromColumn==3 && groupToColumn==3 && groupValue!='col1'");
        TableImpl table = new TableImpl(new String[]{"col1", "col11", "col2"});
        table.addRow(new Object[]{1, 10, 2});
        table.addRow(new Object[]{2, 11, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col11", "col2", "col2 sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 10, 2, 2.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 11, 3, 3.}, rows.get(1));

        assertTrue(resTable.containsColumnTag(3, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertNull(resTable.getColumnTags(2));
    }

    @Test
    public void rowAggregationWithGroupExpressionAndGroupValidatorExpressionTest2() throws Exception
    {
        TableValuesAggregatorNode aggDef = createAggregation(
                "rowAgg", "groupValue+' sum'", null, AggregateFunctionType.SUM, AggregationDirection.ROW, "columnName[0..3]");
        NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.GROUP_VALIDATOR_EXPRESSION_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("groupColumn==3");
        TableImpl table = new TableImpl(new String[]{"col1", "col11", "col2"});
        table.addRow(new Object[]{1, 10, 2});
        table.addRow(new Object[]{2, 11, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col11", "col1 sum", "col2"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{1, 10, 11., 2}, rows.get(0));
        assertArrayEquals(new Object[]{2, 11, 13., 3}, rows.get(1));

        assertTrue(resTable.containsColumnTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertNull(resTable.getColumnTags(3));
    }

    @Test
    public void rowAggregationWithGroupExpressionAndGroupValidatorExpressionTest3() throws Exception {
        TableValuesAggregatorNode aggDef = createAggregation(
                "rowAgg", "groupValue+' sum'", null, AggregateFunctionType.SUM, AggregationDirection.ROW, "columnName[0..3]");
        NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.GROUP_VALIDATOR_EXPRESSION_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("groupColumn==4");
        TableImpl table = new TableImpl(new String[]{"col2", "col1", "col11"});
        table.addRow(new Object[]{2, 1, 10});
        table.addRow(new Object[]{3, 2, 11});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col2", "col1", "col11", "col1 sum"}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(2, rows.size());
        assertArrayEquals(new Object[]{2, 1, 10, 11.}, rows.get(0));
        assertArrayEquals(new Object[]{3, 2, 11, 13.}, rows.get(1));

        assertTrue(resTable.containsColumnTag(3, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getColumnTags(0));
        assertNull(resTable.getColumnTags(1));
        assertNull(resTable.getColumnTags(2));
    }

    @Test
    public void colAggregationWithrowAggregationTest() throws Exception
    {
        createAggregation("rowAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.ROW, null);
        createAggregation("colAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.COLUMN, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.addRow(new Object[]{1, 2});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertArrayEquals(new String[]{"col1", "col2", null}, resTable.getColumnNames());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[]{1, 2, 3.}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3, 5.}, rows.get(1));
        assertArrayEquals(new Object[]{3., 5., 8.}, rows.get(2));
    }

     @Test
    public void aggregationWithNotValidTest() throws Exception
    {
        createAggregation("colAgg", null, null, AggregateFunctionType.SUM, AggregationDirection.COLUMN, null);
        TableImpl table = new TableImpl(new String[]{"col1", "col2"});
        table.setTitle("table title");
        table.addRow(new Object[]{1, "blabla"});
        table.addRow(new Object[]{2, 3});

        ds.addDataPortion(table);

        List dataList = (List) consumer.refereshData(null);
        assertNotNull(dataList);
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Table);

        Table resTable = (Table)dataList.get(0);
        assertEquals("table title", resTable.getTitle());
        List<Object[]> rows = RavenUtils.tableAsList(resTable);
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[]{1, "blabla"}, rows.get(0));
        assertArrayEquals(new Object[]{2, 3}, rows.get(1));
        assertArrayEquals(new Object[]{3., 3.}, rows.get(2));

        assertTrue(resTable.containsRowTag(2, TableSummaryNode.AGGREGATION_TAG_ID));
        assertNull(resTable.getRowTags(0));
        assertNull(resTable.getRowTags(1));
    }

   private TableValuesAggregatorNode createAggregation(
            String name, String title, String selectorExpression, AggregateFunctionType aggType
            , AggregationDirection dir, String groupExpression)
        throws Exception
    {
        TableValuesAggregatorNode aggDef = new TableValuesAggregatorNode();
        aggDef.setName(name);
        tablesum.addAndSaveChildren(aggDef);
        if (groupExpression==null || title==null)
            aggDef.setTitle(title);
        else
        {
            NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.TITLE_ATTR);
            attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
            attr.setValue(title);
        }
        if (selectorExpression!=null)
        {
            NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.SELECTOR_ATTR);
            attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
            aggDef.getNodeAttribute(TableValuesAggregatorNode.SELECTOR_ATTR).setValue(selectorExpression);
        }
        if (groupExpression!=null)
        {
            NodeAttribute attr = aggDef.getNodeAttribute(TableValuesAggregatorNode.GROUP_EXPRESSION_ATTR);
            attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
            attr.setValue(groupExpression);
        }
        aggDef.setAggregateFunction(aggType);
        aggDef.setAggregationDirection(dir);
        assertTrue(aggDef.start());

        return aggDef;
    }
}