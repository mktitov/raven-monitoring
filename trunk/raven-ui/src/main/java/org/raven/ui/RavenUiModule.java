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

package org.raven.ui;

import java.util.ResourceBundle;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.raven.auth.UserContextService;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.ui.services.LocaleService;
import org.raven.ui.services.SessionAttributeReferenceValues;
import org.raven.ui.services.SessionAttributeValueHandlerFactory;
import org.raven.ui.services.SessionCache;
import org.raven.ui.services.UserContextServiceImpl;
import org.weda.internal.Cache;
import org.weda.internal.CacheScope;
import org.weda.internal.services.Locale;

/**
 *
 * @author Mikhail Titov
 */
public class RavenUiModule {
    private final static String versionLock = "";
    private static volatile String VERSION = null;
    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(Locale.class, LocaleService.class);
        binder.bind(UserContextService.class, UserContextServiceImpl.class);
    }

    public static void contributeCacheManager(MappedConfiguration<CacheScope, Cache> conf)
    {
        conf.add(CacheScope.SESSION, new SessionCache());
    }

    public static void contributeAttributeValueHandlerRegistry(
            MappedConfiguration<String, AttributeValueHandlerFactory> conf)
    {
        conf.add(
            SessionAttributeValueHandlerFactory.TYPE
            , new SessionAttributeValueHandlerFactory());
    }

    public static void contributeAttributeReferenceValues(
            OrderedConfiguration<AttributeReferenceValues> conf)
    {
        conf.add(
                SessionAttributeReferenceValues.class.getSimpleName()
                , new SessionAttributeReferenceValues()
                , "after:*");
    }    
}
