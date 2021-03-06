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

/**
 * Gets data from the {@link DataSource data source} and transmits it to all 
 * {@link DataConsumer data consumers} in the 
 * {@link org.raven.tree.Node#getDependentNodes()  dependency list}.
 * Data pipe does not generate attributes, so {@link AttributesGenerator#generateAttributes()} 
 * returns null.
 * 
 * @author Mikhail Titov 
 */
public interface DataPipe extends DataConsumer, DataSource
{
    public final static String SKIP_DATA="SKIP_DATA";
    public final static String SKIP_DATA_BINDING = "SKIP_DATA";
}
