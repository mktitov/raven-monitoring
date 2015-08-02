/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.auth.impl;

import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.auth.LoginPathChecker;
import org.raven.net.ResponseContext;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class)
public class LoginPathsNode extends BaseNode implements LoginPathChecker {
    public final static String NAME = "Login paths";

    public LoginPathsNode() {
        super(NAME);
    }

    @Override
    public boolean isLoginAllowedFromPath(ResponseContext responseContext) {
        List<LoginPathChecker> checkers = NodeUtils.getChildsOfType(this, LoginPathChecker.class);
        if (checkers.isEmpty())
            return true;
        for (LoginPathChecker checker: checkers)
            if (checker.isLoginAllowedFromPath(responseContext))
                return true;
        return false;
    }
}
