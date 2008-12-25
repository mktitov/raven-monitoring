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

package org.raven.statdb.rrd;

import org.raven.annotations.NodeClass;
import org.raven.rrd.DataSourceType;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={RRDNode.class})
public class DatabaseTemplatesNode extends TemplateEntry
{
	public final static String NAME = "Database templates";

	public DatabaseTemplatesNode()
	{
		super();
		setName(NAME);
	}

	@Override
	public void childrenAdded(Node owner, Node children)
	{
		super.childrenAdded(owner, children);

		if (children.getChildren(RrdStatisticsDatabaseNode.DATASOURCE_NAME)==null)
		{
			RRDataSource ds = new RRDataSource();
			ds.setName(RrdStatisticsDatabaseNode.DATASOURCE_NAME);
			ds.setParent(children);
			ds.save();
			children.addChildren(ds);
			ds.init();
			ds.setDataSourceType(DataSourceType.GAUGE);
		}
	}
}
