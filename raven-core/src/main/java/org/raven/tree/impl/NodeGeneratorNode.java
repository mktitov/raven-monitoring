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

package org.raven.tree.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.apache.commons.lang.text.StrSubstitutor;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.Table;
import org.raven.template.GroupNode;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.GroupsOrganizerNodeTuner;
import org.raven.template.impl.TemplateNode;
import org.raven.tree.ConfigurableNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeTuner;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class NodeGeneratorNode extends DataPipeImpl implements ConfigurableNode
{
    public final static String INDEX_COLUMN_VALUE = "tableIndexColumnValue";

    public final static String INDEXCOLUMNNAME_ATTRIBUTE = "indexColumnName";
    public final static String ADDPOLICY_ATTRIBUTE = "addPolicy";
    public final static String REMOVEPOLICY_ATTRIBUTE = "removePolicy";
    private final static String ROW_EXPRESSION_VARIABLE = "row";
    private final static String INDEX_COLUMN_VALUE_VARIABLE ="indexColumnValue";
    private final static String ROWNUM_VARIABLE = "rownum";

    public enum AddPolicy {DO_NOTHING, AUTO_ADD, AUTO_ADD_AND_START}
    public enum RemovePolicy {DO_NOTHING, STOP_NODE, AUTO_REMOVE, REMOVE_BEFORE_PROCESSING}

	@Service
	private static GroupsOrganazier groupsOrganazier;
    
    @Parameter @NotNull
    private String indexColumnName;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private String indexExpression;
    
    @Parameter(defaultValue="DO_NOTHING")
    @NotNull
    private AddPolicy addPolicy;
    
    @Parameter(defaultValue="STOP_NODE")
    @NotNull
    private RemovePolicy removePolicy;

    private Lock dataLock;
    private NodeGeneratorNodeTemplate template;
    private boolean needTableForConfiguration;
    private Table table;
    private Map<String, Object> expressionBindings;

    @Override
    protected void initFields() 
    {
        super.initFields();
        dataLock = new ReentrantLock();
        template = null;
        needTableForConfiguration = false;
        table = null;
        expressionBindings = new HashMap<String, Object>();
    }
    
    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        
        template = (NodeGeneratorNodeTemplate) getChildren(NodeGeneratorNodeTemplate.NAME);
        if (template==null)
        {
            template = new NodeGeneratorNodeTemplate();
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

    public String getIndexExpression() {
        return indexExpression;
    }

    public void setIndexExpression(String indexExpression) {
        this.indexExpression = indexExpression;
    }

    public AddPolicy getAddPolicy()
    {
        return addPolicy;
    }

    public RemovePolicy getRemovePolicy()
    {
        return removePolicy;
    }

    public void setAddPolicy(AddPolicy addPolicy) {
        this.addPolicy = addPolicy;
    }

    public void setIndexColumnName(String indexColumnName) {
        this.indexColumnName = indexColumnName;
    }

    public void setRemovePolicy(RemovePolicy removePolicy) {
        this.removePolicy = removePolicy;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
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
    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return super.getDataImmediate(dataConsumer, sessionAttributes);
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
                    getDataSource().getDataImmediate(this, null);
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
                    NodeAttribute colName = dep.getNodeAttribute(
                            NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME);
                    if (colName == null || colName.getValue() == null)
                    {
                        ((DataConsumer) dep).setData(this, data);
                    } else
                    {
                        String indexColumnValue = getIndexValue(dep);
                        if (indexColumnValue == null)
                        {
                            logger.error(String.format(
                                    "Table index column value not found for node (%s)"
                                    , dep.getPath()));
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

    private void createNewNode(Map<String, Object> values, String indexValue, boolean autoStart)
        throws Exception
    {
//        StrSubstitutor subst = new StrSubstitutor(values);

		NodeTuner tuner = new Tuner(indexValue, configurator);

		groupsOrganazier.organize(this, template, tuner, null, autoStart);
    }

    private Node getTemplateNode()
    {
        Collection<Node> templateNodes = template.getEffectiveChildrens();

        if (templateNodes==null || templateNodes.size()==0)
            return null;
//            throw new NodeError("Template node must be created");

        if (templateNodes.size()>1)
            throw new NodeError("Only one node must created in the template");

        return templateNodes.iterator().next();
    }
    
    private void getTable()
    {
        needTableForConfiguration = true;
        table = null;
        try{
            getDataSource().getDataImmediate(this, null);
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
        if (RemovePolicy.REMOVE_BEFORE_PROCESSING==removePolicy)
            removeGeneratedNodes();
        Set<Node> deps = getDependentNodes()==null? null : new HashSet<Node>(getDependentNodes());
//        if (deps==null || deps.size()==0)
//            return;
        
        Map<String, List<DataConsumer>> consumers = collectConsumers(deps, data);
        Map<String, Node> indexValues = getIndexValues();
        Set<String> indexesInTable = new HashSet<String>();
        Table tab = (Table) data;
        int rownum = 1;
        for (Iterator<Object[]> it = tab.getRowIterator(); it.hasNext();)
        {
            Object[] tableRow = it.next();
            Map<String, Object> namedRow = new HashMap<String, Object>();
            for (int col=0; col<tab.getColumnNames().length; ++col)
                namedRow.put(tab.getColumnNames()[col], tableRow[col]);
            expressionBindings.put(ROWNUM_VARIABLE, rownum++);
            expressionBindings.put(ROW_EXPRESSION_VARIABLE, namedRow);
            Object val = namedRow.get(indexColumnName);
            expressionBindings.put(INDEX_COLUMN_VALUE_VARIABLE, val);
            String indexColumnValue = indexExpression;
            if (indexColumnValue==null)
				indexColumnValue = converter.convert(String.class, val, null);

            indexesInTable.add(indexColumnValue);

            sendDataToConsumers(namedRow, indexColumnValue, consumers);

            if (!indexValues.containsKey(indexColumnValue))
                processAddOperation(namedRow, indexColumnValue);

        }
        if (indexesInTable.size()<indexValues.size())
        {
			if (isLogLevelEnabled(LogLevel.WARN))
				warn(String.format(
						"Incoming table does not contains values for (%d) indexes"
						, indexValues.size()-indexesInTable.size()));
            processRemoveOperation(indexValues, indexesInTable);
        }
    }
    
    private void processAddOperation(
            Map<String, Object> namedRow, String indexColumnValue)
        throws Exception
    {
        if (   addPolicy==AddPolicy.AUTO_ADD
            || addPolicy==AddPolicy.AUTO_ADD_AND_START 
            || needTableForConfiguration)
        {
            createNewNode(
                    namedRow, indexColumnValue
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
		SearchIndexedNodes handler = new SearchIndexedNodes();
		tree.scanSubtree(this, handler, ScanOptionsImpl.EMPTY_OPTIONS);

		return handler.getIndexedNodes();
    }
    
    private String getIndexValue(Node node)
    {
		NodeAttribute indexAttr = null;
        while ( (indexAttr = node.getNodeAttribute(INDEX_COLUMN_VALUE))==null )
		{
            node = node.getParent();
			if (node==this)
				return null;
		}
        
        return indexAttr==null? null : indexAttr.getValue();
    }
    
    private String getTableCoumnName(Node node)
    {
        return node.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME).getValue();
    }

    private void sendDataToConsumers(
            Map<String, Object> namedRow, String indexColumnValue
            , Map<String, List<DataConsumer>> consumers) 
        throws TypeConverterException
    {
        List<DataConsumer> list = consumers.get(indexColumnValue);
        if (list != null)
            for (DataConsumer consumer : list)
            {
                String columnName = getTableCoumnName((Node)consumer);
                Object value = namedRow.get(columnName);
                consumer.setData(this, value);
            }
    }

    //TODO: преобразовать в NodeTuner
    private void tuneNode(NodeGeneratorNode tableNode, StrSubstitutor subst, Node newNode)
            throws Exception
    {
        String newName = subst.replace(newNode.getName());
        if (!newName.equals(newNode.getName()))
        {
            newNode.setName(newName);
            configurator.getTreeStore().saveNode(newNode);
        }
        
        NodeAttribute columnNameAttr = 
                newNode.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME);
        if (columnNameAttr!=null)
        {
            if (columnNameAttr.getRawValue()==null)
            {
                newNode.removeNodeAttribute(columnNameAttr.getName());
                configurator.getTreeStore().removeNodeAttribute(columnNameAttr.getId());
            } 
            else if (newNode instanceof AbstractDataConsumer)
            {
                NodeAttribute dataSourceAttr = 
                        newNode.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
                dataSourceAttr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
                dataSourceAttr.setValue(tableNode.getPath());
                dataSourceAttr.save();
            }
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

    @Override
    public void formExpressionBindings(Bindings bindings) 
    {
        super.formExpressionBindings(bindings);
        if (!isTemplate())
        {
            bindings.putAll(expressionBindings);
            bindings.put(TemplateNode.TEMPLATE_EXPRESSION_BINDING, this);
        }
    }

    private void removeGeneratedNodes()
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null)
        {
            childs = new ArrayList<Node>(childs);
            for (Node child: childs)
                if (child.getNodeAttribute(INDEX_COLUMN_VALUE)!=null)
                    tree.remove(child);
        }
    }

	private class SearchIndexedNodes implements ScannedNodeHandler
	{
		private final Map<String, Node> indexedNodes = new HashMap<String, Node>();

		public Map<String, Node> getIndexedNodes()
		{
			return indexedNodes;
		}

		public ScanOperation nodeScanned(Node node)
		{
			if (NodeGeneratorNodeTemplate.class.equals(node.getClass()))
				return ScanOperation.SKIP_NODE;
			
			NodeAttribute indexAttr = node.getNodeAttribute(INDEX_COLUMN_VALUE);
			if (indexAttr!=null)
			{
				String indexValue = indexAttr.getValue();
				if (indexValue!=null)
				{
					indexedNodes.put(indexValue, node);
					return ScanOperation.SKIP_NODE;
				}
			}

			return ScanOperation.CONTINUE;
		}
	}

	private class Tuner extends GroupsOrganizerNodeTuner
	{
		private final String indexValue;
		private final Configurator configurator;

		public Tuner(String indexValue, Configurator configurator)
		{
			this.indexValue = indexValue;
			this.configurator = configurator;
		}

		@Override
		public void tuneNode(Node sourceNode, Node sourceClone)
		{
			super.tuneNode(sourceNode, sourceClone);

			if (!(sourceNode instanceof GroupNode))
			{
				NodeAttribute indexAttr = new NodeAttributeImpl(
						INDEX_COLUMN_VALUE, String.class, indexValue, null);
				indexAttr.setOwner(sourceClone);
				sourceClone.addNodeAttribute(indexAttr);
			}
		}

		@Override
		public void finishTuning(Node sourceClone)
		{
			super.finishTuning(sourceClone);

			NodeAttribute columnNameAttr =
					sourceClone.getNodeAttribute(NodeGeneratorNodeTemplate.TABLE_COLUMN_NAME);
			if (columnNameAttr!=null)
			{
				if (columnNameAttr.getRawValue()==null)
				{
					sourceClone.removeNodeAttribute(columnNameAttr.getName());
					configurator.getTreeStore().removeNodeAttribute(columnNameAttr.getId());
				}
				else if (sourceClone instanceof AbstractDataConsumer)
				{
					try
					{
						NodeAttribute dataSourceAttr = sourceClone.getNodeAttribute(
								AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
						dataSourceAttr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
						dataSourceAttr.setValue(getPath());
						dataSourceAttr.save();
					}
					catch (Exception ex)
					{
						String message = 
								String.format(
									"Error configuring (%s) attribute for node (%s)"
									, AbstractDataConsumer.DATASOURCE_ATTRIBUTE
									, sourceClone.getPath());
						error(message, ex);
						throw new NodeError(message, ex);
					}
				}
			}
		}
	}
}
