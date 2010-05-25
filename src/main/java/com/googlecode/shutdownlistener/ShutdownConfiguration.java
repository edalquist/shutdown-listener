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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for centralizing the default configuration and configuration loading.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public final class ShutdownConfiguration {
    private static ShutdownConfiguration INSTANCE = null;
    
    public synchronized static ShutdownConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShutdownConfiguration();
        }
        
        return INSTANCE;
    }
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private String host = "localhost";
    private int port = 7456;
    private String shutdownNoWaitCommand = "SHUTDOWN_NO_WAIT";
    private String shutdownWaitCommand = "SHUTDOWN_AND_WAIT";
    private String statusCommand = "STATUS";
    
    private ShutdownConfiguration() {
        final InputStream shutdownConfigStream = this.getClass().getResourceAsStream("/spring-shutdown.properties");
        if (shutdownConfigStream != null) {
            final Properties shutdownConfig = new Properties();
            try {
                shutdownConfig.load(shutdownConfigStream);
                shutdownConfigStream.close();
            }
            catch (IOException e) {
                this.logger.warn("Failed to read '/spring-shutdown.properties' from classpath. Default configuration will be used", e);
                return;
            }
            
            this.host = shutdownConfig.getProperty("host", this.host);
            this.port = this.getProperty(shutdownConfig, "port", this.port);
            this.shutdownNoWaitCommand = shutdownConfig.getProperty("shutdownNoWaitCommand", this.shutdownNoWaitCommand);
            this.shutdownWaitCommand = shutdownConfig.getProperty("shutdownWaitCommand", this.shutdownWaitCommand);
            this.statusCommand = shutdownConfig.getProperty("statusCommand", this.statusCommand);
        }
        
        this.logger.debug("Created {}", this.toString());
    }
    
    private int getProperty(Properties p, String name, int defaultValue) {
        try {
            return Integer.parseInt(p.getProperty("port", Integer.toString(this.port)));
        }
        catch (NumberFormatException nfe) {
            this.logger.warn("'" + name + "' property value of '" + p.getProperty("port") + "' could not be parsed to an Integer. The default value will be used", nfe);
            return defaultValue;
        }
    }

    /**
     * @return Host to use for shutdown, defaults to "localhost"
     */
    public String getHost() {
        return host;
    }

    /**
     * @return Port to use for shutdown, defaults to 7456
     */
   public int getPort() {
        return port;
    }

    /**
     * @return Command to shutdown the application and return immediately, defaults to "SHUTDOWN_NO_WAIT"
     */
    public String getShutdownNoWaitCommand() {
        return shutdownNoWaitCommand;
    }

    /**
     * @return Command to shutdown the application and wait for complete shutdown, defaults to "SHUTDOWN_AND_WAIT"
     */
    public String getShutdownWaitCommand() {
        return shutdownWaitCommand;
    }

    /**
     * @return Command to get the application status, defaults to "STATUS"
     */
    public String getStatusCommand() {
        return statusCommand;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result + ((shutdownNoWaitCommand == null) ? 0 : shutdownNoWaitCommand.hashCode());
        result = prime * result + ((shutdownWaitCommand == null) ? 0 : shutdownWaitCommand.hashCode());
        result = prime * result + ((statusCommand == null) ? 0 : statusCommand.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ShutdownConfiguration)) {
            return false;
        }
        ShutdownConfiguration other = (ShutdownConfiguration) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        }
        else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (shutdownNoWaitCommand == null) {
            if (other.shutdownNoWaitCommand != null) {
                return false;
            }
        }
        else if (!shutdownNoWaitCommand.equals(other.shutdownNoWaitCommand)) {
            return false;
        }
        if (shutdownWaitCommand == null) {
            if (other.shutdownWaitCommand != null) {
                return false;
            }
        }
        else if (!shutdownWaitCommand.equals(other.shutdownWaitCommand)) {
            return false;
        }
        if (statusCommand == null) {
            if (other.statusCommand != null) {
                return false;
            }
        }
        else if (!statusCommand.equals(other.statusCommand)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ShutdownConfiguration [host=" + host + ", port=" + port + ", shutdownNoWaitCommand="
                + shutdownNoWaitCommand + ", shutdownWaitCommand=" + shutdownWaitCommand + ", statusCommand="
                + statusCommand + "]";
    }
}
