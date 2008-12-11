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

package org.raven.statdb;

/**
 * The result of the 
 * {@link Rule#processRule(java.lang.String, java.lang.Double, org.raven.statdb.StatisticsRecord)
 * rule processing}
 * 
 * @author Mikhail Titov
 */
public interface RuleProcessingResult
{
    /**
     * Return the processing instruction that must be done after rule processing
     *
     * @see Rule#processRule
     */
    public ProcessingInstruction getInstruction();
    /**
     * Sets the instruction that must be done after rule processing
     * 
     * @see Rule#processRule
     */
    public void setInstruction(ProcessingInstruction instruction);
    /**
     * Must return the same value as value passed to the <code>value</code> parameter of the method
     * {@link Rule#processRule}
     * or new value.
     */
    public Double getValue();
    /**
     * Changes the statistics value that processing by the rule.
     * @param value statistics value.
     *
     * @see Rule#processRule
     */
    public void setValue(Double value);
}
