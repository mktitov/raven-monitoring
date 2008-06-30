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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
@Description("Automaticly stores data from data sources to round robin databases")
public class RRDatabaseManager extends BaseNode
{
    public static final String DEFAULT_DATABASE_TEMPLATE = "DEFAULT";
    public final static String DEFAULT_DATA_TYPE_ATTRIBUTE_NAME = "dataType";

    public enum RemovePolicy {STOP_DATABASES, REMOVE_DATABASES}
    
    @Parameter(defaultValue=DEFAULT_DATA_TYPE_ATTRIBUTE_NAME)
    @NotNull
    @Description("The attribute name by which value data pipes will in one database")
    private String dataTypeAttributeName;
    
    @Parameter @NotNull
    @Description("The count of data sources per database")
    private Integer dataSourcesPerDatabase;
    
    private final static String STARING_POINT_ATTR_NAME = "startingPoint";
    @Parameter @NotNull
    @Description(
        "The node from which database manager take a control on data pipe nodes with seted " +
        "dataType attribute value")
    private Node startingPoint;
    
    @Parameter (defaultValue="STOP_DATABASES")
    @NotNull
    @Description("Defines the remove policy")
    private RemovePolicy removePolicy;
    
    private Lock lock = new ReentrantLock();
    private RRDatabaseManagerTemplate template;
    private Set<Integer> newDataSources = new HashSet<Integer>();

