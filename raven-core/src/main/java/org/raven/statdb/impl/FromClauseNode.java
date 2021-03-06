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

import org.raven.annotations.Parameter;
import org.raven.statdb.query.FromClause;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class FromClauseNode extends BaseNode implements FromClause
{
    public final static String NAME = "from";
    
    @Parameter @NotNull
    private String keyExpression;

    public FromClauseNode()
    {
        super(NAME);
    }

    public String getKeyExpression()
    {
        return keyExpression;
    }

    public void setKeyExpression(String keyExpression)
    {
        this.keyExpression = keyExpression;
    }
}
