package org.raven;

import org.apache.tapestry.ioc.IOCUtilities;
import org.apache.tapestry.ioc.Registry;
import org.apache.tapestry.ioc.RegistryBuilder;

public class RavenRegistry 
{
	private static Registry registry = null;

	private RavenRegistry()
	{
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        builder.add(EnLocaleModule.class,RavenCoreModule.class);
        registry = builder.build();
        registry.performRegistryStartup();
	}
	
	public static synchronized Registry getRegistry() 
	{
		if(registry==null) new RavenRegistry(); 
		return registry;
	}
}
