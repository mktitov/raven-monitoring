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
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class RrdUpdateQueueNodeTest extends RavenCoreTestCase
{
	@Before
	public void before()
	{
		try{
			FileUtils.forceDelete(new File("target/u_rrd"));
		}catch(Exception e){}
		new File("target/u_rrd").mkdirs();
	}

	@Test
	public void test() throws Exception
	{
		RrdUpdateQueueNode queue = new RrdUpdateQueueNode();
		queue.setName("queue");
		queue.setParent(tree.getRootNode());
		queue.save();
		tree.getRootNode().addChildren(queue);
		queue.init();
		queue.setCorePoolSize(1);
		queue.start();
		assertEquals(Status.STARTED, queue.getStatus());

		String path1 = "target/u_rrd/rrd1.jrrd";
		String path2 = "target/u_rrd/rrd2.jrrd";

		createDb(path1);
		createDb(path2);

		queue.pushUpdateRequest(new RrdUpdateRequest(path1, 5, 1.));
		queue.pushUpdateRequest(new RrdUpdateRequest(path2, 5, 10.));
		TimeUnit.MILLISECONDS.sleep(500);
		queue.pushUpdateRequest(new RrdUpdateRequest(path1, 10, 2.));
		queue.pushUpdateRequest(new RrdUpdateRequest(path2, 10, 11.));

		TimeUnit.SECONDS.sleep(1);

		double[] values = fetchData(path1);
		assertNotNull(values);
		assertTrue(Arrays.equals(new double[]{1., 2.}, values));
		
		values = fetchData(path2);
		assertNotNull(values);
		assertTrue(Arrays.equals(new double[]{10., 11.}, values));
	}

	private double[] fetchData(String path) throws IOException, RrdException
	{
		RrdDb db = RrdDbPool.getInstance().requestRrdDb(path);
		FetchRequest req = db.createFetchRequest("MAX", 5, 10);
		return req.fetchData().getValues(0);
	}

	private void createDb(String path) throws Exception
	{
		RrdDef dbDef = new RrdDef(path, 0, 5);
		DsDef dsDef = new DsDef("ds", "GAUGE", 5, 0, 100);
		dbDef.addDatasource(dsDef);
		ArcDef arcDef = new ArcDef("MAX", .5, 1, 100);
		dbDef.addArchive(arcDef);

		RrdDb db = new RrdDb(dbDef);
		db.close();
	}
}