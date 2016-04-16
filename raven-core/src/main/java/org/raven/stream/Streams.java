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

import java.util.Collection;

/**
 *
 * @author Mikhail Titov
 */
public class Streams {
//    public static <A> SourceDesc<A> fromSource(Source<A>) {
//        return null;
//    }
//    
    public interface Materializer
    
    public interface Sender<T> {
        public Collection<Consumer> getConsumers();
    }
    
    public interface Consumer<T> {
        public Collection<Sender<T>> getSenders();
    }
    
    
//    public final class SourceDesc<T> {
//        private final Source<T> source;
//
//        public SourceDesc(Source<T> source) {
//            this.source = source;
//        }
//
//        public Source<T> getSource() {
//            return source;
//        }
//                
//    }
//    
//    public final class SinkDesc {
//        private final Sink<T> sink;
//        private final SourceDesc<T> source;
//    }
}
