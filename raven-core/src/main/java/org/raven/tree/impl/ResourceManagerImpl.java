/*
 * Copyright 2012 Mikhail Titov.
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

import java.util.Collection;
import java.util.Locale;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.tree.*;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceManagerImpl implements ResourceManager, TreeListener {
    
    private final Collection<ResourceRegistrator> resourceRegistrators;
    private ResourcesNode resourcesNode;

    public ResourceManagerImpl(Collection<ResourceRegistrator> resourceRegistrators) {
        this.resourceRegistrators = resourceRegistrators;
    }
    
    public boolean containsResource(String key, Locale locale) {
        return resourcesNode.getChildrenByPath(getPath(key, locale))!=null;
    }

    public boolean registerResource(String key, Locale locale, Node resource) {
        StrTokenizer tokenizer = new StrTokenizer(key, Node.NODE_SEPARATOR, NodePathResolver.QUOTE);
        Node node = resourcesNode;
        while (tokenizer.hasNext()) {
            String nodeName = tokenizer.nextToken();
            Node child = node.getChildren(nodeName);
            if (child==null) {
                child = new ContainerNode(nodeName);
                node.addAndSaveChildren(child);
                node.start();
            } 
            node = child;
        }
        Locale loc = getLocale(locale);
        if (node.getChildren(loc.toString())==null) {
            resource.setName(loc.toString());
            node.addAndSaveChildren(resource);
            return true;
        } else
            return false;
    }

    public Node getResource(String key, Locale locale) {
        return resourcesNode.getChildrenByPath(getPath(key, locale));
    }

    public void treeReloaded(Tree tree) {
        resourcesNode = (ResourcesNode) tree.getRootNode().getChildren(ResourcesNode.NAME);
        for (ResourceRegistrator registrator: resourceRegistrators)
            registrator.registerResources(this);
    }
    
    private String getPath(String key, Locale locale) {
        return key+Node.NODE_SEPARATOR+getLocale(locale).toString();
    }
    
    private Locale getLocale(Locale locale) {
        if (locale==null)
            locale = resourcesNode.getDefaultLocale();
        if (locale==null)
            locale = Locale.getDefault();
        return locale;
    }
}
