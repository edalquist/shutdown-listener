/**
 * Copyright (c) 2000-2010, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
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
            this.logger.debug("No explicit shutdownListeners configured, using " + this.shutdownListeners.size() + " ShutdownListeners from application context");
        }
        
        this.start();
    }
    
    public void destroy() throws Exception {
        this.shutdown();
    }

}
