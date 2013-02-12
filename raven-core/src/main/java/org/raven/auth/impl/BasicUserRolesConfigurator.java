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
package org.raven.auth.impl;

import java.util.Collections;
import java.util.Set;
import static org.raven.RavenUtils.*;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContextConfig;
import org.raven.auth.UserContextConfigurator;
import org.raven.auth.UserContextConfiguratorException;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = UserContextConfiguratorsNode.class)
public class BasicUserRolesConfigurator extends BaseNode implements UserContextConfigurator {
    
    @NotNull @Parameter
    private String users;
    
    @NotNull @Parameter
    private String groups;
    
    private Set<String> usersSet;
    private Set<String> groupsSet;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        usersSet = arrayToSet(split(users));
        groupsSet = arrayToSet(split(groups));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        usersSet = Collections.EMPTY_SET;
        groupsSet = Collections.EMPTY_SET;
    }

    public void configure(UserContextConfig user) throws UserContextConfiguratorException {
        if (isStarted() && usersSet.contains(user.getLogin()))
            user.getGroups().addAll(groupsSet);
    }

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }
}
