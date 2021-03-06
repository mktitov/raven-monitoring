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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.tree.impl.LoggerHelper;
import static org.raven.dp.impl.DataProcessorFacadeImpl.TIMEOUT_MESSAGE;
import org.raven.sched.ExecutorServiceException;
//import static org.easymock.EasyMock.*;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.raven.MockitoAnswer;
import org.raven.dp.FutureCallback;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorLogic;
import org.raven.dp.FutureCanceledException;
import org.raven.dp.FutureTimeoutException;
import org.raven.dp.RavenFuture;
import org.raven.dp.Terminated;
import org.raven.dp.UnhandledMessage;
import org.raven.ds.TimeoutMessageSelector;
import org.raven.log.LogLevel;
import org.raven.sched.Task;
import org.raven.sched.impl.AbstractTask;
import org.raven.test.InThreadExecutorService;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class DataProcessorFacadeImplTest extends RavenCoreTestCase {
    private LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "[TEST]", "[TEST] ", LoggerFactory.getLogger("TEST"));
    
    private final static Answer VOID_ANSWER = new Answer() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            return DataProcessor.VOID;
        }
    };        
    
    @Before
    public void prepare() {
        testsNode.setLogLevel(LogLevel.TRACE);        
//        logger = new LoggerHelper(testsNode, null);
    }
    
    @Test(timeout = 5000l)
    public void sendTest() throws Exception {
        DataProcessor realProcessor = mock(DataProcessor.class);
        when(realProcessor.processData(any())).thenReturn(Boolean.TRUE);
        
        InThreadExecutorService executor = new InThreadExecutorService();
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("receiver", executor, realProcessor, executor, logger));
        processor.send("test");
        
        verify(realProcessor).processData("test");
        verifyNoMoreInteractions(realProcessor);
    }
  
    @Test(timeout = 5000l)
    public void maxMessagesPerCycleTest(
            @Mocked final ExecutorService executor,
            @Mocked final DataProcessor processor
    ) throws Exception
    {
        new Expectations() {{
            executor.execute((Task)any); times=2; result = new Delegate(){
                boolean execute(final Task task) {
                    new Thread(){
                        @Override public void run() {
                            task.run();
                        }
                    }.start();
                    return true;
                }
            };
            processor.processData("test"); times=2; result = new Delegate() {
                void processData(Object message) throws Exception {
                    Thread.sleep(100);
                }
            };
        }};
        DataProcessorFacade facade = new DataProcessorFacadeConfig(
                "receiver", executor, processor, executor, logger).withMaxMessagesPerCycle(1).build();
        facade.send("test");
        facade.send("test");
        Thread.sleep(500);
    }
    
    @Test(timeout = 5000l)
    public void sendToWithReply() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor sender = mock(DataProcessor.class);
        DataProcessor receiver = mock(DataProcessor.class);
        when(receiver.processData("test")).thenReturn("ok");
        when(sender.processData("ok")).thenReturn(DataProcessor.VOID);
        
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("sender", testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("receiver", testsNode, receiver, executor, logger));
        senderFacade.sendTo(receiverFacade, "test");
        
        Thread.sleep(100);
        verify(receiver).processData("test");
        verify(sender).processData("ok");
    }
    
    @Test(timeout = 5000l)
    public void sendToWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor sender = mock(DataProcessor.class, VOID_ANSWER);
        DataProcessor receiver = mock(DataProcessor.class);
        
        DataProcessorFacadeImpl senderFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("sender", testsNode, sender, executor, logger));
        DataProcessorFacadeImpl receiverFacade = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("receiver", testsNode, receiver, executor, logger));
        senderFacade.sendTo(receiverFacade, "test");
        Thread.sleep(100);
        
        verify(receiver).processData("test");
        verifyZeroInteractions(receiver);
    }
    
    @Test(timeout = 5000l)
    public void capacityTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = mock(DataProcessor.class, new MockitoAnswer(true));
        when(realProcessor.processData("test")).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(100);
                return Boolean.TRUE;
            }
        });
        when(realProcessor.processData("test3")).thenReturn(Boolean.TRUE);
        
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("receiver", executor, realProcessor, executor, logger).withQueueSize(1));
        assertTrue(processor.send("test"));
        assertFalse(processor.send("test2"));
        Thread.sleep(110);
        assertTrue(processor.send("test3"));
        
        Thread.sleep(200l);
        
        verify(realProcessor).processData("test");
        verify(realProcessor).processData("test3");
        verifyNoMoreInteractions(realProcessor);
    }
    
    @Test(timeout = 4000l)
    public void loadTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor realProcessor = mock(DataProcessor.class, new MockitoAnswer(true));
        when(realProcessor.processData("test")).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(1);
                return true;
            }
        });
        
        DataProcessorFacadeImpl processor = new DataProcessorFacadeImpl(
                new DataProcessorFacadeConfig("receiver", executor, realProcessor, executor, logger));
        Thread source1 = new Thread(new Source(processor, 1, 500));
        Thread source2 = new Thread(new Source(processor, 2, 500));
        source1.start();
        source2.start();
        
        Thread.sleep(2000);
        verify(realProcessor, times(1000)).processData("test");
    }
    
    @Test(timeout = 5000l)
    public void setReceiveTimeoutTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, collector, executor, logger).build();
        Thread.sleep(1000l);
        facade.setReceiveTimeout(100l);
        long initialTime = System.currentTimeMillis();
        Thread.sleep(215);
        facade.setReceiveTimeout(200l);
        Thread.sleep(395l);
        facade.send("TEST");
        Thread.sleep(203);
        facade.stop();
        checkMessages(collector, 
                new Object[]{TIMEOUT_MESSAGE, TIMEOUT_MESSAGE, TIMEOUT_MESSAGE, "TEST", TIMEOUT_MESSAGE}, 
                new long[]{100, 200, 405, 600, 800}, 
                new long[]{ 15,  15,  20,  25,  30}, 
                initialTime);
    }
    
    @Test(timeout = 5000l)
    public void setReceiveTimeoutWithSelectorTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, collector, executor, logger).build();
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.setReceiveTimeout(50l, new TimeoutMessageSelector() {
            public boolean resetTimeout(Object message) {
                return "TEST".equals(message);
            }
        });
        Thread.sleep(40);
        facade.send("TEST1");
        Thread.sleep(50);
        facade.send("TEST");
        Thread.sleep(20);
        facade.stop();
        checkMessages(collector, 
                new Object[]{"TEST1",  TIMEOUT_MESSAGE, "TEST"}, 
                new long[]{40, 50, 90}, 
                new long[]{ 15,  15,  15}, 
                initialTime);
    }
    
    @Test(timeout = 5000l)
    public void sendDelayedTest() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, collector, executor, logger).build();
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.sendDelayed(100l, "TEST");
        Thread.sleep(110);
        facade.stop();
        checkMessages(collector, 
                new Object[]{"TEST"}, 
                new long[]{100}, 
                new long[]{ 15}, 
                initialTime);
    }
    
    @Test(timeout = 5000l)
    public void sendToDelayedWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector sender = new MessageCollector();
        MessageCollector receiver = new MessageCollector();
        DataProcessorFacade senderFacade = new DataProcessorFacadeConfig("sender", testsNode, sender, executor, logger).build();
        DataProcessorFacade receiverFacade = new DataProcessorFacadeConfig("receiver", testsNode, receiver, executor, logger).build();
        long ts = System.currentTimeMillis();
        senderFacade.sendDelayedTo(receiverFacade, 50, "test");
        Thread.sleep(100);
        checkMessages(receiver, new Object[]{"test"}, new long[]{50}, new long[]{15}, ts);
        assertTrue(sender.messages.isEmpty());
    }
    
    @Test(timeout = 5000l)
    public void sendRepeatedly() throws Exception {
        MessageCollector collector = new MessageCollector();
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, collector, executor, logger).build();
        Thread.sleep(1000l);
        long initialTime = System.currentTimeMillis();
        facade.sendRepeatedly(100l, 50l, 5, "TEST");
        Thread.sleep(400);
        facade.stop();
        checkMessages(collector, 
                new Object[]{"TEST", "TEST", "TEST", "TEST", "TEST"}, 
                new long[]{100, 150, 200, 250, 300}, 
                new long[]{ 15,  15,  15,  15,  20}, 
                initialTime);
        
    }
    
    @Test(timeout = 5000l)
    public void sendRepeatedlyToWithoutReply() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector sender = new MessageCollector();
        MessageCollector receiver = new MessageCollector();
        DataProcessorFacade senderFacade = new DataProcessorFacadeConfig("sender", testsNode, sender, executor, logger).build();
        DataProcessorFacade receiverFacade = new DataProcessorFacadeConfig("receiver", testsNode, receiver, executor, logger).build();
        long ts = System.currentTimeMillis();
        senderFacade.sendRepeatedlyTo(receiverFacade, 0, 50, 2, "test");
        Thread.sleep(120);
        checkMessages(receiver, new Object[]{"test", "test"}, new long[]{0, 50}, new long[]{10, 5}, ts);
        assertTrue(sender.messages.isEmpty());
    }
    
    
    @Test(timeout = 6000l)
    public void logicTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, new Logic(), executor, logger).build();
        Thread.sleep(5000);
    }
    
    @Test(timeout = 5000l)
    public void askTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        when(processor.processData("test")).thenReturn("ok");

        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        assertEquals("ok", facade.ask("test").get());
        verify(processor).processData("test");
        verifyNoMoreInteractions(processor);
    }
    
    @Test(timeout = 5000l)
    public void askWithFutureAsResultTest() throws Exception {
        final ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        when(processor.processData("test")).then(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                RavenFutureImpl future = new RavenFutureImpl(executor);
                future.set("ok");
                return future;
            }
        });
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        assertEquals("ok", facade.ask("test").get());
        verify(processor).processData("test");
        verifyNoMoreInteractions(processor);
    }
    
    @Test(timeout = 5000, expected = ExecutionException.class)
    public void askWithErrorTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        when(processor.processData("test")).thenThrow(new Exception("error"));

        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        try {
            assertEquals("ok", facade.ask("test").get());
        } finally {
            verify(processor).processData("test");
            verifyNoMoreInteractions(processor);
        }
    }
    
    @Test(timeout = 5000, expected = ExecutionException.class)
    public void askWithErrorFutureAsResultTest() throws Exception {
        final ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        when(processor.processData("test")).then(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                RavenFutureImpl future = new RavenFutureImpl(executor);
                future.setError(new Exception("error"));
                return future;
            }
        });

        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        try {
            assertEquals("ok", facade.ask("test").get());
        } finally {
            verify(processor).processData("test");
            verifyNoMoreInteractions(processor);
        }
    }
    
    @Test(timeout = 5000, expected = FutureCanceledException.class)
    public void askWithCanceledFutureAsResultTest() throws Exception {
        final ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        when(processor.processData("test")).then(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                RavenFutureImpl future = new RavenFutureImpl(executor);
                future.cancel(true);
                return future;
            }
        });

        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        try {
            assertEquals("ok", facade.ask("test").get());
        } finally {
            verify(processor).processData("test");
            verifyNoMoreInteractions(processor);
        }
    }
    
    @Test(timeout = 5000)
    public void askWithCallbackTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessor processor = mock(DataProcessor.class);
        FutureCallback callback = mock(FutureCallback.class);
        when(processor.processData("test")).thenReturn("ok");
        
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        RavenFuture future = facade.ask("test");
        future.onComplete(callback);
        Thread.sleep(100);
        
        verify(processor).processData("test");
        verify(callback).onSuccess("ok");
        verifyNoMoreInteractions(processor, callback);
    }
    
    @Test(timeout = 5000l)
    public void stopTest() throws Exception {
        DataProcessorLogic processor = mock(DataProcessorLogic.class, VOID_ANSWER);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, processor, executor, logger).build();
        facade.send("test");
        facade.stop();
        facade.send("test2");
        facade.stop();
        Thread.sleep(100);
        InOrder order = inOrder(processor);
        order.verify(processor).init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
        order.verify(processor).processData("test");
        order.verify(processor).postStop();
        verifyNoMoreInteractions(processor);
    }
    
    @Test(timeout = 5000l)
