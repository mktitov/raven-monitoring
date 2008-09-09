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

import java.lang.annotation.Annotation;
import javassist.CannotCompileException;
import javassist.expr.FieldAccess;
import org.raven.annotations.Parameter;
import org.weda.internal.FieldTransformWorker;
import org.weda.internal.services.ClassTransformer.Phase;

/**
 *
 * @author Mikhail Titov
 */
public class ParameterFieldTransformerWorker implements FieldTransformWorker
{
    public void transform(Annotation firedAnnotation, FieldAccess fieldAccess) 
            throws CannotCompileException
    {
        try
        {
            Parameter paramAnn = (Parameter) firedAnnotation;
            if (paramAnn.readOnly())
                return;
            if (!fieldAccess.getField().getType().isPrimitive() && fieldAccess.isReader())
            {
                String body = String.format(
                        "{ " +
                        "   $_ = ($r)%2$s.getParameterValue($0, \"%1$s\", $0.%1$s);" +
                        "}"
                        , fieldAccess.getFieldName(), NodeListenerExecutorHelper.class.getName());
                
                fieldAccess.replace(body);
            }
            if (fieldAccess.isWriter())
            {
                String body = String.format(
                        "{ " +
                        "   $proceed($1);" +
                        "   %2$s.setParameterValue($0, \"%1$s\", ($w)$0.%1$s);" +
                        "} "
                        , fieldAccess.getFieldName(), NodeListenerExecutorHelper.class.getName());
                fieldAccess.replace(body);
            }
        } catch (Exception e)
        {
            throw new CannotCompileException(e);
        }
    }

    public Phase getExecutionPhase()
    {
        return Phase.EXECUTION;
    }

}
