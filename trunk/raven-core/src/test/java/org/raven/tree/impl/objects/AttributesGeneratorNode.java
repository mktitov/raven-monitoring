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

package org.raven.tree.impl.objects;

import java.util.Arrays;
import java.util.Collection;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class AttributesGeneratorNode extends ContainerNode implements AttributesGenerator
{
    public Collection<NodeAttribute> generateAttributes()
    {
        NodeAttribute attr = new NodeAttributeImpl("gAttr", String.class, null, null);
        
        return Arrays.asList(attr);
    }
}
