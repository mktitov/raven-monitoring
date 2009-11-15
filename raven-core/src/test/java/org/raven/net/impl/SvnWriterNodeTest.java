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
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 *
 * @author Mikhail Titov
 */
public class SvnWriterNodeTest extends RavenCoreTestCase
{
    private SvnWriterNode svn;
    private PushDataSource ds;
    private RecordSchemaNode schema;
    private File repFile;
    private File wcFile;
    private File wc2File;
    private File testFile;

    @Before
    public void prepare() throws IOException
    {
        schema = new RecordSchemaNode();
        schema.setName("schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        RecordSchemaFieldNode pathField = new RecordSchemaFieldNode();
        pathField.setName(SvnWriterNode.PATH_FIELD);
        schema.addAndSaveChildren(pathField);
        pathField.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(pathField.start());

        RecordSchemaFieldNode dataField = new RecordSchemaFieldNode();
        dataField.setName(SvnWriterNode.DATA_FIELD);
        schema.addAndSaveChildren(dataField);
        dataField.setFieldType(RecordSchemaFieldType.OBJECT);
        assertTrue(dataField.start());

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        repFile = new File("target/svnrep");
        if (repFile.exists())
            FileUtils.forceDelete(repFile);
        wcFile = new File("target/svn_wc");
        if (wcFile.exists())
            FileUtils.forceDelete(wcFile);
        wc2File = new File("target/svn_wc2");
        if (wc2File.exists())
            FileUtils.forceDelete(wc2File);
        testFile = new File(wcFile, "test/file.txt");
        
        svn = new SvnWriterNode();
        svn.setName("svn");
        tree.getRootNode().addAndSaveChildren(svn);
        svn.setUsername("test");
        svn.setPassword("test");
        svn.setRepositoryUrl("file://"+repFile.getAbsolutePath());
        svn.setWorkDirectory(wcFile.getAbsolutePath());
        svn.setDataSource(ds);
        svn.setLogLevel(LogLevel.DEBUG);
        assertTrue(svn.start());
    }

    @Test
    public void baseTest() throws RecordException, IOException, SVNException
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");

        ds.pushData(rec);

        assertTrue(repFile.exists());
        assertTrue(wcFile.exists());
        assertTrue(new File(wcFile, ".svn").exists());

        File testDir = new File(wcFile, "test");
        assertTrue(testDir.exists());
        testFile = new File(testDir, "file.txt");
        assertTrue(testFile.exists());
        assertEquals("file content", FileUtils.readFileToString(testFile));

        assertTrue(new File(testDir, ".svn").exists());

        rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content modified");
        ds.pushData(rec);

        assertTrue(testFile.exists());
        assertEquals("file content modified", FileUtils.readFileToString(testFile));

        wc2File.mkdirs();
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager svnClient = SVNClientManager.newInstance(options, "test", "test");
        SVNURL svnurl = SVNURL.parseURIDecoded(svn.getRepositoryUrl());
        svnClient.getUpdateClient().doCheckout(
                svnurl, wc2File, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        assertTrue(new File(wc2File, "test").exists());
        File newTestFile = new File(wc2File, "test/file.txt");
        assertTrue(newTestFile.exists());
        assertEquals("file content modified", FileUtils.readFileToString(newTestFile));
    }

    @Test
    public void stringDataTypeTest() throws Exception
    {
        svn.setTargetEncoding(Charset.forName("UTF-8"));
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "Содержимое файла");

        ds.pushData(rec);

        assertTrue(testFile.exists());
        assertEquals("Содержимое файла", FileUtils.readFileToString(testFile, "UTF-8"));
    }

    @Test
    public void byteDataTypeTest() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "Содержимое файла".getBytes("Cp1251"));
        
        ds.pushData(rec);
        assertTrue(testFile.exists());
        assertEquals("Содержимое файла", FileUtils.readFileToString(testFile, "Cp1251"));

        svn.setSourceEncoding(Charset.forName("Cp1251"));
        svn.setTargetEncoding(Charset.forName("UTF-8"));

        ds.pushData(rec);
        assertTrue(testFile.exists());
        assertEquals("Содержимое файла", FileUtils.readFileToString(testFile, "UTF-8"));
    }
    
    @Test
    public void streamDataTypeTest() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(
                SvnWriterNode.DATA_FIELD
                , new ByteArrayInputStream("Содержимое файла".getBytes("Cp1251")));

        ds.pushData(rec);
        assertTrue(testFile.exists());
        assertEquals("Содержимое файла", FileUtils.readFileToString(testFile, "Cp1251"));

        svn.setSourceEncoding(Charset.forName("Cp1251"));
        svn.setTargetEncoding(Charset.forName("UTF-8"));

        rec.setValue(
                SvnWriterNode.DATA_FIELD
                , new ByteArrayInputStream("Содержимое файла".getBytes("Cp1251")));
        ds.pushData(rec);
        assertTrue(testFile.exists());
        assertEquals("Содержимое файла", FileUtils.readFileToString(testFile, "UTF-8"));
    }
}