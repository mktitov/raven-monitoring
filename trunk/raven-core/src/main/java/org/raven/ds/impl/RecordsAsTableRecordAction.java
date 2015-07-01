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
import java.util.List;
import java.util.Map;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AbstractActionNode;
import org.raven.tree.impl.ActionNodeAction;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsTableRecordAction extends ActionNodeAction
{
    public static final String DATALIST_BINDING = "dataList";
    public final DataSource owner;

    public RecordsAsTableRecordAction(AbstractActionNode actionNode, DataContext context
            , Map<String, Object> additionalBindings, Map<String, NodeAttribute> actionAttributes
            , DataSource owner)
    {
        super(actionNode, context, additionalBindings, actionAttributes);
        this.owner = owner;
    }

    @Override
    public Object getData()
    {
        List dataList = new ArrayList();
        bindingSupport.put(DATALIST_BINDING, dataList);

        Object res = super.getData();

        if (dataList.isEmpty() && additionalBindings.containsKey(RecordsAsTableNode.RECORD_BINDING)){
            Record rec = (Record) additionalBindings.get(RecordsAsTableNode.RECORD_BINDING);
//            RecordValidationErrors errors = rec.validate();
//            if (errors!=null){
//                StringBuilder msg = new StringBuilder();
//            }
            dataList.add(rec);
            dataList.add(null);
        }
        for (Object data: dataList)
            DataSourceHelper.sendDataToConsumers(owner, data, context);

        return res;
    }
}
