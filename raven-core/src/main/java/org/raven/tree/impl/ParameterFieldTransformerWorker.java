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
            if (!fieldAccess.getField().getType().isPrimitive() && fieldAccess.isReader())
            {
                String body = String.format(
                        "{ " +
                        "System.out.println(\">>>getter(): \"+%1$s);" +
                        "if (this.%1$s==null) " +
                        "{ " +
                        "   $_ = ($r)%2$s.getParentAttributeValue(this, \"%1$s\", $type); " +
                        "} " +
                        "else " +
                        "{ " +
//                        "   $_=($r)this.%1$s; " +
                        "   $_ = $proceed(); " +
                        "} }"
                        , fieldAccess.getFieldName(), NodeListenerExecutorHelper.class.getName());
                
                fieldAccess.replace(body);
            }
            if (fieldAccess.isWriter())
            {
                String body = String.format(
                        "{ " +
                        "   Object oldValue = %2$s.getOldValue(this, \"%1$s\");" +
                        "   $proceed($1);" +
                        "   %2$s.fireNodeAttributeValueChanged(this, \"%1$s\", oldValue, ($w)this.%1$s);" +
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
