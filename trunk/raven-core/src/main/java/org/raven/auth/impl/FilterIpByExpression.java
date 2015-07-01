/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.auth.impl;

import java.net.InetAddress;
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IpFiltersNode.class)
public class FilterIpByExpression extends AbstractIpFilterNode {
    
    public final static String FILTER_ATTR = "filter";
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean filter;
    
    private BindingSupportImpl bindingSupport;

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

    @Override
    public boolean doIsIpAllowed(String ip) throws Exception {
        try {
            bindingSupport.put(BindingNames.HOST_BINDING, new HostResolver(ip));
            Boolean res = filter;
            return res==null? false : res;
        } finally {
            bindingSupport.reset();
        }
    }

    public Boolean getFilter() {
        return filter;
    }

    public void setFilter(Boolean filter) {
        this.filter = filter;
    }
    
    private class HostResolver {
        private final String ip;

        public HostResolver(String ip) {
            this.ip = ip;
        }

        public String getIp() {
            return ip;
        }
        
        public String getName() throws Exception {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getCanonicalHostName();
        }
    }
}
