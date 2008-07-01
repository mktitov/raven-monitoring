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

package org.raven.tree.impl;

import org.junit.Assert;
import org.junit.Test;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;
import org.raven.tree.PathElement;
import org.raven.tree.PathInfo;
import org.raven.tree.Tree;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class NodePathResolverImplTest extends Assert
{
    
    @Test
    public void absoluteReferenceTest() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node rootNode = createMock(Node.class);
        Node childNode = createMock(Node.class);
        
        expect(tree.getRootNode()).andReturn(rootNode);
        expect(rootNode.getChildren("child node")).andReturn(childNode);
        expect(childNode.getName()).andReturn("child node");
        replay(tree, rootNode, childNode);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("/child node", null);
        
        assertSame(childNode, pathInfo.getReferencedObject());
        PathElement[] elements = pathInfo.getPathElements();
        assertNotNull(elements);
        assertEquals(2, elements.length);
        assertEquals("", elements[0].getElement());
        assertEquals("\"child node\"", elements[1].getElement());
        
        verify(tree, rootNode, childNode);
    }
    
    @Test(expected=InvalidPathException.class)
    public void nullCurrentNodeOnRelativePath() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        replay(tree, node);
        
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("node", null);
        
        verify(tree, node);
    }
    
    @Test
    public void relativeReferenceTest() throws InvalidPathException 
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        Node childNode = createMock(Node.class);
        
        expect(node.getChildren("child")).andReturn(childNode);
        replay(tree, node, childNode);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("child", node);
        
        verify(tree, node, childNode);
    }
    
    @Test
    public void selfReferenceTest() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        Node childNode = createMock(Node.class);
        
        expect(node.getChildren("child")).andReturn(childNode);
        replay(tree, node, childNode);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("./child", node);
        
        verify(tree, node, childNode);
    }
    
    @Test
    public void parentReferenceTest() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        Node childNode = createMock(Node.class);
        
        expect(childNode.getParent()).andReturn(node);
        expect(node.getChildren("child")).andReturn(childNode);
        replay(tree, node, childNode);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("../child", childNode);
        
        verify(tree, node, childNode);
    }
    
    @Test
    public void quoteTest() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        Node childNode = createMock(Node.class);
        
        expect(childNode.getParent()).andReturn(node);
        expect(node.getChildren("child")).andReturn(childNode);
        replay(tree, node, childNode);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath(
                String.format("../%1$schild%1$s", NodePathResolver.QUOTE)
                , childNode);
        
        verify(tree, node, childNode);
    }
    
    @Test(expected=InvalidPathException.class)
    public void invalidParentTest() throws InvalidPathException
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        
        expect(node.getParent()).andReturn(null);
        replay(tree, node);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("../", node);
        
        verify(tree, node);
    }
    
    @Test(expected=InvalidPathException.class)
    public void invalidChildTest() throws InvalidPathException 
    {
        Tree tree = createMock(Tree.class);
        Node node = createMock(Node.class);
        
        expect(node.getChildren("child")).andReturn(null);
        expect(node.getPath()).andReturn("node");
        replay(tree, node);
        
        TreeImpl.INSTANCE = tree;
        NodePathResolverImpl pathResolver = new NodePathResolverImpl();
        PathInfo pathInfo = pathResolver.resolvePath("child", node);
        
        verify(tree, node);
    }
}
