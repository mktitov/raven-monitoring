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

package org.raven.net;

/**
 *
 * @author Mikhail Titov
 */
public class ContextUnavailableException extends NetworkResponseServiceExeption
{
    public ContextUnavailableException(String context) {
        super(String.format("Context (%s) unavailable", context));
    }
    
    public ContextUnavailableException(String contextPath, String pathElem) {
        super(String.format("Context (%s) unavailable. Can't resolve (%s) path element", contextPath, pathElem));
    }

    public ContextUnavailableException(String contextPath, Throwable cause) {
        super(String.format("Context (%s) unavailable", contextPath), cause);
    }
    
}
