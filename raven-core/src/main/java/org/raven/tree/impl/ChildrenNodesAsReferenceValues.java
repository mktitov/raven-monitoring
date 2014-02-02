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
import org.raven.prj.Project;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.util.NodeUtils;
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
    private final String nodeInProjectPath;

    public ChildrenNodesAsReferenceValues(String attributeType, String nodePath, String nodeInProjectPath)
    {
        this.attributeType = attributeType;
        this.nodePath = nodePath;
        this.nodeInProjectPath = nodeInProjectPath;
    }

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException
    {
        if (!attributeType.equals(attr.getValueHandlerType()))
            return false;
        try {
            if (nodeInProjectPath!=null) {
                Node project = NodeUtils.getParentOfType(attr.getOwner(), Project.class, true);
                if (project!=null) {
                    Node node = project.getNodeByPath(nodeInProjectPath);
                    if (node!=null)
                        addReferenceValues(node, attr.getOwner(), referenceValues, true);
                }
            }
            Node node = (Node) pathResolver.resolvePath(nodePath, null).getReferencedObject();
            addReferenceValues(node, attr.getOwner(), referenceValues, false);
        }
        catch (InvalidPathException ex) {
            logger.error(String.format(
                    "Error creating attribute reference values for value handler type (%s). %s"
                    , attributeType, ex.getMessage()));
        }

        return true;
    }
    
    private void addReferenceValues(Node node, Node attrNode, ReferenceValueCollection referenceValues, boolean relative) 
            throws TooManyReferenceValuesException 
    {
        for (Node child: node.getEffectiveNodes()) {
            String path = relative? pathResolver.getRelativePath(attrNode, node): child.getPath();
            ReferenceValue refVal = new ReferenceValueImpl(path, (relative? "(Project) ":"")+child.getName());
            referenceValues.add(refVal, null);
        }
    }
}
