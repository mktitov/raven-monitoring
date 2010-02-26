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

import java.util.Collection;
import javax.script.Bindings;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractRecordFieldExtension extends BaseNode
{
    public Object prepareValue(Object value, Bindings bindings)
    {
        Collection<Node> childs = getChildrens();
        
        if (childs==null || childs.size()==0)
            return value;

        for (Node child: childs)
            if (child instanceof ValuePrepareRecordFieldExtension && Status.STARTED.equals(child.getStatus()))
                return ((ValuePrepareRecordFieldExtension)child).prepareValue(value, bindings);

        return value;
    }
}
