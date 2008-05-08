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

import org.apache.tapestry.ioc.MappedConfiguration;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.ParameterFieldTransformerWorker;
import org.weda.internal.TransformWorker;
import org.weda.internal.annotations.WedaPluginModule;

/**
 *
 * @author Mikhail Titov
 */
//@WedaPluginModule
public class TransformModule 
{
    public static void contributeClassTransformer(MappedConfiguration<Class, TransformWorker> conf)
    {
        conf.add(Parameter.class, new ParameterFieldTransformerWorker());
    }
}
