/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.tree.impl;

import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.raven.ds.InputStreamSource;
import org.raven.tree.Node;
import org.raven.tree.NodeBuilder;
import org.raven.tree.store.impl.XMLReader;

/**
 *
 * @author Mikhail Titov
 */
public class XmlNodeBuilder implements NodeBuilder {
    private final String path;
    private final InputStreamSource xmlSource;

    public XmlNodeBuilder(String path, InputStreamSource xmlSource) {
        this.path = path;
        this.xmlSource = xmlSource;
    }

    public String getPath() {
        return path;
    }

    public Node build(Node parent) throws Exception {
        XMLReader reader = new XMLReader();
        InputStream xmlStream = xmlSource.getInputStream();
        try {
            List<Node> nodes = reader.read(parent, xmlSource.getInputStream());
            if (nodes.isEmpty())
                throw new Exception(String.format("None node were created from a template. "
                    + "May be empty template (%s)?", xmlSource.getPath()));
            if (nodes.size()>1)
                throw new Exception("More than one node created from template: "+xmlSource.getPath());
            return nodes.get(0);
        } finally {
            IOUtils.closeQuietly(xmlStream);
        }
    }
}
