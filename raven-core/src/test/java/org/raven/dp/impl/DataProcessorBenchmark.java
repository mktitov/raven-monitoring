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
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class DataProcessorBenchmark extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private LoggerHelper logger;
    
    @Before
    public void prepare() throws Exception {
//        testsNode.setLogLevel(LogLevel.TRACE);
        logger = new LoggerHelper(testsNode, "Tests. ");
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        executor.setCorePoolSize(8);
        assertTrue(executor.start());
    }
    
//    @Before
    public void warmup() throws Exception {
        runBenchmark(2, true);
        runBenchmark(8, true);
        runBenchmark(8, true);
        
    }
    
    @Test
    public void test() throws Exception {       
        runBenchmark(2, true);
        runBenchmark(8, true);
        runBenchmark(8, true);
        runBenchmark(4, false);
//        System.out.println("Executed tasks count: "+executor.getExecutedTasks());
        runBenchmark(4, false);
//        executor.stop();
//        executor.start();
        runBenchmark(8, false);
//        System.out.println("Executed tasks count: "+executor.getExecutedTasks());
        runBenchmark(8, false);
        runBenchmark(16, false);
        runBenchmark(32, false);
        runBenchmark(40, false);
        runBenchmark(48, false);
        runBenchmark(64, false);
    }
    
//    @Test
    public void test4() throws Exception {       
        runBenchmark(4, false);
        runBenchmark(4, false);
    }
    
//    @Test
    public void test8() throws Exception {       
        runBenchmark(8, false);
        runBenchmark(8, false);
    }
    
//    @Test
    public void test16() throws Exception {       
        runBenchmark(16, false);
        runBenchmark(32, false);
        runBenchmark(64, false);
    }
    
    private final long repeat = 200000000l;
//    private final long repeat = 50l;
    
    public void runBenchmark(int numberOfClients, boolean warmup) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        long repeatsPerClient = repeat / numberOfClients;
        Collection<DataProcessorFacade> dests = new ArrayList<>(numberOfClients);
        Collection<DataProcessorFacade> clients = new ArrayList<>(numberOfClients);
        for (int i=0; i<numberOfClients; ++i) {
            DataProcessorFacade dest = new DataProcessorFacadeConfig(
                    "Destination", testsNode, new Destination(), executor, logger).build();
            dests.add(dest);
            clients.add(new DataProcessorFacadeConfig(
                    "Client", testsNode, new Client(
                            latch, dest, repeatsPerClient), executor, logger).build());
        }
        for (DataProcessorFacade client: clients)
            client.send(INIT);
        long start = System.nanoTime();
        for (DataProcessorFacade client: clients)
            client.send(RUN);
        boolean ok = latch.await(15000, TimeUnit.MILLISECONDS);
        long duration =(System.nanoTime()-start);
        for (DataProcessorFacade client: clients)
            client.stop();
        for (DataProcessorFacade client: dests)
            client.stop();
        if (!warmup) {
            assertTrue(ok);
            long ms = TimeUnit.NANOSECONDS.toMillis(duration);
            System.out.println("\n------ REPORT --------");
            System.out.println("Number of clients: "+numberOfClients);
            System.out.println("Duration (ms): "+TimeUnit.NANOSECONDS.toMillis(duration));
            long messagesPerSecond = repeat*1000 / ms;
            System.out.println("Messages per second: "+messagesPerSecond);
            System.out.println();
        }
        Thread.sleep(1000);
//        System.gc();
    }
    
    private final static String RUN = "RUN";
    private final static String MSG = "MESSAGE";
    private final static String INIT = "INIT";
    
//    private static class Destination extends AbstractDataProcessorLogic {
    private static class Destination implements DataProcessor {
        private DataProcessorFacade client;
        
        private final Behaviour executing = new Behaviour("Executing") {
            @Override public final Object processData(Object message) throws Exception {
                client.send(message);
                return VOID;
            }
        };
        
        @Override public final Object processData(final Object message) throws Exception {
            if (client!=null) {
                client.send(message);
                return VOID;
            } else if (message instanceof DataProcessorFacade) {
                client = (DataProcessorFacade) message;
//                become(executing);
                return VOID;
            } else
                return message;
        }
    }
    
    private static class Client extends AbstractDataProcessorLogic {
        private final CountDownLatch latch;
        private final DataProcessorFacade dest;
        private final long repeat;
        
        private long sent = 0l;
        private long received = 0l;

        public Client(CountDownLatch latch, DataProcessorFacade dest, long repeat) {
            this.latch = latch;
            this.dest = dest;
            this.repeat = repeat;
        }
        
        private final Behaviour start = new Behaviour("Starting") {
            @Override public final Object processData(Object message) throws Exception {
                if (message==RUN) {
                    for (long i=0; i<Math.min(1000, repeat); i++) {
                        dest.send(MSG);
                        sent++;
                    }
                    become(executing);
                    return VOID;
                } else return UNHANDLED;
            }
        };
        
        private final Behaviour executing = new Behaviour("Executing") {
            @Override public final Object processData(Object dataPackage) throws Exception {
                received++;
                if (sent<repeat) {
                    dest.send(MSG);
                    sent++;
                } else if (received >= repeat)
                    latch.countDown();
                return VOID;
            }
        };

        @Override
        public final Object processData(Object message) throws Exception {
            if (message==MSG) {
                received++;
                if (sent<repeat) {
                    getFacade().sendTo(dest, MSG);
                    sent++;
                } else if (received >= repeat)
                    latch.countDown();
            } else if (message==RUN) {
                for (long i=0; i<Math.min(1000, repeat); i++) {
                    getFacade().sendTo(dest, MSG);
                    sent++;
                }
            } else if (message==INIT) {
                dest.send(getFacade());
                become(start);
            }
            return VOID;
        }
        
    }
    
}
