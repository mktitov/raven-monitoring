/*
 *  Copyright 2007 Mikhail Titov.
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

import org.raven.EnLocaleModule;
import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.raven.conf.impl.PropertiesConfig;

/**
 *
 * @author Mikhail Titov
 */
public class ServiceTestCase extends Assert
{
    protected Registry registry;
    
    protected void configureRegistry(RegistryBuilder builder)
    {
    }

    @Before
    public void setUp() 
    {
        System.setProperty(
                PropertiesConfig.CONFIG_PROPERTY_NAME, "src/test/conf/raven.properties");
        
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        builder.add(EnLocaleModule.class);
        builder.add(UserContextServiceModule.class);
        configureRegistry(builder);

        registry = builder.build();
        registry.performRegistryStartup();
    }

    @After
    public void tearDown() 
    {
        registry.shutdown();
    }

}
