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

package com.googlecode.shutdownlistener.mock;

import com.googlecode.shutdownlistener.ShutdownListener;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class StaticTrackingShutdownListener implements ShutdownListener {
    private static boolean shutdown = false;
    
    public static void reset() {
        shutdown = false;
    }
    
    public static boolean isShutdown() {
        return shutdown;
    }

    /* (non-Javadoc)
     * @see com.googlecode.shutdownlistener.ShutdownListener#shutdown()
     */
    public void shutdown() {
        shutdown = true;
    }

    /* (non-Javadoc)
     * @see org.springframework.core.Ordered#getOrder()
     */
    public int getOrder() {
        return 0;
    }

}
