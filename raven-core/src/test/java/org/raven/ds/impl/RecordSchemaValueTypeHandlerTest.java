/*
 * Copyright 2014 Mikhail Titov.
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
package org.raven.ds.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.prj.impl.ProjectNode;
import org.raven.prj.impl.UserInterfaceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaValueTypeHandlerTest extends RavenCoreTestCase {
    private ProjectNode project;
    private RecordSchemaNode schema;
    private NodePathResolver pathResolver;
    
    @Before
    public void prepare() {
        pathResolver = registry.getService(NodePathResolver.class);
        assertNotNull(pathResolver);
        
        project = new ProjectNode();
        project.setName("test");
        tree.getProjectsNode().addAndSaveChildren(project);
        assertTrue(project.start());
        
        Node schemas = project.getNodeByPath("Schemas/Record schemas");
        assertNotNull(schemas);
        
        schema = new RecordSchemaNode();
        schema.setName("test_schema");
        schemas.addAndSaveChildren(schema);
        assertTrue(schema.start());
    }
    
    @Test
    public void moveReferencedNodeTest() throws Exception {
        Node nodes = project.getNode(UserInterfaceNode.NAME);
        assertNotNull(nodes);
        
        Node refNode = new BaseNode("test");
        nodes.addAndSaveChildren(refNode);
        NodeAttributeImpl attr = new NodeAttributeImpl("schema", RecordSchemaNode.class, null, null);
        attr.setOwner(refNode);
        attr.setValueHandlerType(RecordSchemaValueTypeHandlerFactory.TYPE);
        attr.init();
        refNode.addAttr(attr);
        attr.setValue(pathResolver.getRelativePath(refNode, schema));
        attr.save();        
        assertSame(schema, attr.getRealValue());
        assertEquals(pathResolver.getRelativePath(refNode, schema), attr.getRawValue());
        
        ContainerNode container = new ContainerNode("container");
        nodes.addAndSaveChildren(container);
        assertTrue(container.start());
        tree.move(refNode, container, null);
        assertSame(schema, attr.getRealValue());        
        assertEquals(pathResolver.getRelativePath(refNode, schema), attr.getRawValue());
        
        String path = pathResolver.getRelativePath(refNode, schema);
        String nodePath = refNode.getPath();        
        tree.reloadTree();
        refNode = tree.getNode(nodePath);
        assertEquals(path, refNode.getAttr("schema").getRawValue());
    }
}
