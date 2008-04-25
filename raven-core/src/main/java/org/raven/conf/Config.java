/*
 *  Copyright 2008 Sergey Pinevskiy.
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

public interface Config {

    /**
     * If property exists, then returns value of property, else returns defaultValue.
     * @param property property name
     * @param defaultValue default value of property 
     */
	public String getStringProperty(String property,String defaultValue);
    /**
     * If property exists, then returns value of property, else returns defaultValue.
     * @param property property name
     * @param defaultValue default value of property 
     */
	public Integer getIntegerProperty(String property,Integer defaultValue);
    /**
     * If property exists, then returns value of property, else returns defaultValue.
     * @param property property name
     * @param defaultValue default value of property 
     */
	public Boolean getBooleanProperty(String property,Boolean defaultValue);
	
}
