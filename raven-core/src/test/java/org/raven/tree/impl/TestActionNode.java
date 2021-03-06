/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.tree.impl;

import java.util.Map;
import org.raven.ds.DataContext;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class TestActionNode extends AbstractActionNode 
{
    @Override
    public ViewableObject createActionViewableObject(
            DataContext context, Map<String, Object> additionalBindings)
    {
        return new ActionNodeAction(this, context, additionalBindings, null);
    }

    @Override
    public void prepareActionBindings(
            DataContext context, Map<String, Object> additionalBindings)
    {
    }
}
