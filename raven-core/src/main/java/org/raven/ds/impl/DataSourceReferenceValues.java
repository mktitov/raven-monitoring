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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.raven.ds.DataSource;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.SystemNode;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceReferenceValues implements AttributeReferenceValues
{
    @Service
    private static Tree tree;
    @Service
    private static TypeConverter converter;

    public List<String> getReferenceValues(NodeAttribute attr)
    {
        DataSourcesNode dataSources = 
                (DataSourcesNode) 
                tree.getRootNode().getChildren(SystemNode.NAME).getChildren(DataSourcesNode.NAME);
        if (dataSources.getChildrens()!=null)
        {
            List<String> result = new ArrayList<String>();
            for (Node node: dataSources.getChildrens())
                if (attr.getType().isAssignableFrom(node.getClass()))
                {
                    String nodePath = converter.convert(String.class, node, null);
                    result.add(nodePath);
                }
            
            Collections.sort(result);
            
            Node node = attr.getOwner();
            while ( (node = node.getParent())!=null )
            {
                if (node instanceof DataSource)
                {
                    String nodePath = converter.convert(String.class, node, null);
                    result.add(nodePath);
                }
            }
            
            return result;
        } 
        else
            return null;
    }

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues) 
            throws TooManyReferenceValuesException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
