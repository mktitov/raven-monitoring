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

package org.raven.tree;

/**
 *
 * @author Mikhail Titov
 */
public interface ViewableObject
{
    /**
     * Retruns the mime type of the {@link #getData()}
     */
    public String getMimeType();
    /**
     * Returns the data contained by this object.
     */
    public Object getData();
    /**
     * If returns <code>true</code> then data must be cached by user interface layer.
     */
    public boolean cacheData();

    public int getWidth();
    
    public int getHeight();
}
