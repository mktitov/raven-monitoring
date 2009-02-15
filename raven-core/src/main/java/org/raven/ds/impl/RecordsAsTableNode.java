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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsTableNode extends AbstractDataConsumer
{
    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return super.getRefreshAttributes();
    }

    @Override
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        return super.getViewableObjects(refreshAttributes);
    }

    public class RecordAsTableDataConsumer implements DataConsumer
    {
        private final TableImpl table;
        private final String[] fieldNames;
        private final Map<String, RecordSchemaField> fields;

        public RecordAsTableDataConsumer(RecordSchema schema, String[] fieldsOrder)
        {
            fields = new HashMap<String, RecordSchemaField>();
            RecordSchemaField[] schemaFields = schema.getFields();
            if (fieldsOrder!=null)
            {
                fieldNames = fieldsOrder;
                String[] sortedNames = fieldsOrder.clone();
                Arrays.sort(sortedNames);
                int i=0;
                for (RecordSchemaField field: schemaFields)
                    if (Arrays.binarySearch(sortedNames, field.getName())>=0)
                        fields.put(field.getName(), field);
            }
            else
            {
                fieldNames = new String[schemaFields.length];
                for (int i=0; i<schemaFields.length; ++i)
                {
                    fieldNames[i] = schemaFields[i].getName();
                    fields.put(schemaFields[i].getName(), schemaFields[i]);
                }
            }

            table = new TableImpl(fieldNames);
        }


        public void setData(DataSource dataSource, Object data)
        {
            if (data==null)
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "All records recieved from data source (%s)", dataSource.getPath()));
                return;
            }

            if (!(data instanceof Record))
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format(
                            "Invalid data type recieved from (%s) data source. " +
                            "The valid type is (%s)"
                            , dataSource.getPath(), Record.class.getName()));
                return;
            }

            Record record = (Record) data;

            Object[] row = new Object[fieldNames.length];
            for (int i=0; i<fieldNames.length; ++i)
                row[i] = converter.convert(
                        String.class, record.getValue(fieldNames[i])
                        , fields.get(fieldNames[i]).getPattern());
        }

        public Object refereshData(Collection<NodeAttribute> sessionAttributes)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPath()
        {
            return RecordsAsTableNode.this.getPath();
        }

    }

}
