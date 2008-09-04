/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice.mailsystem.central.server;

import java.net.Socket;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import infoservice.mailsystem.central.server.util.SocketContainer;

/**
 * This is the implementation for handling new connections. It does some general stuff and will
 * look for a server implementation which will process the client request.
 */
public class ConnectionHandle {

  /**
   * Handles a new incoming connection from a client. A new thread for handling the connection is
   * created (also a cleanup thread is started to make sure, that the socket to the client is
   * closed after connection handling has finished even if there occured a runtime exception).
   *
   * @param a_newConnection A socket connected to a client. If this value is null, nothing is
   *                        is done.
   * @param a_serverImplementationFactory The factory creating the server implementation for
   *                                      processing the client request. If this value is null or
   *                                      the factory doesn't return a valid server
   *                                      implementation, the connection will be closed
   *                                      immediately.
   */
  public ConnectionHandle(final Socket a_newConnection, IServerImplementationFactory a_serverImplementationFactory) {
    if (a_newConnection != null) {
      boolean connectionIsHandled = false;
      if (a_serverImplementationFactory != null) {
        AbstractServerImplementation serverImplementation = a_serverImplementationFactory.createServerImplementation();
        if (serverImplementation != null) {
          connectionIsHandled = true;
          serverImplementation.setClientConnection(new SocketContainer(a_newConnection, null, null));
          LogHolder.log(LogLevel.DEBUG, LogType.NET, "ConnectionHandle: Constructor: Server implementation successfully created.");
          final Thread handleThread = new Thread(serverImplementation);
          handleThread.setDaemon(true);
          handleThread.start();
          Thread cleanUpThread = new Thread(new Runnable() {
            public void run() {
              try {
                handleThread.join();
              }
              catch (Exception e) {
                /* should not happen */
              }
              LogHolder.log(LogLevel.DEBUG, LogType.NET, "ConnectionHandle: Constructor: Cleanup-Thread: Main thread finished - starting cleanup. ");      
              /* the thread doing the server stuff has finished -> clean up everything, if it's not
               * already done (maybe it was forgotten or an unexpected runtime exception has
               * prevented it)
               */
              try {
                a_newConnection.getInputStream().close();
              }
              catch (Exception e) {
              }
              try {
                a_newConnection.getOutputStream().close();
              }
              catch (Exception e) {
              }
              try {
                a_newConnection.close();
              }
              catch (Exception e) {
              }
              /* that's it */
              LogHolder.log(LogLevel.DEBUG, LogType.NET, "ConnectionHandle: Constructor: Cleanup-Thread: Cleanup done.");      
            }
          });
          cleanUpThread.setDaemon(true);
          cleanUpThread.start();
        }
        else {
          LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "ConnectionHandle: Constructor: The server implementation factory returned no server implementation. Cannot handle the new connection - closing connection.");
        }  
      }
      else {
        LogHolder.log(LogLevel.ERR, LogType.NET, "ConnectionHandle: Constructor: No server implementation factory specified. Cannot handle the new connection - closing connection.");
      }  
      if (connectionIsHandled == false) {
        /* close everything */
        try {
          a_newConnection.getInputStream().close();
        }
        catch (Exception e) {
        }
        try {
          a_newConnection.getOutputStream().close();
        }
        catch (Exception e) {
        }
        try {
          a_newConnection.close();
        }
        catch (Exception e) {
        } 
        /* that's it */
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "ConnectionHandle: Constructor: Connection closed after error.");
      }
    }  
  }
  
}