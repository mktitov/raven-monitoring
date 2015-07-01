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

package org.raven.tree.store.impl;

import org.apache.commons.digester.Rule;
import org.raven.tree.NodeAttribute;
import org.weda.internal.impl.MessageComposer;

/**
 *
 * @author Mikhail Titov
 */
public class SetDescriptionRule extends Rule
{
    @Override
    public void body(String namespace, String name, String text) throws Exception
    {
        if (text!=null)
        {
            NodeAttribute attr = (NodeAttribute) digester.peek();
            MessageComposer composer = StoreUtils.stringToMessageComposer(text);
            attr.setDescriptionContainer(composer);
        }
    }
}
