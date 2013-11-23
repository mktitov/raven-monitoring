/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.expr.impl;

import org.raven.RavenRuntimeException;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionException extends RavenRuntimeException {
    private final GroovyExpressionExceptionAnalyzator analyzator;
    
    public GroovyExpressionException(String msg, Throwable ex, GroovyExpressionExceptionAnalyzator analyzator) {
        super(msg, ex);
        this.analyzator = analyzator;
    }
    
    public StringBuilder constructMessage(String prefix, StringBuilder builder) {
        builder.append(getMessage()).append('\n');
        analyzator.addResultToBuilder(prefix, builder);
        return builder;
    }
}
