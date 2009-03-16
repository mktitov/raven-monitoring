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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseRecordQuery
{
    private final RecordSchema recordSchema;
    private final String whereExpression;
    private final String orderByExpression;
    private final String queryTemplate;
    private final ConnectionPool connectionPool;
    private final String databaseExtensionName;
    private final String query;
    private final int maxRows;
    private final int fetchSize;
    private final String filterExtensionName;
    private Collection values;
    private Map<String/*column name*/, FieldInfo> selectFields;
    private Connection connection;
    private PreparedStatement statement;
    private String tableName;
    private String idColumnName;

    public DatabaseRecordQuery(
            RecordSchema recordSchema
            , String databaseExtensionName
            , String filterExtensionName
            , Collection<DatabaseFilterElement> filterElements
            , String whereExpression
            , String orderByExpression
            , ConnectionPool connectionPool
            , Integer maxRows
            , Integer fetchSize)
        throws DatabaseRecordQueryException
    {
        this.recordSchema = recordSchema;
        this.whereExpression = whereExpression;
        this.connectionPool = connectionPool;
        this.orderByExpression = orderByExpression;
        this.databaseExtensionName = databaseExtensionName;
        this.filterExtensionName = filterExtensionName;
        this.maxRows = maxRows==null? 0 : maxRows;
        this.fetchSize = fetchSize==null? 0 : fetchSize;

        this.queryTemplate = null;

        this.query = createQuery(filterElements);
    }

    String getQuery()
    {
        return query;
    }

    public DatabaseRecordQuery(
            RecordSchema recordSchema
            , String databaseExtensionName
            , String filterExtensionName
            , Collection<DatabaseFilterElement> filterElements
            , String queryTemplate
            , ConnectionPool connectionPool
            , Integer maxRows
            , Integer fetchSize)
        throws DatabaseRecordQueryException
    {
        this.recordSchema = recordSchema;
        this.queryTemplate = queryTemplate;
        this.connectionPool = connectionPool;
        this.databaseExtensionName = databaseExtensionName;
        this.filterExtensionName = filterExtensionName;
        this.maxRows = maxRows==null? 0 : maxRows;
        this.fetchSize = fetchSize==null? 0 : fetchSize;

        whereExpression = null;
        orderByExpression = null;

        this.query = createQuery(filterElements);
    }

    public RecordIterator execute() throws DatabaseRecordQueryException
    {
        connection = connectionPool.getConnection();
        try
        {
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(query);
            statement.setMaxRows(maxRows);
            statement.setFetchSize(fetchSize);
            if (values!=null)
            {
                int i=1;
                for (Object value: values)
                    statement.setObject(i++, value);
            }
            ResultSet resultSet = statement.executeQuery();

            return new RecordIterator(resultSet, recordSchema, selectFields, idColumnName);
        }
        catch(Throwable e)
        {
            close();
            throw new DatabaseRecordQueryException(e);
        }
    }

    public void close() throws DatabaseRecordQueryException
    {
        try
        {
            if (statement != null)
                statement.close();
            if (connection != null)
            {
                try{
                    connection.commit();
                }
                finally{
                    connection.close();
                }
            }
            statement = null;
            connection = null;
        }
        catch (SQLException e)
        {
            throw new DatabaseRecordQueryException(e);
        }
    }

    private String createQuery(Collection<DatabaseFilterElement> filterElements)
            throws DatabaseRecordQueryException
    {
        try
        {
            values = null;
            
            RecordSchemaField[] fields = recordSchema.getFields();
            if (fields==null || fields.length==0)
                throw new DatabaseRecordQueryException("Record schema does not contains fields");

            DatabaseRecordExtension dbExtension = recordSchema.getRecordExtension(
                    DatabaseRecordExtension.class, databaseExtensionName);
            if (dbExtension==null)
                throw new DatabaseRecordQueryException(String.format(
                        "Record schema does not have (%s) extension"
                        , DatabaseRecordExtension.class.getSimpleName()));

            tableName = dbExtension.getTableName();

            StrBuilder queryBuf = new StrBuilder();

            selectFields = new HashMap<String, FieldInfo>();
            Set<String> caseInSensitiveFields = new HashSet<String>();
            List<String> columnNames = new ArrayList<String>();
            boolean hasBinaryFields = false;
            for (RecordSchemaField field: fields)
            {
                DatabaseRecordFieldExtension dbFieldExtension = field.getFieldExtension(
                            DatabaseRecordFieldExtension.class, databaseExtensionName);
                if (dbFieldExtension!=null)
                {
                    FieldInfo fieldInfo = new FieldInfo(field, dbFieldExtension);
                    selectFields.put(
                            dbFieldExtension.getColumnName().toUpperCase(), fieldInfo);
                    IdRecordFieldExtension idExtension =
                            field.getFieldExtension(IdRecordFieldExtension.class, null);
                    if (idExtension!=null)
                        idColumnName = dbFieldExtension.getColumnName();
                    if (field.getFieldType().equals(RecordSchemaFieldType.BINARY))
                        hasBinaryFields = true;
                    else
                        columnNames.add(dbFieldExtension.getColumnName());

                }
                FilterableRecordFieldExtension filterFieldExtension = field.getFieldExtension(
                        FilterableRecordFieldExtension.class, filterExtensionName);
                if (filterFieldExtension!=null && !filterFieldExtension.getCaseSensitive())
                {
                    if (dbFieldExtension!=null)
                        caseInSensitiveFields.add(dbFieldExtension.getColumnName());
                    else
                        caseInSensitiveFields.add(field.getName());
                }
            }

            if (selectFields.size()==0)
                throw new DatabaseRecordQueryException(
                        String.format(
                            "Record schema does not containts fields with extension (%s)"
                            , DatabaseRecordExtension.class.getSimpleName()));
            if (hasBinaryFields && idColumnName==null)
                throw new DatabaseRecordQueryException(
                        String.format(
                            "Record schema (%s) has binary fields but does not contains " +
                            "field with %s and %s extensions"
                            , recordSchema.getName()
                            , IdRecordFieldExtension.class.getSimpleName()
                            , DatabaseRecordFieldExtension.class.getSimpleName()));

            if (queryTemplate==null)
            {
                //constructing SELECT clause
                queryBuf = new StrBuilder("\nSELECT ");
                boolean firstRow = true;
                for (String columnName: columnNames)
                {
                    if (!firstRow)
                        queryBuf.append(", ");
                    else
                        firstRow = false;
                    queryBuf.append(columnName);
                }

                //constructing FROM clause
                queryBuf.append("\nFROM "+tableName);
            }

            //constructing WHERE clause
            if (queryTemplate==null)
                queryBuf.append("\nWHERE 1=1");
            if (whereExpression!=null)
                queryBuf.append("\n   AND ("+whereExpression+")");

            //injecting ${var} parameter types

            Map<String, FilterInfo> filterInfos = new HashMap<String, FilterInfo>();
            if (filterElements!=null && filterElements.size()>0)
            {
                values = new ArrayList();
                for (DatabaseFilterElement filterElement: filterElements)
                {
                    boolean caseSensitive = !caseInSensitiveFields.contains(
                            filterElement.getColumnName());
                    filterInfos.put(
                            filterElement.getColumnName().toUpperCase()
                            , new FilterInfo(filterElement, caseSensitive));
                    if (!filterElement.isVirtual())
                    {
                        queryBuf.append(
                                "\n   AND (${"+filterElement.getColumnName().toUpperCase()+"})");
                    }
//                    queryBuf.append(
//                            "\n   AND ("+createWhereEntry(filterElement, caseSensitive)+")");
                }
            }

            //constructing ORDER clause
            if (queryTemplate==null && orderByExpression!=null)
                queryBuf.append("\nORDER BY "+orderByExpression);

            String resultQuery = queryTemplate==null?
                queryBuf.toString()
                : StringUtils.replace(queryTemplate, "{#}", queryBuf.toString());

            StrSubstitutor subst = new StrSubstitutor(new ParameterLookup(filterInfos));
            
            return subst.replace(resultQuery);
        }
        catch (Throwable e)
        {
            throw new DatabaseRecordQueryException(
                    String.format(
                        "Error creating database query for record schema (%s)"
                        , recordSchema.getName())
                    , e);
        }
    }

    private String createWhereEntry(DatabaseFilterElement filterElement, boolean caseSensitive)
    {
        String columnNameExpr = getColumnNameExpression(filterElement, caseSensitive);
        StringBuilder queryBuf = new StringBuilder(columnNameExpr);
        switch(filterElement.getExpressionType())
        {
            case COMPLETE:
                queryBuf.append((columnNameExpr.isEmpty()? "":" ")+filterElement.getValue());
                break;
            case OPERATOR:
                switch (filterElement.getOperatorType())
                {
                    case SIMPLE:
                        queryBuf.append(filterElement.getOperator()+"?");
                        values.add(getValue(filterElement.getValue(), caseSensitive));
                        break;
                    case LIKE:
                        queryBuf.append(
                                " LIKE '"
                                +getValue(filterElement.getValue(), caseSensitive)
                                +"'");
                        break;
                    case BETWEEN:
                        queryBuf.append(" BETWEEN ? AND ?");
                        Object[] betweenValues = (Object[]) filterElement.getValue();
                        for (Object value: betweenValues)
                            values.add(value);
                        break;
                    case IN:
                        Object[] inValues = (Object[]) filterElement.getValue();
                        queryBuf.append(" IN (");
                        for (int i=0; i<inValues.length; ++i)
                        {
                            if (i>0)
                                queryBuf.append(", ");
                            queryBuf.append("?");
                            values.add(getValue(inValues[i], caseSensitive));
                        }
                        queryBuf.append(")");
                        break;
                }
                break;
        }
        return queryBuf.toString();
    }

    private String getColumnNameExpression(
            DatabaseFilterElement filterElement, boolean caseSensitive)
    {
        if (filterElement.isVirtual())
            return "";
        else
            return !caseSensitive && String.class.equals(filterElement.getColumnType())?
                    "upper("+filterElement.getColumnName()+")" : filterElement.getColumnName();
    }

    private Object getValue(Object value, boolean caseSensitive)
    {
        return (value instanceof String) && !caseSensitive? ((String)value).toUpperCase() : value;
    }

    public class RecordIterator implements Iterator<Record>
    {
        private final ResultSet resultSet;
        private final RecordSchema schema;
        private final Map<String, FieldInfo> fields;
        private final String idColumnName;
        private Record next;

        public RecordIterator(
                ResultSet resultSet, RecordSchema schema, Map<String, FieldInfo> fields
                , String idColumnName)
        {
            this.resultSet = resultSet;
            this.schema = schema;
            this.fields = fields;
            this.idColumnName = idColumnName==null? null : idColumnName.toUpperCase();

            createNextRecord();
        }

        public boolean hasNext()
        {
            return next!=null;
        }

        public Record next()
        {
            Record result = next;
            if (next!=null)
                createNextRecord();
            return result;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("" +
                    "Remove operation not supported in the RecordIterator");
        }

        private void createNextRecord() 
        {
            try
            {
                if (!resultSet.next())
                {
                    next = null;
                    close();
                }
                else
                {
                    next = schema.createRecord();
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    Object idColumnValue = null;
                    for (int i=1; i<=metaData.getColumnCount(); ++i)
                    {
                        String columnName = metaData.getColumnLabel(i).toUpperCase();
                        FieldInfo fieldInfo = fields.get(columnName);
                        if (fieldInfo==null)
                            throw new RecordIteratorError(String.format(
                                    "Not found field for column name (%s)", columnName));
                        if (RecordSchemaFieldType.BINARY!=fieldInfo.getField().getFieldType())
                        {
                            Object value = resultSet.getObject(i);
                            if (idColumnName!=null && idColumnName.equals(columnName))
                                idColumnValue=value;
                            value = fieldInfo.getDbExtension().prepareValue(value);
                            next.setValue(fieldInfo.getFieldName(), value);
                        }
                    }
                    if (idColumnValue!=null)
                        for (FieldInfo fieldInfo: fields.values())
                            if (fieldInfo.getField().getFieldType()==RecordSchemaFieldType.BINARY)
                            {
                                DatabaseBinaryDataReader reader = new DatabaseBinaryDataReader(
                                        tableName, idColumnName
                                        , fieldInfo.getDbExtension().getColumnName(), idColumnValue
                                        , connectionPool);
                                next.setValue(fieldInfo.getFieldName(), reader);
                            }
                }
            }
            catch(Exception e)
            {
                close();
                throw new RecordIteratorError(e);
            }
        }

        public void close() 
        {
            try
            {
//                resultSet.getStatement().getConnection().commit();
                resultSet.close();
            }
            catch (SQLException ex)
            {
                throw new RecordIteratorError(ex);
            }
        }
    }

    private class FieldInfo
    {
        private final RecordSchemaField field;
        private final DatabaseRecordFieldExtension dbExtension;

        public FieldInfo(RecordSchemaField field, DatabaseRecordFieldExtension dbExtension)
        {
            this.field = field;
            this.dbExtension = dbExtension;
        }

        public RecordSchemaField getField()
        {
            return field;
        }

        public DatabaseRecordFieldExtension getDbExtension()
        {
            return dbExtension;
        }

        public String getFieldName()
        {
            return field.getName();
        }
    }

    private class FilterInfo
    {
        private final DatabaseFilterElement filterElement;
        private final boolean caseSensitive;

        public FilterInfo(DatabaseFilterElement filterElement, boolean caseSensitive)
        {
            this.filterElement = filterElement;
            this.caseSensitive = caseSensitive;
        }

        public boolean isCaseSensitive()
        {
            return caseSensitive;
        }

        public DatabaseFilterElement getFilterElement()
        {
            return filterElement;
        }
    }

    private class ParameterLookup extends StrLookup
    {
        private final Map<String, FilterInfo> filterElements;

        public ParameterLookup(Map<String, FilterInfo> filterElements)
        {
            this.filterElements = filterElements;
        }

        @Override
        public String lookup(String key)
        {
            FilterInfo filterInfo = filterElements.get(key.toUpperCase());
            if (filterInfo==null)
                throw new Error(String.format("Filter element (%s) was not defined"));
            
            return createWhereEntry(filterInfo.getFilterElement(), filterInfo.isCaseSensitive());
        }
        
    }
}
