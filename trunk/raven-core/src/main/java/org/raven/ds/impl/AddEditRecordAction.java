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

import java.util.Map;
import org.raven.RavenUtils;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AbstractActionNode;

/**
 *
 * @author Mikhail Titov
 */
public class AddEditRecordAction extends RecordsAsTableRecordAction
{
        private final String errorMessage;
        private final RecordSchema recordSchema;

        public AddEditRecordAction(AbstractActionNode actionNode, DataContext context,
                Map<String, Object> additionalBindings, Map<String, NodeAttribute> actionAttributes,
                String errorMessage, DataSource owner, RecordSchema recordSchema)
        {
            super(actionNode, context, additionalBindings, actionAttributes, owner);
            this.errorMessage = errorMessage;
            this.recordSchema = recordSchema;
        }

        @Override
        public Object getData()
        {
            try{
                if (actionAttributes!=null) {
                    Record record = (Record) additionalBindings.get(RecordsAsTableNode.RECORD_BINDING);
                    Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(recordSchema);
                    for (NodeAttribute attr: actionAttributes.values()){
                        if (fields.containsKey(attr.getName()))
                            record.setValue(attr.getName(), attr.getValue());
                    }
                }

                return super.getData();

            }catch(Exception e){
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error("Creating/editing record error", e);

                return String.format(errorMessage, e.getMessage());
            }
        }

}
