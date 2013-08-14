/*
 * Copyright 2013 Mikhail TItov.
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

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataChainNode;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = DataChainNode.class)
public class DataChainStubNode extends BaseNode implements DataConsumer {

    public void setData(DataSource dataSource, Object data, DataContext context) {
        DataChainNode chain = (DataChainNode) getEffectiveParent();
        chain.
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported operation");
    }
}
