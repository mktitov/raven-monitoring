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
package org.raven;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Mikhail Titov
 */

@RunWith(JMockit.class)
public class JMockTest extends Assert {
    public interface ITest {
        public void test();
        public int test2();
    }

   @Test
   public void test(@Mocked final ITest test) throws InterruptedException {
        new Expectations() {{
            test.test2(); result = 1; times = 1;
        }};
        Thread t = new Thread(){
            @Override
            public void run() {
                test.test2();
                test.test2();
            }
        };
//       assertEquals(1, test.test2());
//       assertEquals(1, test.test2());
   }
   
   @Test
   public void doOperationAbc(@Mocked final ITest test) throws InterruptedException {
//      new Expectations() {{
//         test.test();
//      }};

//        test.test();
        Thread t = new Thread(){
            @Override
            public void run() {
                test.test();
                test.test();
            }
        };
        t.start();
        t.join();
      
//      test.test();

      new Verifications() {{ 
          test.test(); times = 2;
//          mockXyz.complexOperation(true, anyInt, null); times = 1; 
      }};
   }    
}
