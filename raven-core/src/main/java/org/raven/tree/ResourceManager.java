/*
 * Copyright 2012 Mikhail Titov.
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
package org.raven.tree;

import java.util.Locale;

/**
 *
 * @author Mikhail Titov
 */
public interface ResourceManager extends TreeListener {
    /**
     * Returns <b>true</b> if resource with specified key for passed locale exists
     * @param key resource key (the subpath from the resources node)
     * @param locale resource locale. If locale is null then default locale will be used. 
     *      default locale must be specified in the <i>Resources</i> node
     */
    public boolean containsResource(String key, Locale locale);
    /**
     * Register resource
     * @param key resource key (the subpath from the resources node)
     * @param locale resource locale. If locale is null then default locale will be used. 
     *      default locale must be specified in the <i>Resources</i> node
     * @return <b>true</b> if node was successfully registered or <b>false</b> if resource is already
     *      exists
     */
    public boolean registerResource(String key, Locale locale, Node resource) throws ResourceManagerException;
    /**
     * Returns resource for specified key and locale or null if resource not exists
     * @param key resource key (the subpath from the resources node)
     * @param locale resource locale. If locale is null then default locale will be used. 
     *      default locale must be specified in the <i>Resources</i> node
     */
    public Node getResource(String key, Locale locale);
    /**
     * Returns the key for resource node or null if node is not a resource node
     * @param resource This node or it's parent must be an instance of ResouceNode
     */
    public String getKeyForResource(Node resource);
    
//    public ResourcesNode
    
}
