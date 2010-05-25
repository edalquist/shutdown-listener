/**
 * Copyright 2010 Eric Dalquist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.shutdownlistener;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.googlecode.shutdownlistener.ShutdownConfiguration;
import com.googlecode.shutdownlistener.ShutdownUtility;
import com.googlecode.shutdownlistener.mock.StaticTrackingShutdownListener;
import com.googlecode.shutdownlistener.spring.ApplicationContextShutdownWrapper;


/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class ContextShutdownTest {
    @Test
    public void testAddedShutdownHandler() throws Exception {
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("/shutdownTestContext.xml");
        final ApplicationContextShutdownWrapper shutdownWrapper = new ApplicationContextShutdownWrapper(context);
        
        Assert.assertFalse(StaticTrackingShutdownListener.isShutdown());
        
        final Thread shutdownCall = new Thread(new Runnable() {
            
            public void run() {
                try {
                    Thread.sleep(100);
                    ShutdownUtility.main(new String[] { });
                    ShutdownUtility.main(new String[] { ShutdownConfiguration.getInstance().getShutdownWaitCommand() });
                }
                catch (Exception e) {
                    Assert.fail("failed to shutdown: " + e);
                }
            }
        });
        shutdownCall.setDaemon(true);
        shutdownCall.start();
        
        shutdownWrapper.waitForShutdown();
        
        Assert.assertTrue(StaticTrackingShutdownListener.isShutdown());
    }
}


