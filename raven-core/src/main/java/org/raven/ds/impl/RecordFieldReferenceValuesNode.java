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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.ReferenceValuesSource;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class RecordFieldReferenceValuesNode extends BaseNode
{
    @NotNull @Parameter
    private String fieldName;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private ReferenceValuesSource referenceValuesSource;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public ReferenceValuesSource getReferenceValuesSource() {
        return referenceValuesSource;
    }

    public void setReferenceValuesSource(ReferenceValuesSource referenceValuesSource) {
        this.referenceValuesSource = referenceValuesSource;
    }
}
