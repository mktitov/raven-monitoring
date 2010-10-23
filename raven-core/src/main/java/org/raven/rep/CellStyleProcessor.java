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
                            if (selector.isLogLevelEnabled(LogLevel.ERROR))
                                selector.getLogger().error("Invalid cell style label ({})", selector.getStyleCellLabel());
                        }
                        else
                            copyStyle((HSSFWorkbook)row.getSheet().getPoiWorkbook()
                                    , (HSSFCell)styleCell.getPoiCell(), (HSSFCell)cell.getPoiCell(), styleName, selector);
                    }
                }
            }
        }
    }

    private void copyStyle(HSSFWorkbook workbook, HSSFCell fromCell, HSSFCell toCell, String styleName, CellStyleSelectorNode selector)
    {
        if (owner.isLogLevelEnabled(LogLevel.TRACE))
            owner.getLogger().trace("Applying style to the cell");

        HSSFCellStyle toStyle = toCell.getCellStyle();
        String toStyleId = getStyleId(styleName, toStyle, toStyle, selector);
        if (!styles.containsKey(toStyleId))
            styles.put(toStyleId, toStyle);

        HSSFCellStyle fromStyle = fromCell.getCellStyle();
        String styleId = getStyleId(styleName, fromStyle, toStyle, selector);
        HSSFCellStyle style = styles.get(styleId);
        if (style==null)
        {
            if (owner.isLogLevelEnabled(LogLevel.TRACE))
                owner.getLogger().trace("Creating new style ({})", styleId);
            HSSFCellStyle st;
            style = workbook.createCellStyle();
            style.setAlignment((selector.getUnchangeAligment()?toStyle:fromStyle).getAlignment());
            
            st = selector.getUnchangeBorder()? toStyle : fromStyle;
            style.setBorderBottom(st.getBorderBottom());
            style.setBorderLeft(st.getBorderLeft());
            style.setBorderRight(st.getBorderRight());
            style.setBorderTop(st.getBorderTop());

            st = selector.getUnchangeBorderColor()? toStyle : fromStyle;
            style.setBottomBorderColor(st.getBottomBorderColor());
            style.setLeftBorderColor(st.getLeftBorderColor());
            style.setRightBorderColor(st.getBorderRight());
            style.setTopBorderColor(st.getBorderTop());
            
            style.setDataFormat((selector.getUnchangeDataFormat()?toStyle:fromStyle).getDataFormat());
            style.setFillBackgroundColor((selector.getUnchangeBackgroundColor()?toStyle:fromStyle).getFillBackgroundColor());
            style.setFillForegroundColor((selector.getUnchangeForegroundColor()?toStyle:fromStyle).getFillForegroundColor());
            style.setFillPattern(fromStyle.getFillPattern());
            style.setFont(workbook.getFontAt(fromStyle.getFontIndex()));
            style.setHidden(fromStyle.getHidden());
            style.setIndention(fromStyle.getIndention());
            style.setLocked(toStyle.getLocked());
            style.setVerticalAlignment(fromStyle.getVerticalAlignment());
            style.setWrapText(fromStyle.getWrapText());
            styles.put(styleId, style);
        }else if (owner.isLogLevelEnabled(LogLevel.TRACE))
            owner.getLogger().trace("Style ({}) found in cache", styleId);
        
        toCell.setCellStyle(style);
    }

    private String getStyleId(String styleName, HSSFCellStyle from, HSSFCellStyle to, CellStyleSelectorNode selector)
    {
        HSSFCellStyle st = selector.getUnchangeBorder()? to : from;
        String borderId = "_"+st.getBorderBottom()+"_"+st.getBorderLeft()+"_"+st.getBorderRight()+"_"+st.getBorderTop();
        st = selector.getUnchangeBackgroundColor()? to : from;
        String borderColorId = "_"+st.getBottomBorderColor()+"_"+st.getLeftBorderColor()+"_"+st.getRightBorderColor()+"_"+st.getTopBorderColor();
        return  styleName+"_"
                + (selector.getUnchangeDataFormat()? to : from).getDataFormat()
                + borderId + borderColorId
                + "_"+(selector.getUnchangeBackgroundColor()? to : from).getFillBackgroundColor()
                + "_"+(selector.getUnchangeForegroundColor()? to : from).getFillForegroundColor()
                + "_"+(selector.getUnchangeAligment()? to : from).getAlignment();
    }
}
