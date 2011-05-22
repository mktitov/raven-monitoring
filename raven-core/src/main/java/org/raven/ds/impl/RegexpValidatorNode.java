/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.ds.impl;

import java.util.regex.Pattern;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.ValueValidator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class RegexpValidatorNode extends BaseNode implements ValueValidator
{
    @NotNull @Parameter
    private String regexp;

    @Message
    private static String errorMessage;

    private Pattern compiledRegexp;

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        compiledRegexp = Pattern.compile(regexp);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        compiledRegexp = null;
    }

    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue
            , Object newValue)
    {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        if (   node==this && Status.STARTED.equals(node.getStatus())
            && "regexp".equals(attr.getName()) && newValue!=null)
        {
            compiledRegexp = Pattern.compile((String)newValue);
        }
    }

    public String validate(Object value)
    {
        return value==null || !compiledRegexp.matcher(value.toString()).matches()?
            String.format(errorMessage, value, regexp)
            : null;
    }
}
