/*
 *  Copyright 2010 Mikhail Titov.
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

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ExcelRecordReaderNode extends AbstractDataPipe
{
    @NotNull @Parameter(defaultValue="1")
    private Integer sheetNumber;

    @NotNull @Parameter(defaultValue="1")
    private Integer startFromRow;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    @Parameter
    private String cvsExtensionName;

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (data==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Recieved null data from node (%s)", dataSource.getPath()));
            return;
        }

        Map<String, FieldInfo> fieldsColumns = getFieldsColumns();
        if (fieldsColumns==null)
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                debug(String.format(
                        "CsvRecordFieldExtension was not defined for fields in the record schema (%s)"
                        , recordSchema.getName()));
            return;
        }

        InputStream dataStream = converter.convert(InputStream.class, data, null);
        Workbook wb = WorkbookFactory.create(new PushbackInputStream(dataStream));
        Sheet sheet = wb.getSheetAt(sheetNumber-1);
        try{
            for (int r=startFromRow-1; r<=sheet.getLastRowNum(); ++r)
            {
                Row row = sheet.getRow(r);
                if (row!=null){
                    Record record = recordSchema.createRecord();
                    for (Map.Entry<String, FieldInfo> fieldCol: fieldsColumns.entrySet()) {
                        Cell cell = row.getCell(fieldCol.getValue().getColumnNumber()-1, Row.RETURN_BLANK_AS_NULL);
                        if (cell!=null){
                            Object value = null;
                            switch(cell.getCellType()){
                                case Cell.CELL_TYPE_BOOLEAN: value = cell.getBooleanCellValue(); break;
                                case Cell.CELL_TYPE_NUMERIC:
                                    double num = cell.getNumericCellValue();
                                    if (HSSFDateUtil.isCellDateFormatted(cell) && HSSFDateUtil.isValidExcelDate(num))
                                        value = cell.getDateCellValue();
                                    else
                                        value = num;
                                    break;
                                case Cell.CELL_TYPE_STRING : value = cell.getStringCellValue(); break;
                            }
                            value = fieldCol.getValue().prepareValue(value);
                            record.setValue(fieldCol.getKey(), value);
                        }
                    }
                    sendDataToConsumers(record);
                }
            }
        }finally{
            sendDataToConsumers(null);
        }
    }

    public Integer getSheetNumber() {
        return sheetNumber;
    }

    public void setSheetNumber(Integer sheetNumber) {
        this.sheetNumber = sheetNumber;
    }

    public Integer getStartFromRow() {
        return startFromRow;
    }

    public void setStartFromRow(Integer startFromRow) {
        this.startFromRow = startFromRow;
    }

    public String getCvsExtensionName() {
        return cvsExtensionName;
    }

    public void setCvsExtensionName(String cvsExtensionName) {
        this.cvsExtensionName = cvsExtensionName;
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    private Map<String, FieldInfo> getFieldsColumns()
    {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields==null)
            return null;

        Map<String, FieldInfo> result = new HashMap<String, FieldInfo>();
        for (RecordSchemaField field: fields)
        {
            CsvRecordFieldExtension extension =
                    field.getFieldExtension(CsvRecordFieldExtension.class, cvsExtensionName);
            if (extension!=null)
                result.put(field.getName(), new FieldInfo(extension));
        }
        return result;
    }

    private class FieldInfo
    {
        private final int columnNumber;
        private final CsvRecordFieldExtension extension;

        public FieldInfo(CsvRecordFieldExtension extension)
        {
            this.extension = extension;
            this.columnNumber = extension.getColumnNumber();
        }

        public int getColumnNumber()
        {
            return columnNumber;
        }

        public Object prepareValue(Object value)
        {
            return extension.prepareValue(value, null);
        }
    }
}
