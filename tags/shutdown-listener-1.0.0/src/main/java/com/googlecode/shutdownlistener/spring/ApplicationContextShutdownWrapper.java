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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import com.googlecode.shutdownlistener.ShutdownHandler;

/**
 * Utility that wraps an {@link ApplicationContext} to handle shutting down. If the provided context contains
 * a {@link ShutdownHandlerBean} it is used, if not a child {@link ApplicationContext} is created and a {@link ShutdownHandlerBean}
 * is registered there.
 * 
 * To use create a new instance and then call {@link #waitForShutdown()} to wait for a shutdown call to the
 * {@link ShutdownHandlerBean}'s socket.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public final class ApplicationContextShutdownWrapper {
    private final ApplicationContext applicationContext;
    private final DisposableBean disposableBean;
    private final ShutdownHandler shutdownHandler;
    private final GenericApplicationContext shutdownBeanFactory;

    public ApplicationContextShutdownWrapper(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "ApplicationContext cannot be null");
        this.applicationContext = applicationContext;
        if (!(applicationContext instanceof DisposableBean)) {
            throw new IllegalArgumentException("The provided BeanFactory must implement DisposableBean");
        }
        this.disposableBean = (DisposableBean)this.applicationContext;

        ShutdownHandler shutdownHandler = null;
        try {
            shutdownHandler = this.applicationContext.getBean(ShutdownHandlerBean.class);
        }
        catch (NoSuchBeanDefinitionException e) {
            //Ignore
        }
        
        if (shutdownHandler != null) {
            this.shutdownHandler = shutdownHandler;
            this.shutdownBeanFactory = null;
        }
        else {
            this.shutdownBeanFactory = new GenericApplicationContext(this.applicationContext);
            
            final GenericBeanDefinition shutdownBeanDefinition = new GenericBeanDefinition();
            shutdownBeanDefinition.setBeanClass(ShutdownHandlerBean.class);
            
            this.shutdownBeanFactory.registerBeanDefinition(ShutdownHandler.class.getName(), shutdownBeanDefinition);
            this.shutdownBeanFactory.refresh();

            this.shutdownHandler = this.shutdownBeanFactory.getBean(ShutdownHandler.class);
        }
    }

    /**
     * Uses {@link ShutdownHandler#waitForShutdown()} then calls {@link DisposableBean#destroy()} on the
     * {@link ApplicationContext}
     */
    public void waitForShutdown() throws Exception {
        this.shutdownHandler.waitForShutdown();
        this.disposableBean.destroy();
        if (this.shutdownBeanFactory != null) {
            this.shutdownBeanFactory.destroy();
        }
    }
}
