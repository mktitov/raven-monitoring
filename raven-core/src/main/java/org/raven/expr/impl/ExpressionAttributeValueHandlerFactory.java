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

package org.raven.expr.impl;

import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionAttributeValueHandlerFactory implements AttributeValueHandlerFactory
{
    public final static String TYPE = "Expression";
    
    public AttributeValueHandler createValueHandler(NodeAttribute attribute) 
    {
        return new ExpressionAttributeValueHandler(attribute);
    }

    public String getName() 
    {
        return "Expression";
    }

}
