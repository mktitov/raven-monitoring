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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.raven.dp.DataProcessorFacade;

/**
 *
 * @author Mikhail Titov
 */
public class Streams {
//    public static <A> SourceDesc<A> fromSource(Source<A>) {
//        return null;
//    }
//    
    public interface Materializer {
        public DataProcessorFacade materialize();
    }
    
    public interface Sender<T> extends Materializer {
        public List<Consumer<T>> getConsumers();
    }
    
    public interface Consumer<T> extends Materializer {
        public List<Sender<T>> getSenders();
    }
    
    public interface Pipe<S,T> extends Sender<S>, Consumer<T> {        
    }
    
    protected abstract class AbstractSender<T> implements Sender<T> {
        protected final List<Consumer<T>> consumers = new ArrayList<>();
        
        @Override public List<Consumer<T>> getConsumers() {
            return consumers;
        }        
    }
    
    protected abstract class AbstractConsumer<T> implements Consumer<T> {
        protected final List<Sender<T>> senders = new ArrayList<>();

        @Override public List<Sender<T>> getSenders() {
            return senders;
        }
    }
    
    public class Source<T> extends AbstractSender<T> {
        private final SourceLogic<T> logic;
        
        public Source(final SourceLogic<T> logic) {
            this.logic = logic;
        }

        public Source() {
            this(null);
        }
        
        @Override public DataProcessorFacade materialize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }        
    }
    
    public class Sink<T> extends AbstractConsumer<T> {
        public final SinkLogic<T> logic;

        public Sink(SinkLogic<T> logic) {
            this.logic = logic;
        }

        public Sink() {
            this(null);
        }

        @Override public DataProcessorFacade materialize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }        
    }
    
    public class Flow<S, T> implements Sender<S>, Consumer<T> {
        private final FlowLogic<S,T> logic;
        private final List<Consumer<S>> consumers = new ArrayList<>();
        private final List<Sender<T>> senders = new ArrayList<>();

        public Flow(FlowLogic<S, T> logic) {
            this.logic = logic;
        }

        @Override public List<Consumer<S>> getConsumers() {
            return consumers;
        }

        @Override public DataProcessorFacade materialize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override public List<Sender<T>> getSenders() {
            return senders;
        }        
    }
    
    
//    public class Flow<S, T>         
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
