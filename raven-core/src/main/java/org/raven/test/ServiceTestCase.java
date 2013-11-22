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

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
    protected Properties privateProperties;
    
    protected void configureRegistry(Set<Class> builder) {
    }

    @Before
    public void setUp() throws Exception 
    {
        System.setProperty(PropertiesConfig.CONFIG_PROPERTY_NAME, "src/test/conf/raven.properties");
        String home = System.getProperty("user.home");
        File propFile = new File(home+"/.raven/dev.properties");
        privateProperties = new Properties();
        if (propFile.exists()) {
            FileInputStream is = new FileInputStream(propFile);
            try {
                privateProperties.load(is);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);

        Set<Class> modules = new HashSet<Class>();
        modules.add(EnLocaleModule.class);
        modules.add(UserContextServiceModule.class);
        configureRegistry(modules);
        for (Class moduleClass: modules)
            builder.add(moduleClass);

        registry = builder.build();
        registry.performRegistryStartup();
    }

    @After
    public void tearDown() {
        registry.shutdown();
    }
}
