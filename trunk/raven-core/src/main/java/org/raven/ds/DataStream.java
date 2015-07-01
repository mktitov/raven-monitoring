/*
 *  Copyright 2011 Mikhail Titov.
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

/**
 *
 * @author Mikhail Titov
 */
public interface DataStream
{
    /**
     * Pushes data to the stream and returns reference to itself
     */
    public DataStream push(Object data);
    /**
     * Pushes data to the stream and returns reference to itself. In groovy  you can use &lt;&lt; operator 
     */
    public DataStream leftShift(Object data);
    /**
     * Returns the data context for this stream
     */
    public DataContext getContext();
}
