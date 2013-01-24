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

package org.raven.api;

import java.util.Map;

/**
 * Stricted access to the {@link org.raven.tree.NodeAttribute}.
 * @author Mikhail Titov
 */
public interface NodeAttributeAccess 
{
    public String getName();
    public Object getValue();
    public void setValue(Object val) throws Exception;
    public Object getValue(Map args);
    public String getValueAsString();
}
