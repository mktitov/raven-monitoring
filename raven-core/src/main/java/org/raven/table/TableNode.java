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
import java.util.HashSet;
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
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.converter.TypeConverterException;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
@Description("Allows to create child nodes dynamically")
public class TableNode extends DataPipeImpl implements ConfigurableNode
{
    public final static String INDEX_COLUMN_VALUE = "tableIndexColumnValue";

    public final static String INDEXCOLUMNNAME_ATTRIBUTE = "indexColumnName";
    public final static String ADDPOLICY_ATTRIBUTE = "addPolicy";
    public final static String REMOVEPOLICY_ATTRIBUTE = "removePolicy";
    
    public enum AddPolicy {DO_NOTHING, AUTO_ADD, AUTO_ADD_AND_START}
    public enum RemovePolicy {DO_NOTHING, STOP_NODE, AUTO_REMOVE}
    
    @Parameter
    @NotNull
    @Description("The table column name which is the index column for this node")
    private String indexColumnName;
    
    @Parameter(defaultValue="DO_NOTHING")
    @NotNull
    @Description("Add policy")
    private AddPolicy addPolicy;
    
    @Parameter(defaultValue="STOP_NODE")
    @NotNull
    @Description("Remove policy")
    private RemovePolicy removePolicy;
    
    private final Lock dataLock = new ReentrantLock();
    private TableNodeTemplate template;
    private boolean needTableForConfiguration = false;
    private Table table = null;
    
    @Override
    protected void doInit() throws Exception
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

    public AddPolicy getAddPolicy()
    {
        return addPolicy;
    }

    public RemovePolicy getRemovePolicy()
    {
        return removePolicy;
    }

