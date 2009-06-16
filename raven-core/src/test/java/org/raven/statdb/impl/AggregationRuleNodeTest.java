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

package org.raven.statdb.impl;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.RuleProcessingResult;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.Node.Status;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class AggregationRuleNodeTest extends RavenCoreTestCase
{
	@Test
	public void test() throws Exception
	{
		StatisticsDatabase db = createMock(StatisticsDatabase.class);
		expect(db.getStep()).andReturn(10l).atLeastOnce();
		db.saveStatisticsValue("/1/", "n1", 6., 0l);
		db.saveStatisticsValue("/1/", "n1", 10., 10l);

		StatisticsRecord record = createMock(StatisticsRecord.class);
		expect(record.getTime()).andReturn(5l);
		expect(record.getTime()).andReturn(6l);
		expect(record.getTime()).andReturn(7l);
		expect(record.getTime()).andReturn(11l);
		expect(record.getTime()).andReturn(12l);
		expect(record.getTime()).andReturn(21l);


		RuleProcessingResult result = createMock(RuleProcessingResult.class);
//		result.set

		replay(db, record, result);

		AggregationRuleNode rule = new AggregationRuleNode();
		rule.setName("rule");
        tree.getRootNode().addAndSaveChildren(rule);
		rule.setAggregationFunction(AggregationFunction.MAX);
		rule.start();
		assertEquals(rule.getStatus(), Status.STARTED);

		rule.processRule("/1/", "n1", 5., record, result, db);
		rule.processRule("/1/", "n1", 6., record, result, db);
		rule.processRule("/1/", "n1", 4., record, result, db);
		rule.processRule("/1/", "n1", 10., record, result, db);
		rule.processRule("/1/", "n1", 9., record, result, db);
		rule.processRule("/1/", "n1", 5., record, result, db);

		verify(db, record, result);
	}
}