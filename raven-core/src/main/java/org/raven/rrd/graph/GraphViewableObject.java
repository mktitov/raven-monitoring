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

package org.raven.rrd.graph;

import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ViewableObjectImpl;

/**
 *
 * @author Mikhail Titov
 */
public class GraphViewableObject implements ViewableObject
{
    private final RRGraphNode graphNode;
    private final String startTime;
    private final String endTime;

    public GraphViewableObject(RRGraphNode graphNode, String startTime, String endTime)
    {
        this.graphNode = graphNode;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Object getData()
    {
        return graphNode.render(startTime, endTime);
    }

    public String getMimeType()
    {
        return graphNode.getImageFormat().getMimeType();
    }

    public boolean cacheData()
    {
        return true;
    }

    public int getWidth()
    {
        return graphNode.getWidth();
    }

    public int getHeight()
    {
        return graphNode.getHeight();
    }
}
