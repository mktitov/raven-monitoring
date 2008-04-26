/*
 *  Copyright 2008 Mikhail Titov.
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
package org.raven;

import org.apache.tapestry.ioc.Configuration;
import org.apache.tapestry.ioc.ServiceBinder;
import org.raven.impl.NodeToStringConverter;

/**
 * Tapestry IOC module for raven-core module
 * @author Mikhail Titov
 */
public class RavenCoreModule
{
    public static void bind(ServiceBinder binder)
    {
        
    }

    public static void contributeTypeConverter(Configuration conf)
    {
        conf.add(new NodeToStringConverter());
    }
}
