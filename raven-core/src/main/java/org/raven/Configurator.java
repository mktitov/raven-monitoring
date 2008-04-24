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

package org.raven;

/**
 * The goal of the configurator is to store and restore the application state.
 * 
 * @author Mikhail Titov
 */
public interface Configurator 
{
    /**
     * Returns the root node of the observable objects tree.
     * @return
     */
    public Node getRootNode();
    /**
     * Saves the state of the object in configuration database.
     * @param object object 
     */
    public void save(Object object);

    /**
     * Returns the configurations parameters storage.
     */
    public Config getConfig();
}
