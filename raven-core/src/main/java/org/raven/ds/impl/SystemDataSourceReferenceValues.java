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

package org.raven.ds.impl;

import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.SystemNode;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;

/**
 * Returns the list of the system data sources for attributes with 
 * {@link org.raven.tree.NodeAttribute#getValueHandlerType() value handler type} equals to
 * {@link SystemDataSourceValueHandlerFactory#TYPE}
 * @author Mikhail Titov
 */
public class SystemDataSourceReferenceValues implements AttributeReferenceValues
{
    @Service
    private static Tree tree;
    @Service 
    private static NodePathResolver pathResolver;

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues) 
            throws TooManyReferenceValuesException
    {
        if (!SystemDataSourceValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
            return false;
        
        DataSourcesNode dataSources = 
                (DataSourcesNode) 
                tree.getRootNode().getChildren(SystemNode.NAME).getChildren(DataSourcesNode.NAME);
        if (dataSources.getChildrens()!=null)
        {
            for (Node node: dataSources.getSortedChildrens())
            {
                ReferenceValue referenceValue = 
                        new ReferenceValueImpl(pathResolver.getAbsolutePath(node), node.getName());
                referenceValues.add(referenceValue, null);
            }
        }
        return true;
    }
}
