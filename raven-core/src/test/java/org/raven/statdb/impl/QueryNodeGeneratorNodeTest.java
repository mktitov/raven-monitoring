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

import org.junit.Before;
import org.junit.Test;
import org.raven.PushDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class QueryNodeGeneratorNodeTest extends RavenCoreTestCase
{
    private QueryNodeGeneratorNode generator;
    private TestStatisticsDatabase database;
    private PushDataSource datasource;

    @Before
    public void prepare()
    {
        datasource = new PushDataSource();
        datasource.setName("datasource");
        tree.getRootNode().addAndSaveChildren(datasource);
        assertTrue(datasource.start());

        database = new TestStatisticsDatabase();
        database.setName("database");
        tree.getRootNode().addAndSaveChildren(database);
        database.setStep(5);
        database.setDataSource(datasource);
        assertTrue(database.start());

        generator = new QueryNodeGeneratorNode();
        generator.setName("generator");
        tree.getRootNode().addAndSaveChildren(generator);
        generator.setStatisticsDatabase(database);
        generator.setGropNames("group1");
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
        assertTrue(node instanceof QueryNodeGeneratorGroupNode);
        QueryNodeGeneratorGroupNode group = (QueryNodeGeneratorGroupNode) node;
        assertEquals("/@r .*", group.getChildsKeyExpression());
    }
}