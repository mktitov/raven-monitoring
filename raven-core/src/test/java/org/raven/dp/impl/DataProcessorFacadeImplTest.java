/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.dp.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.tree.impl.LoggerHelper;
import static org.raven.dp.impl.DataProcessorFacadeImpl.TIMEOUT_MESSAGE;
import org.raven.sched.ExecutorServiceException;
import static org.easymock.EasyMock.*;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.raven.dp.FutureCallback;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorLogic;
import org.raven.dp.FutureTimeoutException;
import org.raven.dp.RavenFuture;
import org.raven.dp.Terminated;
import org.raven.dp.UnbecomeFailureException;
import org.raven.dp.UnhandledMessage;
import org.raven.dp.UnhandledMessageException;
import org.raven.ds.TimeoutMessageSelector;
import org.raven.log.LogLevel;
import org.raven.test.InThreadExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorFacadeImplTest extends RavenCoreTestCase {
    private LoggerHelper logger;
    
    @Before
    public void prepare() {
        testsNode.setLogLevel(LogLevel.TRACE);
        logger = new LoggerHelper(testsNode, null);
    }
    
    @Test
    public void sendTest() throws Exception {
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData("test")).andReturn(Boolean.TRUE);
        replay(realProcessor);
        
        InThreadExecutorService executor = new InThreadExecutorService();
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(executor, realProcessor, executor, logger));
        processor.send("test");
        
        verify(realProcessor);
    }
    
    @Test
    public void sendToWithReply() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor sender = createMock(DataProcessor.class);
        DataProcessor receiver = createMock(DataProcessor.class);
        expect(receiver.processData("test")).andReturn("ok");
        expect(sender.processData("ok")).andReturn(DataProcessor.VOID);
        replay(sender, receiver);
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, receiver, executor, logger));
        senderFacade.sendTo(receiverFacade, "test");
        Thread.sleep(100);
        verify(sender, receiver);
    }
    
    @Test
    public void sendToWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor sender = createMock(DataProcessor.class);
        DataProcessor receiver = createMock(DataProcessor.class);
        expect(receiver.processData("test")).andReturn(DataProcessor.VOID);
        replay(sender, receiver);
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, receiver, executor, logger));
        senderFacade.sendTo(receiverFacade, "test");
        Thread.sleep(100);
        verify(sender, receiver);
    }
    
    @Test(timeout = 500l)
    public void capacityTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData(checkDataPacket("test", 100))).andReturn(Boolean.TRUE);
        expect(realProcessor.processData(checkDataPacket("test3",0))).andReturn(Boolean.TRUE);
        replay(realProcessor);        
        
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(executor, realProcessor, executor, logger).withQueueSize(1));
        assertTrue(processor.send("test"));
        assertFalse(processor.send("test2"));
        Thread.sleep(110);
        assertTrue(processor.send("test3"));
        
        Thread.sleep(200l);
        
        verify(realProcessor);        
    }
    
    @Test(timeout = 4000l)
    public void loadTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = createMock(DataProcessor.class);
        expect(realProcessor.processData(checkDataPacket("test", 1))).andReturn(Boolean.TRUE).times(1000);
        replay(realProcessor);
        
        
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(executor, realProcessor, executor, logger));
        Thread source1 = new Thread(new Source(processor, 1, 500));
        Thread source2 = new Thread(new Source(processor, 2, 500));
        source1.start();
        source2.start();
        
        Thread.sleep(2000);
        verify(realProcessor);
    }
    
    @Test
    public void setTimeoutTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(new DataProcessorFacadeConfig(testsNode, collector, executor, logger));
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.setTimeout(100l, 2l);
        Thread.sleep(205);
        facade.setTimeout(200l, 2l);
        Thread.sleep(395l);
        facade.send("TEST");
        Thread.sleep(203);
        facade.terminate();
        checkMessages(collector, 
                new Object[]{TIMEOUT_MESSAGE, TIMEOUT_MESSAGE, TIMEOUT_MESSAGE, "TEST", TIMEOUT_MESSAGE}, 
                new long[]{100, 200, 405, 600, 800}, 
                new long[]{ 10,  10,  10,  10,  10}, 
                initialTime);
    }
    
    @Test
    public void setTimeoutWithSelectorTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, collector, executor, logger));
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.setTimeout(50l, 2l, new TimeoutMessageSelector() {
            public boolean resetTimeout(Object message) {
                return "TEST".equals(message);
            }
        });
        Thread.sleep(40);
        facade.send("TEST1");
        Thread.sleep(50);
        facade.send("TEST");
        Thread.sleep(20);
        facade.terminate();
        checkMessages(collector, 
                new Object[]{"TEST1",  TIMEOUT_MESSAGE, "TEST"}, 
                new long[]{40, 50, 90}, 
                new long[]{ 10,  10,  10}, 
                initialTime);
    }
    
    @Test
    public void sendDelayedTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(new DataProcessorFacadeConfig(testsNode, collector, executor, logger));
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.sendDelayed(100l, "TEST");
        Thread.sleep(110);
        facade.terminate();
        checkMessages(collector, 
                new Object[]{"TEST"}, 
                new long[]{100}, 
                new long[]{ 10}, 
                initialTime);
    }
    
    @Test
    public void sendToDelayedWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector sender = new MessageCollector();
        MessageCollector receiver = new MessageCollector();
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, receiver, executor, logger));
        long ts = System.currentTimeMillis();
        senderFacade.sendDelayedTo(receiverFacade, 50, "test");
        Thread.sleep(100);
        checkMessages(receiver, new Object[]{"test"}, new long[]{50}, new long[]{5}, ts);
        assertTrue(sender.messages.isEmpty());
    }
    
    @Test
    public void sendRepeatedly() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(new DataProcessorFacadeConfig(testsNode, collector, executor, logger));
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.sendRepeatedly(100l, 50l, 5, "TEST");
        Thread.sleep(400);
        facade.terminate();
        checkMessages(collector, 
                new Object[]{"TEST", "TEST", "TEST", "TEST", "TEST"}, 
                new long[]{100, 150, 200, 250, 300}, 
                new long[]{ 10,  10,  15,  15,  15}, 
                initialTime);
        
    }
    
    @Test
    public void sendRepeatedlyToWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector sender = new MessageCollector();
        MessageCollector receiver = new MessageCollector();
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, receiver, executor, logger));
        long ts = System.currentTimeMillis();
        senderFacade.sendRepeatedlyTo(receiverFacade, 0, 50, 2, "test");
        Thread.sleep(120);
        checkMessages(receiver, new Object[]{"test", "test"}, new long[]{0, 50}, new long[]{5, 5}, ts);
        assertTrue(sender.messages.isEmpty());
    }
    
    
    @Test
    public void logicTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, new Logic(), executor, logger));
        Thread.sleep(5000);
        
    }
    
    @Test
    public void askTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = createMock(DataProcessor.class);
        expect(processor.processData("test")).andReturn("ok");
        replay(processor);
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, processor, executor, logger));
        assertEquals("ok", facade.ask("test").get());
        verify(processor);
    }
    
    @Test(expected = ExecutionException.class)
    public void askWithErrorTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = createMock(DataProcessor.class);
        expect(processor.processData("test")).andThrow(new Exception("error"));
        replay(processor);
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, processor, executor, logger));
        try {
            assertEquals("ok", facade.ask("test").get());
        } finally {
            verify(processor);
        }
    }
    
    @Test
    public void askWithCallbackTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = createMock(DataProcessor.class);
        FutureCallback callback = createMock(FutureCallback.class);
        expect(processor.processData("test")).andReturn("ok");
        callback.onSuccess("ok");
        replay(processor, callback);
        DataProcessorFacadeImpl facade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig(testsNode, processor, executor, logger));
        RavenFuture future = facade.ask("test");
        future.onComplete(callback);
        Thread.sleep(100);
        
        verify(processor, callback);
    }
    
    @Test
    public void stopTest() throws Exception {
        DataProcessorLogic processor = createStrictMock(DataProcessorLogic.class);
        processor.init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
//        processor.setSender(null);
        expect(processor.processData("test")).andReturn(DataProcessor.VOID);
        processor.postStop();
        replay(processor);
        
//        InThreadExecutorService executor = new InThreadExecutorService();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, processor, executor, logger).build();
        facade.send("test");
        facade.stop();
        facade.send("test2");
        facade.stop();
        Thread.sleep(100);
        verify(processor);
    }
    
    @Test()
