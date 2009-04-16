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

import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.PushDataSource;
import org.raven.statdb.ProcessingInstruction;
import org.raven.statdb.Rule;
import org.raven.statdb.RuleProcessingResult;
import org.raven.tree.Node.Status;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class AbstractStatisticsDatabaseTest extends RavenCoreTestCase
{
	private TestStatisticsDatabase db;
	private PushDataSource ds;

	@Before
	public void before()
	{
		ds = new PushDataSource();
		ds.setName("ds");
		ds.setParent(tree.getRootNode());
		ds.save();
		tree.getRootNode().addChildren(ds);
		ds.init();
		ds.start();
		assertEquals(Status.STARTED, ds.getStatus());

		db = new TestStatisticsDatabase();
		db.setName("db");
		db.setParent(tree.getRootNode());
		db.save();
		tree.getRootNode().addChildren(db);
		db.init();
		db.setDataSource(ds);
		db.setStep(5l);
		db.setLogLevel(LogLevel.DEBUG);
		db.start();
		assertEquals(Status.STARTED, db.getStatus());

		StatisticsDefinitionNode s1 = new StatisticsDefinitionNode();
		s1.setName("s1");
		s1.setParent(db.getStatisticsDefinitionsNode());
		s1.save();
		db.getStatisticsDefinitionsNode().addChildren(s1);
		s1.init();
		s1.setType("s");
		s1.start();
		assertEquals(Status.STARTED, s1.getStatus());

		StatisticsDefinitionNode s2 = new StatisticsDefinitionNode();
		s2.setName("s2");
		s2.setParent(db.getStatisticsDefinitionsNode());
		s2.save();
		db.getStatisticsDefinitionsNode().addChildren(s2);
		s2.init();
		s2.setType("s");
		s2.start();
		assertEquals(Status.STARTED, s2.getStatus());
	}

	@Test
	public void configNodesCreationTest()
	{
		assertNotNull(db.getChildren(StatisticsDefinitionsNode.NAME));
		assertNotNull(db.getChildren(RulesNode.NAME));
	}

	@Test
	public void saveStatisticsValueTest()
	{
		AbstractStatisticsRecord rec = new AbstractStatisticsRecord("/1/2/", 0);
		rec.put("s1", 1.0);
		rec.put("s2", 2.0);

		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		replay(saveStatisticsValue);
		ds.pushData(rec);
		verify(saveStatisticsValue);
	}

	@Test
	public void undefinedStatisticsTest()
	{
		AbstractStatisticsRecord rec = new AbstractStatisticsRecord("/1/2/", 0);
		rec.put("s3", 1.0);

		SaveStatisticsValue saveStatisticsValue = createMock(SaveStatisticsValue.class);
		replay(saveStatisticsValue);
		ds.pushData(rec);
		verify(saveStatisticsValue);
	}

	@Test
	public void rulesExecutionTest() throws Exception
	{
		AbstractStatisticsRecord rec = new AbstractStatisticsRecord("/1/2/", 0);
		rec.put("s1", 1.0);
		rec.put("s2", 2.0);
		
		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), same(rec)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), same(rec)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule2.processRule(
				eq("/1/2/"), eq("s1"), eq(1.0), same(rec)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule2.processRule(
				eq("/1/2/"), eq("s2"), eq(2.0), same(rec)
				, isA(RuleProcessingResultImpl.class), same(db));
		replay(rule1, rule2, saveStatisticsValue);

		createIfNode("if1", "key=='/1/'", rule1);
		createIfNode("if2", "key=='/1/2/'", rule2);

		ds.pushData(rec);
		
		verify(rule1, rule2, saveStatisticsValue);
	}

	@Test
	public void stopProcessingSubkeyTest() throws Exception
	{
		AbstractStatisticsRecord rec = new AbstractStatisticsRecord("/1/2/", 0);
		rec.put("s1", 1.0);
		rec.put("s2", 2.0);

		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), same(rec)
				, changeProcessingInstruction(ProcessingInstruction.STOP_PROCESSING_SUBKEY)
				, same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), same(rec), isA(RuleProcessingResultImpl.class)
				, same(db));
		rule2.processRule(
				eq("/1/"), eq("s2"), eq(2.0), same(rec), isA(RuleProcessingResultImpl.class)
				, same(db));
		replay(rule1, rule2, saveStatisticsValue);

		createIfNode("if1", "key=='/1/'", rule1, rule2);

		ds.pushData(rec);

		verify(rule1, rule2, saveStatisticsValue);
	}

	@Test
	public void stopProcessingKeyTest() throws Exception
	{
		AbstractStatisticsRecord rec = new AbstractStatisticsRecord("/1/2/", 0);
		rec.put("s1", 1.0);
		rec.put("s2", 2.0);

		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createMock(SaveStatisticsValue.class);
		db.setDatabaseMock(saveStatisticsValue);
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), same(rec)
				, changeProcessingInstruction(ProcessingInstruction.STOP_PROCESSING_KEY)
				, same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), same(rec), isA(RuleProcessingResultImpl.class)
				, same(db));
		rule2.processRule(
				eq("/1/"), eq("s2"), eq(2.0), same(rec), isA(RuleProcessingResultImpl.class)
				, same(db));
		saveStatisticsValue.saveStatisticsValue("/1/2/", "s2", 2., 0);
		replay(rule1, rule2, saveStatisticsValue);

		createIfNode("if1", "key=='/1/'", rule1, rule2);

		ds.pushData(rec);

		verify(rule1, rule2, saveStatisticsValue);
	}

	private RuleProcessingResult changeProcessingInstruction(
			final ProcessingInstruction instruction)
	{
		reportMatcher(new IArgumentMatcher() {

			public boolean matches(Object result)
			{
				((RuleProcessingResult)result).setInstruction(instruction);
				return true;
			}

			public void appendTo(StringBuffer buffer)
			{
//				throw new UnsupportedOperationException("Not supported yet.");
			}
		});

		return null;
	}

	private SaveStatisticsValue createSaveStatisticsValueMock()
	{
		SaveStatisticsValue mock = createMock(SaveStatisticsValue.class);
		db.setDatabaseMock(mock);
		mock.saveStatisticsValue("/1/2/", "s1", 1.0, 0);
		mock.saveStatisticsValue("/1/2/", "s2", 2.0, 0);
		return mock;
	}

	private void createIfNode(String nodeName, String expression, Rule... rules) throws Exception
	{
		IfNode ifNode = new IfNode();
		ifNode.setName(nodeName);
		ifNode.setParent(db.getRulesNode());
		ifNode.save();
		db.getRulesNode().addChildren(ifNode);
		ifNode.init();
		ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue(expression);
		assertFalse(ifNode.getUsedInTemplate());
		ifNode.start();
		assertEquals(Status.STARTED, ifNode.getStatus());

		for (int i=0; i<rules.length; ++i)
		{
			Rule rule = rules[i];
			TestRule ruleNode = new TestRule();
			ruleNode.setName("rule-"+i);
			ruleNode.setParent(ifNode);
			ruleNode.save();
			ifNode.addChildren(ruleNode);
			ruleNode.init();
			ruleNode.setRuleMock(rule);
			ruleNode.start();
			assertEquals(Status.STARTED, ruleNode.getStatus());
		}
	}
}