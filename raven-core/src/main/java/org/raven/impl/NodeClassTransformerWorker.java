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

package org.raven.impl;

import java.lang.annotation.Annotation;
import javassist.CannotCompileException;
import javassist.CtClass;
import org.weda.internal.impl.ClassTransformWorker;
import org.weda.internal.services.ClassTransformer.Phase;
import org.weda.internal.services.ProjectBuild;

/**
 *
 * @author Mikhail Titov
 */
public class NodeClassTransformerWorker implements ClassTransformWorker
{
    public final static String NODES_TYPES_RESOURCE = "nodes_types";
    
    private final ProjectBuild buildInfo;

    public NodeClassTransformerWorker(ProjectBuild buildInfo)
    {
        this.buildInfo = buildInfo;
    }
    
    public void transform(Annotation firedAnnotation, CtClass target) throws CannotCompileException
    {
        buildInfo.getResource(NODES_TYPES_RESOURCE).add(target.getName());
    }

    public Phase getExecutionPhase()
    {
        return Phase.EXECUTION;
    }

}
