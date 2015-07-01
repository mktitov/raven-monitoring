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

package org.raven.template.impl;

import org.raven.annotations.NodeClass;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.GroupNode;

/**
 *
 * @author Mikhail Titov
 */
//@Description("The root for template")
@NodeClass(parentNode=org.raven.tree.impl.InvisibleNode.class, childNodes={GroupNode.class})
public class TemplatesNode extends BaseNode {
    public static String NAME = "Templates";
    
    public TemplatesNode() {
        setName(NAME);
    }
}
