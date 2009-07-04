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

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.FilterableRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;

/**
 *
 * @author Mikhail Titov
 */
public class ReportRecordSchemaNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        ReportRecordSchemaNode reportSchema = new ReportRecordSchemaNode();
        reportSchema.setName("report schema");
        tree.getRootNode().addAndSaveChildren(reportSchema);
        assertTrue(reportSchema.start());

        DatabaseRecordExtension dbExtension =
                reportSchema.getRecordExtension(DatabaseRecordExtension.class, null);
        assertNotNull(dbExtension);
        assertEquals("RAVEN_REPORTS", dbExtension.getTableName());

        checkField(reportSchema, ReportRecordSchemaNode.ID_FIELD_NAME, "ID"
                , RecordSchemaFieldType.LONG);
        checkField(reportSchema, ReportRecordSchemaNode.TYPE_FIELD_NAME, "TYPE"
                , RecordSchemaFieldType.STRING);
        checkField(reportSchema, ReportRecordSchemaNode.NAME_FIELD_NAME, "NAME"
                , RecordSchemaFieldType.STRING);
        checkField(reportSchema, ReportRecordSchemaNode.GENERATIONDATE_FIELD_NAME, "GENERATION_DATE"
                , RecordSchemaFieldType.TIMESTAMP);
        checkField(reportSchema, ReportRecordSchemaNode.REPORTDATA_FIELD_NAME, "REPORT_DATA"
                , RecordSchemaFieldType.BINARY);
    }

    private void checkField(
            ReportRecordSchemaNode schemaNode, String fieldName, String columnName
            , RecordSchemaFieldType type)
    {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) schemaNode.getChildren(fieldName);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());
        assertNotNull(field.getDisplayName());
        if (type.equals(RecordSchemaFieldType.TIMESTAMP))
            assertNotNull(field.getPattern());
        if (ReportRecordSchemaNode.ID_FIELD_NAME.equals(fieldName))
        {
            IdRecordFieldExtension idExtension = field.getFieldExtension(
                    IdRecordFieldExtension.class, null);
            assertNotNull(idExtension);
        }
        DatabaseRecordFieldExtension dbExtension = 
                field.getFieldExtension(DatabaseRecordFieldExtension.class, null);
        assertNotNull(dbExtension);
        assertEquals(columnName, dbExtension.getColumnName());
        FilterableRecordFieldExtension filterExtension =
                field.getFieldExtension(FilterableRecordFieldExtension.class, null);
        if (!type.equals(RecordSchemaFieldType.BINARY))
        {
            assertNotNull(filterExtension);
            assertFalse(filterExtension.getCaseSensitive());
        }
        else
            assertNull(filterExtension);
    }
}