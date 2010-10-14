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

package org.raven.rep;

import java.util.List;
import java.util.Map;
import net.sf.jxls.parser.Cell;
import net.sf.jxls.processor.RowProcessor;
import net.sf.jxls.transformer.Row;
import net.sf.jxls.transformer.RowCollection;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class CellStyleProcessor implements RowProcessor
{
    private final List<CellStyleSelectorNode> selectors;
    private final BindingSupport bindingSupport;
    private final Node owner;

    public CellStyleProcessor(List<CellStyleSelectorNode> selectors, BindingSupport bindingSupport, Node owner)
    {
        this.selectors = selectors;
        this.bindingSupport = bindingSupport;
        this.owner = owner;
    }

    public void processRow(Row row, Map namedCells)
    {
        List<Cell> cells = row.getCells();
        if (cells!=null && cells.size()>0){
            bindingSupport.put("row", row);
            bindingSupport.put("rowNumber", row.getPoiRow().getRowNum()+1);
            if (row.getParentRow()!=null){
                RowCollection collection = (RowCollection)row.getParentRow().getRowCollections().get(0);
                bindingSupport.put("collection", collection);
                bindingSupport.put("rowObject", collection.getIterateObject());
            }
            for (int c=0; c<cells.size(); ++c)
            {
                Cell cell = cells.get(c);
                bindingSupport.put("columnNumber", c+1);
                bindingSupport.put("cell", cell);
                for (CellStyleSelectorNode selector: selectors)
                    if (selector.getSelector() && cell.getPoiCell()!=null){
                        Cell styleCell = (Cell) namedCells.get(selector.getStyleCellLabel());
                        if (styleCell==null){
                            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                                owner.getLogger().error("Invalid cell style label ({})", selector.getStyleCellLabel());
                        }
                        else
                            copyStyle((HSSFWorkbook)row.getSheet().getPoiWorkbook()
                                    , (HSSFCell)styleCell.getPoiCell(), (HSSFCell)cell.getPoiCell());
                    }
            }
        }
    }

    private void copyStyle(HSSFWorkbook workbook, HSSFCell fromCell, HSSFCell toCell)
    {
        HSSFCellStyle toStyle = toCell.getCellStyle();
        HSSFCellStyle fromStyle = fromCell.getCellStyle();
        if (fromStyle.getDataFormat() == toStyle.getDataFormat())
            toCell.setCellStyle(fromStyle);
        else
        {
            HSSFCellStyle newStyle = workbook.createCellStyle();
            newStyle.setAlignment( toStyle.getAlignment() );
            newStyle.setBorderBottom( toStyle.getBorderBottom() );
            newStyle.setBorderLeft( toStyle.getBorderLeft() );
            newStyle.setBorderRight( toStyle.getBorderRight() );
            newStyle.setBorderTop( toStyle.getBorderTop() );
            newStyle.setBottomBorderColor( toStyle.getBottomBorderColor() );
            newStyle.setDataFormat( toStyle.getDataFormat() );
            newStyle.setFillBackgroundColor( fromStyle.getFillBackgroundColor() );
            newStyle.setFillForegroundColor( fromStyle.getFillForegroundColor() );
            newStyle.setFillPattern( fromStyle.getFillPattern() );
            newStyle.setFont( workbook.getFontAt( fromStyle.getFontIndex() ) );
            newStyle.setHidden( toStyle.getHidden() );
            newStyle.setIndention( toStyle.getIndention() );
            newStyle.setLeftBorderColor( toStyle.getLeftBorderColor() );
            newStyle.setLocked( toStyle.getLocked() );
            newStyle.setRightBorderColor( toStyle.getRightBorderColor() );
            newStyle.setTopBorderColor( toStyle.getTopBorderColor() );
            newStyle.setVerticalAlignment( toStyle.getVerticalAlignment() );
            newStyle.setWrapText( toStyle.getWrapText() );
            toCell.setCellStyle( newStyle );
        }
    }
}
