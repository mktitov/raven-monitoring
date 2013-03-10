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

package org.raven.tree.store.impl;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreationFactory;
import org.raven.tree.Node;
import org.xml.sax.Attributes;

/**
 * Factory thats create {@link Node} from xml
 * @author Mikhail Titov
 */
public class NodeCreationFactory implements ObjectCreationFactory, XMLConsts
{
    private Digester digester;

    public Object createObject(Attributes attributes) throws Exception
    {
        String nodeClass = attributes.getValue(CLASS_ATTRIBUTE);
        String nodeName = attributes.getValue(NAME_ATTRIBUTE);
        Node parent = (Node) digester.peek();
        Node node = parent.getNode(nodeName);
        if (node==null){
            node = (Node) Class.forName(nodeClass).newInstance();
            node.setName(nodeName);
            parent.addAndSaveChildren(node);
        } else if (!node.getClass().getName().equals(nodeClass))
            throw new Exception(String.format(
                    "Node (%s) already exists and has different type. Expected (%s) but was (%s)"
                    , node.getPath(), nodeClass, node.getClass().getName()));
        digester.push(XMLReader.NODES_STACK, node);
        return node;
    }

    public Digester getDigester()
    {
        return digester;
    }

    public void setDigester(Digester digester)
    {
        this.digester = digester;
    }
}
