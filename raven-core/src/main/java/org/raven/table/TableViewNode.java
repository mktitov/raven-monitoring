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
import org.raven.annotations.Parameter;
import org.raven.ds.impl.SafeDataConsumer;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
public class TableViewNode extends SafeDataConsumer implements Viewable
{
    @NotNull @Parameter(defaultValue="false")
    private Boolean autoRefresh;

    //@Override
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        Object data = refereshData(refreshAttributes==null? null : refreshAttributes.values());
//        if (!(tableObj instanceof Table))
//        {
//            if (isLogLevelEnabled(LogLevel.DEBUG))
//                debug(String.format(
//                        "Invalid data type recieved from (%s). Expected (%s) but recieved (%s)"
//                        , getDataSource().getPath()
//                        , tableObj==null? "NULL" : tableObj.getClass().getName()));
//            return null;
//        }

        Table table = converter.convert(Table.class, data, null);
        if (table==null)
            return null;
        else
        {
            ViewableObject tableObject = new ViewableObjectImpl(RAVEN_TABLE_MIMETYPE, table);
            return Arrays.asList(tableObject);
        }
    }

    //@Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return NodeUtils.extractRefereshAttributes(this);
    }

    public void setAutoRefresh(Boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getAutoRefresh()
    {
        return autoRefresh;
    }
}
