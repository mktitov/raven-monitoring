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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.log.LogLevel;
import org.raven.sched.impl.QuartzScheduler;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.FileViewableObject;
import org.raven.tree.impl.NodeAttributeImpl;

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

        List<Node> nodes = svnBrowser.getSortedChildrens();
        assertNotNull(nodes);
        assertEquals(2, nodes.size());
        Node testNode = nodes.get(1);
        assertTrue(testNode instanceof SvnDirectoryNode);
        assertStarted(testNode);
        assertEquals("test", testNode.getName());

        nodes = testNode.getSortedChildrens();
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        Node fileNode = nodes.get(0);
        assertStarted(fileNode);
        assertTrue(fileNode instanceof SvnFileNode);
        assertEquals("file.txt", fileNode.getName());

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file2.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);
        nodes = testNode.getSortedChildrens();
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

        List<Node> nodes = svnBrowser.getSortedChildrens();
        assertNotNull(nodes);
        assertEquals(2, nodes.size());
        Node fileNode = nodes.get(1);
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

//        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());
        Node node = svnBrowser.getSortedChildrens().get(1).getSortedChildrens().get(0);
        assertTrue(node instanceof SvnFileNode);
        assertStarted(node);
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
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());
        Node node = svnBrowser.getSortedChildrens().get(1);
        assertTrue(node instanceof SvnFileNode);
        SvnFileRevisionsNode revisions = 
                (SvnFileRevisionsNode) node.getChildren(SvnFileRevisionsNode.NAME);
        assertNotNull(revisions);
        assertStarted(revisions);

        List<Object[]> rows = getRevisionsTable(revisions, null);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nmodified");
        ds.pushData(rec);

        rows = getRevisionsTable(revisions, null);
        assertNotNull(rows);
        assertEquals(2, rows.size());
        assertEquals(2l, rows.get(0)[0]);
        assertEquals(1l, rows.get(1)[0]);

        checkFileContent(rows.get(0)[2], "file content\nmodified", "text/plain");
        checkFileContent(rows.get(1)[2], "file content", "text/plain");

        checkFileContent(rows.get(0)[3]
                , "<html><body><pre>file content\nmodified</pre></body></html>", "text/html");
        checkFileContent(rows.get(1)[3]
                , "<html><body><pre>file content</pre></body></html>", "text/html");

        Object diffObj = rows.get(1)[4];
        assertNotNull(diffObj);
        assertTrue(diffObj instanceof SvnFileDiffViewableObject);
        ViewableObject diff = (ViewableObject) diffObj;
        assertEquals("text/x-diff", diff.getMimeType());
        Object data = diff.getData();
        assertNotNull(data);
        assertTrue(data instanceof byte[]);
        String diffRes = new String((byte[])data);
        assertFalse(diffRes.isEmpty());

        diffObj = rows.get(1)[5];
        assertNotNull(diffObj);
        assertTrue(diffObj instanceof SvnFileDiffViewableObject);
        diff = (ViewableObject) diffObj;
        assertEquals("text/html", diff.getMimeType());
        data = diff.getData();
        assertNotNull(data);
        assertTrue(data instanceof byte[]);
        diffRes = new String((byte[])data);
        assertFalse(diffRes.isEmpty());
        assertTrue(diffRes.startsWith("<html><body>"));
        assertTrue(diffRes.endsWith("</body></html>"));
    }

    @Test
    public void revisionsNodeRefreshAttributesTest1() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2\nline3");
        ds.pushData(rec);
        
        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());
        
        Node node = svnBrowser.getSortedChildrens().get(1);
        assertTrue(node instanceof SvnFileNode);
        SvnFileRevisionsNode revisions =
                (SvnFileRevisionsNode) node.getChildren(SvnFileRevisionsNode.NAME);
        assertNotNull(revisions);
        assertStarted(revisions);

        NodeAttribute attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.FROM_REVISION_ATTR, Long.class, 2, null);
        attr.setOwner(revisions);
        attr.init();
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        attrs.put(attr.getName(), attr);

        List<Object[]> rows = getRevisionsTable(revisions, null);
        assertNotNull(rows);
        assertEquals(3, rows.size());
        rows = getRevisionsTable(revisions, attrs);
        assertNotNull(rows);
        assertEquals(2, rows.size());
    }

    @Test
    public void revisionsNodeRefreshAttributesTest2() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2\nline3");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());

        Node node = svnBrowser.getSortedChildrens().get(1);
        assertTrue(node instanceof SvnFileNode);
        SvnFileRevisionsNode revisions =
                (SvnFileRevisionsNode) node.getChildren(SvnFileRevisionsNode.NAME);
        assertNotNull(revisions);
        assertStarted(revisions);

        NodeAttribute attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.TO_REVISION_ATTR, Long.class, 2, null);
        attr.setOwner(revisions);
        attr.init();
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        attrs.put(attr.getName(), attr);

        List<Object[]> rows = getRevisionsTable(revisions, null);
        assertNotNull(rows);
        assertEquals(3, rows.size());
        rows = getRevisionsTable(revisions, attrs);
        assertNotNull(rows);
        assertEquals(2, rows.size());
    }

    @Test
    public void revisionsNodeRefreshAttributesTest3() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2\nline3");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());

        Node node = svnBrowser.getSortedChildrens().get(1);
        assertTrue(node instanceof SvnFileNode);
        SvnFileRevisionsNode revisions =
                (SvnFileRevisionsNode) node.getChildren(SvnFileRevisionsNode.NAME);
        assertNotNull(revisions);
        assertStarted(revisions);

        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        NodeAttribute attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.TO_REVISION_ATTR, Long.class, 2, null);
        attr.setOwner(revisions);
        attr.init();
        attrs.put(attr.getName(), attr);
        attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.FROM_REVISION_ATTR, Long.class, 2, null);
        attr.setOwner(revisions);
        attr.init();
        attrs.put(attr.getName(), attr);

        List<Object[]> rows = getRevisionsTable(revisions, null);
        assertNotNull(rows);
        assertEquals(3, rows.size());
        rows = getRevisionsTable(revisions, attrs);
        assertNotNull(rows);
        assertEquals(1, rows.size());
    }

    @Test
    public void directoryRevisionsTest() throws Exception
    {
        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2");
        ds.pushData(rec);

        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file2.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content\nline2");
        ds.pushData(rec);

        svnBrowser.setInitialPath("test");
        assertTrue(svnBrowser.start());

        Node node = svnBrowser.getSortedChildrens().get(0);
        assertNotNull(node);
        assertTrue(node instanceof SvnFileRevisionsNode);
        SvnFileRevisionsNode revisionsNode = (SvnFileRevisionsNode) node;
        assertFalse(revisionsNode.isRevisionsForFile());

        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        NodeAttribute attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.TO_REVISION_ATTR, Long.class, 1, null);
        attr.setOwner(revisionsNode);
        attr.init();
        attrs.put(attr.getName(), attr);
        attr = new NodeAttributeImpl(
                SvnFileRevisionsNode.FROM_REVISION_ATTR, Long.class, 3, null);
        attr.setOwner(revisionsNode);
        attr.init();
        attrs.put(attr.getName(), attr);
        List<Object[]> rows = getRevisionsTable(revisionsNode, attrs);
        assertNotNull(rows);
        assertEquals(3, rows.size());
    }

    @Test
    public void childrenExpirationTest() throws Exception
    {
        QuartzScheduler scheduler = new QuartzScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        Record rec = schema.createRecord();
        rec.setValue(SvnWriterNode.PATH_FIELD, "test/file.txt");
        rec.setValue(SvnWriterNode.DATA_FIELD, "file content");
        ds.pushData(rec);

        assertTrue(svnBrowser.start());

        svnBrowser.getChildrens();
        Node testNode = svnBrowser.getChildren("test");
        assertNotNull(testNode);

        svnBrowser.setChildrenExpirationInterval(1l);
        TimeUnit.SECONDS.sleep(2);
        svnBrowser.executeScheduledJob();
        Node testNode2 = svnBrowser.getChildren("test");
        assertNotSame(testNode2, testNode);
    }

    private void checkFileContent(Object contentObj, String expectContent, String contentType)
    {
        assertNotNull(contentObj);
        assertTrue(contentObj instanceof SvnFileContentVieableObject);
        ViewableObject content = (ViewableObject) contentObj;
        assertEquals(contentType, content.getMimeType());
        Object data = content.getData();
        assertNotNull(data);
        assertTrue(data instanceof byte[]);
        byte[] buf = (byte[]) data;
        assertEquals(expectContent, new String(buf));
    }

    private List<Object[]> getRevisionsTable(
            SvnFileRevisionsNode revisionsNode, Map<String, NodeAttribute> refreshAttributes)
        throws Exception
    {
        Collection<ViewableObject> objects = revisionsNode.getViewableObjects(refreshAttributes);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        ViewableObject object = objects.iterator().next();
        assertNotNull(object);
        Object data = object.getData();
        assertNotNull(data);
        assertTrue(data instanceof Table);
        return RavenUtils.tableAsList((Table)data);
    }
}