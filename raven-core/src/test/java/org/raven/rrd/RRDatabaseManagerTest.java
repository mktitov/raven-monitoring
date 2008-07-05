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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class RRDatabaseManagerTest extends RavenCoreTestCase
{
    RRDatabaseManager databaseManager;
    DataPipeImpl source_d1;
    DataPipeImpl source_i1;
    DataPipeImpl source_i2;
    DataPipeImpl source_i3;
    
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
    public void setStartingPointTest() throws InvalidPathException, Exception 
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("2");
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.start();
        databaseManager.stop();
        databaseManager.start();
        assertEquals(Status.STARTED, databaseManager.getStatus());
        checkDefaultEntry();
        checkInterfaceEntry();
        
        tree.reloadTree();
        
        checkDefaultEntry();
        checkInterfaceEntry();
    }

    private void checkDefaultEntry() throws InvalidPathException
    {
        RRDatabaseManager databaseManager = 
                (RRDatabaseManager) tree.getNode(this.databaseManager.getPath());
        DatabasesEntry defaultEntry = (DatabasesEntry) databaseManager.getChildren(
                RRDatabaseManager.DEFAULT_DATABASE_TEMPLATE);
        assertNotNull(defaultEntry);
        assertEquals(1, defaultEntry.getChildrenCount());
        RRDNode rrd = (RRDNode) defaultEntry.getChildren("1");
        assertNotNull(rrd);
        assertEquals(Status.STARTED, rrd.getStatus());
        assertEquals(2, rrd.getChildrenCount());

        RRArchive archive = (RRArchive) rrd.getChildren("archive");
        assertNotNull(archive);
        assertEquals(Status.STARTED, archive.getStatus());

        List<RRDataSource> sources = getRRDataSources(rrd);
        assertEquals(1, sources.size());
        assertEquals(Status.STARTED, sources.get(0).getStatus());
        assertEquals(source_d1, sources.get(0).getDataSource());
    }
    
    private List<RRDataSource> getRRDataSources(RRDNode rrd)
    {
        List<RRDataSource> result = new ArrayList<RRDataSource>();
        for (Node child: rrd.getChildrens())
            if (child instanceof RRDataSource)
                result.add((RRDataSource) child);
        return result;
    }

    private void checkInterfaceEntry() throws InvalidPathException
    {
        RRDatabaseManager databaseManager = 
                (RRDatabaseManager) tree.getNode(this.databaseManager.getPath());
        DatabasesEntry interfaceEntry = (DatabasesEntry) databaseManager.getChildren("interface");
        assertNotNull(interfaceEntry);
        assertEquals(2, interfaceEntry.getChildrenCount());
        
        Map<DataSource, Boolean> flags = new HashMap<DataSource, Boolean>();
        for (DataSource dataSource: new DataSource[]{source_i1, source_i2, source_i3})
            flags.put(dataSource, false);
        
        checkInterfaceDatabase(interfaceEntry, "1", 2, flags);
        checkInterfaceDatabase(interfaceEntry, "2", 1, flags);
        
        for (Map.Entry<DataSource, Boolean> entry: flags.entrySet())
            assertTrue(entry.getValue());
    }
    
    private void checkInterfaceDatabase(
            DatabasesEntry databaseEntry, String databaseName, int dsCount
            , Map<DataSource, Boolean> flags)
    {
        RRDNode rrd = (RRDNode) databaseEntry.getChildren(databaseName);
        assertNotNull(rrd);
        assertEquals(Status.STARTED, rrd.getStatus());
        assertEquals(dsCount+1, rrd.getChildrenCount());

        RRArchive archive = (RRArchive) rrd.getChildren("archive");
        assertNotNull(archive);
        assertEquals(Status.STARTED, archive.getStatus());

        for (RRDataSource datasource: getRRDataSources(rrd))
        {
            assertNotNull(datasource);
            assertEquals(Status.STARTED, datasource.getStatus());
            assertNotNull(datasource.getDataSource());
            assertTrue(flags.containsKey(datasource.getDataSource()));
            flags.put(datasource.getDataSource(), true);
        }
    }
    
    private Node initDatasources() throws Exception
    {
        Node sourcesRoot = new BaseNode("sources");
        tree.getRootNode().addChildren(sourcesRoot);
        store.saveNode(sourcesRoot);
        sourcesRoot.init();
        
        source_d1 = createSource(sourcesRoot, "source_d1", "default");
        source_i1 = createSource(sourcesRoot, "source_i1", "interface");
        
        Node groupNode = new BaseNode("other sources");
        sourcesRoot.addChildren(groupNode);
        store.saveNode(groupNode);
        groupNode.init();
        
        source_i2 = createSource(groupNode, "source_i2", "interface");
        source_i3 = createSource(groupNode, "source_i3", "interface");
        
        return sourcesRoot;
    }
    
    private DataPipeImpl createSource(Node parent, String sourceName, String datatype) 
            throws Exception
    {
        DataPipeImpl source = new DataPipeImpl();
        source.setName(sourceName);
        parent.addChildren(source);
        store.saveNode(source);
        source.init();
        
        NodeAttribute dataTypeAttr = new NodeAttributeImpl(
                databaseManager.getDataTypeAttributeName(), String.class, datatype, null);
        dataTypeAttr.setOwner(source);
        source.addNodeAttribute(dataTypeAttr);
        dataTypeAttr.init();
        dataTypeAttr.save();
        
        return source;
    }

    private void initTemplate() throws Exception
    {
        RRDatabaseManagerTemplate templatesNode = 
                (RRDatabaseManagerTemplate) 
                databaseManager.getChildren(RRDatabaseManagerTemplate.NAME);
        addDatabase(templatesNode, RRDatabaseManager.DEFAULT_DATABASE_TEMPLATE);
        addDatabase(templatesNode, "interface");
    }
    
    private void addDatabase(RRDatabaseManagerTemplate templatesNode, String databaseName) 
            throws Exception
    {
        RRDNode db = new RRDNode();
        db.setName(databaseName);
        templatesNode.addChildren(db);
        store.saveNode(db);
        db.init();
        
        RRArchive archive = new RRArchive();
        archive.setName("archive");
        db.addChildren(archive);
        store.saveNode(archive);
        archive.init();
        archive.getNodeAttribute(RRArchive.ROWS_ATTRIBUTE).setValue("100");
        archive.getNodeAttribute(RRArchive.ROWS_ATTRIBUTE).save();
        
        RRDataSource datasource = new RRDataSource();
        datasource.setName("datasource");
        db.addChildren(datasource);
        store.saveNode(datasource);
        datasource.init();
    }
}
