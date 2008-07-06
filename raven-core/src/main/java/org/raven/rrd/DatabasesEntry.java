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
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.NodeAttribute;
/**
 *
 * @author Mikhail Titov
 */
public class DatabasesEntry extends BaseNode
{

    public DatabasesEntry()
    {
        setSubtreeListener(true);
    }

    @Override
    public void childrenAdded(Node owner, Node children)
    {
        super.childrenAdded(owner, children);
        
        if (children instanceof DataSource)
            getDatabaseManager().addManagedDatasource((DataSource) children);
    }
    
    public void addDataSource(Node templateNode, DataSource dataSource)
    {
        Collection<Node> childs = getChildrens();
        RRDNode rrd = null;
        if (childs!=null)
            for (Node child: childs)
            {
                RRDNode rrdnode = (RRDNode) child;
                if (rrdnode.getDataSourceCount()<getDatabaseManager().getDataSourcesPerDatabase())
                {
                    rrd = rrdnode;
                    break;
                }
            }
        if (rrd==null)
            createNewDatabase(templateNode, dataSource);
        else
            addDataSourceToDatabase(templateNode, rrd, dataSource);
    }

    private void addDataSourceToDatabase(Node template, RRDNode rrd, DataSource dataSource) 
    {
        try
        {
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Adding new datasource (%s) to rrd (%s)"
                        , dataSource.getPath(), rrd.getPath()));
            RRDataSource templateDataSource = getTemplateDataSource(template);
            RRDataSource rrds = (RRDataSource) tree.copy(
                templateDataSource, rrd, ""+(rrd.getChildrenCount()+1), null, true, false);
            NodeAttribute dataSourceAttr = 
                    rrds.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
            dataSourceAttr.setValue(dataSource.getPath());
            dataSourceAttr.save();
            rrds.start();
            
//            getDatabaseManager().addManagedDatasource(dataSource);
            
        } catch (Exception ex)
        {
            logger.error(String.format(
                    "Error creating rrd datasource for datasource (%s)", dataSource.getPath())
                    , ex);
        }
    }

    private void createNewDatabase(Node templateNode, DataSource dataSource)
    {
        try
        {
            if (logger.isDebugEnabled())
                logger.debug(
                        String.format("Creating new database. Initial datasource is (%s)"
                        , dataSource.getPath()));
            String databaseName = "" + (getChildrenCount() + 1);
            RRDNode db = (RRDNode) tree.copy(templateNode, this, databaseName, null, true, false);
            RRDataSource rrds = null;
            for (Node child : db.getChildrens())
            {
                if (child instanceof RRDataSource)
                {
                    rrds = (RRDataSource) child;
                    break;
                }
            }
            rrds.setName("" + db.getChildrenCount());
            rrds.save();
            NodeAttribute dataSourceAttr = 
                    rrds.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
            dataSourceAttr.setValue(dataSource.getPath());
            dataSourceAttr.save();
            dataSource.save();
            
//            getDatabaseManager().addManagedDatasource(dataSource);
            
            tree.start(db);
        } catch (Exception ex)
        {
            logger.error(String.format(
                    "Error creating rrd database for datasource (%s)", dataSource.getPath())
                    , ex);
        }
    }
    
    private RRDataSource getTemplateDataSource(Node templateNode)
    {
        for (Node child: templateNode.getChildrens())
            if (child instanceof RRDataSource)
                return (RRDataSource) child;
        return null;
    }
    
    private RRDatabaseManager getDatabaseManager()
    {
        return (RRDatabaseManager) getParent();
    }
}
