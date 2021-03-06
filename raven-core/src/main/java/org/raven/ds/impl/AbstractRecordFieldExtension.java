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

package org.raven.ds.impl;

import javax.script.Bindings;
import org.raven.ds.RecordSchemaFieldCodec;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractRecordFieldExtension extends BaseNode {
    
    @Deprecated
    public <T> T prepareValue(Object value, Bindings bindings) {
        for (ValuePrepareRecordFieldExtension child: NodeUtils.getChildsOfType(this, ValuePrepareRecordFieldExtension.class))
            return child.prepareValue(value, bindings);
        return (T)value;
    }
    
    public RecordSchemaFieldCodec getCodec() {
        for (RecordSchemaFieldCodec codec: NodeUtils.getChildsOfType(this, RecordSchemaFieldCodec.class))
            return codec;
        return null;
    }
}
