/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.stream;

import org.raven.dp.RavenFuture;

/**
 *
 * @author Mikhail Titov
 */
public interface SinkLogic<B> extends Logic {
    public enum OperationResult {DONE, INPUT_NOT_CONSUMED, COMPUTING};
    
    public interface Result {
        public OperationResult getOperationResult();
        public void setOperationResult(OperationResult operationResult);
        public RavenFuture<OperationResult, Throwable> getResultFuture();
        public void setResultFuture(RavenFuture<OperationResult, Throwable> future);
    };
    
    public void onNext(B message, OperationResult result);
    public void onComplete();
    public void onError();
}
