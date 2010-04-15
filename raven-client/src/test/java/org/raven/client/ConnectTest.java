/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.junit.Test;
import org.raven.remote.RemoteAccess;
import org.raven.remote.RemoteSession;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectTest
{
    @Test
    public void test() throws Exception
    {
        Registry registry = LocateRegistry.getRegistry();
        RemoteAccess ravenAccess = (RemoteAccess) registry.lookup("login");
        RemoteSession session = ravenAccess.login("test", "test", "ru");
        session.getTree();
//        if (session instanceof Remote)
//            System.out.println("session is Remote object");
    }
}
