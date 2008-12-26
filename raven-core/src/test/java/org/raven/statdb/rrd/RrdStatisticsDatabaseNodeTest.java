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

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.objects.PushDataSource;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.impl.StatisticsDefinitionNode;
import org.raven.statdb.impl.StatisticsRecordImpl;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsDatabaseNodeTest extends RavenCoreTestCase
{
	@Test
	public void test() throws Exception
	{
		PushDataSource ds = new PushDataSource();
		ds.setName("ds");
		ds.setParent(tree.getRootNode());
		ds.save();
		tree.getRootNode().addChildren(ds);
		ds.init();
		ds.start();
		assertEquals(Status.STARTED, ds.getStatus());

		RrdStatisticsDatabaseNode db = new RrdStatisticsDatabaseNode();
		db.setName("db");
		db.setParent(tree.getRootNode());
		db.save();
		tree.getRootNode().addChildren(db);
		db.init();
		db.setStep(5);
		db.setDataSource(ds);
		db.setStartTime("0");
		db.start();
		assertEquals(Status.STARTED, db.getStatus());

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