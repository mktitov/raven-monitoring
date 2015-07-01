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

package org.raven.conf;

import java.net.URL;

/**
 * Allows to find the URL to the resource by resource id
 * 
 * @author Mikhail Titov
 */
public interface ResourceLocator 
{
    /**
     * Returns URL to the resource by its id.
     * @param resourceId the unique id of the resource.
     */
    public URL getResourceURL(String resourceId);
}
