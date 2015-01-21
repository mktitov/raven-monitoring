/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ds.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CsvReaderNode extends AbstractSafeDataPipe {
    public enum Format {
        DEFAULT(CSVFormat.DEFAULT), EXCEL(CSVFormat.EXCEL), MYSQL(CSVFormat.MYSQL), RFC4180(CSVFormat.RFC4180), TDF(CSVFormat.TDF);
        private final CSVFormat csvFormat;

        private Format(CSVFormat format) {
            this.csvFormat = format;
        }

        public CSVFormat getCsvFormat() {
            return csvFormat;
        }
    };
    
    @Parameter()
    private Charset dataEncoding;
    
    @Parameter() 
    private String delimiter;

    @Parameter()
    private String quoteChar;
    
    @Parameter()
    private Boolean ignoreEmptyLines;
    
    @Parameter(defaultValue="DEFAULT") @NotNull
    private Format format;

    public Boolean getIgnoreEmptyLines() {
        return ignoreEmptyLines;
    }

    public void setIgnoreEmptyLines(Boolean ignoreEmptyLines) {
        this.ignoreEmptyLines = ignoreEmptyLines;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Charset getDataEncoding() {
        return dataEncoding;
    }

    public void setDataEncoding(Charset dataEncoding) {
        this.dataEncoding = dataEncoding;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getQuoteChar() {
        return quoteChar;
    }

    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data==null) {
            DataSourceHelper.executeContextCallbacks(dataSource, context, data);
            return;
        }
        InputStream stream = converter.convert(InputStream.class, data, null);
        Charset _dataEncoding = dataEncoding;
        InputStreamReader reader = _dataEncoding==null?
                new InputStreamReader(stream) :
                new InputStreamReader(stream, dataEncoding);
        try {
            boolean _stopOnError = getStopProcessingOnError();
            DataConsumer _errorConsumer = getErrorConsumer();
            CSVFormat _format = privateFormat();
            CSVParser parser = _format.parse(reader);
            try {
                try {
                    int recNumber = 1;
                    for (CSVRecord rec: parser) {
                        String[] vals = new String[rec.size()];
                        for (int i=0; i<vals.length; ++i)
                            vals[i] = rec.get(i);
                        context.getParameters().put("recordNumber", recNumber++);
                        context.getParameters().put("lineNumber", parser.getCurrentLineNumber());
                        if (!sendDataAndError(vals, context, _stopOnError, _errorConsumer))
                            break;
                    }
                } finally {
                    sendDataAndError(null, context, _stopOnError, _errorConsumer);
                }
            } finally  {                
                parser.close();
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(stream);
        }
    }

    private CSVFormat privateFormat() {
        CSVFormat _format = format.getCsvFormat().withHeader(null);
        String _quoteChar = quoteChar;
        if (_quoteChar!=null)
            _format = _format.withQuote(_quoteChar.charAt(0));
        String _delimiter = delimiter;
        if (_delimiter!=null)
            _format = _format.withDelimiter(_delimiter.charAt(0));
        Boolean _ignoreEmptyLines = ignoreEmptyLines;
        if (_ignoreEmptyLines!=null)
            _format = _format.withIgnoreEmptyLines(true);
        return _format;
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
    
}
