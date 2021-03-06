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

package org.raven.tree.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ActionNode extends AbstractActionNode implements Viewable
{
    @NotNull @Parameter(defaultValue="true")
    private Boolean autoRefresh;

    @Override
    public ViewableObject createActionViewableObject(
            DataContext context, Map<String, Object> additionalBindings)
    {
        return new ActionNodeAction(this, context, additionalBindings, getActionAttributes());
    }

    @Override
    public void prepareActionBindings(
            DataContext context, Map<String, Object> additionalBindings)
    {
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        return Arrays.asList(getActionViewableObject(new DataContextImpl(refreshAttributes), null));
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return NodeUtils.extractRefereshAttributes(this);
    }

    public Boolean getAutoRefresh()
    {
        return autoRefresh;
    }

    public void setAutoRefresh(Boolean autoReferesh)
    {
        this.autoRefresh = autoReferesh;
    }
}
