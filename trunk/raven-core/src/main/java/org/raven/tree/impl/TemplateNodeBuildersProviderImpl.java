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

import java.util.Collection;
import org.raven.template.impl.TemplatesNode;
import org.raven.tree.Node;
import org.raven.tree.ResourceDescriptor;
import org.raven.tree.TemplateNodeBuildersProvider;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateNodeBuildersProviderImpl extends AbstractXmlNodeBuildersProvider implements TemplateNodeBuildersProvider {
    private final static String BASE_PATH = Node.NODE_SEPARATOR+TemplatesNode.NAME+Node.NODE_SEPARATOR;

    public TemplateNodeBuildersProviderImpl(Collection<ResourceDescriptor> resources) {
        super(resources, BASE_PATH);
    }

    public Node createPathNode() {
        return new GroupNode();
    }
}
