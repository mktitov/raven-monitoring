/*
 *  Copyright 2010 Mikhail Titov.
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

import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface DataHandler
{
    /**
     * If returns <b>true</b> then handler can be used. Else handler must be released.
     */
    public boolean isValid();
    /**
     * Releases handler resources
     */
    public void releaseHandler();
    /**
     * Handle the data
     * @param data the data wich must be handled
     * @param owner the node that owns the handler
     */
    public Object handleData(Object data, DataSource dataSource, DataContext context, Node owner) throws Exception;
    /**
     * Returns data processing status message
     */
    public String getStatusMessage();
}
