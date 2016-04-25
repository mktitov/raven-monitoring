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
public interface FlowLogic<A, B> extends Logic {
    public final static int PROCESSED = 0;
    public final static int INPUT_NOT_CONSUMED = 1;
    public final static int OUTPUT_NOT_READY = 2;
    public final static int COMPUTING = 4;
    
    public interface MapResult<B> {
        public void setValue(B value);
        public void getValue(B vaule);
        public void setFuture(RavenFuture<Integer, Throwable> future);
    }
    
    public int map(A input, MapResult<B> result);
    public void onComplete();
    public void onError();
}
