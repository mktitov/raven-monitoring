/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.dp.impl;

import org.raven.dp.Terminated;
import org.raven.dp.DataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
public class TerminatedImpl implements Terminated {
    private final DataProcessorFacade facade;
    private final boolean successfullyStopped;

    public TerminatedImpl(DataProcessorFacade facade, boolean successfullyStopped) {
        this.facade = facade;
        this.successfullyStopped = successfullyStopped;
    }

    public DataProcessorFacade getFacade() {
        return facade;
    }

    public boolean isSuccessfullyStopped() {
        return successfullyStopped;
    }

    @Override
    public String toString() {
        return "DATA_PROCESSOR_TERMINATED";
    }
}
