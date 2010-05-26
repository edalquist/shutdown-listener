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

/**
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package com.googlecode.shutdownlistener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a shutdown socket service for an application. When the class is created and {@link #afterPropertiesSet()} is called
 * a socket is opened and monitored for shutdown requests. When a shutdown request occurs registered shutdown listeners are called
 * along with pre and post shutdown listener local methods (for subclassing). The {@link #waitForShutdown()} is provided to allow
 * application code, usually the main thread, to wait for a shutdown request. All APIs provided are thread-safe, locking is done
 * within {@link #shutdown()} and {@link #waitForShutdown()} on a single lock so provided {@link ShutdownListener}s should be careful
 * to never wait for shutdown when being notified of a shutdown occurring. 
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class ShutdownHandler {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicBoolean shutdownComplete = new AtomicBoolean(false);

    protected final Collection<ShutdownListener> internalShutdownListeners = new ArrayList<ShutdownListener>();
    protected Collection<ShutdownListener> shutdownListeners = null;

    
    /**
     * {@link ShutdownListener}s that want to be notified of the shutdown
     */
    public void setShutdownListeners(Collection<ShutdownListener> shutdownListeners) {
        if (shutdownListeners == null) {
            this.shutdownListeners = null;
        }
        else {
            this.shutdownListeners = new ArrayList<ShutdownListener>(shutdownListeners);
        }
    }
    public void registerShutdownListener(ShutdownListener shutdownListener) {
        if (this.shutdownListeners == null) {
            this.shutdownListeners = new ArrayList<ShutdownListener>();
        }
        this.shutdownListeners.add(shutdownListener);
    }
    public Collection<ShutdownListener> getShutdownListeners() {
        return this.shutdownListeners;
    }
    
    public final void start() throws Exception {
        final ShutdownConfiguration config = ShutdownConfiguration.getInstance();
        
        final ShutdownSocketListener shutdownSocketListener = new ShutdownSocketListener(config.getHost(), config.getPort());
        
        final Thread shutdownSocketThread = new Thread(shutdownSocketListener, "ShutdownListener-" + config.getHost() + ":" + config.getPort());
        shutdownSocketThread.setDaemon(true);
        shutdownSocketThread.start();
        
        //Add the listener to the shutdown list 
        this.internalShutdownListeners.add(shutdownSocketListener);
        
        //Register a shutdown handler
        final Thread shutdownHook = new Thread(new ShutdownHookHandler(), "JVM Shutdown Hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.logger.debug("Registered JVM shutdown hook");
        
        this.internalShutdownListeners.add(new ShutdownListener() {
            public void shutdown() {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                logger.debug("Removed JVM shutdown hook");
            }

            @Override
            public String toString() {
                return "JVM Shutdown Hook Remover";
            }
        });
    }
    
    /**
     * If shutdown isn't complete will wait on the shutdown lock for shutdown to complete.
     * DOES NOT TRIGGER SHUTDOWN
     */
    public final void waitForShutdown() {
        if (this.shutdownComplete.get()) {
            return;
        }
        
        try {
            this.shutdownLatch.await();
        }
        catch (InterruptedException e) {
            this.logger.warn("Interrupted waiting for shutdown condition", e);
        }
    }

    /**
     * Calls shutdown hooks and cleans up shutdown listener code, notifies all waiting threads on completion
     */
    public final void shutdown() {
        final boolean shuttingDown = this.shutdownRequested.getAndSet(true);
        if (shuttingDown) {
            if (this.shutdownComplete.get()) {
                logger.info("Already shut down, ignoring duplicate request");
            }
            else {
                logger.info("Already shutting down, ignoring duplicate request");
            }
            return;
        }
    
        this.preShutdownListeners();
        
        //Run external shutdown tasks
        this.runShutdownHandlers(this.shutdownListeners);
        
        //Run internal shutdown tasks
        this.runShutdownHandlers(this.internalShutdownListeners);
        
        this.postShutdownListeners();
        
        this.shutdownComplete.set(true);
        this.shutdownLatch.countDown();
    }
    
    /**
     * Called before the shutdown listeners
     */
    protected void preShutdownListeners() {
    }
    
    /**
     * Called after the shutdown listeners, before threads waiting on {@link #waitForShutdown()} are released
     */
    protected void postShutdownListeners() {
    }

    /**
     * Sort a {@link List} of {@link ShutdownListener} before {@link #runShutdownHandlers(Collection)} iterates over them.
     * Default implementation does nothing
     */
    protected void sortShutdownListeners(List<ShutdownListener> shutdownListeners) {
    }
    
    protected final void runShutdownHandlers(Collection<ShutdownListener> shutdownListeners) {
        final List<ShutdownListener> shutdownListenersClone = new ArrayList<ShutdownListener>(shutdownListeners);
        this.sortShutdownListeners(shutdownListenersClone);
        for (final ShutdownListener shutdownListener : shutdownListenersClone) {
            try {
                this.logger.info("Calling ShutdownListener: {}", shutdownListener);
                shutdownListener.shutdown();
                this.logger.info("ShutdownListener {} complete", shutdownListener);
            }
            catch (Exception e) {
                this.logger.warn("ShutdownListener " + shutdownListener + " threw an exception, continuing with shutdown", e);
            }
        }
    }
    
    /**
     * Runable for waiting on connections to the shutdown socket and handling them
     */
    private class ShutdownSocketListener implements Runnable, ShutdownListener {
        private final ServerSocket shutdownSocket;
        private final InetAddress bindHost;
        private final int port;
        
        private ShutdownSocketListener(String host, int port) {
            this.port = port;
            try {
                this.bindHost = InetAddress.getByName(host);
            }
            catch (UnknownHostException uhe) {
                throw new RuntimeException("Failed to create InetAddress for host '" + host  + "'", uhe);
            }
            
            try {
                this.shutdownSocket = new ServerSocket(this.port, 10, this.bindHost);
            }
            catch (IOException ioe) {
                throw new RuntimeException("Failed to create shutdown socket on '" + this.bindHost + "' and " + this.port, ioe);
            }
            
            logger.info("Bound shutdown socket to {}:{}. Starting listener thread for shutdown requests.", this.bindHost, this.port);
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                while (!shutdownSocket.isClosed()) {
                    try {
                        final Socket connection = shutdownSocket.accept();
                        final ShutdownSocketHandler shutdownSocketHandler = new ShutdownSocketHandler(connection);
                        final Thread shutdownRequestThread = new Thread(shutdownSocketHandler, "ShutdownHandler-" + connection.getInetAddress() + ":" + connection.getPort());
                        shutdownRequestThread.setDaemon(true);
                        shutdownRequestThread.start();
                    }
                    catch (SocketException se) {
                        if (shutdownSocket.isClosed()) {
                            logger.info("Caught SocketException on shutdownSocket, assuming close() was called: " + se);
                        }
                        else {
                            logger.warn("Exception while handling connection to shutdown socket, ignoring", se);
                        }
                    }
                    catch (IOException ioe) {
                        logger.warn("Exception while handling connection to shutdown socket, ignoring", ioe);
                    }
                }
            }
            finally {
                this.shutdown();
            }
        }
        
        public void shutdown() {
            if (!shutdownSocket.isClosed()) {
                try {
                    shutdownSocket.close();
                    logger.debug("Closed shutdown socket {}:{}", this.bindHost, this.port);
                }
                catch (IOException ioe) {
                    //Ignore
                }
            }
        }

        @Override
        public String toString() {
            return "ShutdownSocketListener [bindHost=" + bindHost + ", port=" + port + "]";
        }
    }
    
    /**
     * Runnable to handle connections to the shutdown socket
     */
    private class ShutdownSocketHandler implements Runnable {
        private final Socket shutdownConnection;
        
        public ShutdownSocketHandler(Socket shutdownConnection) {
            this.shutdownConnection = shutdownConnection;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            boolean shutdownNoWait = false;

            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(shutdownConnection.getInputStream()));
                final PrintWriter writer = new PrintWriter(this.shutdownConnection.getOutputStream());
                try {
                    final String recievedCommand = reader.readLine();
                
                    final ShutdownConfiguration config = ShutdownConfiguration.getInstance();
                    if (config.getShutdownWaitCommand().equals(recievedCommand)) {
                        logger.info("Recieved request for shutdown");
                        writer.println(new Date() + ": Starting Shutdown and waiting");
                        writer.flush();
                        shutdown();
                        writer.println(new Date() + ": Shutdown Complete");
                    }
                    else if (config.getShutdownNoWaitCommand().equals(recievedCommand)) {
                        logger.info("Recieved request for shutdown");
                        writer.println(new Date() + ": Starting Shutdown and disconnecting shutdown socket");
                        shutdownNoWait = true;
                    }
                    else if (config.getStatusCommand().equals(recievedCommand)) {
                        logger.debug("Recieved request for status");
                        if (shutdownRequested.get()) {
                            writer.println(new Date() + ": Shutting down");
                        }
                        else {
                            writer.println(new Date() + ": Running");
                        }
                    }
                    else {
                        writer.println(new Date() + ": Unknown command '" + recievedCommand + "'");
                    }
                }
                finally {
                    writer.flush();
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(writer);
                }
            }
            catch (IOException e) {
                logger.warn("Exception while hanlding connection to shutdown socket, ignoring", e);
            }
            finally {
                if (this.shutdownConnection != null) {
                    try {
                        this.shutdownConnection.close();
                    }
                    catch (IOException ioe) {
                        //Ignore
                    }
                }
                
                //To handle shutdown-no-wait calls
                if (shutdownNoWait) {
                    shutdown();
                }
            }
        }
    }

    /**
     * Runnable that calls shutdown, used for JVM shutdown hook
     */
    private class ShutdownHookHandler implements Runnable {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            logger.info("JVM shutdown hook called");
            shutdown();
        }
    }
}