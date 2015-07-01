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

package org.raven.graph.impl;

import org.raven.annotations.Parameter;
import org.raven.graph.Print;
import org.raven.rrd.ConsolidationFunction;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class PrintNode extends DataDefConsumer implements Print
{
    @Parameter @NotNull
    private ConsolidationFunction consolidationFunction;

    @Parameter @NotNull
    private String format;

    public ConsolidationFunction getConsolidationFunction()
    {
        return consolidationFunction;
    }

    public void setConsolidationFunction(ConsolidationFunction consolidationFunction)
    {
        this.consolidationFunction = consolidationFunction;
    }

    public String getFormat()
    {
        return format;
    }
    
    public void setFormat(String format)
    {
        this.format = format;
    }

}
