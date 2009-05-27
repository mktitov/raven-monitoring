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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
import org.raven.tree.impl.DataSourcesNode;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
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

    private SVNURL repUrl;
    private File workDir;

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
    protected synchronized void doSetData(DataSource dataSource, Object data) throws Exception
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

        SVNClientManager svnClient = setupSubversion();

//        OutputStreamWriter writer = new OutputStreamWriter
        Object inputStream = prepareInputStream(fileData);
        File dataFile = new File(workDir, path);
        boolean dataFileExists = dataFile.exists();
        Object outputStream = prepareOutputStream(inputStream, dataFile);
        writeDataToFile(inputStream, outputStream);

        commitChanges(svnClient, dataFile, !dataFileExists);
    }

    private void commitChanges(SVNClientManager svnClient, File dataFile, boolean newFile)
            throws SVNException
    {
        if (newFile)
            svnClient.getWCClient().doAdd(
                    dataFile, false, false, true, SVNDepth.INFINITY, false, true);
        svnClient.getCommitClient().doCommit(
                new File[]{workDir}, false
                , ""
                , null, null, false, true
                , SVNDepth.UNKNOWN);
    }

    private Object prepareOutputStream(Object inputStream, File dataFile) throws Exception
    {
        File parentFile = dataFile.getParentFile();
        if (!parentFile.exists())
            if (!parentFile.mkdirs())
                throw new Exception(String.format(
                        "Error creating directory (%s) in the work directory (%s)"
                        , parentFile.getAbsolutePath(), workDir.getPath()));
        
        Object result = new FileOutputStream(dataFile, false);
        Charset _targetEncoding = targetEncoding;
        if (_targetEncoding!=null || inputStream instanceof Reader)
        {
            result = _targetEncoding!=null? 
                new OutputStreamWriter((OutputStream)result, _targetEncoding) :
                new OutputStreamWriter((OutputStream)result);
        }

        return result;
    }

    private Object prepareInputStream(Object fileData)
    {
        Object result = null;
        if (fileData instanceof InputStream)
            result = fileData;
        else if (fileData instanceof byte[])
            result = new ByteArrayInputStream((byte[])fileData);
        else
            result = converter.convert(String.class, fileData, null);

        Charset _sourceEncoding = sourceEncoding;
        if (result instanceof InputStream && _sourceEncoding!=null)
            result  = new InputStreamReader((InputStream)result, _sourceEncoding);
        
        return result;
    }

    private SVNClientManager setupSubversion() throws Exception
    {
        repUrl = SVNURL.parseURIEncoded(repositoryUrl);
        if ("file".equals(repUrl.getProtocol()))
            createLocalRepository(repUrl);

        workDir = new File(workDirectory);
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager svnClient =
                SVNClientManager.newInstance(options, username, password);

        createWorkDir(svnClient, workDir);

        return svnClient;
    }

    private SVNURL createLocalRepository(SVNURL svnurl) throws SVNException
    {
        File file = new File(svnurl.getPath());
        if (!file.exists())
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Creating local subversion repository (%s)", svnurl.getPath()));
            svnurl = SVNRepositoryFactory.createLocalRepository(file, true, false);
        }
        FSRepositoryFactory.setup();
        return svnurl;
    }

    private void createWorkDir(SVNClientManager svnClient, File workDir) throws Exception
    {
        if (!workDir.exists() || !(new File(workDir, ".svn").exists()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Initializing work directory (%s)", workDir.getPath()));
            if (!workDir.exists())
                if (!workDir.mkdirs())
                    throw new Exception(String.format(
                            "Error creating working directory (%s)", workDir.getAbsolutePath()));
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Checkouting to the work directory");
            svnClient.getUpdateClient().doCheckout(
                    repUrl, workDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        }
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

    private void writeDataToFile(Object inputStream, Object outputStream) throws IOException
    {
        if (inputStream instanceof String)
        {
            Charset _targetEncoding = targetEncoding==null?
                    Charset.defaultCharset() : targetEncoding;
            if (outputStream instanceof OutputStream)
                ((OutputStream)outputStream).write(((String)inputStream).getBytes(_targetEncoding));
            else
                ((Writer)outputStream).write((String)inputStream);
        }
        else if (inputStream instanceof InputStream)
        {
            if (outputStream instanceof OutputStream)
                IOUtils.copy((InputStream)inputStream, (OutputStream)outputStream);
            else
                IOUtils.copy((InputStream)inputStream, (Writer)outputStream);
        }
        else
        {
            if (outputStream instanceof OutputStream)
                IOUtils.copy((Reader)inputStream, (OutputStream)outputStream);
            else
                IOUtils.copy((Reader)inputStream, (Writer)outputStream);
        }

        if (outputStream instanceof OutputStream)
            ((OutputStream)outputStream).close();
        else
            ((Writer)outputStream).close();
    }
}