//    @Test(expected = FutureTimeoutException.class)
    public void askStopTestWithTimeout() throws Exception {
        DataProcessorLogic processor = createStrictMock(DataProcessorLogic.class);
        processor.init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
//        processor.setSender(null);
        expect(processor.processData("test")).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Thread.sleep(200);
                return DataProcessor.VOID;
            }
        });
        replay(processor);
        
//        InThreadExecutorService executor = new InThreadExecutorService();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(testsNode, processor, executor, logger).build();
        facade.send("test");
        try {
//            facade.askStop(100);
            assertEquals(Boolean.TRUE, facade.askStop(100).get());
//            Thread.sleep(110);
        } finally {
            assertTrue(facade.isTerminated());
            facade.send("test2");
            facade.stop();
            Thread.sleep(100);
            verify(processor);
        }
    }
    
    @Test
    public void askStopTest() throws Exception {
        DataProcessorLogic processor = createStrictMock(DataProcessorLogic.class);
        processor.init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
//        processor.setSender(null);
        expect(processor.processData("test")).andReturn(DataProcessor.VOID);
        processor.postStop();
        replay(processor);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, processor, executor, logger).build();
        facade.send("test");
        RavenFuture res = facade.askStop();
        facade.send("test2");
        facade.stop();
        
        assertEquals(Boolean.TRUE, res.get());
        
        verify(processor);
        
    }
    
    @Test
    public void stopFromOtherFacadeTest() throws Exception {
        DataProcessorLogic processor = createStrictMock(DataProcessorLogic.class);
        DataProcessor stopListener = createMock(DataProcessor.class);
        processor.init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
//        processor.setSender(null);
        expect(processor.processData("test")).andReturn(DataProcessor.VOID);
        processor.postStop();
        
        expect(stopListener.processData(isA(Terminated.class))).andReturn(DataProcessor.VOID);
        replay(processor, stopListener);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, processor, executor, logger).build();
        DataProcessorFacade stopper = new DataProcessorFacadeConfig(executor, stopListener, executor, logger).build();
        
        facade.send("test");
        stopper.sendTo(facade, DataProcessorFacade.STOP_MESSAGE);
        facade.send("test2");
        facade.stop();
        
        Thread.sleep(100);
                
        verify(processor, stopListener);
        
    }
    
    @Test
    public void watchTest() throws Exception {
        DataProcessor processor = createStrictMock(DataProcessor.class);
        replay(processor);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, processor, executor, logger).build();
        
        //register watcher before stop
        RavenFuture watcher = facade.watch();
        assertFalse(watcher.isDone());
        facade.stop();
        assertTrue((Boolean)watcher.get());
        assertTrue(watcher.isDone());
        
        //register watcher after stop
        watcher = facade.watch();
        assertFalse(watcher.isDone());
        assertTrue((Boolean)watcher.get());
        assertTrue(watcher.isDone());
        
        verify(processor);
    }
    
    @Test
    public void watchFromOtherFacadeTest() throws Exception {
        DataProcessor processor = createStrictMock(DataProcessor.class);
        DataProcessor watchProcessor = createMock(DataProcessor.class);
        
        expect(watchProcessor.processData(isA(Terminated.class))).andReturn(DataProcessor.VOID);
        expect(watchProcessor.processData(isA(Terminated.class))).andReturn(DataProcessor.VOID);
        replay(processor, watchProcessor);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, processor, executor, logger).build();
        DataProcessorFacade watcher = new DataProcessorFacadeConfig(executor, watchProcessor, executor, logger).build();
        
        facade.watch(watcher);
        facade.stop();
        
        Thread.sleep(100);
        facade.watch(watcher);
        Thread.sleep(100);
                
        verify(processor, watchProcessor);
    }
    
    @Test
    public void becomeUnbecomeTest() throws Exception {
        ExecutorService executor = new InThreadExecutorService();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, new LogicWithBehaviour(), executor, logger).build();
        assertEquals("MAIN", facade.ask("CHANGE_TO_B1").get());
        assertEquals("B1", facade.ask("CHANGE_TO_MAIN").get());
        assertEquals("MAIN", facade.ask("TEST").get());
    }
    
    @Test
    public void unbecomeExceptionTest() throws Exception {
        ExecutorService executor = new InThreadExecutorService();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, new LogicWithBehaviour(), executor, logger).build();
        assertEquals("MAIN", facade.ask("CHANGE_TO_B1_WITH_REPLACE").get());
        assertEquals("B1", facade.ask("TEST").get());
        try {
            assertEquals("MAIN", facade.ask("CHANGE_TO_MAIN").get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof UnbecomeFailureException);
        }
    }
    
    @Test
    public void unhandledMessageTest() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector unhandledProcessor = new MessageCollector();
        DataProcessorFacade unhandledChannel = new DataProcessorFacadeConfig(executor, unhandledProcessor, executor, logger).build();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(executor, new LogicWithBehaviour(), executor, logger)
                .withUnhandledMessageProcessor(unhandledChannel)
                .build();
        try {
            facade.ask("UNHANDLE").get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof UnhandledMessageException);
            assertEquals("UNHANDLE", ((UnhandledMessageException)e.getCause()).getUnhandledMessage());
        }
        Thread.sleep(100);
        assertEquals(1, unhandledProcessor.messages.size());
        assertTrue(unhandledProcessor.messages.get(0).message instanceof UnhandledMessage);
        UnhandledMessage mess = (UnhandledMessage) unhandledProcessor.messages.get(0).message;
        assertEquals("UNHANDLE", mess.getMessage());
        assertNull(mess.getSender());
        assertSame(facade, mess.getReceiver());
    }
    
    @Test
    public void unhandledMessageFromFacadeTest() throws Exception {
        DataProcessor emptyProcessor = createMock(DataProcessor.class);
        replay(emptyProcessor);
        
        ExecutorService executor = createExecutor();
        MessageCollector unhandledProcessor = new MessageCollector();
        DataProcessorFacade sender = new DataProcessorFacadeConfig(testsNode, emptyProcessor, executor, createLogger("Sender. ")).build();
        DataProcessorFacade unhandledChannel = new DataProcessorFacadeConfig(testsNode, unhandledProcessor, executor, createLogger("Unhandled channel. ")).build();
        DataProcessorFacade facade = new DataProcessorFacadeConfig(testsNode, new LogicWithBehaviour(), executor, createLogger("Receiver. "))
                .withUnhandledMessageProcessor(unhandledChannel)
                .build();
        sender.sendTo(facade, "UNHANDLE");
        Thread.sleep(100);
        assertEquals(1, unhandledProcessor.messages.size());
        assertTrue(unhandledProcessor.messages.get(0).message instanceof UnhandledMessage);
        UnhandledMessage mess = (UnhandledMessage) unhandledProcessor.messages.get(0).message;
        assertEquals("UNHANDLE", mess.getMessage());
        assertSame(sender, mess.getSender());
        assertSame(facade, mess.getReceiver());
        
        verify(emptyProcessor);
    }
    
    private LoggerHelper createLogger(String prefix) {
        return new LoggerHelper(logger, prefix);
    }
    
    private void checkMessages(MessageCollector collector, Object[] messages, long[] timings, long[] accuracy, long initialTime) {
        assertEquals(String.format("Expected messages: %s; Received messages: %s", Arrays.toString(messages), collector.messages),
                messages.length, collector.messages.size());
        for (int i=0; i<messages.length; ++i) {
            Message message = collector.messages.get(i);
            assertEquals("Invalid message at position "+i, messages[i], message.message);
            long timing = message.ts-initialTime;
            String assertMess = String.format("Timing %s for message (%s:%s) must be between (%s) and (%s)", 
                    timing, i, messages[i], timings[i], timings[i]+accuracy[i]);
            assertTrue(assertMess, timing>=timings[i] && timing<=timings[i]+accuracy[i]);
        }
    }
    
    public static String checkDataPacket(final String dataPacket, final long pause) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                assertEquals(dataPacket, argument);
                try {
                    if (pause>0)
                        Thread.sleep(pause);
                } catch (InterruptedException ex) {
                }
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    private ExecutorService createExecutor() {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setMaximumQueueSize(10);
        executor.setCorePoolSize(40);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        
        return executor;
    }
    
    private class LogicWithBehaviour extends AbstractDataProcessorLogic {
        
        private final Behaviour b1 = new Behaviour("B1") {
            public Object processData(Object message) throws Exception {
                if ("CHANGE_TO_MAIN".equals(message))
                    unbecome();
                return "B1";
            }
        };

        public Object processData(Object message) throws Exception {
            if ("CHANGE_TO_B1".equals(message))
                become(b1, false);
            else if ("CHANGE_TO_B1_WITH_REPLACE".equals(message))
                become(b1, true);
            else if ("UNHANDLE".equals(message)) 
                return unhandled();
            return "MAIN";
        }
    }
    
    private class Logic extends AbstractDataProcessorLogic {

        @Override
        public void init(DataProcessorFacade facade, DataProcessorContext context) {
            super.init(facade, context);
            try {
                facade.sendRepeatedly(50, 50, 0, "TEST");
                System.out.println("\n\n\n!!!INITIALIZED ");
            } catch (ExecutorServiceException ex) {
                System.out.println("\n\n\n!!!ERROR: "+ex.toString());
                Logger.getLogger(DataProcessorFacadeImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public Object processData(Object dataPackage) {
            System.out.println("\n\n>>>\n"+dataPackage);
            return null;
        }        
    }
    
    private class MessageCollector implements DataProcessor {
        private final List<Message> messages = new LinkedList<Message>();
        
        public Object processData(Object dataPackage) {
            messages.add(new Message(dataPackage, System.currentTimeMillis()));
            return VOID;
        }
    }
    
    private class Message {
        private final Object message;
        private final long ts;

        public Message(Object message, long ts) {
            this.message = message;
            this.ts = ts;
        }

        @Override
        public String toString() {
            return message.toString();
        }
    }
    
    private class Source implements Runnable {
        private final DataProcessorFacade processor;
        private final long pause;
        private final int count;

        public Source(DataProcessorFacade processor, long pause, int count) {
            this.processor = processor;
            this.pause = pause;
            this.count = count;
        }
        
        public void run() {
            try {
                for (int i=0; i<count; ++i) {
                    processor.send("test");
                    Thread.sleep(pause);
                }
            } catch (Exception e) {
                
            }
        }        
    }
    
}
