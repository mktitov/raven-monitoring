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
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ExcelRecordReaderNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(defaultValue="1")
    private Integer sheetNumber;

    @NotNull @Parameter(defaultValue="1")
    private Integer startFromRow;

    @NotNull @Parameter(defaultValue="true")
    private Boolean treatEmptyStringAsNull;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean ignoreEmptyRows;
    
    @Parameter
    private Integer maxEmptyRowsCount;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    @Parameter
    private String cvsExtensionName;

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, 
        BindingSupport bindingSupport) 
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
            throws Exception
    {
        if (data==null) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Recieved null data from node (%s)", dataSource.getPath()));
            return;
        }

        Map<String, FieldInfo> fieldsColumns = getFieldsColumns();
        if (fieldsColumns==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                debug(String.format(
                        "CsvRecordFieldExtension was not defined for fields in the record schema (%s)"
                        , recordSchema.getName()));
            return;
        }

        boolean _stopOnError = getStopProcessingOnError();
        DataConsumer _errorConsumer = getErrorConsumer();
        InputStream dataStream = converter.convert(InputStream.class, data, null);
        try{
            Workbook wb = WorkbookFactory.create(new PushbackInputStream(dataStream));
            Sheet sheet = wb.getSheetAt(sheetNumber-1);
            try{
                int nullRowCount = 0;
                boolean _treatEmptyStringAsNull = treatEmptyStringAsNull;
                boolean _ignoreEmptyRows = ignoreEmptyRows;
                int _maxEmptyRowsCount = maxEmptyRowsCount==null? Integer.MAX_VALUE : maxEmptyRowsCount;
                for (int r=startFromRow-1; r<=sheet.getLastRowNum(); ++r) {
                    Row row = sheet.getRow(r);
                    boolean nullRow = true;
                    if (row!=null){
                        Record record = recordSchema.createRecord();
                        for (Map.Entry<String, FieldInfo> fieldCol: fieldsColumns.entrySet()) {
                            Object value = processValue(row, fieldCol.getValue().getColumnNumber()-1
                                    , fieldCol.getValue(), record, fieldCol.getKey(), _treatEmptyStringAsNull);
                            if (nullRow && value!=null)
                                nullRow = false;
                        }
                        if (!_ignoreEmptyRows || !nullRow) {
                            if (!sendDataAndError(record, context, _stopOnError, _errorConsumer))
                                break;
                        }
                    }
                    nullRowCount = nullRow? nullRowCount+1 : 0;
                    if (_maxEmptyRowsCount==nullRowCount) {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            logger.debug("Stopping processing EXCEL file because of maxEmptyRowsCount "
                                    + "value ({}) reached", _maxEmptyRowsCount);
                        break;
                    }
                }
            } finally {
                sendDataAndError(null, context, _stopOnError, _errorConsumer);
            }
        }finally{
            dataStream.close();
        }
    }
    
    private Object processValue(Row row, int colNumber, FieldInfo fieldInfo, Record record
        , String fieldName, boolean emptyStringAsNull) 
            throws Exception 
    {
        Cell cell = row.getCell(colNumber, Row.RETURN_BLANK_AS_NULL);
        Object value = null;
        if (cell!=null){
            switch(cell.getCellType()){
                case Cell.CELL_TYPE_BOOLEAN: value = cell.getBooleanCellValue(); break;
                case Cell.CELL_TYPE_NUMERIC:
                    double num = cell.getNumericCellValue();
                    if (HSSFDateUtil.isCellDateFormatted(cell) && HSSFDateUtil.isValidExcelDate(num))
                        value = cell.getDateCellValue();
                    else
                        value = num;
                    break;
                case Cell.CELL_TYPE_STRING :
                    value = cell.getStringCellValue();
                    if (value!=null && "".equals(value) && emptyStringAsNull)
                        value = null;
                    break;
            }
        }
        value = fieldInfo.prepareValue(value);
        if (value!=null)
            record.setValue(fieldName, value);
        return value;
    }

    public Boolean getIgnoreEmptyRows() {
        return ignoreEmptyRows;
    }

    public void setIgnoreEmptyRows(Boolean ignoreEmptyRows) {
        this.ignoreEmptyRows = ignoreEmptyRows;
    }

    public Integer getMaxEmptyRowsCount() {
        return maxEmptyRowsCount;
    }

    public void setMaxEmptyRowsCount(Integer maxEmptyRowsCount) {
        this.maxEmptyRowsCount = maxEmptyRowsCount;
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

    public Boolean getTreatEmptyStringAsNull() {
        return treatEmptyStringAsNull;
    }

    public void setTreatEmptyStringAsNull(Boolean treatEmptyStringAsNull) {
        this.treatEmptyStringAsNull = treatEmptyStringAsNull;
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