    @Override
    protected void doInit() throws Exception 
    {
        super.doInit();
        template = (RRDatabaseManagerTemplate) getChildren(RRDatabaseManagerTemplate.NAME);
        if (template==null)
        {
            template = new RRDatabaseManagerTemplate();
            addChildren(template);
            configurator.getTreeStore().saveNode(template);
            template.init();
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        
        syncDatabases();
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        if (node==this)
        {
            if (attribute.getName().equals(STARING_POINT_ATTR_NAME) && getStatus()==Status.STARTED)
                syncDatabases();
        }
    }

    @Override
    public void childrenAdded(Node owner, Node children)
    {
        super.childrenAdded(owner, children);
        
        if (owner!=this && isNotInDatabaseManager(children))
        {
            lock.lock();
            try
            {
                DataSource newSource = (DataSource) children;
                if (newSource.getStatus()==Status.CREATED)
                    newDataSources.add(newSource.getId());
                else
                    syncDataSource(newSource, null);
            }finally{
                lock.unlock();
            }
        }
    }

    @Override
    public void childrenRemoved(Node owner, Node children)
    {
        super.childrenRemoved(owner, children);
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        if (isNotInDatabaseManager(node) && node.getStatus()!=Status.CREATED)
        {
            lock.lock();
            try
            {
                if (newDataSources.remove(node.getId()))
                    syncDataSource((DataSource) node, null);
            }finally{
                lock.unlock();
            }
        }
    }
    
    public RemovePolicy getRemovePolicy()
    {
        return removePolicy;
    }

    public Integer getDataSourcesPerDatabase()
    {
        return dataSourcesPerDatabase;
    }

    public String getDataTypeAttributeName()
    {
        return dataTypeAttributeName;
    }

    public Node getStartingPoint()
    {
        return startingPoint;
    }

    private boolean isNotInDatabaseManager(Node node)
    {
        if (!(node instanceof DataSource))
            return false;
        
        while ((node = node.getParent())!=null)
            if (node==this)
                return false;
        
        return true;
    }

    private void syncDatabases()
    {
        if (lock.tryLock())
        {
            try
            {
                Node startNode = startingPoint;

                if (startNode==null)
                    return;

                Map<Integer, RRDataSource> datasources = new HashMap<Integer, RRDataSource>();
                collectDatasources(this, datasources);

                Set<Integer> pipes = new HashSet<Integer>();
                collectDataPipes(startNode, pipes, datasources);

                if (datasources.size()!=pipes.size())
                    for (Map.Entry<Integer, RRDataSource> entry: datasources.entrySet())
                        if (!pipes.contains(entry.getKey()))
                        {
                            switch (removePolicy)
                            {
                                case STOP_DATABASES : entry.getValue().stop(); break;
                                case REMOVE_DATABASES : 
                                    entry.getValue().getParent().removeChildren(entry.getValue()); 
                                    break;
                            }
                            if (removePolicy==RemovePolicy.STOP_DATABASES)
                                entry.getValue().stop();
                        }
            }finally
            {
                lock.unlock();
            }
        }else
            logger.error("Error syncing database manager (%s). Locking error.");
    }
    
    private void syncDataSource(
            DataSource dataSource, Set<Integer> pipes)
    {
        
        NodeAttribute dataTypeAttr = dataSource.getNodeAttribute(dataTypeAttributeName);
        if (dataTypeAttr==null || !String.class.equals(dataTypeAttr.getType()))
        {
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Skiping data source (%s). Data type attribute (%s) not found"
                        , dataSource.getPath(), dataTypeAttributeName));
            return;
        }
        String dataType = dataTypeAttr.getValue();
        if (dataType==null)
        {
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Skiping data source (%s). Data type attribute (%s) value not seted"
                        , dataSource.getPath(), dataTypeAttributeName));
            return;
        }
        if (pipes!=null)
            pipes.add(dataSource.getId());
        addNewDataSource(dataSource, dataType);
    }
    
    private void addNewDataSource(DataSource dataSource, String dataType)
    {
        Node databaseTemplate = getDatabaseTemplate(dataType, dataSource);
        if (databaseTemplate==null)
            return;
        DatabasesEntry databasesEntry = getDatabasesEntry(databaseTemplate.getName());
        databasesEntry.addDataSource(databaseTemplate, dataSource);
    }
    
    private DatabasesEntry getDatabasesEntry(String dataType)
    {
        DatabasesEntry databasesEntry = (DatabasesEntry) getChildren(dataType);
        if (databasesEntry==null)
        {
            databasesEntry = new DatabasesEntry();
            databasesEntry.setName(dataType);
            addChildren(databasesEntry);
            configurator.getTreeStore().saveNode(databasesEntry);
            databasesEntry.init();
            databasesEntry.start();
        }
        return databasesEntry;
    }

    private Node getDatabaseTemplate(String dataType, DataSource dataSource)
    {
        Node databaseTemplate = template.getChildren(dataType);
        if (databaseTemplate == null)
        {
            databaseTemplate = template.getChildren(DEFAULT_DATABASE_TEMPLATE);
        }
        if (databaseTemplate == null)
        {
            logger.error(String.format(
                    "Error creating round robin database/datasource for (%s). " 
                    + "Database template not found for dataType (%s)"
                    , dataSource.getPath(), dataType));
            return null;
        }
        int dsCount = 0;
        int archiveCount = 0;
        Collection<Node> childs = databaseTemplate.getChildrens();
        if (childs != null)
        {
            for (Node child : childs)
            {
                if (child instanceof RRDataSource)
                {
                    ++dsCount;
                } else if (child instanceof RRArchive)
                {
                    ++archiveCount;
                }
            }
        }
        if (dsCount != 1 || archiveCount < 1)
        {
            logger.error(String.format(
                    "Invalid database template (%s). The template must have exactly "
                    + "one datasource (RRDataSource) and at least one archive (RRArhive)"
                    , databaseTemplate.getPath()));
            return null;
        }

        return databaseTemplate;
    }

    private void collectDataPipes(
            Node node, Set<Integer> dataPipes, Map<Integer, RRDataSource> datasources)
    {
        if (node instanceof DataSource && !datasources.containsKey(node.getId()))
            syncDataSource((DataSource)node, dataPipes);
        
        Collection<Node> childs = node.getChildrens();
        if (childs!=null)
            for (Node child: childs)
                collectDataPipes(child, dataPipes, datasources);
    }
    
    private void collectDatasources(Node node, Map<Integer, RRDataSource> datasources)
    {
        Collection<Node> childs = node.getChildrens();
        if (childs!=null)
            for (Node child: childs)
                if (child instanceof RRDataSource)
                {
                    DataSource ds = ((RRDataSource)child).getDataSource();
                    if (ds!=null)
                        datasources.put(ds.getId(),(RRDataSource) child);
                }else if (child!=template)
                    collectDatasources(child, datasources);
    }
}
