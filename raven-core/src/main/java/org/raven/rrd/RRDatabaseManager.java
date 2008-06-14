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
import org.raven.annotations.Parameter;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.rrd.data.RRArchive;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RRDatabaseManager extends BaseNode
{
    public static final String DEFAULT_DATABASE_TEMPLATE = "DEFAULT-TEMPLATE";
    public final static String DEFAULT_DATA_TYPE_ATTRIBUTE_NAME = "dataType";

    public enum RemovePolicy {STOP_DATABASES, REMOVE_DATABASES}
    
    @Parameter @NotNull
    @Description("The attribute name by which value data pipes will in one database")
    private String dataTypeAttributeName = DEFAULT_DATA_TYPE_ATTRIBUTE_NAME;
    
    @Parameter @NotNull
    @Description("The count of data sources per database")
    private Integer dataSourcesPerDatabase;
    
    private final static String STARING_POINT_ATTR_NAME = "startingPoint";
    @Parameter @NotNull
    @Description(
        "The node from which database manager take a control on data pipe nodes with seted " +
        "dataType attribute value")
    private Node startingPoint;
    
    @Parameter @NotNull
    @Description("Defines the remove policy")
    private RemovePolicy removePolicy = RemovePolicy.STOP_DATABASES;
    
    private RRDatabaseManagerTemplate template;

    @Override
    protected void doInit()
    {
        super.doInit();
        template = (RRDatabaseManagerTemplate) getChildren(RRDatabaseManagerTemplate.NAME);
        if (template==null)
        {
            template = new RRDatabaseManagerTemplate();
            addChildren(template);
            configurator.getTreeStore().saveNode(template);
        }
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, String oldValue, String newValue)
    {
        if (node==this)
        {
            if (attribute.getName().equals(STARING_POINT_ATTR_NAME))
            {
                syncDatabases();
                cleanupDatabases();
            }
        }
    }

    public RemovePolicy getRemovePolicy()
    {
        return removePolicy;
    }

    public void setRemovePolicy(RemovePolicy removePolicy)
    {
        this.removePolicy = removePolicy;
    }
    
    public Integer getDataSourcesPerDatabase()
    {
        return dataSourcesPerDatabase;
    }

    public void setDataSourcesPerDatabase(Integer dataSourcesPerDatabase)
    {
        this.dataSourcesPerDatabase = dataSourcesPerDatabase;
    }

    public String getDataTypeAttributeName()
    {
        return dataTypeAttributeName;
    }

    public void setDataTypeAttributeName(String dataTypeAttributeName)
    {
        this.dataTypeAttributeName = dataTypeAttributeName;
    }

    public Node getStartingPoint()
    {
        return startingPoint;
    }

    public void setStartingPoint(Node startingPoint)
    {
        this.startingPoint = startingPoint;
    }
    
    private void syncDatabases()
    {
        Node startNode = startingPoint;
        
        if (startNode==null)
            return;
        
        Map<Integer, RRDataSource> datasources = new HashMap<Integer, RRDataSource>();
        collectDatasources(this, datasources);
        
        Set<DataSource> pipes = new HashSet<DataSource>();
        collectDataPipes(startNode, pipes, datasources);
    }
    
    private void syncDataSource(DataSource dataSource, Map<Integer, RRDataSource> datasources)
    {
        if (datasources.containsKey(dataSource.getId()))
        {
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Data source (%s) already under (%s) control",
                        dataSource.getPath(), getPath()));
            return;
        }
        
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
        
        addNewDataSource(dataSource, dataType);
    }
    
    private void addNewDataSource(DataSource dataSource, String dataType)
    {
        Node databaseTemplate = getDatabaseTemplate(dataType, dataSource);
        if (databaseTemplate==null)
            return;
        DatabasesEntry databasesEntry = getDatabasesEntry(dataType);
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
            Node node, Set<DataSource> dataPipes, Map<Integer, RRDataSource> datasources)
    {
        if (node instanceof DataSource)
        {
            syncDataSource((DataSource)node, datasources);
        }
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
    
    private void cleanupDatabases()
    {
        
    }
    
    private class DataPipesListener implements NodeListener
    {

        public boolean isSubtreeListener()
        {
            return true ;
        }

        public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
        {
        }

        public void nodeNameChanged(Node node, String oldName, String newName)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void childrenAdded(Node owner, Node children)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void nodeAttributeValueChanged(Node node, NodeAttribute attribute, String oldValue, String newValue)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void nodeAttributeRemoved(Node node, NodeAttribute attribute)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
