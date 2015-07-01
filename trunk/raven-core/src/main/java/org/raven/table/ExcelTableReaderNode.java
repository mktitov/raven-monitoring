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

package org.raven.table;

import java.io.InputStream;
import java.io.PushbackInputStream;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ExcelTableReaderNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(defaultValue="1")
    private Integer sheetNumber;

    @NotNull @Parameter(defaultValue="2")
    private Integer startFromRow;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean treatEmptyStringAsNull;

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

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data==null) {
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }
        InputStream dataStream = converter.convert(InputStream.class, data, null);
        Workbook wb = WorkbookFactory.create(new PushbackInputStream(dataStream));
        Sheet sheet = wb.getSheetAt(sheetNumber-1);
        BalancedColumnBasedTable table = null;
        for (int r=startFromRow-1; r<=sheet.getLastRowNum(); ++r) {
            Row row = sheet.getRow(r);
            if (row!=null){
                for (int c=0; c<row.getLastCellNum(); ++c) {
                    Object value = getCellValue(row.getCell(c, Row.RETURN_BLANK_AS_NULL));
                    if (table==null)
                        table = new BalancedColumnBasedTable();
                    table.addValue(""+(c+1), value);
                }
            } else
                table.addValue("1", null);
        }
        if (table!=null){
            table.freeze();
            sendDataToConsumers(table, context);
        } else {
            DataSourceHelper.executeContextCallbacks(this, context, data);
        }
    }

    private Object getCellValue(Cell cell)
    {
        if (cell==null)
            return null;
        Object value = null;
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                double num = cell.getNumericCellValue();
                if (HSSFDateUtil.isCellDateFormatted(cell) && HSSFDateUtil.isValidExcelDate(num)) {
                    value = cell.getDateCellValue();
                } else {
                    value = num;
                }
                break;
            case Cell.CELL_TYPE_STRING:
                value = cell.getStringCellValue();
                if (value != null && "".equals(value) && treatEmptyStringAsNull) {
                    value = null;
                }
                break;
        }
        return value;
    }
}
