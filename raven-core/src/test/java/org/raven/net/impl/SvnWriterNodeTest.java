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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.PushDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class SvnWriterNodeTest extends RavenCoreTestCase
{
    private SvnWriterNode svn;
    private PushDataSource ds;
    private RecordSchemaNode schema;

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
        dataField.setFieldType(RecordSchemaFieldType.STRING);
        assertTrue(dataField.start());

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        File repFile = new File("target/svnrep");
        if (repFile.exists())
            FileUtils.forceDelete(repFile);
        svn = new SvnWriterNode();
        svn.setName("svn");
        tree.getRootNode().addAndSaveChildren(svn);
        svn.setUsername("test");
        svn.setPassword("test");
        svn.setRepositoryUrl("file://"+repFile.getAbsolutePath());
        svn.setWorkDirectory("target/svn_wc");
        svn.setDataSource(ds);
        svn.setLogLevel(LogLevel.DEBUG);
        assertTrue(svn.start());
    }

    @Test
    public void localRepositoryCreationTest() throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");

        ds.pushData(rec);

//        assertTrue()
    }
}