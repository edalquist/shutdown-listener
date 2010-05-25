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

package com.googlecode.shutdownlistener.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;

import com.googlecode.shutdownlistener.ShutdownHandler;
import com.googlecode.shutdownlistener.ShutdownListener;

/**
 * Spring Bean version of {@link ShutdownHandler} that makes use of Spring's initialization and disposal features
 * as well as the ability to lookup {@link ShutdownListener}s registered in the {@link ApplicationContext}.
 * 
 * If no {@link #setShutdownListeners(java.util.Collection)} are set the class will use 
 * {@link BeanFactoryUtils#beansOfTypeIncludingAncestors(org.springframework.beans.factory.ListableBeanFactory, Class)} to
 * find all beans that implement {@link ShutdownListener} and register them.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class ShutdownHandlerBean extends ShutdownHandler implements InitializingBean, DisposableBean, ApplicationContextAware {
    private ApplicationContext applicationContext;
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    protected void sortShutdownListeners(List<ShutdownListener> shutdownListeners) {
        Collections.sort(shutdownListeners, OrderComparator.INSTANCE);
    }

    public void afterPropertiesSet() throws Exception {
        if (this.shutdownListeners == null) {
            final Map<String, ShutdownListener> shutdownListenerMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, ShutdownListener.class);
            this.shutdownListeners = new ArrayList<ShutdownListener>(shutdownListenerMap.values());
            this.logger.debug("No explicit shutdownListeners configured, using {} ShutdownListeners from application context.", this.shutdownListeners.size());
        }
        
        this.start();
    }
    
    public void destroy() throws Exception {
        this.shutdown();
    }

}
