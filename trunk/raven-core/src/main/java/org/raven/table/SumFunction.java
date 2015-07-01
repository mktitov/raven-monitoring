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

/**
 *
 * @author Mikhail Titov
 */
public class SumFunction implements ConsolidationFunction
{
    private Object resultValue;
    private boolean realNumber;
    
    public void startCalculation(Class valueType) throws ConsolidationFunctionException
    {
        resultValue = null;
        if (Number.class.isAssignableFrom(valueType)){
            if (   Double.class.isAssignableFrom(valueType)
                || Float.class.isAssignableFrom(valueType))
            {
                realNumber = true;
            }
            else
                realNumber = false;
        }else
            throw new ConsolidationFunctionException(
                    String.format(
                        "Summary function (%s) doesn't work with values " +
                        "of type (%s)"
                        , SumFunction.class.getName()
                        , (valueType==null ? "null" : valueType.getName())));
    }

    public void nextCalculation(Object nextValue)
    {
        if (nextValue!=null)
        {
            if (realNumber)
            {
                double prevValue = resultValue==null? 0.0 : ((Number)resultValue).doubleValue();
                double val = ((Number)nextValue).doubleValue();
                resultValue =(
                        Double.isNaN(prevValue)? 0.: prevValue) + (Double.isNaN(val)? 0. : val);
            }
            else
            {
                long prevValue = resultValue==null? 0 : ((Number)resultValue).longValue();
                resultValue = prevValue + ((Number)nextValue).longValue();
            }
        }
    }

    public Object getResultValue()
    {
        return resultValue;
    }

    public void finishCalculation()
    {
    }
}
