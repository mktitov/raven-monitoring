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

package org.raven.statdb.rrd;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdException;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.RecordException;
import org.raven.rrd.ConsolidationFunction;
import org.raven.test.PushDataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.OrderClause;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectEntry;
import org.raven.statdb.query.SelectMode;
import org.raven.statdb.query.StatisticsValues;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsDatabaseNodeTest extends RavenCoreTestCase
{
	private RrdStatisticsDatabaseNode db;
	private PushDataSource ds;
	private RrdUpdateQueueNode queue;
    private RecordSchemaNode schema;

	@Before
	public void prepare()
	{
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        createDatabaseTemplate("t1");
        createDatabaseTemplate("t2");

        RecordSchemaFieldNode timeField = new RecordSchemaFieldNode();
        timeField.setName(StatisticsRecord.TIME_FIELD_NAME);
        schema.addAndSaveChildren(timeField);
        timeField.setFieldType(RecordSchemaFieldType.TIMESTAMP);
        assertTrue(timeField.start());

        RecordSchemaFieldNode keyField = new RecordSchemaFieldNode();
        keyField.setName(StatisticsRecord.KEY_FIELD_NAME);
        schema.addAndSaveChildren(keyField);
        keyField.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(keyField.start());

        createField("s1", "t1");
        createField("s2", "t2");

		ds = new PushDataSource();
		ds.setName("ds");
		ds.setParent(tree.getRootNode());
		ds.save();
		tree.getRootNode().addChildren(ds);
		ds.init();
		ds.start();
		assertEquals(Status.STARTED, ds.getStatus());

		queue = new RrdUpdateQueueNode();
		queue.setName("queue");
		queue.setParent(tree.getRootNode());
		queue.save();
		tree.getRootNode().addChildren(queue);
		queue.init();
		queue.setCorePoolSize(1);
		queue.start();
		assertEquals(Status.STARTED, queue.getStatus());

		db = new RrdStatisticsDatabaseNode();
		db.setName("db");
		db.setParent(tree.getRootNode());
		db.save();
		tree.getRootNode().addChildren(db);
		db.init();
		db.setStep(5l);
		db.setDataSource(ds);
		db.setStartTime("epoch");
		db.setUpdateQueue(queue);
        db.setRecordSchema(schema);
		db.start();
		assertEquals(Status.STARTED, db.getStatus());
	}

	@Test
	public void updateTest() throws Exception
	{
		Record record = createRecord("/1/1/", 5, 1., 10.);
		ds.pushData(record);
		record = createRecord("/1/2/", 5, 5., 15.);
		ds.pushData(record);

		record = createRecord("/1/1/", 10, 2., 11.);
		ds.pushData(record);
		record = createRecord("/1/2/", 10, 6., 16.);
		ds.pushData(record);

		TimeUnit.SECONDS.sleep(1);

		//checking values
		String dbFilenamePrefix = "target/stat_db/"+db.getId()+"/";
		double[] values;
		values = fetchData(dbFilenamePrefix+"1/1/s1.jrb");
		assertNotNull(values);
		assertEquals(2, values.length);
		assertTrue(Arrays.equals(new double[]{1., 2.}, values));

		values = fetchData(dbFilenamePrefix+"1/1/s2.jrb");
		assertNotNull(values);
		assertEquals(2, values.length);
		assertTrue(Arrays.equals(new double[]{10., 11.}, values));

		values = fetchData(dbFilenamePrefix+"1/2/s1.jrb");
		assertNotNull(values);
		assertEquals(2, values.length);
		assertTrue(Arrays.equals(new double[]{5., 6.}, values));
		
		values = fetchData(dbFilenamePrefix+"1/2/s2.jrb");
		assertNotNull(values);
		assertEquals(2, values.length);
		assertTrue(Arrays.equals(new double[]{15., 16.}, values));
	}
    //ValueType.INTEGRATED
    //dataStep < queryStep and queryStep % dataStep == 0
    @Test
    public void realignDataTest1()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{2, 4, 6, 8};
        double[] data = new double[]{2., 4., 4., 8.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 4, dataTs, 2, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{6., 12.}, result));
    }

    //ValueType.INTEGRATED
    //dataStep < queryStep and queryStep % dataStep != 0
    @Test
    public void realignDataTest2()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{3, 6, 9};
        double[] data = new double[]{2., 3., 6.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 4, dataTs, 3, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{3., 6.}, result));
    }

    //ValueType.INTEGRATED
    //dataStep > queryStep and dataStep % queryStep == 0
    @Test
    public void realignDataTest3()
    {
        long[] ts = new long[]{2, 4};
        long[] dataTs = new long[]{4};
        double[] data = new double[]{4.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 2, dataTs, 4, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{2., 2.}, result));
    }

    //ValueType.INTEGRATED
    //dataStep > queryStep and dataStep % queryStep != 0
    @Test
    public void realignDataTest4()
    {
        long[] ts = new long[]{3, 6};
        long[] dataTs = new long[]{4, 8};
        double[] data = new double[]{4., 8.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 3, dataTs, 4, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{3., 5.}, result));
    }

    //ValueType.ABSOLUTE
    //dataStep < queryStep and queryStep % dataStep == 0
    @Test
    public void realignDataTest5()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{2, 4, 6, 8};
        double[] data = new double[]{2., 4., 4., 8.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                false, AggregationFunction.AVERAGE, ts, 4, dataTs, 2, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{3., 6.}, result));
    }

    //ValueType.ABSOLUTE
    //dataStep < queryStep and queryStep % dataStep != 0
    @Test
    public void realignDataTest6()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{3, 6, 9};
        double[] data = new double[]{2., 3., 6.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                false, AggregationFunction.AVERAGE, ts, 4, dataTs, 3, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{2.5, 4.5}, result));
    }

    //dataTimestamp[0]>queryTimestamp[1]
    @Test
    public void realignDataTest7()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{6, 8};
        double[] data = new double[]{4., 8.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 4, dataTs, 2, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{Double.NaN, 12.}, result));
    }

    //dataTimestamp[0]>queryTimestamp[1]
    @Test
    public void realignDataTest8()
    {
        long[] ts = new long[]{4, 8};
        long[] dataTs = new long[]{2, 4};
        double[] data = new double[]{4., 8.};
        double[] result = RrdStatisticsDatabaseNode.realignData(
                true, AggregationFunction.AVERAGE, ts, 4, dataTs, 2, data);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertTrue(Arrays.equals(new double[]{12., Double.NaN}, result));
    }

	@Test
	public void query_selectKeys_emptyDb_test() throws Exception
	{
		Query query = createMock(Query.class);
		FromClause from = createMock(FromClause.class);
        SelectClause select = createMock(SelectClause.class);
		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS);
		expect(from.getKeyExpression()).andReturn("/@r .*/").atLeastOnce();

		replay(query, from, select);

		QueryResult result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(0, result.getKeyValues().size());

		verify(query, from, select);
	}

	@Test
	public void query_selectKeys_exactKeyMatch_test() throws Exception
	{
		String dbFilenamePrefix = "target/stat_db/"+db.getId()+"/";

		new File(dbFilenamePrefix+"/1").mkdir();
		new File(dbFilenamePrefix+"/2/1").mkdirs();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        SelectClause select = createMock(SelectClause.class);
		expect(query.getFromClause()).andReturn(from).times(2);
        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS).times(2);
		expect(from.getKeyExpression()).andReturn("/1/");
		expect(from.getKeyExpression()).andReturn("/2/1");

		replay(query, from, select);

		QueryResult result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(1, result.getKeyValues().size());
		assertEquals("/1/", result.getKeyValues().iterator().next().getKey());

		result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(1, result.getKeyValues().size());
		assertEquals("/2/1/", result.getKeyValues().iterator().next().getKey());

		verify(query, from, select);
	}

	@Test
	public void query_selectKeys_regexpMatch() throws Exception
	{
		String dbFilenamePrefix = "target/stat_db/"+db.getId()+"/";

		new File(dbFilenamePrefix+"/1/1").mkdirs();
		new File(dbFilenamePrefix+"/2/1").mkdirs();
		new File(dbFilenamePrefix+"/2/2").mkdirs();
		new File(dbFilenamePrefix+"/2/3").mkdirs();
		new File(dbFilenamePrefix+"/3/1").mkdirs();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        SelectClause select = createMock(SelectClause.class);
		expect(query.getFromClause()).andReturn(from).times(2);
        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS).times(2);
		expect(from.getKeyExpression()).andReturn("/@r [23]/1/");
		expect(from.getKeyExpression()).andReturn("/@r [23]/@r ^2/");

		replay(query, from, select);

		QueryResult result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(2, result.getKeyValues().size());
		Set<String> keys = new HashSet<String>();
		for (KeyValues keyValues: result.getKeyValues())
			keys.add(keyValues.getKey());
		assertTrue(keys.contains("/2/1/"));
		assertTrue(keys.contains("/3/1/"));

		result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(1, result.getKeyValues().size());
		assertEquals("/2/2/", result.getKeyValues().iterator().next().getKey());

		verify(query, from, select);
	}

    @Test
    public void query_selectAll_test() throws Exception
    {
        insertData();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        QueryStatisticsName s1name = createMock("s1", QueryStatisticsName.class);
        QueryStatisticsName s2name = createMock("s2", QueryStatisticsName.class);
        SelectClause select = createMock(SelectClause.class);

		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectClause()).andReturn(select).atLeastOnce();
        expect(query.getStatisticsNames()).andReturn(new QueryStatisticsName[]{s1name, s2name});
        expect(s1name.getName()).andReturn("s1").atLeastOnce();
        expect(s1name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(s2name.getName()).andReturn("s2").atLeastOnce();
        expect(s2name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(query.getStartTime()).andReturn("5L");
        expect(query.getEndTime()).andReturn("10L");
        expect(query.getStep()).andReturn(5l);
        expect(query.getOrderClause()).andReturn(null).anyTimes();

        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS_AND_DATA);
        expect(select.getSelectEntries()).andReturn(null);

		expect(from.getKeyExpression()).andReturn("/@r .*/@r .*/");

        replay(query, from, select, s1name, s2name);

        QueryResult result = db.executeQuery(query);
        assertNotNull(result);

        Collection<KeyValues> keys = result.getKeyValues();
        assertNotNull(keys);
        assertEquals(2, keys.size());

        KeyValues k11, k12; k11 = k12 = null;
        for (KeyValues key: keys)
            if (key.getKey().equals("/1/1/"))
                k11 = key;
            else
                k12 = key;

        assertNotNull(k11);
        assertNotNull(k12);

        checkStatisticsValues(k11, new double[]{1., 2.}, new double[]{10., 11.});
        checkStatisticsValues(k12, new double[]{5., 6.}, new double[]{15., 16.});

        verify(query, from, select, s1name, s2name);
    }

    @Test
    public void query_selectEntry_test() throws Exception
    {
        insertData();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        QueryStatisticsName s1name = createMock("s1", QueryStatisticsName.class);
        QueryStatisticsName s2name = createMock("s2", QueryStatisticsName.class);
        SelectClause select = createMock(SelectClause.class);
        SelectEntry selectEntry = createMock(SelectEntry.class);

		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectClause()).andReturn(select).atLeastOnce();
        expect(query.getStatisticsNames()).andReturn(new QueryStatisticsName[]{s1name, s2name});
        expect(s1name.getName()).andReturn("s1").atLeastOnce();
        expect(s1name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(s2name.getName()).andReturn("s2").atLeastOnce();
        expect(s2name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(query.getStartTime()).andReturn("5L");
        expect(query.getEndTime()).andReturn("10L");
        expect(query.getStep()).andReturn(5l);
        expect(query.getOrderClause()).andReturn(null).anyTimes();

        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS_AND_DATA);
        expect(select.getSelectEntries()).andReturn(new SelectEntry[]{selectEntry});
        expect(selectEntry.getExpression()).andReturn("s1+s2").atLeastOnce();
        expect(selectEntry.getName()).andReturn("e1").atLeastOnce();

		expect(from.getKeyExpression()).andReturn("/@r .*/@r .*/");

        replay(query, from, select, s1name, s2name, selectEntry);

        QueryResult result = db.executeQuery(query);
        assertNotNull(result);

        Collection<KeyValues> keys = result.getKeyValues();
        assertNotNull(keys);
        assertEquals(2, keys.size());

        KeyValues k11, k12; k11 = k12 = null;
        for (KeyValues key: keys)
            if (key.getKey().equals("/1/1/"))
                k11 = key;
            else
                k12 = key;

        assertNotNull(k11);
        assertNotNull(k12);

        checkStatisticsValues(k11, new String[]{"e1"}, new double[]{11., 13.});
        checkStatisticsValues(k12, new String[]{"e1"}, new double[]{20., 22.});

        verify(query, from, select, s1name, s2name, selectEntry);

    }

    @Test
    public void query_aggregateSelectEntry_test() throws Exception
    {
        insertData();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        QueryStatisticsName s1name = createMock("s1", QueryStatisticsName.class);
        QueryStatisticsName s2name = createMock("s2", QueryStatisticsName.class);
        SelectClause select = createMock(SelectClause.class);
        SelectEntry selectEntry = createMock(SelectEntry.class);

		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectClause()).andReturn(select).atLeastOnce();
        expect(query.getStatisticsNames()).andReturn(new QueryStatisticsName[]{s1name, s2name});
        expect(s1name.getName()).andReturn("s1").atLeastOnce();
        expect(s1name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(s2name.getName()).andReturn("s2").atLeastOnce();
        expect(s2name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(query.getStartTime()).andReturn("5L");
        expect(query.getEndTime()).andReturn("10L");
        expect(query.getStep()).andReturn(5l);
        expect(query.getOrderClause()).andReturn(null).anyTimes();

        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS_AND_DATA);
        expect(select.getSelectEntries()).andReturn(new SelectEntry[]{selectEntry});
        expect(selectEntry.getExpression()).andReturn("$sum{s1+s2}").atLeastOnce();
        expect(selectEntry.getName()).andReturn("e1").atLeastOnce();

		expect(from.getKeyExpression()).andReturn("/@r .*/@r .*/");

        replay(query, from, select, s1name, s2name, selectEntry);

        QueryResult result = db.executeQuery(query);
        assertNotNull(result);

        Collection<KeyValues> keys = result.getKeyValues();
        assertEquals(1, result.getValuesCount());
        assertArrayEquals(new long[]{10}, result.getTimestamps());
        assertEquals(10, result.getStep());
        assertNotNull(keys);
        assertEquals(2, keys.size());

        KeyValues k11, k12; k11 = k12 = null;
        for (KeyValues key: keys)
            if (key.getKey().equals("/1/1/"))
                k11 = key;
            else
                k12 = key;

        assertNotNull(k11);
        assertNotNull(k12);

        checkStatisticsValues(k11, new String[]{"e1"}, new double[]{24.});
        checkStatisticsValues(k12, new String[]{"e1"}, new double[]{42.});

        verify(query, from, select, s1name, s2name, selectEntry);

    }

    @Test
    public void query_order_test() throws Exception
    {
        insertData();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        QueryStatisticsName s1name = createMock("s1", QueryStatisticsName.class);
        QueryStatisticsName s2name = createMock("s2", QueryStatisticsName.class);
        SelectClause select = createMock(SelectClause.class);
        OrderClause order = createMock(OrderClause.class);

		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectClause()).andReturn(select).atLeastOnce();
        expect(query.getStatisticsNames()).andReturn(new QueryStatisticsName[]{s1name, s2name});
        expect(s1name.getName()).andReturn("s1").atLeastOnce();
        expect(s1name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(s2name.getName()).andReturn("s2").atLeastOnce();
        expect(s2name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(query.getStartTime()).andReturn("5L");
        expect(query.getEndTime()).andReturn("10L");
        expect(query.getStep()).andReturn(5l);

        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS_AND_DATA);
        expect(select.getSelectEntries()).andReturn(null);

		expect(from.getKeyExpression()).andReturn("/@r .*/@r .*/");

        expect(query.getOrderClause()).andReturn(order).anyTimes();
        expect(order.getStatisticsName()).andReturn("s1");
        expect(order.getReverseOrder()).andReturn(false).anyTimes();

        replay(query, from, select, s1name, s2name, order);

        QueryResult result = db.executeQuery(query);
        assertNotNull(result);

        Collection<KeyValues> keys = result.getKeyValues();
        assertNotNull(keys);
        assertEquals(2, keys.size());

        KeyValues[] keyValues = new KeyValues[2];
        keys.toArray(keyValues);
        assertEquals("/1/1/", keyValues[0].getKey());
        assertEquals("/1/2/", keyValues[1].getKey());
        verify(query, from, select, s1name, s2name, order);
    }

    @Test
    public void query_reverseOrder_test() throws Exception
    {
        insertData();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
        QueryStatisticsName s1name = createMock("s1", QueryStatisticsName.class);
        QueryStatisticsName s2name = createMock("s2", QueryStatisticsName.class);
        SelectClause select = createMock(SelectClause.class);
        OrderClause order = createMock(OrderClause.class);

		expect(query.getFromClause()).andReturn(from);
        expect(query.getSelectClause()).andReturn(select).atLeastOnce();
        expect(query.getStatisticsNames()).andReturn(new QueryStatisticsName[]{s1name, s2name});
        expect(s1name.getName()).andReturn("s1").atLeastOnce();
        expect(s1name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(s2name.getName()).andReturn("s2").atLeastOnce();
        expect(s2name.getAggregationFunction()).andReturn(AggregationFunction.LAST).atLeastOnce();
        expect(query.getStartTime()).andReturn("5L");
        expect(query.getEndTime()).andReturn("10L");
        expect(query.getStep()).andReturn(5l);

        expect(query.getSelectMode()).andReturn(SelectMode.SELECT_KEYS_AND_DATA);
        expect(select.getSelectEntries()).andReturn(null);

		expect(from.getKeyExpression()).andReturn("/@r .*/@r .*/");

        expect(query.getOrderClause()).andReturn(order).anyTimes();
        expect(order.getStatisticsName()).andReturn("s1");
        expect(order.getReverseOrder()).andReturn(true).anyTimes();

        replay(query, from, select, s1name, s2name, order);

        QueryResult result = db.executeQuery(query);
        assertNotNull(result);

        Collection<KeyValues> keys = result.getKeyValues();
        assertNotNull(keys);
        assertEquals(2, keys.size());

        KeyValues[] keyValues = new KeyValues[2];
        keys.toArray(keyValues);
        assertEquals("/1/2/", keyValues[0].getKey());
        assertEquals("/1/1/", keyValues[1].getKey());
        verify(query, from, select, s1name, s2name, order);
    }

    private void insertData() throws InterruptedException, RecordException
    {
		Record record = createRecord("/1/1/", 5, 1., 10.);
		ds.pushData(record);
		record = createRecord("/1/2/", 5, 5., 15.);
		ds.pushData(record);

		record = createRecord("/1/1/", 10, 2., 11.);
		ds.pushData(record);
		record = createRecord("/1/2/", 10, 6., 16.);
		ds.pushData(record);

		TimeUnit.SECONDS.sleep(1);

    }

    private void checkStatisticsValues(KeyValues keys, String[] statNames, double[]... values)
    {
        for (int i=0; i<statNames.length; ++i)
        {
            StatisticsValues statValues = null;
            for (StatisticsValues vals: keys.getStatisticsValues())
                if (vals.getStatisticsName().equals(statNames[i]))
                {
                    statValues = vals;
                    break;
                }
            assertNotNull(statValues);
            assertTrue(Arrays.equals(values[i], statValues.getValues()));
        }
    }

    private void checkStatisticsValues(KeyValues k11, double[] s1Values, double[] s2Values)
    {
        Collection<StatisticsValues> statisticsValueses = k11.getStatisticsValues();
        assertNotNull(statisticsValueses);
        assertEquals(2, statisticsValueses.size());

        StatisticsValues s1, s2; s1 = s2 = null;
        for (StatisticsValues values: statisticsValueses)
            if (values.getStatisticsName().equals("s1"))
                s1 = values;
            else
                s2 = values;

        assertNotNull(s1);
        assertNotNull(s2);

//        assertTrue(Arrays.equals(new long[]{5l, 10l}, s1.getTimestamps()));
        assertTrue(Arrays.equals(s1Values, s1.getValues()));
        
//        assertTrue(Arrays.equals(new long[]{5l, 10l}, s2.getTimestamps()));
        assertTrue(Arrays.equals(s2Values, s2.getValues()));
    }

	private double[] fetchData(String path) throws IOException, RrdException
	{
		RrdDb db = RrdDbPool.getInstance().requestRrdDb(path);
		FetchRequest req = db.createFetchRequest("LAST", 5, 10);
		return req.fetchData().getValues(0);
	}

    private void createField(String fieldName, String databaseTemplateName)
    {
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(fieldName);
        schema.addAndSaveChildren(field);
        field.setFieldType(RecordSchemaFieldType.DOUBLE);
        assertTrue(field.start());

        RrdDatabaseRecordFieldExtension dbExt = new RrdDatabaseRecordFieldExtension();
        dbExt.setName("rrd");
        field.addAndSaveChildren(dbExt);
        dbExt.setDatabaseTemplateName(databaseTemplateName);
        assertTrue(dbExt.start());
    }

	private void createDatabaseTemplate(String name)
	{
		Node extensions = schema.getRecordExtensionsNode();
		RrdDatabaseRecordExtension dbDef = new RrdDatabaseRecordExtension();
		dbDef.setName(name);
        extensions.addAndSaveChildren(dbDef);
        dbDef.setStep(5l);
        dbDef.setStartTime("epoch");
        assertTrue(dbDef.start());

		RrdArchiveDefNode arcDef = new RrdArchiveDefNode();
		arcDef.setName("archive");
        dbDef.addAndSaveChildren(arcDef);
		arcDef.setRows(100);
		arcDef.setSteps(1);
		arcDef.setConsolidationFunction(ConsolidationFunction.LAST);
		assertTrue(arcDef.start());

		Node dsDef = dbDef.getChildren(RrdStatisticsDatabaseNode.DATASOURCE_NAME);
		assertNotNull(dsDef);
		assertTrue(dsDef instanceof RrdDatasourceDefNode);
		assertEquals(Status.STARTED, dsDef.getStatus());
	}

	private Record createRecord(String key, long time, double s1val, double s2val)
            throws RecordException
	{
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, time*1000l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, key);
        rec.setValue("s1", s1val);
        rec.setValue("s2", s2val);
        
		return rec;
	}
}