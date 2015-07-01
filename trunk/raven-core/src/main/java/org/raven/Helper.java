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

package org.raven;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.PathObject;

/**
 *
 * @author Mikhail Titov
 */
public class Helper 
{
    private Helper(){}
    
    public static boolean checkAttributes(
            Node whoCheck, Collection<NodeAttribute> reqAttrs
            , PathObject testObject, Map<String, NodeAttribute> testNodeAttributes)
    {
        if (reqAttrs!=null && !reqAttrs.isEmpty())
            for (NodeAttribute attr: reqAttrs)
                if (!testNodeAttributes.containsKey(attr.getName()))
                {
                    whoCheck.getLogger().error(
                            String.format(
                                "Node (%s) does not have required attribute (%s)"
                                , testObject.getPath(), attr.getName()));
                    return false;
                }
        return true;
    }

    public static boolean checkAttributes(
            Node whoCheck, Collection<NodeAttribute> reqAttrs, Node testNode)
    {
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        Collection<NodeAttribute> testNodeAttrs = testNode.getNodeAttributes();
        if (testNodeAttrs!=null)
            for (NodeAttribute attr: testNodeAttrs)
                attrs.put(attr.getName(), attr);
        return checkAttributes(whoCheck, reqAttrs, testNode, attrs);
    }
}
