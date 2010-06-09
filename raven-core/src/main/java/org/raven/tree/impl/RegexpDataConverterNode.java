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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.log.LogLevel;
import org.raven.table.BalancedColumnBasedTable;
import org.raven.tree.NodeAttribute;
import org.raven.expr.impl.BindingSupportImpl;
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
    public static final String ROWNUM_BINDING = "rownum";
    public static final String ROW_BINDING = "row";

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

    @Parameter()
    private Boolean rowFilter;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useRowFilter;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Boolean getRowFilter()
    {
        return rowFilter;
    }

    public void setRowFilter(Boolean rowFilter)
    {
        this.rowFilter = rowFilter;
    }

    public Boolean getUseRowFilter()
    {
        return useRowFilter;
    }

    public void setUseRowFilter(Boolean useRowFilter)
    {
        this.useRowFilter = useRowFilter;
    }

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

    public Boolean getUseRegexpGroupsInColumnDelimiter()
    {
        return useRegexpGroupsInColumnDelimiter;
    }

    public void setUseRegexpGroupsInColumnDelimiter(Boolean useRegexpGroupsInColumnDelimiter)
    {
        this.useRegexpGroupsInColumnDelimiter = useRegexpGroupsInColumnDelimiter;
    }

    public Boolean getUseRegexpGroupsInRowsDelimiter()
    {
        return useRegexpGroupsInRowsDelimiter;
    }

    public void setUseRegexpGroupsInRowsDelimiter(Boolean useRegexpGroupsInRowsDelimiter)
    {
        this.useRegexpGroupsInRowsDelimiter = useRegexpGroupsInRowsDelimiter;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        Charset _dataEncoding = dataEncoding;
        if (isLogLevelEnabled(LogLevel.DEBUG))
        {
            debug(String.format(
                    "Trying to convert data to table using (%s) charset"
                    , _dataEncoding==null? "DEFAULT" : _dataEncoding.displayName()));
        }
        String str = converter.convert(
                String.class, data, _dataEncoding==null? null : _dataEncoding.name());

        if (str==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Can't convert NULL data to table");
            return;
        }

        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format(
                    "Converting string of %d symbols length to the table", str.length()));
        if (isLogLevelEnabled(LogLevel.TRACE))
            trace(str);

        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Dividing string data on rows");
        String[] rows = extractRows(str);
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("(%d) rows extracted", rows==null? 0 : rows.length));
        if (rows==null)
            return;

        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Creating table");
        BalancedColumnBasedTable table = new BalancedColumnBasedTable();
        String _columnDelimiter = columnDelimiter;
        Pattern columnPattern = null;
        if (_columnDelimiter!=null)
            columnPattern = Pattern.compile(_columnDelimiter);
        boolean _useRegexpGroupsInColumnDelimiter = useRegexpGroupsInColumnDelimiter;
        boolean _useRowFilter = useRowFilter;
        try
        {
            int rownum=0;
            int rowcount=0;
            for (String row: rows)
            {
                ++rownum;
                if (_useRowFilter)
                {
                    bindingSupport.put(ROW_BINDING, row);
                    bindingSupport.put(ROWNUM_BINDING, rownum);
                    Boolean _rowFilter = rowFilter;
                    if (_rowFilter==null || !_rowFilter)
                    {
                        if (isLogLevelEnabled(LogLevel.TRACE))
                            trace("SKIPPING ROW: "+row);
                        continue;
                    }
                }
                if (isLogLevelEnabled(LogLevel.TRACE))
                    trace("PROCESSING ROW: "+row);
                int col = 1;
                String cols[] = extractCols(
                        row, _columnDelimiter, _useRegexpGroupsInColumnDelimiter, columnPattern);
                if (cols!=null)
                {
                    ++rowcount;
                    for (String colValue: cols)
                        table.addValue(""+(col++), colValue);
                }
            }

            if (rowcount==0)
                table = null;
            else
                table.freeze();

            if (isLogLevelEnabled(LogLevel.DEBUG))
            {
                if (table!=null)
                    debug(String.format(
                            "Table created. Row count - %d, cols count %d"
                            , rowcount, table.getColumnNames().length));
                else
                    debug("Empty table created");
            }
        }
        finally
        {
            bindingSupport.reset();
        }

        sendDataToConsumers(table, context);
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
            cols = new String[]{row};
        else
        {
            if (!_useRegexpGroupsInColumnDelimiter)
                cols = columnPattern.split(row);
            else
            {
                Matcher matcher = columnPattern.matcher(row);
                List<String> colsList = new ArrayList<String>();
                while (matcher.find())
                {
                    if (matcher.groupCount()>0)
                    {
                        for (int i=0; i<matcher.groupCount(); ++i)
                            colsList.add(matcher.group(i+1));
                    }
                }
                if (colsList.size()>0)
                {
                    cols = new String[colsList.size()];
                    colsList.toArray(cols);
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
                List<String> rowsList = new ArrayList<String>(512);
                while (rowMatcher.find())
                {
                    if (rowMatcher.groupCount()>0)
                    {
                        for (int i=1; i<=rowMatcher.groupCount(); ++i)
                            rowsList.add(rowMatcher.group(i));
                    }
                }
                if (rowsList.size()>0)
                {
                    rows = new String[rowsList.size()];
                    rowsList.toArray(rows);
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

