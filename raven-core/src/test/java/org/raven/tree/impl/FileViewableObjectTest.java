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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.raven.tree.Node;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class FileViewableObjectTest extends Assert
{
    @Test
    public void test() throws IOException
    {
        File file = new File("target/file_vo.txt");
        file.delete();
        FileUtils.writeStringToFile(file, "test");

        Node node = createMock(Node.class);
        replay(node);

        FileViewableObject fileObject = new FileViewableObject(file, node);
        assertEquals("text/plain", fileObject.getMimeType());
        Object data = fileObject.getData();
        assertNotNull(data);
        assertTrue(data instanceof InputStream);
        String content = IOUtils.toString((InputStream)data);
        ((InputStream)data).close();

        verify(node);
    }
}