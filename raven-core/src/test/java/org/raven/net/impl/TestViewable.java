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

package org.raven.net.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;

/**
 *
 * @author Mikhail Titov
 */
public class TestViewable extends BaseNode implements Viewable
{
    private Map<String, NodeAttribute> refAttrs;
    private List<ViewableObject> vos;

    @Override
    protected void initFields()
    {
        super.initFields();
        refAttrs = new HashMap<String, NodeAttribute>();
    }

    public void addRefreshAttribute(NodeAttribute attr) throws Exception
    {
        attr.setOwner(this);
        attr.init();
        refAttrs.put(attr.getName(), attr);
    }

    public void removeRefreshAttribute(String name){
        refAttrs.remove(name);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return refAttrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        if (vos!=null)
            return vos;
        else{
            ViewableObject vo = new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, refreshAttributes.get("attr1").getValue());
            return Arrays.asList(vo);
        }
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public void setViewableObjects(List<ViewableObject> vos)
    {
        this.vos = vos;
    }
}
