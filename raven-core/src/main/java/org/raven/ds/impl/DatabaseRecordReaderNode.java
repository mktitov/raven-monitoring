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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataConsumer;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DatabaseRecordReaderNode extends AbstractDataSource
{
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";
    public final static String
            PROVIDE_FILTER_ATTRIBUTES_TO_CONSUMERS_ATTR = "provideFilterAttributesToConsumers";

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    @NotNull()
    private ConnectionPool connectionPool;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean provideFilterAttributesToConsumers;

    private List<SelectField> selectFields;

    private Map<RecordSchemaField, FilterField> filterFields;

    private Lock filterFieldLock;

    private SchemaListener schemaListener;

    @Override
    protected void initFields()
    {
        super.initFields();

        filterFieldLock = new ReentrantLock();
        schemaListener = new SchemaListener();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        syncFilterFields(recordSchema);
    }

    @Override
    public boolean gatherDataForConsumer(
        DataConsumer dataConsumer, Map<String, NodeAttribute> attributes)
            throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
                        ((RecordSchemaNode)oldValue).removeListener(this);
                    syncFilterFields((RecordSchema)newValue);
                    if (newValue!=null)
                        ((RecordSchemaNode)newValue).addListener(schemaListener);
                }
                else if (   attribute.getName().equals(PROVIDE_FILTER_ATTRIBUTES_TO_CONSUMERS_ATTR)
                         && Status.STARTED==getStatus())
                {
                    stop();
                    start();
                }
            }
        }
        catch(Throwable e)
        {
            error(e.getMessage(), e);
        }
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

    private void syncFilterFields(RecordSchema recordSchema)
    {
        filterFieldLock.lock();
        try
        {
            RecordSchemaField[] fields = null;
            if (recordSchema!=null)
                fields = recordSchema.getFields();
            filterFields = new HashMap<RecordSchemaField, FilterField>();
            Set<String> usedAttributes = new HashSet<String>();
            if (fields!=null)
                for (RecordSchemaField field: fields)
                {
                    FilterField filterField = FilterField.create(field);
                    if (filterField!=null)
                    {
                        filterFields.put(field, filterField);
                        try
                        {
                            syncFilterFieldWithAttribute(filterField, null);
                            usedAttributes.add(field.getName());
                        }
                        catch (Throwable ex)
                        {
                            logErrorSyncFilterAttr(field.getName(), ex);
                        }
                    }
                }

            removeUnusedFilterAttributes(usedAttributes);

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

    private void syncFilterFieldWithAttribute(FilterField filterField, String oldFilterFieldName)
            throws Exception
    {
        NodeAttribute filterAttr = getNodeAttribute(filterField.fieldInfo.getName());
        if (filterAttr==null && oldFilterFieldName!=null)
            filterAttr = getNodeAttribute(oldFilterFieldName);

        NodeAttribute schemaAttr = getNodeAttribute(RECORD_SCHEMA_ATTR);
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
            if (!filterAttr.getType().equals(filterField.getFieldInfo().getFieldType().getType()))
            {
                filterAttr.setType(filterField.getFieldInfo().getFieldType().getType());
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
            if (hasChanges)
                filterAttr.save();
        }
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
        
        public static FilterField create(RecordSchemaField field)
        {
            DatabaseRecordFieldExtension dbExt =
                    field.getFieldExtension(DatabaseRecordFieldExtension.class);
            FilterableRecordFieldExtension filterExt =
                    field.getFieldExtension(FilterableRecordFieldExtension.class);

            return dbExt==null || filterExt==null? null : new FilterField(filterExt, dbExt, field);
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
                if (!newStatus.equals(Status.STARTED))
                    removeNodeAttribute(field.getName());
                else
                    try
                    {
                        syncFilterFieldWithAttribute(filterFields.get(field), null);
                    }
                    catch (Exception ex)
                    {
                        logErrorSyncFilterAttr(field.getName(), ex);
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
                    RecordSchemaFieldNode field = (RecordSchemaFieldNode) node;
                    if (Status.STARTED.equals(field.getStatus()))
                        try
                        {
                            syncFilterFieldWithAttribute(filterFields.get(field), oldName);
                        } catch (Exception ex)
                        {
                            logErrorSyncFilterAttr(field.getName(), ex);
                        }
                    else
                        removeFilterAttribute(field.getName());
                }
                finally
                {
                    filterFieldLock.unlock();
                }
            }
        }

        public void nodeShutdowned(Node node)
        {
        }

        public void childrenAdded(Node owner, Node children)
        {
        }

        public void dependendNodeAdded(Node node, Node dependentNode)
        {
        }

        public void nodeRemoved(Node removedNode)
        {
        }

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
                FilterField filterField = filterFields.get(node.getEffectiveParent());
                try
                {
                    syncFilterFieldWithAttribute(filterField, null);
                    NodeAttribute attr = getNodeAttribute(filterField.getFieldInfo().getName());
                    if (attr.isRequired() && attr.getValue()==null && Status.STARTED==getStatus())
                    {
                        warn(String.format(
                                "Stoping node because of value for required attribute (%s) " +
                                "not setted", attr.getName()));
                        stop();
                    }
                        
                }
                catch (Exception ex)
                {
                    logErrorSyncFilterAttr(filterField.getFieldInfo().getName(), ex);
                }
            }
        }

        public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute)
        {
            return false;
        }
    }
}
