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
package org.raven.test;

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class PushDataSource extends BaseNode implements DataSource {

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        return true;
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public void pushData(Object data) {
        DataContext context = new DataContextImpl();
        pushData(data, context);
    }

    public void pushData(Object data, DataContext context) {
        Collection<Node> deps = getDependentNodes();
        if (deps != null) {
            for (Node dep : deps) {
                if (dep instanceof DataConsumer) {
                    ((DataConsumer) dep).setData(this, data, context);
                }
            }
        }
    }
}
