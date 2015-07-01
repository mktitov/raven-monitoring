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

import org.raven.net.AccessDeniedException;
import org.raven.net.AddressMatcher;
import org.raven.tree.Node;
import static org.raven.util.NodeUtils.*;

/**
 *
 * @author Mikhail Titov
 */
public class AddressVavleSupport {
    
    public void initNodes(Node owner) {
        if (owner.hasNode(ContextAddressValveNode.NAME))
            return;
        ContextAddressValveNode valve = new ContextAddressValveNode();
        owner.addAndSaveChildren(valve);
        valve.start();
    }
    
    public void checkAddress(Node owner, String address) throws AccessDeniedException {
        ContextAddressValveNode valve = (ContextAddressValveNode) owner.getNode(ContextAddressValveNode.NAME);
        if (valve!=null)
            for (AddressMatcher matcher: getChildsOfType(valve, AddressMatcher.class))
                if (matcher.addressMatches(address))
                    return;
        throw new AccessDeniedException();
    }
}
