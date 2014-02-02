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
package org.raven.net.impl;

import org.raven.annotations.NodeClass;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(importChildTypesFromParent=true, parentNode=NetworkResponseServiceNode.class)
public class NetworkResponseGroupNode extends NetworkResponseBaseNode {

    public NetworkResponseGroupNode() {
        super();
    }
    
    public NetworkResponseGroupNode(String name) {
        super(name);
    }
}
