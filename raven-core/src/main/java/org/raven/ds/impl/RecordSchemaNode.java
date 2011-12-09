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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionViewableObject;
import org.raven.tree.impl.BaseNode;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class RecordSchemaNode extends BaseNode implements RecordSchema, Viewable
{
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode extendsSchema;

    @Parameter
    private String includeFields;
    @Parameter
    private String excludeFields;
    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    private ConnectionPool connectionPool;

    @Message
    private static String createRecordMessage;
    @Message
    private static String defaultDateFormatMessage;
    @Message
    private static String defaultTimestampFormatMessage;


    private RecordExtensionsNode recordExtensionsNode;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        recordExtensionsNode = (RecordExtensionsNode) getChildren(RecordExtensionsNode.NAME);
        if (recordExtensionsNode==null)
        {
            recordExtensionsNode = new RecordExtensionsNode();
            this.addAndSaveChildren(recordExtensionsNode);
            recordExtensionsNode.start();
        }
    }

    public RecordSchemaField[] getFields()
    {
        List<RecordSchemaField> fields = new ArrayList<RecordSchemaField>(32);
        if (extendsSchema!=null)
        {
            RecordSchemaField[] parentFields = extendsSchema.getFields();
            if (parentFields!=null)
            {
                if (includeFields!=null)
                {
                    String[] includeFieldsArr = splitAndSort(includeFields);
                    if (includeFieldsArr!=null && includeFieldsArr.length>0)
                    {
                        for (RecordSchemaField field: parentFields)
                            if (Arrays.binarySearch(includeFieldsArr, field.getName())>=0)
                                fields.add(field);
                    }
                }
                else if (excludeFields!=null)
                {
                    String[] excludeFieldsArr =splitAndSort(excludeFields);
                    if (excludeFieldsArr!=null && excludeFieldsArr.length>0)
                    {
                        for (RecordSchemaField field: parentFields)
                            if (Arrays.binarySearch(excludeFieldsArr, field.getName())<0)
                                fields.add(field);
                    }
                }
                else
                    for (RecordSchemaField field: parentFields)
                        fields.add(field);
            }
        }
        Collection<Node> childs = getSortedChildrens();
        if (childs!=null || childs.size()>0)
            for (Node child: childs)
                if (child instanceof RecordSchemaField && child.getStatus()==Status.STARTED)
                    fields.add((RecordSchemaField)child);

        if (fields.isEmpty())
            return null;

        Set<String> fieldNames = new HashSet<String>();
        ListIterator<RecordSchemaField> it = fields.listIterator(fields.size());
        for (; it.hasPrevious();)
        {
            RecordSchemaField field = it.previous();
            if (fieldNames.contains(field.getName()))
                it.remove();
            else
                fieldNames.add(field.getName());
        }

        RecordSchemaField[] result = new RecordSchemaField[fields.size()];
        fields.toArray(result);

        return result;
    }

    public Record createRecord() throws RecordException
    {
        return new RecordImpl(this);
    }

    public <E> E getRecordExtension(Class<E> extensionType, String extensionName)
    {
        Collection<Node> childs = recordExtensionsNode.getChildrens();
        if (childs!=null && childs.size()>0)
            for (Node child: childs)
                if (   Status.STARTED==child.getStatus()
                    && extensionType.isAssignableFrom(child.getClass())
                    && (extensionName==null || extensionName.equals(child.getName())))
                {
                    return (E)child;
                }

        RecordSchemaNode _extendsSchema = extendsSchema;
        if (_extendsSchema!=null)
            return _extendsSchema.getRecordExtension(extensionType, extensionName);

        return null;
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        DatabaseRecordExtension dbExt = getRecordExtension(DatabaseRecordExtension.class, null);
        if (dbExt==null)
            return null;
        ConnectionPool _connectionPool = connectionPool;
        if (_connectionPool==null)
            return null;
        CreateRecordFromTableAction field = new CreateRecordFromTableAction(
                createRecordMessage, this, connectionPool, dbExt.getSchemaName(), dbExt.getTableName()
                , defaultDateFormatMessage, defaultTimestampFormatMessage);
        return Arrays.asList((ViewableObject)field);
    }

    public RecordExtensionsNode getRecordExtensionsNode()
    {
        return recordExtensionsNode;
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public String getExcludeFields()
    {
        return excludeFields;
    }

    public void setExcludeFields(String excludeFields)
    {
        this.excludeFields = excludeFields;
    }

    public RecordSchemaNode getExtendsSchema()
    {
        return extendsSchema;
    }

    public void setExtendsSchema(RecordSchemaNode extendsSchema)
    {
        this.extendsSchema = extendsSchema;
    }

    public String getIncludeFields()
    {
        return includeFields;
    }

    public void setIncludeFields(String includeFields)
    {
        this.includeFields = includeFields;
    }

    /**
     * Creates and returns {@link RecordSchemaFieldNode the record schema field node}. If record
     * schema already contains the field node then this field will be returned.
     * @param name the name of the field node
     * @param fieldType {@link RecordSchemaField#getFieldType()  the field type}
     * @param format {@link RecordSchemaField#getPattern() the pattern} of record field
     */
    protected RecordSchemaFieldNode createField(
            String name, RecordSchemaFieldType fieldType, String format)
    {
        RecordSchemaFieldNode fieldNode = (RecordSchemaFieldNode) getChildren(name);
        if (fieldNode!=null)
            return fieldNode;
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(name);
        addAndSaveChildren(field);
        field.setDisplayName(StringUtils.capitalize(name));
        field.setFieldType(fieldType);
        field.setPattern(format);
        field.start();
        return field;
    }

    private String[] splitAndSort(String str)
    {
        StrTokenizer tokenizer = new StrTokenizer(str, ',');
        tokenizer.setTrimmerMatcher(StrMatcher.trimMatcher());
        String[] result = tokenizer.getTokenArray();
        Arrays.sort(result);
        return result;
    }

    public class CreateRecordFromTableAction extends AbstractActionViewableObject
    {
        private final ConnectionPool connectionPool;
        private final String schemaName;
        private final String tableName;
        private final String dateFormat;
        private final String timestampFormat;

        public CreateRecordFromTableAction(
                String displayMessage, Node owner,
                ConnectionPool connectionPool, String schemaName, String tableName,
                String dateFormat, String timestampFormat)
        {
            super(null, displayMessage, owner, false);
            this.connectionPool = connectionPool;
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.dateFormat = dateFormat;
            this.timestampFormat = timestampFormat;
        }

        @Override
        public String executeAction() throws Exception {
            try{
                Connection con = connectionPool.getConnection();
                try {
                    DatabaseMetaData meta = con.getMetaData();
                    ResultSet rs = meta.getColumns(null, schemaName, tableName, null);
                    try {
                        Map<String, RecordSchemaField> fields =
                                RavenUtils.getRecordSchemaFields(RecordSchemaNode.this);
                        while (rs.next()) {
                            String columnName = rs.getString("COLUMN_NAME");
                            String fieldName = RavenUtils.dbNameToName(columnName);
                            if (fields.containsKey(fieldName))
                                continue;
                            RecordSchemaFieldType type = RecordSchemaFieldType.getTypeBySqlType(
                                    rs.getInt("DATA_TYPE"));
                            String pattern = null;
                            if (type!=null)
                                switch (type) {
                                    case DATE: pattern = dateFormat; break;
                                    case TIMESTAMP: pattern = timestampFormat; break;
                                }
                            RecordSchemaFieldNode field = RecordSchemaFieldNode.create(
                                    RecordSchemaNode.this, fieldName, null, type, pattern);
                            if (field!=null)
                                DatabaseRecordFieldExtension.create(field, "dbColumn", columnName);
                        }
                        return "Record successfully created";
                    } finally {
                        rs.close();
                    }
                } finally {
                    con.close();
                }
            }catch (Exception e) {
                String mess = String.format(
                        "Error creating record from the database table (%s)", tableName);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(mess, e);
                return mess+". "+e.getMessage();
            }
        }

    }
}
