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
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.objects.TestDataSource2;
import org.raven.rrd.objects.TestDataSource3;
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
    DataSource source_d1 = null;
    DataSource source_i1 = null;
    DataSource source_i2 = null;
    DataSource source_i3 = null;
	RRIoQueueNode queueNode;
    
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
        
   		queueNode = new RRIoQueueNode();
		queueNode.setName("ioqueue");
		tree.getRootNode().addChildren(queueNode);
		queueNode.save();
		queueNode.init();
		queueNode.setCorePoolSize(1);
		queueNode.setMaximumPoolSize(1);
		queueNode.start();
		assertEquals(Status.STARTED, queueNode.getStatus());
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
        assertEquals(Status.STARTED, databaseManager.getStatus());
        checkDefaultEntry();
        checkInterfaceEntry();
        
        tree.reloadTree();
        
        checkDefaultEntry();
        checkInterfaceEntry();
    }
    
    @Test
    public void addDataToStartingPoint() throws Exception
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("2");
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").save();
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.getNodeAttribute("startingPoint").save();
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE)
                .setValue(RRDatabaseManager.RemovePolicy.STOP_DATABASES.toString());
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE).save();

        databaseManager.start();
        
        DataSource newDs = createSource2(sourcesRoot, "newDataPipe", "interface");
        
        Set<Node> dependentNodes = newDs.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        
        tree.reloadTree();
        
        databaseManager = (RRDatabaseManager) tree.getNode(databaseManager.getPath());
        assertEquals(Status.STARTED, databaseManager.getStatus());
        
        newDs = (DataSource) tree.getNode(newDs.getPath());
        assertEquals(newDs.getStatus(), Status.INITIALIZED);
        assertNotNull(newDs);
        newDs.start();
        dependentNodes = newDs.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
    }
    
    @Test
    public void stopDatabaseRemovePolicyTest() throws Exception
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("2");
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE)
                .setValue(RRDatabaseManager.RemovePolicy.STOP_DATABASES.toString());
        databaseManager.start();
        
        assertNotNull(source_d1);
        Set<Node> dependentNodes = source_d1.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        RRDataSource rrds = (RRDataSource) dependentNodes.iterator().next();
        assertNotNull(rrds);
        assertEquals(Status.STARTED, rrds.getStatus());
        
        tree.remove(source_d1);
        
        Node parent = tree.getNode(rrds.getParent().getPath());
        assertNotNull(parent);
        assertSame(rrds, parent.getChildren(rrds.getName()));
        assertEquals(Status.INITIALIZED, rrds.getStatus());
    }
    
    @Test
    public void removeDatabaseRemovePolicyTest() throws Exception
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("2");
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE)
                .setValue(RRDatabaseManager.RemovePolicy.REMOVE_DATABASES.toString());
        databaseManager.start();
        
        assertNotNull(source_d1);
        Set<Node> dependentNodes = source_d1.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        RRDataSource rrds = (RRDataSource) dependentNodes.iterator().next();
        assertNotNull(rrds);
        assertEquals(Status.STARTED, rrds.getStatus());
        
        tree.remove(source_d1);
        
        assertEquals(Status.REMOVED, rrds.getStatus());
        assertEquals(Status.REMOVED, rrds.getParent().getStatus());
        try{
            tree.getNode(rrds.getPath());
            fail();
        }catch(InvalidPathException e){}
        try{
            tree.getNode(rrds.getParent().getPath());
            fail();
        }catch(InvalidPathException e){}
        
        tree.reloadTree();
        
        try{
            tree.getNode(rrds.getPath());
            fail();
        }catch(InvalidPathException e){}
        try{
            tree.getNode(rrds.getParent().getPath());
            fail();
        }catch(InvalidPathException e){}
    }

    @Test
    public void removeDatabaseRemovePolicyTest2() throws Exception
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("3");
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE)
                .setValue(RRDatabaseManager.RemovePolicy.REMOVE_DATABASES.toString());
        databaseManager.start();

//        RRDataSource rrds = null;
        Node dataSource = source_i1;
        
        assertNotNull(dataSource);
        Set<Node> dependentNodes = dataSource.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        RRDataSource rrds = (RRDataSource) dependentNodes.iterator().next();
        assertNotNull(rrds);
        assertEquals(Status.STARTED, rrds.getStatus());
        
        assertNotNull(rrds);
        
        assertEquals(Status.STARTED, rrds.getStatus());
        
        tree.remove(dataSource);
        
        assertEquals(Status.REMOVED, rrds.getStatus());
        assertEquals(Status.STARTED, rrds.getParent().getStatus());
        Node rrd = tree.getNode(rrds.getParent().getPath());
        assertNotNull(rrd);
        assertEquals(Status.STARTED, rrd.getStatus());
        try{
            tree.getNode(rrds.getPath());
            fail();
        }catch(InvalidPathException e){}
        
        tree.reloadTree();
        
        rrd = tree.getNode(rrds.getParent().getPath());
        assertNotNull(rrd);
        assertEquals(Status.STARTED, rrd.getStatus());
        try{
            tree.getNode(rrds.getPath());
            fail();
        }catch(InvalidPathException e){}
    }
    
    @Test
    public void manualRemoveRRDataSource() throws Exception
    {
        initTemplate();
        Node sourcesRoot = initDatasources();
        databaseManager.getNodeAttribute("dataSourcesPerDatabase").setValue("3");
        databaseManager.getNodeAttribute("startingPoint").setValue(sourcesRoot.getPath());
        databaseManager.getNodeAttribute(RRDatabaseManager.REMOVEPOLICY_ATTRIBUTE)
                .setValue(RRDatabaseManager.RemovePolicy.REMOVE_DATABASES.toString());
        databaseManager.start();

//        RRDataSource rrds = null;
        Node dataSource = source_i1;
        
        assertNotNull(dataSource);
        Set<Node> dependentNodes = dataSource.getDependentNodes();
        assertNotNull(dependentNodes);
        assertEquals(1, dependentNodes.size());
        RRDataSource rrds = (RRDataSource) dependentNodes.iterator().next();
        assertNotNull(rrds);
        assertEquals(Status.STARTED, rrds.getStatus());
        
        assertNotNull(rrds);
        
        assertEquals(Status.STARTED, rrds.getStatus());
        
        tree.remove(rrds);
        
        assertEquals(Status.REMOVED, rrds.getStatus());
        dependentNodes = dataSource.getDependentNodes();
        assertEquals(0, dependentNodes.size());
        
        try{
            tree.getNode(rrds.getPath());
            fail();
        }catch(InvalidPathException e){}
        
        databaseManager.stop();
        databaseManager.start();
        
        dependentNodes = dataSource.getDependentNodes();
        assertEquals(1, dependentNodes.size());
        rrds = (RRDataSource) dependentNodes.iterator().next();
        assertNotNull(rrds);
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
    
    private DataSource createSource(Node parent, String sourceName, String datatype) 
            throws Exception
    {
        DataSource source = new TestDataSource2();
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
        
        source.start();
        
        assertEquals(Status.STARTED, source.getStatus());
        
        return source;
    }

    private DataSource createSource2(Node parent, String sourceName, String datatype) 
            throws Exception
    {
        DataSource source = new TestDataSource3();
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
        
        source.start();
        
        assertEquals(Status.STARTED, source.getStatus());
        
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
		db.setIoQueue(queueNode);
        
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
