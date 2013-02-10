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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Mikhail Titov
 */
public class AccessGroup extends AccessControlList {
	private final String group;
	private final Set<String> users;

    public AccessGroup() {
        super(null, null, Collections.EMPTY_LIST);
        group = null;
        users = null;
    }
    
    public AccessGroup(String name, String group, Collection<AccessResource> resources, Collection<String> users) {
        super(name, null, Collections.EMPTY_LIST);
        this.group = group;
        this.users = users!=null && !users.isEmpty()? new HashSet<String>(users) : null;
        if (resources!=null && !resources.isEmpty())
            for (AccessResource resource: resources) 
                addAll(resource);
    }

    public String getGroup() {
        return group;
    }

	public boolean allowedUser(String user) {
		return users==null? true : users.contains(user);
	}
    
    @Override
    protected boolean applyExpression(String[] tokens) {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
