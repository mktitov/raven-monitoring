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

import java.util.List;
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface Viewable extends Node
{
    public final static String RAVEN_TABLE_MIMETYPE = "raven/table";
    public final static String RAVEN_NODE_MIMETYPE = "raven/node";
    public final static String RAVEN_TEXT_MIMETYPE = "raven/text";
    public final static String RAVEN_ACTION_MIMETYPE = "raven/action";
    public final static String RAVEN_UPLOAD_FILE_MIMETYPE = "raven/upload-file";

    /**
     * Returns the list of the attributes the values of which will be used in the view refresh
     * operation.
     * @see #getViewableObjects(refreshAttributes)
     */
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception;
    /**
     * Returns the list of the viewable objects contained by this node
     */
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception;
    /**
     * If method returns <code>true</code> then user interface can automatically refresh viewable
     * objects else user interface must refresh viewable objects only on user action.
     */
    public Boolean getAutoRefresh();
}
