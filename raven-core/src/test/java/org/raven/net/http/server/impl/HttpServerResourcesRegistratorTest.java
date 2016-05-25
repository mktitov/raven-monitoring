/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import java.util.Locale;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.net.http.server.HttpConsts;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.FileNode;
import org.raven.tree.impl.PropertiesNode;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerResourcesRegistratorTest extends RavenCoreTestCase {
    
    @Test
    public void test() throws Exception {
        ResourceManager resourceManager = registry.getService(ResourceManager.class);
        assertNotNull(resourceManager);
        checkRes(resourceManager, "pages/error_page", "error_page_en.html", "en", true);
        checkRes(resourceManager, "messages/messages", "messages_en.properties", "en", false);
        checkRes(resourceManager, "messages/messages", "messages_ru.properties", "ru", false);
    }
    
    public void checkRes(ResourceManager resourceManager, String path, String filename, String locale, boolean isHtml) throws Exception {
        Node res = resourceManager.getResource(HttpConsts.RESOURCES_BASE+path, new Locale(locale));
        assertNotNull(res);
        DataFile file;
        if (isHtml) {
            assertTrue(res instanceof FileNode);
            FileNode htmlRes = (FileNode) res;
            file = htmlRes.getFile();
            assertEquals("text/html", file.getMimeType());
        } else {
            assertTrue(res instanceof PropertiesNode);
            PropertiesNode propsRes = (PropertiesNode) res;
            file = propsRes.getPropertiesFile();
            assertEquals("text/plain", file.getMimeType());
            Properties props = propsRes.getProperties();
            assertNotNull(props.getProperty("pageTitle"));
        }
        assertNotNull(file);
        assertEquals(filename, file.getFilename());
        assertNotNull(file.getFileSize());
        assertTrue(file.getFileSize()>0);
    }
    
}
