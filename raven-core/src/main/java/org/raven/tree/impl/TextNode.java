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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class TextNode extends AbstractViewableNode
{
    public final static String REFRESH_ATTRIBUTES_BINDING = "refreshAttributes";
    public final static String TEXT_ATTR = "text";

    @Parameter @NotNull
    private String text;

    private BindingSupport bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (getStatus()!=Status.STARTED)
            return null;
        try{
            bindingSupport.put(REFRESH_ATTRIBUTES_BINDING, refreshAttributes==null? Collections.EMPTY_MAP : refreshAttributes);
            ViewableObject textObj = new ViewableObjectImpl(RAVEN_TEXT_MIMETYPE, text);
            return Arrays.asList(textObj);
        }finally{
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

}
