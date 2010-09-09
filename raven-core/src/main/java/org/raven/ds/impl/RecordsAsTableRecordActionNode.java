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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
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
            DataContext context, Map<String, Object> additionalBindings)
    {
    }

    @Override
    public ViewableObject createActionViewableObject(DataContext context, Map<String, Object> additionalBindings)
            throws Exception
    {
        return new Action(this, context, additionalBindings, getActionAttributes());
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException("The pull operation not supported by this datasource");
    }

    public Collection<NodeAttribute> generateAttributes() 
    {
        return null;
    }

    protected class Action extends ActionNodeAction
    {
        public static final String DATALIST_BINDING = "dataList";

        public Action(AbstractActionNode actionNode, DataContext context
                , Map<String, Object> additionalBindings, Map<String, NodeAttribute> actionAttributes)
        {
            super(actionNode, context, additionalBindings, actionAttributes);
        }

        @Override
        public Object getData()
        {
            List dataList = new ArrayList();
            bindingSupport.put(DATALIST_BINDING, dataList);

            Object res = super.getData();

            if (dataList.isEmpty() && additionalBindings.containsKey(RecordsAsTableNode.RECORD_BINDING)){
                dataList.add(additionalBindings.get(RecordsAsTableNode.RECORD_BINDING));
                dataList.add(null);
            }
            for (Object data: dataList)
                DataSourceHelper.sendDataToConsumers(RecordsAsTableRecordActionNode.this, data, context);

            return res;
        }
    }
}
