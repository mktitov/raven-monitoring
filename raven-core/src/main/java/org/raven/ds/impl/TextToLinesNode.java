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
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class TextToLinesNode extends AbstractSafeDataPipe
{
    public static final String LINE_BINDING = "line";
    public static String LINE_NUMBER_PARAM = "linenumber";
    public static String LINE_FILTER_ATTR = "lineFilter";

    @NotNull @Parameter()
    private Charset encoding;

    @NotNull @Parameter(defaultValue="true")
    private Boolean lineFilter;

    @NotNull @Parameter(defaultValue="1")
    private Integer startFromLine;

    public Integer getStartFromLine() {
        return startFromLine;
    }

    public void setStartFromLine(Integer startFromLine) {
        this.startFromLine = startFromLine;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public Boolean getLineFilter() {
        return lineFilter;
    }

    public void setLineFilter(Boolean lineFilter) {
        this.lineFilter = lineFilter;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) 
            throws Exception
    {
        if (data==null) {
            sendDataToConsumers(null, context);
            return;
        }

        InputStream is = converter.convert(InputStream.class, data, encoding.name());
        if (is==null)
            return;

        LineIterator it = IOUtils.lineIterator(is, encoding.name());
        int lineNumber = 0;
        int _startFromLine = startFromLine;
        boolean _stopOnError = getStopProcessingOnError();
        try{
            while (it.hasNext()) {
                if (_stopOnError && context.hasErrors())
                    break;
                ++lineNumber;
                String line = it.nextLine();
                if (lineNumber<_startFromLine)
                    continue;
                bindingSupport.put(LINE_NUMBER_PARAM, lineNumber);
                bindingSupport.put(LINE_BINDING, line);
                if (lineFilter){
                    context.putAt(LINE_NUMBER_PARAM, lineNumber);
                    sendDataToConsumers(line, context);
                }
            }
            sendDataToConsumers(null, context);
        } finally {
            bindingSupport.reset();
        }

    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }
}
