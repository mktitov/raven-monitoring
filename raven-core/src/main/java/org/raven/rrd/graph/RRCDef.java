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

package org.raven.rrd.graph;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
@Description(
    "The virtual datasource. The values of this datasource are evaluating using" +
    "mathematical expression, specified in Reverse Polish Notation (RPN).")
public class RRCDef extends LeafNode implements DataDefinition
{
    @Parameter @NotNull @Description("Reverse Polish Notation (RPN) expression")
    private String expression;

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
