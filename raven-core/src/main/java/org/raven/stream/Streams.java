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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.stream.impl.StreamControllerDP;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class Streams {
//    public static <A> SourceDesc<A> fromSource(Source<A>) {
//        return null;
//    }
//    
//    public interface StreamElement {
//        public DataProcessor materialize();
//        public String getName();
//        public String getType();
//    }
//    
//    public abstract class AbstractStreamElem implements StreamElement{
//        private final String name;
//
//        public AbstractStreamElem(String name) {
//            this.name = name;
//        }
//
//        @Override public String getName() {
//            return name;
//        }
//    }
//    
//    public interface SourceDsl<A> {
//        public <B> SourceDsl<B> via(FlowLogic<A,B> logic);
//        public <B> SourceDsl<B> alsoVia(Flow<A,B> flow);        
//        public SinkDsl<A> to(SinkLogic<A> logic);
//        public SinkDsl<A> alsoTo(SinkLogic<A> logic);
//    }
//    
//    public interface SinkDsl<A> {
//        public SinkDsl<A> alsoFrom(SourceDsl<A> source);
//        public Stream materialize(ExecutorService executor);
//    }
//    
//    public interface FlowDsl {
//        
//    }
//    
//    public interface Sender<T> extends StreamElement {
//        public List<Consumer<T>> getConsumers();
//    }
//    
//    public interface Consumer<T> extends StreamElement {
//        public List<Sender<T>> getSenders();
//    }
//    
//    public interface Pipe<S,T> extends Sender<S>, Consumer<T> {        
//    }
//    
//    protected static abstract class AbstractSender<T> extends AbstractStreamElem implements Sender<T>, SourceDsl<T> {
//        protected final List<Consumer<T>> consumers = new ArrayList<>();
//
//        public AbstractSender(String name) {
//            super(name);
//        }
//        
//        @Override public List<Consumer<T>> getConsumers() {
//            return consumers;
//        }
//
//        @Override
//        public <B> SourceDsl<B> alsoVia(Flow<T, B> flow) {
//            return null;
//        }
//        
//    }
//    
//    protected static abstract class AbstractConsumer<T> extends AbstractStreamElem implements Consumer<T> {
//        protected final List<Sender<T>> senders = new ArrayList<>();
//
//        public AbstractConsumer(String name) {
//            super(name);
//        }
//
//        @Override public List<Sender<T>> getSenders() {
//            return senders;
//        }
//    }
//    
//    public abstract class Source<T> extends AbstractSender<T> {
//        private final SourceLogic<T> logic;
//
//        public Source(String name, SourceLogic<T> logic) {
//            super(name);
//            this.logic = logic;
//        }
//
//        @Override
//        public String getType() {
//            return logic.getLogicType();
//        }
//        
//        public Source(final SourceLogic<T> logic) {
//            this(null, logic);
//        }
//
//        public Source() {
//            this(null, null);
//        }
//        
//        @Override public DataProcessor materialize() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }        
//    }
//    
//    public class Sink<T> extends AbstractConsumer<T> {
//        private final SinkLogic<T> logic;
//
//        public Sink(String name, SinkLogic<T> logic) {
//            super(name);
//            this.logic = logic;
//        }
//
//        @Override
//        public String getType() {
//            return logic.getLogicType();
//        }
//
//        public Sink(SinkLogic<T> logic) {
//            this(null, logic);
//        }
//
//        public Sink() {
//            this(null);
//        }
//
//        @Override public DataProcessor materialize() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }     
//        
////        public Stream toStream(ExecutorService executor) {
////            return null;
////        }
//    }
//    
////    public class Stream {
////        private final Sink sink;
////        private final ExecutorService executor;
////        private final DataProcessorFacade controller;
////
////        public Stream(Sink sink, ExecutorService executor) {
////            this.sink = sink;
////            this.executor = executor;
////        }
////        
////        
////    }
//    
//    public class Flow<S, T> extends AbstractStreamElem implements Sender<S>, Consumer<T> {
//        private final FlowLogic<S,T> logic;
//        private final List<Consumer<S>> consumers = new ArrayList<>();
//        private final List<Sender<T>> senders = new ArrayList<>();
//
//        public Flow(String name, FlowLogic<S, T> logic) {
//            super(name);
//            this.logic = logic;
//        }
//
//        public Flow(FlowLogic<S, T> logic) {
//            this(null, logic);
//        }
//
//        @Override
//        public String getType() {
//            return logic.getLogicType();
//        }
//
//        @Override public List<Consumer<S>> getConsumers() {
//            return consumers;
//        }
//
//        @Override public DataProcessor materialize() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override public List<Sender<T>> getSenders() {
//            return senders;
//        }        
//    }
//    
//    public static class Stream {}
//    
//    public static class StreamMaterializer {
//
//        public final static Stream materialize(String name, Node owner, Consumer consumer, ExecutorService executor) {
//            Map<StreamElement, DataProcessorFacade> cache = new HashMap<>();
//            final LoggerHelper logger = new LoggerHelper(owner, "Stream -> ");
//            DataProcessorFacade controller = new DataProcessorFacadeConfig(name, owner, new StreamControllerDP(), executor, logger).build();
//            return null;
//        }
//    }
//    
    
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
