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

import java.io.InputStream;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.raven.tree.Node;

/**
 * Reads nodes from xml file
 * 
 * @author Mikhail Titov
 */
public class XMLReader {
    public final static String NODES_STACK = "nodes";
    
//    private final String
    
    public List<Node> read(Node owner, InputStream xmlStream) throws Exception {
        Digester digester = new Digester();
        digester.push(owner);
        digester.setValidating(false);
        digester.addRule("nodes", new NodesRule());
        digester.addFactoryCreate("*/node", new NodeCreationFactory());
        digester.addFactoryCreate("*/node/attribute", new AttributeCreationFactory());
        digester.addRule("*/node/attribute/description", new SetDescriptionRule());
        digester.addRule("*/node/attribute/value", new SetValueRule());
        digester.addRule("*/node/attribute", new SaveAttributeRule());
        List<Node> nodes = (List<Node>)digester.parse(xmlStream);
        for (Iterator<Node> it=nodes.iterator(); it.hasNext();)
            if (!owner.equals(it.next().getParent()))
                it.remove();
        return nodes;
    }
    
    private static class NodesRule extends Rule {
        @Override
        public void end(String namespace, String name) throws Exception {
            digester.pop();
            LinkedList<Node> nodes = new LinkedList<Node>();
            Node node;
            try {
                while ( (node=(Node)digester.pop(NODES_STACK)) != null)
                    nodes.add(node);
            } catch (EmptyStackException e) { }
            Collections.reverse(nodes);
            digester.push(nodes);
        }
    }
}
