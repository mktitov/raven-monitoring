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

package org.raven.server.remote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.raven.EnLocaleModule;
import org.raven.RavenCoreModule;
import org.raven.remote.RemoteAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class RemoteServerImpl
{
    private final static Logger logger = LoggerFactory.getLogger(RemoteServerImpl.class);

    private RemoteAccess remoteAccess;
    private org.apache.tapestry5.ioc.Registry serviceRegistry;

    public void start() throws Exception
    {
        logger.info("Starting R A V E N server");

        logger.info("  Creating service registry");
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        builder.add(RavenCoreModule.class, EnLocaleModule.class);
        serviceRegistry = builder.build();
        serviceRegistry.performRegistryStartup();

        logger.info("  Creating RMI registry");
        RemoteAccessImpl obj = new RemoteAccessImpl();
        remoteAccess = (RemoteAccess) UnicastRemoteObject.exportObject(obj, 0);
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.bind("login", remoteAccess);
        logger.info("Server successfully started");
    }
}
