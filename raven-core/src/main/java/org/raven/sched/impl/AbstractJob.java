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

package org.raven.sched.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.sched.Job;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractJob implements Job
{
    private AtomicBoolean running = new AtomicBoolean(false);

    public boolean isRunning()
    {
        return running.get();
    }

    public void setRunning(boolean running)
    {
        this.running.set(running);
    }
    
}
