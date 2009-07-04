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

package org.raven.tree.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.objects.NodeWithDataFileAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class DataFileAttributeValueHandlerTest extends RavenCoreTestCase
{
    private NodeWithDataFileAttribute node;

    @Before
    public void prepareTest()
    {
        node = new NodeWithDataFileAttribute();
        node.setName("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
    }
    
    @Test
    public void nullDataFileTest() throws DataFileException
    {
        DataFile dataFile = node.getDataFile();
        assertNotNull(dataFile);

        assertNull(dataFile.getDataStream());
        NodeAttribute attr = node.getNodeAttribute("dataFile.filename");
        assertNotNull(attr);
        assertEquals(String.class, attr.getType());

        attr = node.getNodeAttribute("dataFile.mimeType");
        assertNotNull(attr);
        assertEquals(String.class, attr.getType());
    }

    @Test
    public void setFilePropertiesTest() throws Exception
    {
        checkFileProperties(node);

        tree.reloadTree();

        node = (NodeWithDataFileAttribute) tree.getNode(node.getPath());

        checkFileProperties(node);
    }

    private void checkFileProperties(NodeWithDataFileAttribute node) throws Exception
    {
        DataFile datafile = node.getDataFile();
        datafile.setFilename("file name");
        assertEquals("file name", datafile.getFilename());
        assertEquals("file name", node.getNodeAttribute("dataFile.filename").getValue());
        datafile.setMimeType("mime type");
        assertEquals("mime type", datafile.getMimeType());
        assertEquals("mime type", node.getNodeAttribute("dataFile.mimeType").getValue());

        byte[] oArr = new byte[]{1,2,3};
        ByteArrayInputStream data = new ByteArrayInputStream(oArr);
        datafile.setDataStream(data);

        InputStream rs = datafile.getDataStream();
        assertNotNull(rs);
        byte[] rArr = IOUtils.toByteArray(rs);
        assertArrayEquals(oArr, rArr);
    }
}