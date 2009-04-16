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

package org.raven.statdb.rrd;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.statdb.impl.AbstractStatisticsRecord;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsRecord extends AbstractStatisticsRecord
{
    private final RrdDatabaseRecordExtension databaseDef;
    private final Map<String, RrdDatabaseRecordExtension> fieldsDatabaseTemplates =
            new HashMap<String, RrdDatabaseRecordExtension>();

    public RrdStatisticsRecord(Record record, TypeConverter converter, Node owner) throws Exception
    {
        super(record, converter, owner);

        databaseDef = schema.getRecordExtension(RrdDatabaseRecordExtension.class, null);
        if (databaseDef==null)
            throw new Exception(String.format(
                    "Record schema (%s) does not contains record schema extension (%s)"
                    , schema.getName(), RrdDatabaseRecordExtension.class.getName()));

    }

    @Override
    protected boolean isFieldValueValid(RecordSchemaField field)
    {
        RrdDatabaseRecordFieldExtension fieldExt =
                field.getFieldExtension(RrdDatabaseRecordFieldExtension.class, null);
        RrdDatabaseRecordExtension dbTemplate = schema.getRecordExtension(
                RrdDatabaseRecordExtension.class, fieldExt.getDatabaseTemplateName());
        if (dbTemplate==null)
        {
            owner.getLogger().warn(String.format(
                    "Record field (%s) references to the undefined record extension " +
                    "(%s) of type (%s)"
                    , field.getName()
                    , fieldExt.getDatabaseTemplateName()
                    , RrdDatabaseRecordExtension.class.getSimpleName()));
            return false;
        }

		boolean hasDatasources = false;
		boolean hasArchives = false;

		Collection<Node> childs = dbTemplate.getChildrens();
		if (childs!=null && childs.size()>0)
		{
			for (Node child: childs)
			{
				if (child.getStatus()!=Status.STARTED)
					continue;
				if (child instanceof RrdArchiveDefNode)
					hasArchives = true;
				else if (   child instanceof RrdDatasourceDefNode
						 && RrdStatisticsDatabaseNode.DATASOURCE_NAME.equals(child.getName()))
				{
					hasDatasources = true;
				}
			}
		}
		if (!hasArchives)
		{
			owner.getLogger().warn(String.format(
					"Field (%s) of the record (%s) references to the invalid rrd template (%s)." +
                    "Database template must have at least one archive"
					, field.getName(), schema.getName(), dbTemplate.getPath()));
			return false;
		}
		if (!hasDatasources)
		{
			owner.getLogger().warn(String.format(
					"Field (%s) of the record (%s) references to the invalid rrd template (%s). " +
                    "Database template must have exactly " +
					"one datasource with name (%s)"
					, field.getName()
                    , schema.getName()
                    , dbTemplate.getPath()
                    , RrdStatisticsDatabaseNode.DATASOURCE_NAME));
			return false;
		}

        fieldsDatabaseTemplates.put(field.getName(), dbTemplate);
        
        return true;
    }

}
