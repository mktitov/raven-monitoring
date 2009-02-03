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

package org.raven.tree.impl;

import java.util.Collection;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class ChildrenNodesAsReferenceValues implements AttributeReferenceValues
{
    @Service
    private static NodePathResolver pathResolver;

    private Logger logger = LoggerFactory.getLogger(ChildrenNodesAsReferenceValues.class);

    private final String attributeType;
    private final String nodePath;

    public ChildrenNodesAsReferenceValues(String attributeType, String nodePath)
    {
        this.attributeType = attributeType;
        this.nodePath = nodePath;
    }

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException
    {
        if (!attributeType.equals(attr.getValueHandlerType()))
            return false;
        try
        {
            Node node = (Node) pathResolver.resolvePath(nodePath, null).getReferencedObject();
            Collection<Node> childs = node.getSortedChildrens();
            if (childs!=null && childs.size()>0)
                for (Node child: childs)
                {
                    ReferenceValue refVal = new ReferenceValueImpl(
                            pathResolver.getAbsolutePath(child), child.getName());
                    referenceValues.add(refVal, null);
                }
        }
        catch (InvalidPathException ex)
        {
            logger.error(String.format(
                    "Error creating attribute reference values for value handler type (%s). %s"
                    , attributeType, ex.getMessage()));
        }

        return true;
    }
}