//    @Test(expected = FutureTimeoutException.class)
    public void askStopTestWithTimeout() throws Exception {
        DataProcessorLogic processor = mock(DataProcessorLogic.class);
//        processor.init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
//        processor.setSender(null);
        when(processor.processData("test")).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(200);
                return DataProcessor.VOID;
            }
        });
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        facade.send("test");
        try {
//            facade.askStop(100);
            assertEquals(Boolean.TRUE, facade.askStop(150, TimeUnit.MILLISECONDS).get());
//            Thread.sleep(110);
        } finally {
            assertTrue(facade.isTerminated());
            facade.send("test2");
            facade.stop();
            Thread.sleep(100);
            verify(processor).init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
            verify(processor).processData("test");
            verifyNoMoreInteractions(processor);
        }
    }
    
    @Test(timeout = 5000l)
    public void askStopTest() throws Exception {
        DataProcessorLogic processor = mock(DataProcessorLogic.class, VOID_ANSWER);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, processor, executor, logger).build();
        facade.send("test");
        RavenFuture res = facade.askStop();
        facade.send("test2");
        facade.stop();
        
        assertEquals(Boolean.TRUE, res.get());
        verify(processor).init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
        verify(processor).processData("test");
        verify(processor).postStop();
        verifyNoMoreInteractions(processor);
    }
    
    @Test(timeout = 5000l)
    public void stopFromOtherFacadeTest() throws Exception {
        DataProcessorLogic processor = mock(DataProcessorLogic.class, VOID_ANSWER);
        DataProcessor stopListener = mock(DataProcessor.class);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, processor, executor, logger).build();
        DataProcessorFacade stopper = new DataProcessorFacadeConfig("stopper", executor, stopListener, executor, logger).build();
        
        facade.send("test");
        stopper.sendTo(facade, DataProcessorFacade.STOP_MESSAGE);
        facade.send("test2");
        facade.stop();
        
        Thread.sleep(100);
                
        verify(processor).init(isA(DataProcessorFacade.class), isA(DataProcessorContext.class));
        verify(processor).processData("test");
        verify(processor).postStop();
        verifyNoMoreInteractions(processor);
        verifyZeroInteractions(stopListener);
    }
    
    @Test(timeout = 5000l)
    public void watchTest() throws Exception {
        DataProcessor processor = mock(DataProcessor.class);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, processor, executor, logger).build();
        
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
        
        verifyZeroInteractions(processor);
    }
        
    @Test(timeout = 5000l)
    public void watchUnwatchFromOtherFacadeTest() throws Exception {
        DataProcessor processor = mock(DataProcessor.class);
        DataProcessor watchProcessor = mock(DataProcessor.class, VOID_ANSWER);
        DataProcessor watchProcessor2 = mock(DataProcessor.class);
        
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, processor, executor, logger).build();
        DataProcessorFacade watcher = new DataProcessorFacadeConfig("watcher", testsNode, watchProcessor, executor, logger).build();
        DataProcessorFacade watcher2 = new DataProcessorFacadeConfig("watcher2", testsNode, watchProcessor2, executor, logger).build();
        
        facade.watch(watcher2);
        facade.watch(watcher);
        facade.unwatch(watcher2);
        facade.stop();
        
        Thread.sleep(100);
        facade.watch(watcher);
        Thread.sleep(100);
        
        verify(watchProcessor, times(2)).processData(isA(Terminated.class));
        verifyNoMoreInteractions(watchProcessor);
        verifyZeroInteractions(watchProcessor2);
    }
        
    @Test(timeout = 5000l)
    public void becomeUnbecomeTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, new LogicWithBehaviour(), executor, logger).build();
        assertEquals("MAIN", facade.ask("CHANGE_TO_B1").get());
        assertEquals("B1", facade.ask("CHANGE_TO_MAIN").get());
        assertEquals("MAIN", facade.ask("TEST").get());
    }
    
    @Test(timeout = 5000l)
    public void unbecomeExceptionTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, new LogicWithBehaviour(), executor, logger).build();
        assertEquals("MAIN", facade.ask("CHANGE_TO_B1_WITH_REPLACE").get());
        assertEquals("B1", facade.ask("TEST").get());
        try {
            assertEquals("MAIN", facade.ask("CHANGE_TO_MAIN").get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof FutureTimeoutException);
        }
    }
    
    @Test(timeout = 5000l)
    public void unhandledMessageTest() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector unhandledProcessor = new MessageCollector();
        DataProcessorFacade unhandledChannel = new DataProcessorFacadeConfig("unhandledChannel", executor, unhandledProcessor, executor, logger).build();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, new LogicWithBehaviour(), executor, logger)
                .withUnhandledMessageProcessor(unhandledChannel)
                .build();
        try {
            facade.ask("UNHANDLE").get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof FutureTimeoutException);
