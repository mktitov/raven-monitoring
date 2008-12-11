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

package org.raven.statdb.impl;

import org.raven.statdb.ProcessingInstruction;
import org.raven.statdb.RuleProcessingResult;

/**
 *
 * @author Mikhail Titov
 */
public class RuleProcessingResultImpl implements RuleProcessingResult
{
    private ProcessingInstruction instruction;
    private Double value;

    public RuleProcessingResultImpl(ProcessingInstruction instruction, Double value)
    {
        this.instruction = instruction;
        this.value = value;
    }

    public ProcessingInstruction getInstruction()
    {
        return instruction;
    }

    public void setInstruction(ProcessingInstruction instruction)
    {
        this.instruction = instruction;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }
}
