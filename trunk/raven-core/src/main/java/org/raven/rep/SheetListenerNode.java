/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.rep;

import javax.script.Bindings;
import net.sf.jett.event.SheetEvent;
import net.sf.jett.event.SheetListener;
import org.raven.annotations.Parameter;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class SheetListenerNode extends BaseNode implements SheetListener {
    
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean beforeSheetProcessed;
    
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Object sheetProcessed;
    
    private BindingSupport bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public boolean beforeSheetProcessed(SheetEvent event) {
        bindingSupport.put("event", event);
        try {
            final Boolean res = beforeSheetProcessed;
            return res==null? true : res;
        } finally {
            bindingSupport.reset();
        }
    }

    public void sheetProcessed(SheetEvent event) {
        bindingSupport.put("event", event);
        try {
            final Object res = sheetProcessed;
        } finally {
            bindingSupport.reset();
        }
    }

    public Boolean getBeforeSheetProcessed() {
        return beforeSheetProcessed;
    }

    public void setBeforeSheetProcessed(Boolean beforeSheetProcessed) {
        this.beforeSheetProcessed = beforeSheetProcessed;
    }

    public Object getSheetProcessed() {
        return sheetProcessed;
    }

    public void setSheetProcessed(Object sheetProcessed) {
        this.sheetProcessed = sheetProcessed;
    }   
}
