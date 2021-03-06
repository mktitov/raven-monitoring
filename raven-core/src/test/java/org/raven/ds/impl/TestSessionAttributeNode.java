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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import org.raven.ds.DataContext;
import org.raven.ds.SessionAttributeGenerator;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestSessionAttributeNode extends BaseNode implements SessionAttributeGenerator
{
    private Collection<NodeAttribute> consumerAttrs = new ArrayList<NodeAttribute>();

    public void addConsumerAttribute(NodeAttribute attr)
    {
        consumerAttrs.add(attr);
    }

    public Class getAttributeType()
    {
        return Integer.class;
    }

    public void fillConsumerAttributes(Collection<NodeAttribute> attributes)
    {
        attributes.addAll(consumerAttrs);
    }

    public Object getFieldValue(DataContext context)
    {
        return 10;
    }
}
