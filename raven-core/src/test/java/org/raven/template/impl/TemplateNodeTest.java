/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.template.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateNodeTest extends RavenCoreTestCase
{
    @Test
    public void init() throws InvalidPathException 
    {
        store.removeNodes();
        tree.reloadTree();
        
        TemplateNode template = new TemplateNode();
        template.setName("template");
        tree.getRootNode().addChildren(template);
        tree.saveNode(template);
        template.init();
        
        Node varNode = template.getVariablesNode();
        assertNotNull(varNode);
        Node entryNode = template.getEntryNode();
        assertNotNull(entryNode);
        
        tree.reloadTree();
        
        template = (TemplateNode) tree.getNode(template.getPath());
        assertNotNull(template);
        assertEquals(varNode, template.getVariablesNode());
        assertEquals(entryNode, template.getEntryNode());
    }
}
