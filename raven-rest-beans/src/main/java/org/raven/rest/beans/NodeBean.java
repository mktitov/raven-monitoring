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

package org.raven.rest.beans;

/**
 *
 * @author Mikhail Titov
 */
public class NodeBean
{
    public int id;
    public String name;
    public String path;
    public String type;
    public String iconPath;
    public boolean hasChilds;
    public int rights;

    public NodeBean() {
    }

    public NodeBean(
            int id, String name, String path, String type, String iconPath
            , boolean hasChilds, int rights)
    {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.iconPath = iconPath;
        this.hasChilds = hasChilds;
        this.rights = rights;
    }
}
