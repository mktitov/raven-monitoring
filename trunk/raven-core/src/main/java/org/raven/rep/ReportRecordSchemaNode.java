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

package org.raven.rep;

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.FilterableRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;
import org.raven.tree.Node;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class ReportRecordSchemaNode extends RecordSchemaNode
{
    public final static String ID_FIELD_NAME = "id";
    public final static String TYPE_FIELD_NAME = "type";
    public final static String NAME_FIELD_NAME = "name";
    public final static String GENERATIONDATE_FIELD_NAME = "generationDate";
    public final static String REPORTDATA_FIELD_NAME = "reportData";

    @Message private String generationDatePattern;

    @Message private String idFieldDisplayName;
    @Message private String typeFieldDisplayName;
    @Message private String nameFieldDisplayName;
    @Message private String generationDateFieldDisplayName;
    @Message private String reportDataFieldDisplayName;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        generateFields();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        generateFields();
    }

    private void generateFields()
    {
        Node node = getRecordExtensionsNode().getChildren("db");
        if (node==null)
        {
            DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
            dbExtension.setName("db");
            getRecordExtensionsNode().addAndSaveChildren(dbExtension);
            dbExtension.setTableName("RAVEN_REPORTS");
            dbExtension.start();
        }
        createField(ID_FIELD_NAME, idFieldDisplayName, "ID", RecordSchemaFieldType.LONG);
        createField(TYPE_FIELD_NAME, typeFieldDisplayName, "TYPE", RecordSchemaFieldType.STRING);
        createField(NAME_FIELD_NAME, nameFieldDisplayName, "NAME", RecordSchemaFieldType.STRING);
        createField(GENERATIONDATE_FIELD_NAME, generationDateFieldDisplayName, "GENERATION_DATE"
                , RecordSchemaFieldType.TIMESTAMP);
        createField(REPORTDATA_FIELD_NAME, reportDataFieldDisplayName, "REPORT_DATA"
                , RecordSchemaFieldType.BINARY);
    }

    private void createField(
            String fieldName, String displayName, String columnName
            , RecordSchemaFieldType fieldType)
    {
        if (getChildren(fieldName)!=null)
            return;
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(fieldName);
        addAndSaveChildren(field);
        field.setFieldType(fieldType);
        field.setDisplayName(displayName);
        if (fieldType.equals(RecordSchemaFieldType.TIMESTAMP))
            field.setPattern(generationDatePattern);

        field.start();

        if (ID_FIELD_NAME.equals(fieldName))
        {
            IdRecordFieldExtension idExtension = new IdRecordFieldExtension();
            idExtension.setName(columnName);
            field.addAndSaveChildren(idExtension);
            idExtension.start();
        }

        DatabaseRecordFieldExtension dbExtension = new DatabaseRecordFieldExtension();
        dbExtension.setName("db");
        field.addAndSaveChildren(dbExtension);
        dbExtension.setColumnName(columnName);
        dbExtension.start();

        if (!fieldType.equals(RecordSchemaFieldType.BINARY))
        {
            FilterableRecordFieldExtension filterExtension = new FilterableRecordFieldExtension();
            filterExtension.setName("filter");
            field.addAndSaveChildren(filterExtension);
            filterExtension.setCaseSensitive(false);
            filterExtension.start();
        }
    }
}
