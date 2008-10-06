/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.tree.impl;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.table.BalancedColumnBasedTable;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RegexpDataConverterNode extends AbstractDataPipe
{
    public static String COLUMNDELIMITER_ATTRIBUTE = "columnDelimiter";
    public static String ROWDELIMITER_ATTRIBUTE = "rowDelimiter";

    @Parameter(defaultValue="\\s+")
    private String columnDelimiter;

    @Parameter(defaultValue="\\r?\\n")
    private String rowDelimiter;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useRegexpGroupsInRowsDelimiter;
    
    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useRegexpGroupsInColumnDelimiter;

    @Parameter()
    private Charset dataEncoding;

    public String getColumnDelimiter()
    {
        return columnDelimiter;
    }

    public void setColumnDelimiter(String columnDelimiter)
    {
        this.columnDelimiter = columnDelimiter;
    }

    public Charset getDataEncoding()
    {
        return dataEncoding;
    }

    public void setDataEncoding(Charset dataEncoding)
    {
        this.dataEncoding = dataEncoding;
    }

    public String getRowDelimiter()
    {
        return rowDelimiter;
    }

    public void setRowDelimiter(String rowDelimiter)
    {
        this.rowDelimiter = rowDelimiter;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        Charset _dataEncoding = dataEncoding;
        String str = converter.convert(
                String.class, data, _dataEncoding==null? null : _dataEncoding.name());

        if (str==null)
            return;

        String[] rows = extractRows(str);
        if (rows==null)
            return;

        BalancedColumnBasedTable table = new BalancedColumnBasedTable();
        String _columnDelimiter = columnDelimiter;
        Pattern columnPattern = null;
        if (_columnDelimiter!=null)
            columnPattern = Pattern.compile(_columnDelimiter);
        boolean _useRegexpGroupsInColumnDelimiter = useRegexpGroupsInColumnDelimiter;
        for (String row: rows)
        {
            int col = 1;
            String cols[] = extractCols(
                    row, _columnDelimiter, _useRegexpGroupsInColumnDelimiter, columnPattern);
            if (cols!=null)
                for (String colValue: cols)
                    table.addValue(""+(col++), colValue);
        }

        table.freeze();

        sendDataToConsumers(data);
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    private String[] extractCols(
            String row, String _columnDelimiter, boolean _useRegexpGroupsInColumnDelimiter
            , Pattern columnPattern)
    {
        String[] cols = null;
        if (_columnDelimiter==null)
            cols = new String[]{};
        else
        {
            if (!_useRegexpGroupsInColumnDelimiter)
                cols = columnPattern.split(row);
            else
            {
                Matcher matcher = columnPattern.matcher(row);
                if (matcher.groupCount()>0)
                {
                    cols = new String[matcher.groupCount()];
                    for (int i=0; i<matcher.groupCount(); ++i)
                        cols[i]=matcher.group(i+1);
                }
            }
        }

        return cols;
    }

    private String[] extractRows(String str)
    {
        String rows[] = null;
        String _rowDelimiter = rowDelimiter;
        if (_rowDelimiter!=null)
        {
            if (useRegexpGroupsInRowsDelimiter)
            {
                Pattern rowPattern = Pattern.compile(_rowDelimiter);
                Matcher rowMatcher = rowPattern.matcher(str);
                if (rowMatcher.groupCount()>0)
                {
                    rows = new String[rowMatcher.groupCount()];
                    for (int i=1; i<=rowMatcher.groupCount(); ++i)
                        rows[i-1]=rowMatcher.group(i);
                        
                }
            }
            else
                rows = str.split(_rowDelimiter);
        }
        else
            rows = new String[]{str};

        return rows;
    }
}

