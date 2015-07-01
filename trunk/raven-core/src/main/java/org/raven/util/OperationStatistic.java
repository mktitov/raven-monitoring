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

package org.raven.util;

import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public class OperationStatistic
{
    private long startTime;
    private long operationsCount;
    private long sumMillisPerOperation;

    @Message
    private static String avgMillisecondsPerOperationDesc;
    @Message
    private static String operationsCountDesc;

    public synchronized void reset()
    {
        startTime = System.currentTimeMillis();
        operationsCount = 0l;
    }

    public synchronized long markOperationProcessingStart()
    {
        ++operationsCount;
        return System.currentTimeMillis();
    }

    public synchronized void markOperationProcessingEnd(long operationProcessingStart)
    {
        sumMillisPerOperation+=System.currentTimeMillis()-operationProcessingStart;
    }

    public synchronized long getOperationsCount()
    {
        return operationsCount;
    }

    public synchronized long getStartTime() {
        return startTime;
    }

    public synchronized double getAvgOperationsPerSecond()
    {
        double period = ((System.currentTimeMillis()-startTime)/1000);
        return period==0.? 1 : operationsCount/period;
    }

    public synchronized double getAvgMillisecondsPerOperation()
    {
        return operationsCount==0? 0 : sumMillisPerOperation/operationsCount;
    }

    @Override
    public String toString()
    {
        return String.format(
                "%s (<b>operationsCount</b>): %d;<br>%s (<b>avgMillisecondsPerOperation</b>): %f"
                , operationsCountDesc, operationsCount
                , avgMillisecondsPerOperationDesc, getAvgMillisecondsPerOperation());
    }
}
