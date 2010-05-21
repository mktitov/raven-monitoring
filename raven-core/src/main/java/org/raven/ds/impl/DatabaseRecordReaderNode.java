/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataConsumer;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DatabaseRecordReaderNode extends AbstractDataSource
{
    public final static String TEMPLATE_MESSAGE_KEY = "template";
    public final static String
            FILTER_ATTRIBUTE_DESCRIPTION_MESSAGE_KEY = "filterAttributeDescription";
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";
    public final static String
            PROVIDE_FILTER_ATTRIBUTES_TO_CONSUMERS_ATTR = "provideFilterAttributesToConsumers";

    @Service
    private static MessagesRegistry messagesRegistry;

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter
    private String databaseExtensionName;
    
    @Parameter
    private String filterExtensionName;

    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    @NotNull()
    private ConnectionPool connectionPool;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean provideFilterAttributesToConsumers;

    @Parameter
    private String query;

    @Parameter
    private String whereExpression;

    @Parameter
    private String orderByExpression;

    @Parameter
    private Integer maxRows;

    @Parameter(defaultValue="100")
    private Integer fetchSize;

    @Parameter(readOnly=true)
    private long validRecords;

    @Parameter(readOnly=true)
    private long errorRecords;

    private long processingTime;
    private List<SelectField> selectFields;
    private Map<RecordSchemaField, FilterField> filterFields;
    private Lock filterFieldLock;
    private SchemaListener schemaListener;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();

        filterFieldLock = new ReentrantLock();
        schemaListener = new SchemaListener();
        
        validRecords = 0l;
        errorRecords = 0l;
        processingTime = 0l;
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void init() throws NodeError
    {
        super.init();

        RecordSchemaNode _recordSchema = recordSchema;
        if (_recordSchema!=null)
            _recordSchema.addListener(schemaListener);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        syncFilterFields(recordSchema, true);
        
        validRecords = 0l;
        errorRecords = 0l;
        processingTime = 0l;
    }

    @Override
    public boolean gatherDataForConsumer(
        DataConsumer dataConsumer, Map<String, NodeAttribute> attributes)
            throws Exception
    {
        bindingSupport.enableScriptExecution();
        try{
            bindingSupport.put("sessAttrs", attributes);
            long startTime = System.currentTimeMillis();
            long realProcessingTime = processingTime;
            long procStart = startTime;
            List<DatabaseFilterElement> filterElements = createFilterElements(attributes);

            DatabaseRecordQuery recordQuery = null;
            String _query = query;
            if (_query==null || _query.trim().isEmpty())
                recordQuery = new DatabaseRecordQuery(
                        recordSchema, databaseExtensionName, filterExtensionName, filterElements
                        , whereExpression
                        , orderByExpression, connectionPool, maxRows, fetchSize, converter);
            else
                recordQuery = new DatabaseRecordQuery(
                        recordSchema, databaseExtensionName, filterExtensionName
                        , filterElements, _query, connectionPool
                        , maxRows, fetchSize, converter);

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Executing query:\n"+recordQuery.getQuery());
            DatabaseRecordQuery.RecordIterator it = recordQuery.execute();
            processingTime+=System.currentTimeMillis()-startTime;
            try
            {
                try
                {
                    startTime = System.currentTimeMillis();
                    int i=0;
                    while (it.hasNext())
                    {
                        dataConsumer.setData(this, it.next());
                        ++validRecords;
                        if (i % 1000 == 0)
                        {
                            processingTime+=System.currentTimeMillis()-startTime;
                            startTime = System.currentTimeMillis();
                        }
                    }
                    dataConsumer.setData(this, null);

                    return true;
                }
                catch(Exception e)
                {
                    ++errorRecords;
                    throw e;
                }
            }
            finally
            {
                processingTime = realProcessingTime + (System.currentTimeMillis()-procStart);
                recordQuery.close();
            }
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    @Override
    public Collection<NodeAttribute> generateAttributes()
    {
        if (!provideFilterAttributesToConsumers)
            return null;
        else
        {
            Collection<NodeAttribute> result = new ArrayList<NodeAttribute>();
            for (NodeAttribute attr: getNodeAttributes())
                if (RECORD_SCHEMA_ATTR.equals(attr.getParentAttribute()))
                {
                    try
                    {
                        NodeAttribute clone = (NodeAttribute) attr.clone();
                        result.add(clone);
                    }
                    catch (CloneNotSupportedException ex)
                    {
                    }
                }

            return result.size()==0? null : result;
        }
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);

        try
        {
            if (node==this)
            {
                if (attribute.getName().equals(RECORD_SCHEMA_ATTR))
                {
                    if (oldValue!=null)
                        ((RecordSchemaNode)oldValue).removeListener(schemaListener);
                    syncFilterFields((RecordSchema)newValue, false);
                    if (newValue!=null)
                        ((RecordSchemaNode)newValue).addListener(schemaListener);
                }
//                else if (   attribute.getName().equals(PROVIDE_FILTER_ATTRIBUTES_TO_CONSUMERS_ATTR)
//                         && Status.STARTED==getStatus())
//                {
//                    stop();
//                    start();
//                }
            }
        }
        catch(Throwable e)
        {
            error(e.getMessage(), e);
        }
    }

    @Parameter(readOnly=true)
    public double getRecordsPerSecond()
    {
        double count = validRecords+errorRecords;
        return count==0.? 0. : processingTime/count*1000;
    }

    public long getErrorRecords()
    {
        return errorRecords;
    }

    public long getValidRecords()
    {
        return validRecords;
    }
    
    public ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }

    public Boolean getProvideFilterAttributesToConsumers()
    {
        return provideFilterAttributesToConsumers;
    }

    public void setProvideFilterAttributesToConsumers(Boolean provideFilterAttributesToConsumers)
    {
        this.provideFilterAttributesToConsumers = provideFilterAttributesToConsumers;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getDatabaseExtensionName()
    {
        return databaseExtensionName;
    }

    public void setDatabaseExtensionName(String databaseExtensionName)
    {
        this.databaseExtensionName = databaseExtensionName;
    }

    public String getFilterExtensionName()
    {
        return filterExtensionName;
    }

    public void setFilterExtensionName(String filterExtensionName)
    {
        this.filterExtensionName = filterExtensionName;
    }

    public String getOrderByExpression()
    {
        return orderByExpression;
    }

    public void setOrderByExpression(String orderByExpression)
    {
        this.orderByExpression = orderByExpression;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getWhereExpression()
    {
        return whereExpression;
    }

    public Integer getFetchSize()
    {
        return fetchSize;
    }

    public void setFetchSize(Integer fetchSize)
    {
        this.fetchSize = fetchSize;
    }

    public Integer getMaxRows()
    {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows)
    {
        this.maxRows = maxRows;
    }

    public void setWhereExpression(String whereExpression)
    {
        this.whereExpression = whereExpression;
    }

    private List<DatabaseFilterElement> createFilterElements(Map<String, NodeAttribute> attributes)
            throws DatabaseFilterElementException
    {
        List<DatabaseFilterElement> filterElements = null;
        if (filterFields != null && filterFields.size() > 0)
        {
            filterElements = new ArrayList<DatabaseFilterElement>(filterFields.size());
            for (FilterField field : filterFields.values())
            {
                String expression = null;
                String fieldName = field.getFieldInfo().getName();
                NodeAttribute consumerAttr = attributes.get(fieldName);
                if (consumerAttr != null)
                    expression = consumerAttr.getValue();
                if (expression == null)
                    expression = getNodeAttribute(fieldName).getValue();

                Class fieldType = RecordSchemaFieldType.getSqlType(
                        field.getFieldInfo().getFieldType());
                DatabaseFilterElement element = new DatabaseFilterElement(
                        field.getColumnName(), fieldType
                        , field.getFieldInfo().getPattern(), field.isVirtual(), converter);
                element.setExpression(expression);
                if (element.getExpressionType() != DatabaseFilterElement.ExpressionType.EMPTY)
                    filterElements.add(element);
            }
        }

        return filterElements;
    }

    private void syncFilterFields(RecordSchema recordSchema, boolean insideStartLifeCycle)
            throws Exception
    {
        filterFieldLock.lock();
        try
        {
            boolean stop = false;
            RecordSchemaField[] fields = null;
            if (recordSchema!=null)
                fields = recordSchema.getFields();
            filterFields = new HashMap<RecordSchemaField, FilterField>();
            Set<String> usedAttributes = new HashSet<String>();
            String dbExtension = databaseExtensionName;
            String filterExtension = filterExtensionName;
            if (fields!=null)
                for (RecordSchemaField field: fields)
                {
                    FilterField filterField = 
                            FilterField.create(field, dbExtension, filterExtension);
                    if (filterField!=null)
                    {
                        filterFields.put(field, filterField);
                        try
                        {
                            NodeAttribute attr = syncFilterFieldWithAttribute(filterField, null);
                            if (attr.isRequired() && attr.getValue()==null)
                                stop = true;
                            usedAttributes.add(field.getName());
                        }
                        catch (Throwable ex)
                        {
                            logErrorSyncFilterAttr(field.getName(), ex);
                        }
                    }
                }

            removeUnusedFilterAttributes(usedAttributes);

            if (stop)
            {
                if (insideStartLifeCycle)
                    throw new Exception(
                            "Node has filter attributes with not seted required values");
                else if (Status.STARTED==getStatus())
                {
                    error("Stoping node because of there are filter attributes with not seted " +
                            "required values");
                    stop();
                }
            }
        }
        finally
        {
            filterFieldLock.unlock();
        }
    }

    private void removeUnusedFilterAttributes(Set<String> usedAttributes)
    {
        Collection<NodeAttribute> attrs = getNodeAttributes();
        if (attrs!=null && attrs.size()>0)
        {
            Collection<NodeAttribute> attrList = new ArrayList<NodeAttribute>(attrs);
            for (NodeAttribute attr: attrList)
                if (   RECORD_SCHEMA_ATTR.equals(attr.getParentAttribute())
                    && !usedAttributes.contains(attr.getName()))
                {
                    removeFilterAttribute(attr);
                }
        }
    }

    private NodeAttribute syncFilterFieldWithAttribute(
            FilterField filterField, String oldFilterFieldName)
        throws Exception
    {
        NodeAttribute filterAttr = getNodeAttribute(filterField.fieldInfo.getName());
        if (filterAttr==null && oldFilterFieldName!=null)
            filterAttr = getNodeAttribute(oldFilterFieldName);

        NodeAttribute schemaAttr = getNodeAttribute(RECORD_SCHEMA_ATTR);
        MessageComposer description = new MessageComposer(messagesRegistry);
        String pattern = filterField.getFieldInfo().getPattern();
        if (pattern!=null)
            description
                .append(messagesRegistry.createMessageKeyForStringValue(
                    getClass().getName(), TEMPLATE_MESSAGE_KEY))
                .append(pattern).append("<p/>");
        description.append(messagesRegistry.createMessageKeyForStringValue(
                getClass().getName(), FILTER_ATTRIBUTE_DESCRIPTION_MESSAGE_KEY));
        if (filterAttr==null)
        {
            filterAttr = new NodeAttributeImpl(
                    filterField.getFieldInfo().getName()
                    , String.class
                    , filterField.getFilterInfo().getDefaultValue()
                    , null);
            filterAttr.setOwner(this);
            filterAttr.setParentAttribute(schemaAttr.getName());
            filterAttr.setRequired(
                    filterField.getFilterInfo().getFilterValueRequired()
                    && !provideFilterAttributesToConsumers);
            filterAttr.setDescriptionContainer(description);
            filterAttr.setDisplayName(filterField.getFieldInfo().getDisplayName());
            filterAttr.save();
            filterAttr.init();
            addNodeAttribute(filterAttr);
        }
        else
        {
            boolean hasChanges = false;
            if (!filterAttr.getName().equals(filterField.getFieldInfo().getName()))
            {
                filterAttr.setName(filterField.getFieldInfo().getName());
                hasChanges = true;
            }
            boolean required = 
                    filterField.getFilterInfo().getFilterValueRequired()
                    && !provideFilterAttributesToConsumers;
            if (filterAttr.isRequired() != required)
            {
                filterAttr.setRequired(required);
                hasChanges = true;
            }
            filterAttr.setDescriptionContainer(description);
//            if (hasChanges)
            filterAttr.save();
        }

        return filterAttr;
    }

    public void removeFilterAttribute(NodeAttribute attr)
    {
        removeNodeAttribute(attr.getName());
        configurator.getTreeStore().removeNodeAttribute(attr.getId());
    }

    public void removeFilterAttribute(String attrName)
    {
        NodeAttribute attr = getNodeAttribute(attrName);
        if (attr!=null)
            removeFilterAttribute(attr);
    }

    private void logErrorSyncFilterAttr(String fieldName, Throwable e)
    {
        error(String.format(
                "Error syncing filter field (%s) of record schema (%s) with " +
                "filter attribute",
                getName(), recordSchema.getName())
                , e);
    }

    private class SelectField
    {
        private final String fieldName;
        private final String columnName;

        public SelectField(String fieldName, String columnName)
        {
            this.fieldName = fieldName;
            this.columnName = columnName;
        }

        public String getColumnName()
        {
            return columnName;
        }

        public String getFieldName()
        {
            return fieldName;
        }
    }

    private static class FilterField
    {
        private final FilterableRecordFieldExtension filterInfo;
        private final DatabaseRecordFieldExtension dbInfo;
        private final RecordSchemaField fieldInfo;

        public FilterField(
                FilterableRecordFieldExtension filterInfo
                , DatabaseRecordFieldExtension dbInfo, RecordSchemaField fieldInfo)
        {
            this.filterInfo = filterInfo;
            this.dbInfo = dbInfo;
            this.fieldInfo = fieldInfo;
        }
        
        public static FilterField create(
                RecordSchemaField field, String dbExtension, String filterExtension)
        {
            DatabaseRecordFieldExtension dbExt =
                    field.getFieldExtension(DatabaseRecordFieldExtension.class, dbExtension);
            FilterableRecordFieldExtension filterExt =
                    field.getFieldExtension(FilterableRecordFieldExtension.class, filterExtension);

            return filterExt==null? null : new FilterField(filterExt, dbExt, field);
        }

        public boolean isVirtual()
        {
            return dbInfo==null;
        }

        public String getColumnName()
        {
            return dbInfo==null? fieldInfo.getName() : dbInfo.getColumnName();
        }

        public DatabaseRecordFieldExtension getDbInfo()
        {
            return dbInfo;
        }

        public RecordSchemaField getFieldInfo()
        {
            return fieldInfo;
        }

        public FilterableRecordFieldExtension getFilterInfo()
        {
            return filterInfo;
        }
    }

    private class SchemaListener implements NodeListener
    {
        public boolean isSubtreeListener()
        {
            return true;
        }

        public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
        {
            RecordSchemaField field = null;
            if (node instanceof RecordSchemaField)
                field = (RecordSchemaField) node;
            else if (node instanceof DatabaseRecordFieldExtension)
                field = (RecordSchemaField) node.getEffectiveParent();
            else if (node instanceof FilterableRecordFieldExtension)
                field = (RecordSchemaField) node.getEffectiveParent();

            if (field==null)
                return;

            filterFieldLock.lock();
            try
            {
                try
                {
                    syncFilterFields(recordSchema, false);
                } catch (Exception ex)
                {
                    error("Error syncing filter attributes");
                }
            }
            finally
            {
                filterFieldLock.unlock();
            }
        }

        public void nodeNameChanged(Node node, String oldName, String newName)
        {
            if (node instanceof RecordSchemaFieldNode)
            {
                filterFieldLock.lock();
                try
                {
                    try
                    {
                        syncFilterFields(recordSchema, false);
                    } catch (Exception ex)
                    {
                        error("Error syncing filter attributes");
                    }
                }
                finally
                {
                    filterFieldLock.unlock();
                }
            }
        }

        public void nodeShutdowned(Node node) { }

        public void childrenAdded(Node owner, Node children) { }

        public void dependendNodeAdded(Node node, Node dependentNode) { }

        public void nodeRemoved(Node removedNode) { }

        public void nodeAttributeNameChanged(
                NodeAttribute attribute, String oldName, String newName)
        {
        }

        public void nodeAttributeValueChanged(
                Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue)
        {
            if (   Status.STARTED.equals(node.getStatus())
                && node instanceof FilterableRecordFieldExtension
                && FilterableRecordFieldExtension.FILTER_VALUE_REQUIRED_ATTR.equals(
                        attribute.getName()))
            {
                try
                {
                    syncFilterFields(recordSchema, false);
                } catch (Exception ex)
                {
                    error("Error syncing filter attributes");
                }
            }
        }

        public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute)
        {
            return false;
        }

        public void nodeMoved(Node node) {
        }
    }
}
