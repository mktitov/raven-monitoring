/*
 *  Copyright 2011 Mikhail Titov.
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

import org.raven.annotations.NodeClass;
import org.raven.ds.ValueValidator;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class RequiredValueValidatorNode extends BaseNode implements ValueValidator
{
    @Message
    private static String requiredMessage;

    public String validate(Object value) {
        return value==null? requiredMessage : null;
    }
    
    public static RequiredValueValidatorNode create(Node owner, String name) {
        if (owner.getNode(name)!=null)
            return null;
        RequiredValueValidatorNode validator = new RequiredValueValidatorNode();
        validator.setName(name);
        owner.addAndSaveChildren(validator);
        validator.start();
        return validator;
    }
}
