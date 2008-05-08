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

package org.raven.tree.impl;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.ServiceTestCase;
import org.raven.TransformModule;
import org.raven.tree.impl.objects.NodeWithParameters;
import org.weda.internal.services.ClassTransformer;
import org.weda.services.JavassistClassPool;

/**
 *
 * @author Mikhail Titov
 */
public class ParameterFieldTransformerWorkerTest extends ServiceTestCase 
{
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(TransformModule.class);
    }
    
    @Test
    public void test() throws NotFoundException, CannotCompileException
    {
        ClassTransformer transformer = registry.getService(ClassTransformer.class);
        JavassistClassPool classPool = registry.getService(JavassistClassPool.class);
        CtClass clazz = classPool.getClassPool().get(
                "org.raven.tree.impl.objects.NodeWithParameter2");
        transformer.transform(clazz, ClassTransformer.Phase.EXECUTION);
    }
}
