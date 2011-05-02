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

import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.ReferenceValuesSource;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.constraints.ReferenceValue;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ReferenceToReferenceValuesSourceNode extends BaseNode implements ReferenceValuesSource
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private ReferenceValuesSource referenceValuesSource;

    public ReferenceValuesSource getReferenceValuesSource() {
        return referenceValuesSource;
    }

    public void setReferenceValuesSource(ReferenceValuesSource referenceValuesSource) {
        this.referenceValuesSource = referenceValuesSource;
    }

    public List<ReferenceValue> getReferenceValues() 
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        return getReferenceValuesSource().getReferenceValues();
    }
}
