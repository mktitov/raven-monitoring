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

package org.raven.statdb.impl;

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectEntry;
import org.raven.statdb.query.SelectMode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=SelectEntryNode.class)
public class SelectClauseNode extends BaseNode implements SelectClause
{
    public final static String NAME = "Select";

    @Parameter(defaultValue="SELECT_KEYS_AND_DATA")
    @NotNull
    private SelectMode selectMode;

    public SelectClauseNode()
    {
        super(NAME);
    }

    public SelectMode getSelectMode()
    {
        return selectMode;
    }

    public void setSelectMode(SelectMode selectMode)
    {
        this.selectMode = selectMode;
    }

    public boolean hasSelectEntries()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SelectEntry[] getSelectEntries()
    {
        Collection<Node> childs = getSortedChildrens();
        if (childs==null || childs.size()==0)
            return null;

        SelectEntry[] result = new SelectEntry[childs.size()];
        int i=0;
        for (Node child: childs)
            if (child.getStatus()==Status.STARTED)
                result[i++] = (SelectEntry) child;

        if (i==0)
            return null;
        else if (i<result.length)
        {
            SelectEntry[] newResult = new SelectEntry[i];
            System.arraycopy(result, 0, newResult, 0, i);
            result = newResult;
        }

        return result;

    }
}
