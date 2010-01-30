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

package org.raven.ds.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionNode;
import org.raven.tree.impl.ActionNodeAction;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordsAsTableNode.class)
public class RecordsAsTableRecordActionNode extends AbstractActionNode implements DataSource
{
    @Override
    public void prepareActionBindings(
            Map<String, NodeAttribute> refreshAttributes, Map<String, Object> additionalBindings)
    {
    }

    @Override
    public ViewableObject createActionViewableObject(
            Map<String, NodeAttribute> refreshAttributes, Map<String, Object> additionalBindings)
    {
        return new Action(this, refreshAttributes, additionalBindings);
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("The pull operation not supported by this datasource");
    }

    public Collection<NodeAttribute> generateAttributes() 
    {
        return null;
    }

    private class Action extends ActionNodeAction
    {
        public Action(
                AbstractActionNode actionNode, Map<String, NodeAttribute> refreshAttributes
                , Map<String, Object> additionalBindings)
        {
            super(actionNode, refreshAttributes, additionalBindings);
        }

        @Override
        public Object getData()
        {
            Object res = super.getData();
            DataSourceHelper.sendDataToConsumers(
                    RecordsAsTableRecordActionNode.this,
                    additionalBindings.get(RecordsAsTableNode.RECORD_BINDING));
            return res;
        }
    }
}
