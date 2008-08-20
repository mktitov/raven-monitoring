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

package org.raven.ds.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AttributeReferenceHandlerFactory;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes=true)
@Description("Allows to change value of the attribute by data reciving by this consumer")
public class DataToAttributeValueConsumer extends AbstractDataConsumer
{
    public final static String ATTRIBUTE_ATTRIBUTE = "attribute";
    
    @Parameter(valueHandlerType=AttributeReferenceHandlerFactory.TYPE)
    @Description("The attribute which value will changed")
    @NotNull
    private NodeAttribute attribute;

    public NodeAttribute getAttribute() 
    {
        return attribute;
    }
    
    @Override
    protected void doSetData(DataSource dataSource, Object data) 
    {
        NodeAttribute attr = attribute;
        if (!ObjectUtils.equals(attr.getRealValue(), data))
        {
            if (Status.STARTED!=attr.getOwner().getStatus())
                logger.warn(String.format(
                        "Can't set value for attribute (%s) of the node (%s). " +
                        "Node must be started", attr.getName(), attr.getOwner().getPath()));
            else
            {
                String stringData = null;
                try {
                    stringData = converter.convert(String.class, data, null);
                    attr.setValue(stringData);
                    attr.save();
                } 
                catch (Exception ex) 
                {
                    logger.error(String.format(
                            "Error setting value (%s) of the attribute (%s) of the node (%s) " +
                            "by the node (%s)"
                            , stringData, attr.getName(), attr.getOwner().getPath(), getPath())
                        , ex);
                }
            }
        }
    }
}
