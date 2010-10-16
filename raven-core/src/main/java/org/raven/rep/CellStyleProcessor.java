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

import java.util.HashMap;
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
    private final Map<String, HSSFCellStyle> styles = new HashMap<String, HSSFCellStyle>();

    public CellStyleProcessor(List<CellStyleSelectorNode> selectors, BindingSupport bindingSupport, Node owner)
    {
        this.selectors = selectors;
        this.bindingSupport = bindingSupport;
        this.owner = owner;
    }

    public void processRow(Row row, Map namedCells)
    {
        List<Cell> cells = row.getCells();
        if (cells!=null && cells.size()>0)
        {
            bindingSupport.put("row", row);
            int rowNumber = row.getPoiRow().getRowNum()+1;
            bindingSupport.put("rowNumber", rowNumber);
            if (row.getParentRow()!=null){
                RowCollection collection = (RowCollection)row.getParentRow().getRowCollections().get(0);
                bindingSupport.put("collection", collection);
                bindingSupport.put("rowObject", collection.getIterateObject());
            }else{
                bindingSupport.put("collection", null);
                bindingSupport.put("rowObject", null);
            }
            for (int c=0; c<cells.size(); ++c)
            {
                Cell cell = cells.get(c);
                bindingSupport.put("columnNumber", c+1);
                bindingSupport.put("cell", cell);
                for (CellStyleSelectorNode selector: selectors)
                {
                    Integer ignoreRow = selector.getIgnoreRowNumber();
                    Boolean selected=selector.getSelector();
                    if ((ignoreRow==null || rowNumber!=ignoreRow) && selected!=null && selected && cell.getPoiCell()!=null)
                    {
                        String styleName = selector.getStyleCellLabel();
                        Cell styleCell = (Cell) namedCells.get(styleName);
                        if (styleCell==null){
                            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                                owner.getLogger().error("Invalid cell style label ({})", selector.getStyleCellLabel());
                        }
                        else
                            copyStyle((HSSFWorkbook)row.getSheet().getPoiWorkbook()
                                    , (HSSFCell)styleCell.getPoiCell(), (HSSFCell)cell.getPoiCell(), styleName);
                    }
                }
            }
        }
    }

    private void copyStyle(HSSFWorkbook workbook, HSSFCell fromCell, HSSFCell toCell, String styleName)
    {
        if (owner.isLogLevelEnabled(LogLevel.TRACE))
            owner.getLogger().trace("Applying style to the cell");
        HSSFCellStyle toStyle = toCell.getCellStyle();
        HSSFCellStyle fromStyle = fromCell.getCellStyle();
        if (fromStyle.getDataFormat() == toStyle.getDataFormat()){
            if (owner.isLogLevelEnabled(LogLevel.TRACE))
                owner.getLogger().trace("Style cell data format and current cell data format equals.");
            toCell.setCellStyle(fromStyle);
        } else
        {
            String styleIndex = styleName+"_"+toStyle.getDataFormat();
            HSSFCellStyle style = styles.get(styleIndex);
            if (style==null){
                if (owner.isLogLevelEnabled(LogLevel.TRACE))
                    owner.getLogger().trace("Creating new style ({})", styleIndex);
                style = workbook.createCellStyle();
                style.setAlignment(fromStyle.getAlignment());
                style.setBorderBottom(fromStyle.getBorderBottom());
                style.setBorderLeft(fromStyle.getBorderLeft());
                style.setBorderRight(fromStyle.getBorderRight());
                style.setBorderTop(fromStyle.getBorderTop());
                style.setBottomBorderColor(fromStyle.getBottomBorderColor());
                style.setDataFormat(toStyle.getDataFormat());
                style.setFillBackgroundColor(fromStyle.getFillBackgroundColor());
                style.setFillForegroundColor(fromStyle.getFillForegroundColor());
                style.setFillPattern(fromStyle.getFillPattern());
                style.setFont(workbook.getFontAt( fromStyle.getFontIndex()));
                style.setHidden(fromStyle.getHidden());
                style.setIndention(fromStyle.getIndention());
                style.setLeftBorderColor(fromStyle.getLeftBorderColor());
                style.setLocked(toStyle.getLocked());
                style.setRightBorderColor(fromStyle.getRightBorderColor());
                style.setTopBorderColor(fromStyle.getTopBorderColor());
                style.setVerticalAlignment(fromStyle.getVerticalAlignment());
                style.setWrapText(fromStyle.getWrapText());
                styles.put(styleIndex, style);
            }else if (owner.isLogLevelEnabled(LogLevel.TRACE))
                owner.getLogger().trace("Style ({}) found in cache", styleIndex);
            toCell.setCellStyle(style);
        }
    }
}
