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

package org.raven.table;

import org.raven.tree.impl.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.table.Table;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class TableViewNode extends AbstractDataConsumer implements Viewable
{
    private ThreadLocal<Table> table = new ThreadLocal<Table>();

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        refereshData(refreshAttributes.values());

        if (table.get()==null)
            return null;

        ViewableObject tableObject = new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, table.get());
        table.remove();

        return Arrays.asList(tableObject);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        if (data==null)
            return;
        Table tableData = converter.convert(Table.class, data, null);
        table.set(tableData);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return NodeUtils.extractRefereshAttributes(this);
    }
}