//            assertEquals("UNHANDLE", ((UnhandledMessageException)e.getCause()).getUnhandledMessage());
        }
        Thread.sleep(100);
        assertEquals(1, unhandledProcessor.messages.size());
        assertTrue(unhandledProcessor.messages.get(0).message instanceof UnhandledMessage);
        UnhandledMessage mess = (UnhandledMessage) unhandledProcessor.messages.get(0).message;
        assertEquals("UNHANDLE", mess.getMessage());
        assertNotNull(mess.getSender());
        assertSame(facade, mess.getReceiver());
    }
    
    @Test(timeout = 5000l)
    public void unhandledMessageTest2() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector unhandledProcessor = new MessageCollector();
        DataProcessorFacade unhandledChannel = new DataProcessorFacadeConfig("unhandledChannel", executor, unhandledProcessor, executor, logger).build();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", executor, new LogicWithBehaviour(), executor, logger)
                .withUnhandledMessageProcessor(unhandledChannel)
                .build();
        try {
            facade.ask("UNHANDLE2").get();
            fail(); 
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof FutureTimeoutException);
//            assertTrue(e.getCause() instanceof UnhandledMessageException);
//            assertEquals("UNHANDLE2", ((UnhandledMessageException)e.getCause()).getUnhandledMessage());
        }
        Thread.sleep(100);
        assertEquals(1, unhandledProcessor.messages.size());
        assertTrue(unhandledProcessor.messages.get(0).message instanceof UnhandledMessage);
        UnhandledMessage mess = (UnhandledMessage) unhandledProcessor.messages.get(0).message;
        assertEquals("UNHANDLE2", mess.getMessage());
        assertNotNull(mess.getSender());
        assertSame(facade, mess.getReceiver());
    }
    
    @Test(timeout = 5000l)
    public void unhandledMessageFromFacadeTest() throws Exception {
        DataProcessor emptyProcessor = mock(DataProcessor.class);
        
        ExecutorService executor = createExecutor();
        MessageCollector unhandledProcessor = new MessageCollector();
        DataProcessorFacade sender = new DataProcessorFacadeConfig("sender", testsNode, emptyProcessor, executor, createLogger("Sender. ")).build();
        DataProcessorFacade unhandledChannel = new DataProcessorFacadeConfig("unhandledChannel", testsNode, unhandledProcessor, executor, createLogger("Unhandled channel. ")).build();
        DataProcessorFacade facade = new DataProcessorFacadeConfig("receiver", testsNode, new LogicWithBehaviour(), executor, createLogger("Receiver. "))
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
        
        verifyZeroInteractions(emptyProcessor);
    }
    
    @Test(timeout = 5000l)
    public void childTest() throws Exception {
        LogicWithChild logic = new LogicWithChild();
        DataProcessorFacade parent = new DataProcessorFacadeConfig("parent", testsNode, logic, createExecutor(), logger).build();
        
        assertEquals(0, parent.ask("COUNT").get());
        assertNull(parent.ask("GET").get());
        assertFalse((Boolean)parent.ask("IS_CHILD_TERMINATED").get());
        
        //creating child
        DataProcessorFacade child = (DataProcessorFacade) parent.ask("CREATE").get();
        assertEquals(1, parent.ask("COUNT").get());
        assertSame(child, parent.ask("GET").get());
        assertFalse((Boolean)parent.ask("IS_CHILD_TERMINATED").get());
        assertEquals("PONG", child.ask("PING").get());
        
        //stopping child
        assertEquals(true, child.askStop().get());
        assertEquals(0, parent.ask("COUNT").get());
        assertNull(parent.ask("GET").get());
        assertTrue((Boolean)parent.ask("IS_CHILD_TERMINATED").get());
        
        //creating child again
        child = (DataProcessorFacade) parent.ask("CREATE").get();
        assertEquals(1, parent.ask("COUNT").get());
        assertSame(child, parent.ask("GET").get());
        assertFalse((Boolean)parent.ask("IS_CHILD_TERMINATED").get());
        assertEquals("PONG", child.ask("PING").get());
        //trying to create child with the same name
        try {
            parent.ask("CREATE").get();
            fail();
        } catch (ExecutionException e) {
//            assertTrue(e.getCause() instanceof NonUniqueNameException);
        }
        
        //stopping parent and checking that child is terminated too
        assertFalse(child.isTerminated());
        assertEquals(true, parent.askStop().get()); 
        assertTrue(child.isTerminated());
        assertTrue(logic.childTerminated);
    }
    
    @Test(timeout = 5000l)
    public void stashUnstashTest() throws Exception {
        ExecutorService executor = createExecutor();
        MessageCollector collector = new MessageCollector();
        DataProcessorFacade receiver = new DataProcessorFacadeConfig("receiver", testsNode, collector, executor, logger).build();
        DataProcessorFacade stashHandler = new DataProcessorFacadeConfig("stash test", testsNode, new StashLogic(), executor, logger).build();
        receiver.sendTo(stashHandler, "1");
        receiver.sendTo(stashHandler, "2");
        stashHandler.send("UNSTASH");
        Thread.sleep(100);
        assertArrayEquals(new Object[]{"1", "2"}, collector.getMessages().toArray());
    }
    
    @Test(timeout = 5000l)
    public void stashUnstashWithAskTest() throws Exception {
        ExecutorService executor = createExecutor();
        DataProcessorFacade stashHandler = new DataProcessorFacadeConfig("stash test", testsNode, new StashLogic(), executor, logger).build();
        RavenFuture res = stashHandler.ask("1");
        Thread.sleep(100);
        assertFalse(res.isDone());
        stashHandler.send("UNSTASH");        
        assertEquals("1", res.get());
    }
    
    private LoggerHelper createLogger(String prefix) {
        return new LoggerHelper(logger, prefix);
    }
    
    private class LogicWithChild extends AbstractDataProcessorLogic {
        private boolean childTerminated = false;

        @Override
        public void childTerminated(DataProcessorFacade child) {
            if ("child".equals(child.getName()))
                childTerminated = true;
        }

        @Override
        public Object processData(Object message) throws Exception {
            if ("CREATE".equals(message)) {
                childTerminated = false;
                return getContext().addChild(getContext().createChild("child", new ChildLogic()));
            } else if ("GET".equals(message))
                return getContext().getChild("child");
            else if ("COUNT".equals(message))
                return getContext().getChildren().size();
            else if ("IS_CHILD_TERMINATED".equals(message))
                return childTerminated;
            return VOID;
        }
        
    }
    
    private class StashLogic extends AbstractDataProcessorLogic {
        @Override
        public Object processData(Object message) throws Exception {
            if (message.equals("UNSTASH")) {
                getContext().unstashAll();
                become(unstash, true);
            } else {
                return getContext().stash();                
            }
            return VOID;
        }
        
        private final Behaviour unstash = new Behaviour("Unstashing") {
            @Override public Object processData(Object dataPackage) throws Exception {
//                getSender().send(dataPackage);
                return dataPackage;
            }
        };
    }
    
    private class ChildLogic extends AbstractDataProcessorLogic {
        @Override
        public Object processData(Object message) throws Exception {
            if ("PING".equals(message))
                return "PONG";
            return VOID;
        }        
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
    
//    public static String checkDataPacket(final String dataPacket, final long pause) {
//        reportMatcher(new IArgumentMatcher() {
//            public boolean matches(Object argument) {
//                assertEquals(dataPacket, argument);
//                try {
//                    if (pause>0)
//                        Thread.sleep(pause);
//                } catch (InterruptedException ex) {
//                }
//                return true;
//            }
//            public void appendTo(StringBuffer buffer) {
//            }
//        });
//        return null;
//    }
    
    private ExecutorService createExecutor() throws Exception {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
//        executor.setMaximumQueueSize(10);
        executor.setCorePoolSize(8);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
//        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        //warmup executor
        for (int i=0; i<executor.getCorePoolSize(); i++)
            executor.execute(new AbstractTask(testsNode, "warm up") {
                @Override public void doRun() throws Exception {
                }
            });
        //warm up delayed tasks 
        for (int i=0; i<10; i++)
            executor.executeQuietly(1, new AbstractTask(testsNode, "warm up") {
                @Override public void doRun() throws Exception {
                }
            });
        Thread.sleep(100);
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
                return UNHANDLED;
            else if ("UNHANDLE2".equals(message)) 
                return UNHANDLED;
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
        
        public List<Object> getMessages() {
            List _messages = new ArrayList(messages.size());
            for (Message message: messages)
                _messages.add(message.message);
            return _messages;
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
