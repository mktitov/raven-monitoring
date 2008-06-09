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

package org.raven.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.text.StrSubstitutor;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.tree.ConfigurableNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
@Description("Allows to create child nodes dynamically")
public class TableNode extends DataPipeImpl implements ConfigurableNode
{
    private final Lock dataLock = new ReentrantLock();
    private TableNodeTemplate template;
    private boolean needTableForConfiguration = false;
    private Table table = null;
    
    @Override
    protected void doInit()
    {
        super.doInit();
        
        template = (TableNodeTemplate) getChildren(TableNodeTemplate.NAME);
        if (template==null)
        {
            template = new TableNodeTemplate();
            addChildren(template);
            configurator.getTreeStore().saveNode(template);
            template.init();
            template.start();
        }
    }

    @Override
    public void setData(DataSource dataSource, Object data)
    {
        if (dataLock.tryLock())
        {
            try {
                if (data==null)
                    return;
                if (!(data instanceof Table))
                {
                    logger.error(String.format(
                        "Invalid data type (%s) recieved from data source (%s) for node (%s). " +
                        "The valid data type is (%s)"
                        , data.getClass().getName(), getDataSource().getPath(), getPath()
                        , Table.class.getName()));
                    logger.warn(String.format("Stopping node (%s) due errors", getPath()));
                    stop();
                    return;
                }
                if (needTableForConfiguration)
                    table = (Table) data;
                else
                {
                    //
                }
            } finally {
                dataLock.unlock();
            }
        }
    }

    @Override
    public void getDataImmediate(DataConsumer dataConsumer)
    {
        super.getDataImmediate(dataConsumer);
    }

    public void configure()
    {
        try
        {
            if (dataLock.tryLock(500, TimeUnit.MILLISECONDS))
            {
                try{
                    Collection<Node> templateNodes = template.getChildrens();

                    if (templateNodes==null || templateNodes.size()==0)
                        return;
                    
                    needTableForConfiguration = true;
                    table = null;
                    getDataSource().getDataImmediate(this);
                    
                    if (table==null)
                        throw new NodeError(String.format(
                                "Error geting table data from data source (%s)"
                                , getDataSource().getPath()));
                    for (int row=0; row<table.getRowCount(); ++row)
                    {
                        Map<String, Object> values = new HashMap<String, Object>();
                        for (String columnName: table.getColumnNames())
                            values.put(columnName, table.getValue(columnName, row));
                        
                        StrSubstitutor subst = new StrSubstitutor(values);
                        
                        for (Node templateNode : templateNodes)
                        {
                            Node newNode = tree.copy(templateNode, this, null, null, true, true);
                            tuneNode(this, subst, newNode);    
                        }
                    }
                }finally{
                    dataLock.unlock();
                }
            }
        } catch (Exception e)
        {
            throw new NodeError(
                    String.format("Node (%s) configuration error", getPath()) , e);
        }
    }

    private void tuneNode(TableNode tableNode, StrSubstitutor subst, Node newNode)
    {
        newNode.setName(subst.replace(newNode.getName()));
        
        if (getNodeAttributes()!=null)
        {
            List<NodeAttribute> attrs = new ArrayList<NodeAttribute>(getNodeAttributes());
            for (NodeAttribute attr: attrs)
            {
                attr.setName(subst.replace(attr.getName()));
                attr.setRawValue(subst.replace(attr.getRawValue()));
            }
        }
        
//        if (newNode instanceof AbstractDataConsumer)
//        {
//            NodeAttribute tableColumnNameAttr = 
//                    newNode.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
//            if (tableColumnNameAttr!=null && tableColumnNameAttr.getValue()!=null)
//            {
//                
//            }
//        }
        
        Collection<Node> childs = getChildrens();
        if (childs!=null)
            for (Node child: childs)
                tuneNode(tableNode, subst, child);
    }
}
