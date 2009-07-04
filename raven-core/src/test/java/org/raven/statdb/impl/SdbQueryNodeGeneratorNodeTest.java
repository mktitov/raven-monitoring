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

package org.raven.statdb.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.raven.DummyScheduler;
import org.raven.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.SelectMode;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryNodeGeneratorNodeTest extends RavenCoreTestCase
{
    private SdbQueryNodeGeneratorNode generator;
    private TestStatisticsDatabase database;
    private PushDataSource datasource;
    private DummyScheduler scheduler;

    @Before
    public void prepare()
    {
        datasource = new PushDataSource();
        datasource.setName("datasource");
        tree.getRootNode().addAndSaveChildren(datasource);
        assertTrue(datasource.start());

        RecordSchemaNode schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        database = new TestStatisticsDatabase();
        database.setName("database");
        tree.getRootNode().addAndSaveChildren(database);
        database.setStep(5l);
        database.setDataSource(datasource);
        database.setRecordSchema(schema);
        assertTrue(database.start());

        scheduler = new DummyScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        generator = new SdbQueryNodeGeneratorNode();
        generator.setName("generator");
        tree.getRootNode().addAndSaveChildren(generator);
        generator.setStatisticsDatabase(database);
        generator.setGropNames("group1, group2, group3");
        generator.setScheduler(scheduler);
        generator.setLogLevel(LogLevel.DEBUG);
    }

    @Test
    public void templateGeneration_test()
    {
        TemplateEntry template = generator.getQueryNodeGeneratorTemplate();
        assertNotNull(template);
        assertTrue(template.getId()>0);
    }

    @Test
    public void createFirstGroup_test()
    {
        assertTrue(generator.start());
        Node node = generator.getChildren("group1");
        assertNotNull(node);
        assertTrue(node instanceof SdbQueryNodeGeneratorGroupNode);
        assertEquals(Node.Status.STARTED, node.getStatus());
        SdbQueryNodeGeneratorGroupNode group = (SdbQueryNodeGeneratorGroupNode) node;
        assertEquals("/@r .*", group.getChildsKeyExpression());
    }

    @Test
    public void generating_test()
    {
        initTemplate();
        QueryResult queryResult = createQueryResult("/key1/", "/key2/");

        ExecuteDatabaseQuery queryExecuter = createMock(ExecuteDatabaseQuery.class);
        expect(queryExecuter.executeQuery(checkQuery("/@r .*", database)))
                .andReturn(createQueryResult("/key1/", "/key2/"));

        replay(queryExecuter);
        database.setQueryMock(queryExecuter);

        assertTrue(generator.start());
        
        Node group = generator.getChildren("group1");
        Collection<Node> childs = group.getChildrens();
        SdbQueryNodeGeneratorGroupNode group2_1 = checkNode(group, "group2", "/key1/", "key1", 1);
        SdbQueryNodeGeneratorGroupNode group2_2 = checkNode(group, "group2", "/key2/", "key2", 1);

        verify(queryExecuter);

        reset(queryExecuter);
        expect(queryExecuter.executeQuery(checkQuery("/key1/@r .*", database)))
                .andReturn(createQueryResult("/key1/subkey1/"));
        replay(queryExecuter);
        checkNode(group2_1, "group3", "/key1/subkey1/", "subkey1", 2);
        
        verify(queryExecuter);
        
        reset(queryExecuter);
        expect(queryExecuter.executeQuery(checkQuery("/key2/@r .*", database)))
                .andReturn(createQueryResult("/key2/subkey1/"));
        replay(queryExecuter);
        checkNode(group2_2, "group3", "/key2/subkey1/", "subkey1", 2);

        verify(queryExecuter);
    }

    @Test
    public void cleanup_test() throws InvalidPathException
    {
        assertTrue(generator.start());
        
        initTemplate();
        QueryResult queryResult = createQueryResult("/key1/", "/key2/");

        ExecuteDatabaseQuery queryExecuter = createMock(ExecuteDatabaseQuery.class);
        expect(queryExecuter.executeQuery(checkQuery("/@r .*", database)))
                .andReturn(createQueryResult("/key1/", "/key2/")).times(2);
        
        replay(queryExecuter);

        database.setQueryMock(queryExecuter);

        Node node1 = tree.getNode("/generator/group1/key1/group2");
        assertNotNull(node1);

        generator.executeScheduledJob();

        Node node2 = tree.getNode("/generator/group1/key1/group2");
        assertNotNull(node2);
        assertNotSame(node1, node2);

        verify(queryExecuter);
    }

    private SdbQueryNodeGeneratorGroupNode checkNode(
            Node parentNode, String groupName, String key, String lastKeyElement, int level)
    {
        Node node = parentNode.getChildren(lastKeyElement);
        assertNotNull(node);
        assertStarted(node);

        node = node.getChildren(groupName+":"+level);
        assertNotNull(node);
        assertStarted(node);
        node = node.getParent().getChildren(groupName);
        assertNotNull(node);
        assertTrue(node instanceof SdbQueryNodeGeneratorGroupNode);
        SdbQueryNodeGeneratorGroupNode group = (SdbQueryNodeGeneratorGroupNode) node;
        assertStarted(node);
        assertSame(generator, group.getNodeGenerator());
        assertEquals(key+"@r .*", group.getChildsKeyExpression());
        
        return group;
    }

    private QueryResult createQueryResult(String... keys)
    {
        List<KeyValues> keyValuesList = new ArrayList<KeyValues>();
        for (String key: keys)
            keyValuesList.add(new KeyValuesImpl(key));
        QueryResultImpl result = new QueryResultImpl(keyValuesList);

        return result;
    }

    private void initTemplate()
    {
        Node template = generator.getQueryNodeGeneratorTemplate();
        ContainerNode tRoot = new ContainerNode("^t lastKeyElement");
        template.addAndSaveChildren(tRoot);
        ContainerNode node = new ContainerNode("^t groupName+':'+groupLevel");
        tRoot.addAndSaveChildren(node);
        SdbQueryNodeGeneratorGroupNode group = new SdbQueryNodeGeneratorGroupNode();
        group.setName("group");
        tRoot.addAndSaveChildren(group);
    }

    public static Query checkQuery(final String keyExpression, final StatisticsDatabase database)
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(Object argument)
            {
                assertNotNull(argument);
                Query query = (Query) argument;
                assertEquals(keyExpression, query.getFromClause().getKeyExpression());
                assertEquals(SelectMode.SELECT_KEYS, query.getSelectMode());

                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });

        return null;
    }
}