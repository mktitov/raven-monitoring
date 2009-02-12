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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;

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
    private Collection values;
    private Map<String/*column name*/, String/*field name*/> selectFields;
    private Connection connection;
    private PreparedStatement statement;

    public DatabaseRecordQuery(
            RecordSchema recordSchema
            , String databaseExtensionName
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

            return new RecordIterator(resultSet, recordSchema, selectFields);
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
                connection.close();
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

            String tableName = dbExtension.getTableName();

            StringBuilder queryBuf = new StringBuilder();

            selectFields = new HashMap<String, String>();
            List<String> columnNames = new ArrayList<String>();
            for (RecordSchemaField field: fields)
            {
                DatabaseRecordFieldExtension dbFieldExtension = field.getFieldExtension(
                            DatabaseRecordFieldExtension.class, databaseExtensionName);
                if (dbFieldExtension!=null)
                {
                    selectFields.put(
                            dbFieldExtension.getColumnName().toUpperCase(), field.getName());
                    columnNames.add(dbFieldExtension.getColumnName());
                }
            }

            if (selectFields.size()==0)
                throw new DatabaseRecordQueryException(
                        String.format(
                            "Record schema does not containts fields with extension (%s)"
                            , DatabaseRecordExtension.class.getSimpleName()));
            if (queryTemplate==null)
            {
                //constructing SELECT clause
                queryBuf = new StringBuilder("\nSELECT ");
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
            if (filterElements!=null && filterElements.size()>0)
            {
                values = new ArrayList();
                for (DatabaseFilterElement filterElement: filterElements)
                {
                    switch(filterElement.getExpressionType())
                    {
                        case COMPLETE:
                            queryBuf.append("\n   AND ("+filterElement.getColumnName()+" "
                                    +filterElement.getValue()+")");
                            break;
                        case OPERATOR:
                            switch (filterElement.getOperatorType())
                            {
                                case SIMPLE:
                                    queryBuf.append("\n   AND "
                                            +filterElement.getColumnName()
                                            +filterElement.getOperator()+"?");
                                    values.add(filterElement.getValue());
                                    break;
                                case LIKE:
                                    queryBuf.append("\n   AND "+filterElement.getColumnName()
                                            +" LIKE '"+filterElement.getValue()+"'");
                                    break;
                                case BETWEEN:
                                    queryBuf.append("\n   AND "+filterElement.getColumnName()
                                            +" BETWEEN ? AND ?");
                                    Object[] betweenValues = (Object[]) filterElement.getValue();
                                    for (Object value: betweenValues)
                                        values.add(value);
                                    break;
                                case IN:
                                    Object[] inValues = (Object[]) filterElement.getValue();
                                    queryBuf.append("\n   AND "+filterElement.getColumnName()
                                            +" IN (");
                                    for (int i=0; i<inValues.length; ++i)
                                    {
                                        if (i>0)
                                            queryBuf.append(", ");
                                        queryBuf.append("?");
                                        values.add(inValues[i]);
                                    }
                                    queryBuf.append(")");
                                    break;
                            }
                            break;
                    }
                }
            }

            //constructing ORDER clause
            if (queryTemplate==null && orderByExpression!=null)
                queryBuf.append("\nORDER BY "+orderByExpression);

            if (queryTemplate!=null)
                return StringUtils.replace(queryTemplate, "{#}", queryBuf.toString());
            else
                return queryBuf.toString();
        }
        catch (Exception e)
        {
            throw new DatabaseRecordQueryException(
                    String.format(
                        "Error creating database query for record schema (%s)"
                        , recordSchema.getName())
                    , e);
        }
    }

    public class RecordIterator implements Iterator<Record>
    {
        private final ResultSet resultSet;
        private final RecordSchema schema;
        private final Map<String, String> fields;
        private Record next;

        public RecordIterator(ResultSet resultSet, RecordSchema schema, Map<String, String> fields)
        {
            this.resultSet = resultSet;
            this.schema = schema;
            this.fields = fields;

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
                    for (int i=1; i<=metaData.getColumnCount(); ++i)
                    {
                        Object value = resultSet.getObject(i);
                        String columnName = metaData.getColumnLabel(i).toUpperCase();
                        String fieldName = fields.get(columnName);
                        if (fieldName==null)
                            throw new RecordIteratorError(String.format(
                                    "Not found field for column name (%s)", columnName));
                        next.setValue(fieldName, value);
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
                resultSet.close();
            }
            catch (SQLException ex)
            {
                throw new RecordIteratorError(ex);
            }
        }
    }
}
