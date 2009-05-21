/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.log.LogLevel;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SvnWriterNode extends AbstractDataConsumer
{
    public static final String DATA_FIELD = "data";
    public static final String PATH_FIELD = "path";
    @Parameter @NotNull
    private String repositoryUrl;

    @Parameter @NotNull
    private String workDirectory;

    @Parameter @NotNull
    private String username;

    @Parameter @NotNull
    private String password;

    @Parameter
    private Charset sourceEncoding;
    @Parameter
    private Charset targetEncoding;

    public Charset getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(Charset sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public Charset getTargetEncoding() {
        return targetEncoding;
    }

    public void setTargetEncoding(Charset targetEncoding) {
        this.targetEncoding = targetEncoding;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (data==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Recieved the end marker of the records sequence");
            return;
        }

        if (!(data instanceof Record))
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Invalid data type recieved from the data source (%s). " +
                        "Expected (%s) recieved (%s)"
                        , dataSource.getPath(), Record.class.getName(), data.getClass().getName()));
            return;
        }

        Record record = (Record) data;
        validateRecord(record);
        String path = (String) record.getValue(PATH_FIELD);
        if (path==null || path.isEmpty())
            throw new Exception("The value of the (%s) field can not be NULL or EMPTY");

        Object fileData = record.getValue(DATA_FIELD);
        if (fileData==null)
            throw new Exception("The value of the (%s) field can not be NULL");

        setupSubversion();

//        OutputStreamWriter writer = new OutputStreamWriter
    }

    private void setupSubversion() throws SVNException
    {
        SVNURL svnurl = SVNURL.parseURIEncoded(repositoryUrl);
        if ("file".equals(svnurl.getPath()))
            createLocalRepository(svnurl);

        
    }

    private void createLocalRepository(SVNURL svnurl) throws SVNException
    {
        File file = new File(svnurl.getPath());
        if (!file.exists())
            svnurl = SVNRepositoryFactory.createLocalRepository(file, true, false);
    }

    private void validateRecord(Record record) throws Exception
    {
        try
        {
            RecordSchema schema = record.getSchema();
            Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(schema);

            RecordSchemaField keyField = fields.get(PATH_FIELD);
            if (keyField==null)
                throw new Exception(String.format(
                        "Schema does not contains (%s) field", PATH_FIELD));
            if (!keyField.getFieldType().equals(RecordSchemaFieldType.STRING))
                throw new Exception(String.format(
                        "Invalid type of the (%s) field. Expected (%s) but have (%s)"
                        , PATH_FIELD, RecordSchemaFieldType.STRING.toString()
                        , keyField.getFieldType().toString()));

            RecordSchemaField dataField = fields.get(DATA_FIELD);
            if (dataField==null)
                throw new Exception(String.format(
                        "Schema does not contains (%s) field", DATA_FIELD));
        }
        catch(Exception e)
        {
            throw new Exception(String.format(
                    "Record schema (%s) validation error. %s"
                    , record.getSchema().getName(), e.getMessage()));
        }
    }
}
