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
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFileException;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class FileNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws DataFileException, Exception
    {
        FileNode fileNode = new FileNode();
        fileNode.setName("file");
        tree.getRootNode().addAndSaveChildren(fileNode);
        fileNode.getFile().setFilename("testFileName");
        fileNode.getFile().setMimeType("mime/type");
        byte[] inData = new byte[]{1,2,3};
        ByteArrayInputStream is = new ByteArrayInputStream(inData);
        fileNode.getFile().setDataStream(is);
        assertEquals(new Long(3), fileNode.getFile().getFileSize());
        assertTrue(fileNode.start());
        String desc = fileNode.getNodeAttribute("file.filename").getDescription();
        assertNotNull(desc);
        assertFalse(desc.startsWith(DataFileValueHandler.class.getName()));
        desc = fileNode.getNodeAttribute("file.mimeType").getDescription();
        assertNotNull(desc);
        assertFalse(desc.startsWith(DataFileValueHandler.class.getName()));
        desc = fileNode.getNodeAttribute("file.filesize").getDescription();
        assertNotNull(desc);
        assertFalse(desc.startsWith(DataFileValueHandler.class.getName()));

        List<ViewableObject> viewableObjects = fileNode.getViewableObjects(null);
        assertNotNull(viewableObjects);
        assertEquals(1, viewableObjects.size());
        ViewableObject obj = viewableObjects.get(0);
        assertSame(fileNode, obj);
        assertEquals("testFileName", obj.toString());
        assertEquals("mime/type", obj.getMimeType());
        Object data = obj.getData();
        assertNotNull(data);
        assertTrue(data instanceof InputStream);
        byte[] res = IOUtils.toByteArray((InputStream)data);
        assertArrayEquals(inData, res);

    }
}