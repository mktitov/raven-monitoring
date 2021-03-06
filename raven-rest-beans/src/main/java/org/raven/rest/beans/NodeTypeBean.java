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

import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public class NodeTypeBean
{
    public String type;
    public String shortDescription;
    public String iconPath;
    public List<String> childTypes;

    public NodeTypeBean() {
    }

    public NodeTypeBean(String type, String shortDescription, String iconPath)
    {
        this(type, shortDescription, iconPath, null);
    }

    public NodeTypeBean(String type, String shortDescription, String iconPath, List<String> childTypes)
    {
        this.type = type;
        this.shortDescription = shortDescription;
        this.iconPath = iconPath;
        this.childTypes = childTypes;
    }

}
