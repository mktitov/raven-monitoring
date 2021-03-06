/*
 * Copyright 2014 Mikhail Titov.
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
package org.raven.ds.impl;

import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.RecordSchemaFieldCodec;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class FieldCodecReferenceNode extends BaseNode implements RecordSchemaFieldCodec {
    
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private RecordSchemaFieldCodec codec;

    public <T> T encode(Object value, Bindings bindings) {
        return codec.encode(value, bindings);
    }

    public <T> T decode(Object value, Bindings bindings) {
        return codec.decode(value, bindings);
    }

    public RecordSchemaFieldCodec getCodec() {
        return codec;
    }

    public void setCodec(RecordSchemaFieldCodec codec) {
        this.codec = codec;
    }
}
