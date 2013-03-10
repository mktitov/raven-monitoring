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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.raven.ds.InputStreamSource;
import org.raven.tree.NodeBuilder;
import org.raven.tree.NodeBuildersProvider;
import org.raven.tree.ResourceDescriptor;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractXmlNodeBuildersProvider implements NodeBuildersProvider {
    private final Collection<ResourceDescriptor> resources;
    private final String basePath;

    public AbstractXmlNodeBuildersProvider(Collection<ResourceDescriptor> resources, String basePath) {
        this.resources = resources;
        this.basePath = basePath;
    }
    
    public Collection<NodeBuilder> getNodeBuilders() {
        if (resources.isEmpty())
            return Collections.EMPTY_LIST;
        List<NodeBuilder> builders = new ArrayList<NodeBuilder>(resources.size());
        for (ResourceDescriptor res: resources) 
            builders.add(new XmlNodeBuilder(
                res.getResourcePath(), 
                new ResourceInputStreamSource(getClass(), res.getResourceBase()+res.getResourcePath())));
        return builders;
    }

    public String getBasePath() {
        return basePath;
    }

    private class ResourceInputStreamSource implements InputStreamSource {
        private final Class resourceLoader;
        private final String path;

        public ResourceInputStreamSource(Class resourceLoader, String path) {
            this.resourceLoader = resourceLoader;
            this.path = path;
        }

        public InputStream getInputStream() throws Exception {
            return resourceLoader.getResourceAsStream(path);
        }

        public String getPath() {
            return path;
        }
    }
}
