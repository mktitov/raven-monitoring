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

import eu.medsea.mimeutil.MimeUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.FileViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class SvnBrowserNodeTest extends RavenCoreTestCase
{
    private SvnWriterNode svnWriter;
    private SvnBrowserNode svnBrowser;
    private PushDataSource ds;
    private File repFile;
    private File wcFile;
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

        svnWriter = new SvnWriterNode();
        svnWriter.setName("svnWriter");
        tree.getRootNode().addAndSaveChildren(svnWriter);
        svnWriter.setUsername("test");
        svnWriter.setPassword("test");
        svnWriter.setRepositoryUrl("file://"+repFile.getAbsolutePath());
        svnWriter.setWorkDirectory(wcFile.getAbsolutePath());
        svnWriter.setDataSource(ds);
        svnWriter.setLogLevel(LogLevel.DEBUG);
        assertTrue(svnWriter.start());

        svnBrowser = new SvnBrowserNode();
        svnBrowser.setName("svnBrowser");
        tree.getRootNode().addAndSaveChildren(svnBrowser);
        svnBrowser.setUsername("test");
        svnBrowser.setPassword("test");
        svnBrowser.setRepositoryUrl("file://"+repFile.getAbsolutePath());
        svnBrowser.setWorkDirectory(wcFile.getAbsolutePath());
        svnBrowser.setLogLevel(LogLevel.DEBUG);
//        assertTrue(svnBrowser.start());
    }

    @Test
    public void filesTreeTest() throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        assertTrue(svnBrowser.start());

        Collection<Node> nodes = svnBrowser.getChildrens();
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        Node testNode = nodes.iterator().next();
        assertTrue(testNode instanceof SvnDirectoryNode);
        assertStarted(testNode);
        assertEquals("test", testNode.getName());

        nodes = testNode.getChildrens();
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        Node fileNode = nodes.iterator().next();
        assertStarted(fileNode);
        assertTrue(fileNode instanceof SvnFileNode);
        assertEquals("file.txt", fileNode.getName());

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file2.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);
        nodes = testNode.getChildrens();
        assertNotNull(nodes);
        assertEquals(2, nodes.size());
        List<Node> nodesList = new ArrayList<Node>(nodes);
        assertStarted(nodesList.get(1));
        assertEquals("file2.txt", nodesList.get(1).getName());
    }

    @Test
    public void initialPathTest() throws RecordException
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());

        Collection<Node> nodes = svnBrowser.getChildrens();
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        Node fileNode = nodes.iterator().next();
        assertStarted(fileNode);
        assertTrue(fileNode instanceof SvnFileNode);
        assertEquals("file.txt", fileNode.getName());
    }

    @Test
    public void fileContentNodeTest() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());
        Node node = svnBrowser.getChildrens().iterator().next();
        assertTrue(node instanceof SvnFileNode);
        SvnFileContentNode content = (SvnFileContentNode) node.getChildren(SvnFileContentNode.NAME);
        assertNotNull(content);
        assertStarted(content);

        Collection<ViewableObject> objects = content.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertTrue(object instanceof FileViewableObject);
        Object data = object.getData();
        assertNotNull(data);
        assertTrue(data instanceof InputStream);
        String text = IOUtils.toString((InputStream)data);
        assertEquals("file content", text);
    }

    @Test
    public void revisionsNodeTest() throws Exception
    {
        
    }
}