    @Override
    public void setData(DataSource dataSource, Object data)
    {
        try{
            if (dataLock.tryLock())
            {
                try {
                    if (data==null)
                        return;
                    if (!(data instanceof Table))
                    {
                        logger.error(String.format(
                            "Invalid data type (%s) recieved from data source (%s) for node (%s). "
                            + "The valid data type is (%s)"
                            , data.getClass().getName(), getDataSource().getPath(), getPath()
                            , Table.class.getName()));
                        logger.warn(String.format("Stopping node (%s) due errors", getPath()));
                        stop();
                        return;
                    }
                    processData(data);
                } finally {
                    dataLock.unlock();
                }
            }
        }catch(Exception e)
        {
            logger.error(String.format("Data processing error in the node (%s)", getPath()), e);
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
                needTableForConfiguration = true;
                try
                {
                    getDataSource().getDataImmediate(this);
                } finally
                {
                    needTableForConfiguration = false;
                    dataLock.unlock();
                }
            } else
            {
                logger.error(
                        String.format("Error locking node (%s) for configuration purposes"
                        , getPath()));
            }
        } catch (InterruptedException e)
        {
            logger.error(String.format("Node (%s) configuration error", getPath()), e);
        }
//        try
//        {
//            if (dataLock.tryLock(500, TimeUnit.MILLISECONDS))
//            {
//                try
//                {
//                    Node templateNode = getTemplateNode();
//                    getTable();
//                    for (int row=0; row<table.getRowCount(); ++row)
//                    {
//                        Object val = table.getValue(indexColumnName, row);
//                        String indexColumnValue = converter.convert(String.class, val, null);
//                        createNewNode(table, row, templateNode, indexColumnValue, false);    
//                    }
//                }finally{
//                    dataLock.unlock();
//                }
//            }
//        } catch (Exception e)
//        {
//            throw new NodeError(
//                    String.format("Node (%s) configuration error", getPath()) , e);
//        }
    }

    private Map<String, List<DataConsumer>> collectConsumers(Set<Node> deps, Object data)
    {
        Map<String, List<DataConsumer>> consumers = new HashMap<String, List<DataConsumer>>();
        if (deps!=null)
            for (Node dep : deps)
            {
                if (dep instanceof DataConsumer && !dep.isTemplate())
                {
                    NodeAttribute colName = dep.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
                    if (colName == null || colName.getValue() == null)
                    {
                        ((DataConsumer) dep).setData(this, data);
                    } else
                    {
                        String indexColumnValue = getIndexValue(dep);
                        if (indexColumnValue == null)
                        {
                            logger.error(String.format("Table index column value not found for node (%s)", dep.getPath()));
                        } else
                        {
                            List<DataConsumer> list = consumers.get(indexColumnValue);
                            if (list == null)
                            {
                                list = new ArrayList<DataConsumer>(2);
                                consumers.put(indexColumnValue, list);
                            }
                            list.add((DataConsumer) dep);
                        }
                    }
                }
            }
        return consumers;
    }

    private void createNewNode(
            Table tab, int row, Node templateNode, String indexValue, boolean autoStart) 
        throws Exception
    {
        Map<String, Object> values = new HashMap<String, Object>();
        for (String columnName : tab.getColumnNames())
            values.put(columnName, tab.getValue(columnName, row));
        StrSubstitutor subst = new StrSubstitutor(values);

        Node newNode = tree.copy(templateNode, this, null, null, true, false);
        NodeAttribute indexAttr = new NodeAttributeImpl(
                INDEX_COLUMN_VALUE, String.class, indexValue, null);
        indexAttr.setOwner(newNode);
        newNode.addNodeAttribute(indexAttr);
        indexAttr.init();
        indexAttr.save();
        
        tuneNode(this, subst, newNode);
        
        if (autoStart)
            tree.start(newNode);
    }

    private Node getTemplateNode()
    {
        Collection<Node> templateNodes = template.getChildrens();

        if (templateNodes==null || templateNodes.size()==0)
            throw new NodeError("Template node must be created");

        if (templateNodes.size()>1)
            throw new NodeError("Only one node must created in the template");

        Node templateNode = templateNodes.iterator().next();
//        NodeAttribute indexColAttr = 
//                templateNode.getNodeAttribute(TableNodeTemplate.TABLE_INDEX_COLUMN_NAME);
//        if (indexColAttr==null || indexColAttr.getValue()==null)
//            throw new NodeError(String.format(
//                    "The attribute (%s) value must be seted for the node (%s)"
//                    , TableNodeTemplate.TABLE_INDEX_COLUMN_NAME, templateNode.getPath()));
        
        return templateNode;
    }
    
    private void getTable()
    {
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
    }

    private void processData(Object data) throws Exception
    {
        Set<Node> deps = getDependentNodes();
//        if (deps==null || deps.size()==0)
//            return;
        
        Map<String, List<DataConsumer>> consumers = collectConsumers(deps, data);
        Map<String, Node> indexValues = getIndexValues();
        Set<String> indexesInTable = new HashSet<String>();
        Table tab = (Table) data;
        if (tab.getRowCount() > 0)
        {
            for (int row = 0; row < tab.getRowCount(); ++row)
            {
                Object val = tab.getValue(indexColumnName, row);
                String indexColumnValue = converter.convert(String.class, val, null);
                
                indexesInTable.add(indexColumnValue);
                
                sendDataToConsumers(tab, row, indexColumnValue, consumers);
                
                if (!indexValues.containsKey(indexColumnValue))
                    processAddOperation(tab, row, indexColumnValue);
                
            }
        }
        if (indexesInTable.size()<indexValues.size())
        {
            processRemoveOperation(indexValues, indexesInTable);
        }
    }
    
    private void processAddOperation(Table tab, int row, String indexColumnValue) throws Exception
    {
        if (   addPolicy==AddPolicy.AUTO_ADD
            || addPolicy==AddPolicy.AUTO_ADD_AND_START 
            || needTableForConfiguration)
        {
            Node templateNode = getTemplateNode();
            createNewNode(
                    tab, row, templateNode, indexColumnValue
                    , addPolicy==AddPolicy.AUTO_ADD_AND_START && !needTableForConfiguration);
        }
    }
    
    private void processRemoveOperation(Map<String, Node> indexValues, Set<String> indexesInTable)
    {
        if (removePolicy==RemovePolicy.DO_NOTHING)
            return;
        for (Map.Entry<String, Node> entry: indexValues.entrySet())
        {
            if (!indexesInTable.contains(entry.getKey()))
            {
                if (removePolicy==RemovePolicy.STOP_NODE)
                    tree.stop(entry.getValue());
                else if (removePolicy==RemovePolicy.AUTO_REMOVE)
                    tree.remove(entry.getValue());
            }
        }
    }

    private Map<String, Node> getIndexValues()
    {
        Map<String, Node> indexValues = new HashMap<String, Node>();
        if (getChildrens()!=null)
            for (Node child: getChildrens())
                if (!(child instanceof TableNodeTemplate))
                    indexValues.put(getIndexValue(child), child);
        return indexValues;
    }
    
    private String getIndexValue(Node node)
    {
        while (!(node.getParent() instanceof TableNode)) 
            node = node.getParent();
        
        NodeAttribute indexAttr = node.getNodeAttribute(INDEX_COLUMN_VALUE);
        return indexAttr==null? null : indexAttr.getValue();
    }
    
    private String getTableCoumnName(Node node)
    {
        return node.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME).getValue();
    }

    private void sendDataToConsumers(
            Table tab, int row, String indexColumnValue, Map<String, List<DataConsumer>> consumers) 
        throws TypeConverterException
    {
        List<DataConsumer> list = consumers.get(indexColumnValue);
        if (list != null)
            for (DataConsumer consumer : list)
            {
                String columnName = getTableCoumnName(consumer);
                Object value = tab.getValue(columnName, row);
                consumer.setData(this, value);
            }
    }

    private void tuneNode(TableNode tableNode, StrSubstitutor subst, Node newNode) throws Exception
    {
        String newName = subst.replace(newNode.getName());
        if (!newName.equals(newNode.getName()))
        {
            newNode.setName(newName);
            configurator.getTreeStore().saveNode(newNode);
        }
        
        NodeAttribute columnNameAttr = 
                newNode.getNodeAttribute(TableNodeTemplate.TABLE_COLUMN_NAME);
        if (columnNameAttr!=null && columnNameAttr.getRawValue()==null)
        {
            newNode.removeNodeAttribute(columnNameAttr.getName());
            configurator.getTreeStore().removeNodeAttribute(columnNameAttr.getId());
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
                    attr.setValue(newVal);
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
