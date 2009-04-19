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
import org.raven.ds.RecordException;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.PushDataSource;
import org.raven.ds.InvalidRecordFieldException;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.statdb.ProcessingInstruction;
import org.raven.statdb.Rule;
import org.raven.statdb.RuleProcessingResult;
import org.raven.statdb.StatisticsRecord;
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
    private RecordSchemaNode schema;

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

        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

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

        RecordSchemaFieldNode s1Field = new RecordSchemaFieldNode();
        s1Field.setName("s1");
        schema.addAndSaveChildren(s1Field);
        s1Field.setFieldType(RecordSchemaFieldType.DOUBLE);
        assertTrue(s1Field.start());

        RecordSchemaFieldNode s2Field = new RecordSchemaFieldNode();
        s2Field.setName("s2");
        schema.addAndSaveChildren(s2Field);
        s2Field.setFieldType(RecordSchemaFieldType.DOUBLE);
        assertTrue(s2Field.start());

		db = new TestStatisticsDatabase();
		db.setName("db");
		db.setParent(tree.getRootNode());
		db.save();
		tree.getRootNode().addChildren(db);
		db.init();
		db.setDataSource(ds);
		db.setStep(5l);
        db.setRecordSchema(schema);
		db.setLogLevel(LogLevel.DEBUG);
		assertTrue(db.start());
	}

	@Test
	public void configNodesCreationTest()
	{
		assertNotNull(db.getChildren(RulesNode.NAME));
	}

	@Test
	public void saveStatisticsValueTest() throws RecordException
	{
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, 0l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, "/1/2/");
        rec.setValue("s1", 1.);
        rec.setValue("s2", 2.);

		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		replay(saveStatisticsValue);
		ds.pushData(rec);
		verify(saveStatisticsValue);
	}

	@Test(expected=InvalidRecordFieldException.class)
	public void undefinedStatisticsTest() throws RecordException
	{
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, 0l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, "/1/2/");
        rec.setValue("s3", 1.);
	}

	@Test
	public void rulesExecutionTest() throws Exception
	{
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, 0l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, "/1/2/");
        rec.setValue("s1", 1.);
        rec.setValue("s2", 2.);

		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), isA(TestStatisticsRecord.class)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule2.processRule(
				eq("/1/2/"), eq("s1"), eq(1.0), isA(TestStatisticsRecord.class)
				, isA(RuleProcessingResultImpl.class), same(db));
		rule2.processRule(
				eq("/1/2/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
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
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, 0l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, "/1/2/");
        rec.setValue("s1", 1.);
        rec.setValue("s2", 2.);

		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createSaveStatisticsValueMock();
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), isA(TestStatisticsRecord.class)
				, changeProcessingInstruction(ProcessingInstruction.STOP_PROCESSING_SUBKEY)
				, same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
                , isA(RuleProcessingResultImpl.class)
				, same(db));
		rule2.processRule(
				eq("/1/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
                , isA(RuleProcessingResultImpl.class)
				, same(db));
		replay(rule1, rule2, saveStatisticsValue);

		createIfNode("if1", "key=='/1/'", rule1, rule2);

		ds.pushData(rec);

		verify(rule1, rule2, saveStatisticsValue);
	}

	@Test
	public void stopProcessingKeyTest() throws Exception
	{
        Record rec = schema.createRecord();
        rec.setValue(StatisticsRecord.TIME_FIELD_NAME, 0l);
        rec.setValue(StatisticsRecord.KEY_FIELD_NAME, "/1/2/");
        rec.setValue("s1", 1.);
        rec.setValue("s2", 2.);

		Rule rule1 = createMock("rule1", Rule.class);
		Rule rule2 = createMock("rule2", Rule.class);
		SaveStatisticsValue saveStatisticsValue = createMock(SaveStatisticsValue.class);
		db.setDatabaseMock(saveStatisticsValue);
		rule1.processRule(
				eq("/1/"), eq("s1"), eq(1.0), isA(TestStatisticsRecord.class)
				, changeProcessingInstruction(ProcessingInstruction.STOP_PROCESSING_KEY)
				, same(db));
		rule1.processRule(
				eq("/1/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
                , isA(RuleProcessingResultImpl.class)
				, same(db));
		rule2.processRule(
				eq("/1/"), eq("s2"), eq(2.0), isA(TestStatisticsRecord.class)
                , isA(RuleProcessingResultImpl.class)
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