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

import java.io.InputStream;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import static  org.raven.net.http.server.HttpConsts.*;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.ResourceRegistrator;
import org.raven.tree.impl.FileNode;
import org.raven.tree.impl.PropertiesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerResourcesRegistrator implements ResourceRegistrator {
    public static final String RES_BASE = "/org/raven/net/http/server/";
    private final static String PROPERTIES_EXT = ".properties";
    private final static Logger logger = LoggerFactory.getLogger(HttpServerResourcesRegistrator.class);
    private final static String[] pages = new String[] {
        "pages/error_page_en"
    };
    private final static String[] messages = new String[] {
        "messages/messages_ru",
        "messages/messages_en"
    };

    @Override
    public void registerResources(ResourceManager resourceManager) {
        createResourcesFromList(resourceManager, pages, ".html", "text/html");
        createResourcesFromList(resourceManager, messages, PROPERTIES_EXT, "text/plain");
    }
    
    private void createResourcesFromList(ResourceManager resourceManager, String[] names, String ext, String mimeType) {
        for (String name: names) 
                createResourceIfNeed(resourceManager, name, ext, mimeType);
    }

    private void createResourceIfNeed(ResourceManager resourceManager, String resName, String ext, String mimeType)  {
        try {
            ResInfo resInfo = new ResInfo(resName, ext);
            if (!resourceManager.containsResource(resInfo.ravenResName, resInfo.locale)) {
                InputStream is = this.getClass().getResourceAsStream(resInfo.resPath);
                try {
                    if (is==null)
                        throw new Exception(String.format("Resource (%s) does not exists", resInfo.resPath));
                    Node node = resInfo.createNode();
                    if (!resourceManager.registerResource(resInfo.ravenResName, resInfo.locale, node))
                        throw new Exception("Resource manager can't register resource");
                    DataFile file = resInfo.getDataFile();
                    file.setFilename(resInfo.fileName);
                    file.setDataStream(is);
                    file.setMimeType(mimeType);
                    if (logger.isDebugEnabled())
                        logger.debug("Registered new resource ({})", resInfo.toString());
                } finally  {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (Throwable e) {
            if (logger.isErrorEnabled())
                logger.error(String.format("Error registering resource (%s)", RESOURCES_BASE+resName), e);            
        }
    }
    
    private class ResInfo {
        private final Locale locale;
        private final String resPath;
        private final String ravenResName;
        private final String fileName;             
        private final boolean isHtml;
        private Node node;

        public ResInfo(String name, String ext) throws Exception {
            this.isHtml = !PROPERTIES_EXT.equals(ext);
            String[] elems = name.split("_");
            if (elems==null || elems.length<=1)
                throw new Exception("Can't detect resource locale");
            String nameWoLocale = name.substring(0, name.lastIndexOf("_"));
            this.locale = new Locale(elems[elems.length-1]);
            resPath = RES_BASE+name+ext; //or may be gsp?
            ravenResName = RESOURCES_BASE+nameWoLocale;
            elems = name.split("/");
            fileName = elems[elems.length-1]+ext;
        }
        
        private Node createNode() {
            return node = isHtml? new FileNode() : new PropertiesNode();
        }
        
        private DataFile getDataFile() {
            return isHtml? ((FileNode)node).getFile() : ((PropertiesNode)node).getPropertiesFile();
        }

        @Override
        public String toString() {
            return String.format("path in library: %s; path in the tree: %s", resPath, node.getPath());
        }
    }
}
