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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

/**
 * Connects to the {@link org.jasig.portal.stats.shutdown.ShutdownHandler} and passes the first parameter passed on 
 * the command line. Any output from the handler is written to standard out.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class ShutdownUtility {
    
    public static void main(String[] args) throws Exception {
        final ShutdownConfiguration config = ShutdownConfiguration.getInstance();
        
        final String command;
        if (args.length > 0) {
            command = args[0];
        }
        else {
            command = config.getStatusCommand();
        }
        
        System.out.println("Calling processor on " + config.getHost() + ":" + config.getPort() + " with command: " + command);
        
        final InetAddress hostAddress = InetAddress.getByName(config.getHost());
        final Socket shutdownConnection = new Socket(hostAddress, config.getPort());
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(shutdownConnection.getInputStream()));
            final PrintStream writer = new PrintStream(shutdownConnection.getOutputStream());
            try {
                writer.println(command);
                writer.flush();

                while (true) {
                    final String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    
                    System.out.println(line);
                }
            }
            finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }
        }
        finally {
            try {
                shutdownConnection.close();
            }
            catch (IOException ioe) {
            }
        }
        
        
    }
}
