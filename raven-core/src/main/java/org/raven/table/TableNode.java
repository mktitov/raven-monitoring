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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.text.StrSubstitutor;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.tree.ConfigurableNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
@Description("Allows to create child nodes dynamically")
public class TableNode extends DataPipeImpl implements ConfigurableNode
{
    @Parameter
    @NotNull
    @Description("The table column name which is the index column for this node")
    private String indexColumnName;
    
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

    public String getIndexColumnName()
    {
        return indexColumnName;
    }

    public void setIndexColumnName(String indexColumnName)
    {
        this.indexColumnName = indexColumnName;
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
                    processData(data);
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
        if (getStatus()!=Status.STARTED)
            return;
        try
        {
            if (dataLock.tryLock(500, TimeUnit.MILLISECONDS))
            {
                try{
                    Collection<Node> templateNodes = template.getChildrens();

                    if (templateNodes==null || templateNodes.size()==0)
                        throw new NodeError("Template node must be created");
                    
                    if (templateNodes.size()>1)
                        throw new NodeError("Only one node must created in the template");
                    
                    Node templateNode = templateNode.getChildrens().iterator().next();
                    NodeAttribute indexColAttr = 
                            templateNode.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
                    if (indexCol)
                    
                    needTableForConfiguration = true;
                    table = null;
                    try{
                        getDataSource().getDataImmediate(this);
                    }finally{
                        needTableForConfiguration = false;
                    }
                    
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

    private void processData(Object data)
    {
        Set<Node> deps = getDependentNodes();
        if (deps==null || deps.size()==0)
            return;
        Map<String, List<DataConsumer>> consumers = new HashMap<String, List<DataConsumer>>();
        for (Node dep: deps)
        {
            if (dep instanceof DataConsumer)
            {
                NodeAttribute colName = dep.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
                if (colName==null || colName.getValue()==null)
                    ((DataConsumer)dep).setData(this, data);
                else
                {
                    String name = colName.getValue();
                    List<DataConsumer> list = consumers.get(name);
                    if (list==null)
                    {
                        list = new ArrayList<DataConsumer>(2);
                        consumers.put(name, list);
                    }
                    list.add((DataConsumer)dep);
                }
            }
        }
        Table tab = (Table) data;
        if (tab.getRowCount()>0 && consumers.size()>0)
            for (int row=0; row<tab.getRowCount(); ++row)
            {
                for (Map.Entry<String, List<DataConsumer>> entry: consumers.entrySet())
                {
                    Object value = tab.getValue(entry.getKey(), row);
                    for (DataConsumer consumer: entry.getValue())
                        consumer.setData(this, value);
                }
            }
    }

    private void tuneNode(TableNode tableNode, StrSubstitutor subst, Node newNode)
    {
        String newName = subst.replace(newNode.getName());
        if (!newName.equals(newNode.getName()))
        {
            newNode.setName(newName);
            configurator.getTreeStore().saveNode(newNode);
        }
        
        if (getNodeAttributes()!=null)
        {
            List<NodeAttribute> attrs = new ArrayList<NodeAttribute>(newNode.getNodeAttributes());
            for (NodeAttribute attr: attrs)
            {
                boolean hasChanges = false;
                String newVal = subst.replace(attr.getName());
                if (!ObjectUtils.equals(newVal, attr.getName()))
                {
                    attr.setName(newVal);
                    hasChanges = true;
                }
                newVal = subst.replace(attr.getRawValue());
                if (!ObjectUtils.equals(newVal, attr.getRawValue()))
                {
                    hasChanges = true;
                    attr.setRawValue(newVal);
                }
                newVal = subst.replace(attr.getDescription());
                if (!ObjectUtils.equals(newVal, attr.getDescription()))
                {
                    hasChanges = true;
                    attr.setDescription(newVal);
                }
                if (hasChanges)
                    configurator.getTreeStore().saveNodeAttribute(attr);
            }
        }
        
        Collection<Node> childs = newNode.getChildrens();
        if (childs!=null)
            for (Node child: childs)
                tuneNode(tableNode, subst, child);
    }
}
