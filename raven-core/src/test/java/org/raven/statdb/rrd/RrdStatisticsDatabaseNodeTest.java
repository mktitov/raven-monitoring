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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdException;
import org.jrobin.core.Util;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.objects.PushDataSource;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.impl.StatisticsDefinitionNode;
import org.raven.statdb.impl.StatisticsRecordImpl;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryResult;
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

	@Before
	public void prepare()
	{
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
		db.setStep(5);
		db.setDataSource(ds);
		db.setStartTime("epoch");
		db.setUpdateQueue(queue);
		db.start();
		assertEquals(Status.STARTED, db.getStatus());
	}

	@Test
	public void updateTest() throws Exception
	{

		createStatisticsDef(db, "s1", "t1");
		createStatisticsDef(db, "s2", "t2");

		createDatabaseTemplate(db, "t1");
		createDatabaseTemplate(db, "t2");

		StatisticsRecord record = createRecord("/1/1/", 5, 1., 10.);
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

	@Test
	public void query_selectKeys_emptyDb_test() throws Exception
	{
		Query query = createMock(Query.class);
		FromClause from = createMock(FromClause.class);
		expect(query.getFromClause()).andReturn(from);
		expect(from.getKeyExpression()).andReturn("/@r .*/").atLeastOnce();

		replay(query, from);

		QueryResult result = db.executeQuery(query);
		assertNotNull(result);
		assertNotNull(result.getKeyValues());
		assertEquals(0, result.getKeyValues().size());

		verify(query, from);
	}

	@Test
	public void query_selectKeys_exactKeyMatch_test() throws Exception
	{
		String dbFilenamePrefix = "target/stat_db/"+db.getId()+"/";

		new File(dbFilenamePrefix+"/1").mkdir();
		new File(dbFilenamePrefix+"/2/1").mkdirs();

		FromClause from = createMock(FromClause.class);
		Query query = createMock(Query.class);
		expect(query.getFromClause()).andReturn(from).times(2);
		expect(from.getKeyExpression()).andReturn("/1/");
		expect(from.getKeyExpression()).andReturn("/2/1");

		replay(query, from);

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

		verify(query, from);
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
		expect(query.getFromClause()).andReturn(from).times(2);
		expect(from.getKeyExpression()).andReturn("/@r [23]/1/");
		expect(from.getKeyExpression()).andReturn("/@r [23]/@r ^2/");

		replay(query, from);

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

		verify(query, from);
	}

	private double[] fetchData(String path) throws IOException, RrdException
	{
		RrdDb db = RrdDbPool.getInstance().requestRrdDb(path);
		FetchRequest req = db.createFetchRequest("LAST", 5, 10);
		return req.fetchData().getValues(0);
	}

	private void createDatabaseTemplate(RrdStatisticsDatabaseNode db, String name)
	{
		Node templates = db.getDatabaseTemplatesNode();
		RrdDatabaseDefNode dbDef = new RrdDatabaseDefNode();
		dbDef.setName(name);
		dbDef.setParent(templates);
		dbDef.save();
		templates.addChildren(dbDef);
		dbDef.init();
		dbDef.start();
		assertEquals(Status.STARTED, dbDef.getStatus());

		RrdArchiveDefNode arcDef = new RrdArchiveDefNode();
		arcDef.setName("archive");
		arcDef.setParent(dbDef);
		arcDef.save();
		dbDef.addChildren(arcDef);
		arcDef.init();
		arcDef.setRows(100);
		arcDef.setSteps(1);
		arcDef.setConsolidationFunction(ConsolidationFunction.LAST);
		arcDef.start();
		assertEquals(Status.STARTED, arcDef.getStatus());

		Node dsDef = dbDef.getChildren(RrdStatisticsDatabaseNode.DATASOURCE_NAME);
		assertNotNull(dsDef);
		assertTrue(dsDef instanceof RrdDatasourceDefNode);
		assertEquals(Status.STARTED, dsDef.getStatus());
		
	}

	private StatisticsRecord createRecord(String key, long time, double s1val, double s2val)
	{
		StatisticsRecordImpl rec = new StatisticsRecordImpl(key, time);
		rec.put("s1", s1val);
		rec.put("s2", s2val);

		return rec;
	}

	private void createStatisticsDef(RrdStatisticsDatabaseNode db, String name, String type)
	{
		StatisticsDefinitionNode sDef =  new StatisticsDefinitionNode();
		sDef.setName(name);
		sDef.setParent(db.getStatisticsDefinitionsNode());
		sDef.save();
		db.getStatisticsDefinitionsNode().addChildren(sDef);
		sDef.init();
		sDef.setType(type);
		sDef.start();
		assertEquals(Status.STARTED, sDef.getStatus());
	}
}