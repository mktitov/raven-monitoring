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

package org.raven.rrd;

import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class RRDatabaseManagerTest extends RavenCoreTestCase
{
    RRDatabaseManager databaseManager;
    DataPipeImpl source1;
    DataPipeImpl source2;
    
    @Before
    public void prepareTest()
    {
        store.removeNodes();
        tree.reloadTree();
        
        databaseManager = new RRDatabaseManager();
        databaseManager.setName("db manager");
        tree.getRootNode().addChildren(databaseManager);
        store.saveNode(databaseManager);
        databaseManager.init();
        assertEquals(Status.INITIALIZED, databaseManager.getStatus());
        
    }
    
    @Test
    public void initNodeTest()
    {
        store.removeNodes();
        tree.reloadTree();
        
        RRDatabaseManagerTemplate templatesNode = 
                (RRDatabaseManagerTemplate) 
                databaseManager.getChildren(RRDatabaseManagerTemplate.NAME);
        assertNotNull(templatesNode);
        
        tree.reloadTree();
        
        RRDatabaseManagerTemplate reloadedTemplatesNode = 
                (RRDatabaseManagerTemplate) 
                databaseManager.getChildren(RRDatabaseManagerTemplate.NAME);
        assertNotNull(reloadedTemplatesNode);
        assertEquals(templatesNode, reloadedTemplatesNode);
    }
    
    @Test
    public void setStartingPointTest() 
    {
        initTemplate();
        initDatasources();
    }
    
    private void initDatasources()
    {
        Node sourcesRoot = new BaseNode("sources");
        tree.getRootNode().addChildren(sourcesRoot);
        store.saveNode(sourcesRoot);
        sourcesRoot.init();
        
        source1 = createSource(sourcesRoot, "source1");
        
        Node groupNode = new BaseNode("other sources");
        sourcesRoot.addChildren(groupNode);
        
        
    }
    
    private DataPipeImpl createSource(Node parent, String sourceName)
    {
        DataPipeImpl source = new DataPipeImpl();
        source.setName(sourceName);
        parent.addChildren(source);
        store.saveNode(source);
        
        return source;
    }

    private void initTemplate()
    {
        RRDatabaseManagerTemplate templatesNode = 
                (RRDatabaseManagerTemplate) 
                databaseManager.getChildren(RRDatabaseManagerTemplate.NAME);
        addDatabase(templatesNode, RRDatabaseManager.DEFAULT_DATABASE_TEMPLATE);
        addDatabase(templatesNode, "interface");
    }
    
    private void addDatabase(RRDatabaseManagerTemplate templatesNode, String databaseName)
    {
        RRDNode db = new RRDNode();
        db.setName(RRDatabaseManager.DEFAULT_DATABASE_TEMPLATE);
        templatesNode.addChildren(db);
        store.saveNode(db);
        db.init();
        
        RRArchive archive = new RRArchive();
        archive.setName("archive");
        db.addChildren(archive);
        store.saveNode(archive);
        archive.init();
        archive.setRows(100);
        
        RRDataSource datasource = new RRDataSource();
        datasource.setName("datasource");
        db.addChildren(datasource);
        store.saveNode(datasource);
        datasource.init();
    }
}
