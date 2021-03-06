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
package org.raven.prj.impl;

import org.raven.annotations.NodeClass;
import org.raven.auth.impl.AnonymousLoginServiceNode;
import org.raven.auth.impl.LoginManagerNode;
import org.raven.auth.impl.LoginServiceNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class, childNodes = {LoginServiceNode.class, AnonymousLoginServiceNode.class})
public class ProjectLoginManagerNode extends LoginManagerNode {

    public ProjectLoginManagerNode() {
        super(false);
    }
    
}
