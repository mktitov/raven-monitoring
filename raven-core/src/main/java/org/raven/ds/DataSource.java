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

package org.raven.ds;

import org.raven.tree.AttributesGenerator;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface DataSource extends Node, AttributesGenerator
{
    /**
     * Immediate gathers data for data consumer.
     * @param dataConsumer data consumer for which data will gather
     * @param context the context of this data processing.
     * @return <b>true</b> if data got successful, else <b>false</b 
     */
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context);
    public Boolean getStopProcessingOnError();
}
