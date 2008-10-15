/*
 *  Copyright 2008 Sergey Pinevskiy.
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

package org.raven.ui.util;

import org.raven.*;
import org.apache.tapestry.ioc.IOCUtilities;
import org.apache.tapestry.ioc.Registry;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.raven.tree.Tree;
import org.raven.ui.RavenUiModule;

public class RavenRegistry 
{
	private static Registry registry = null;

	private RavenRegistry()
	{
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        builder.add(RavenCoreModule.class, RavenUiModule.class);
        registry = builder.build();
        registry.performRegistryStartup();
        registry.getService(Tree.class).reloadTree();
	}
	
	public static synchronized Registry getRegistry() 
	{
		if(registry==null) new RavenRegistry(); 
		return registry;
	}
}
