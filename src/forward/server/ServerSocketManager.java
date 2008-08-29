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
package forward.server;

import java.net.ServerSocket;
import java.net.Socket;

import anon.transport.connection.SocketConnection;


/**
 * This class manages a ServerSocket for a ForwardScheduler.
 */
public class ServerSocketManager implements Runnable, IServerManager {
  
  /**
   * This is the maximum number of unanswered connection requests (connections, which are not
   * accepted yet). If this queue is full, any new connection will be dropped.
   */
  private static final int MAXIMUM_CONNECTION_REQUESTS = 5;   

  /**
   * The associated ServerSocket which listens for all client connections.
   */
  private ServerSocket m_serverSocket;
  
  /**
   * The internal thread, which accepts the client connections and forwards them to the
   * ForwardScheduler.
   */
  private Thread m_managerThread;
    
  /**
   * This is the ForwardScheduler, which owns this ServerManager. Every new client connection
   * is forwarded to this instance.
   */
  private ForwardScheduler m_parentScheduler;
    

  /**
   * This is the portnumber the ServerSocket listens on.
   */
  private int m_portNumber;
  
  /**
   * This creates a new ServerManager. The ServerManager does nothing until startServerManager()
   * is called.
   *
   * @param a_portNumber The port of the listening server socket. This is the port where clients
   *                     can connect to.
   */
  public ServerSocketManager(int a_portNumber) {   
    m_portNumber = a_portNumber;
  }


  /**
   * Returns the ID of this ServerSocketManager. It is a String of the type ClassName%PortNumber,
   * so it should be unique within all possible ServerManagers.
   *
   * @return A unique identifier Object for this ServerSocketManager.
   */
  public Object getId() {
    return (this.getClass().getName() + "%" + Integer.toString(m_portNumber));
  }
  
  /**
   * This starts the ServerManager. Also the included thread is started.
   *
   * @param a_parentScheduler This is the ForwardScheduler where all new connections are reported
   *                          to.
   */
  public void startServerManager(ForwardScheduler a_parentScheduler) throws Exception {
    m_serverSocket = new ServerSocket(m_portNumber, MAXIMUM_CONNECTION_REQUESTS);
    m_serverSocket.setSoTimeout(0);
    m_parentScheduler = a_parentScheduler;
    m_managerThread = new Thread(this);
    m_managerThread.setDaemon(true);
    m_managerThread.start();
  }  
  
  /**
   * This method must be called, if the ServerManager shall come to an end. The associated
   * ServerSocket is closed and the internal thread is stopped. This method will block until
   * the internal thread has come to the end.
   */
  public void shutdown() {
    try {
      m_serverSocket.close();
    }
    catch (Exception e) {
    }
    try {
      m_managerThread.join();
    }
    catch (Exception e) {
    }
  }
  
  /**
   * This is the implementation of the internal thread. It will accept any new connections on
   * the ServerSocket and reports them to the parent ForwardScheduler. If the ServerSocket
   * is closed by the shutdown methodor ther is an serious error on the socket, the thread will
   * halt.
   */
  public void run() {
    boolean shutdown = false;
    while (shutdown == false) {
      Socket newConnection = null;
      try {
        newConnection = m_serverSocket.accept();
      }
      catch (Exception e) {
        /* we got an exception while waiting for new clients -> serious error or socket was closed
         * -> stop the thread
         */
        shutdown = true;
      }
      if (shutdown == false) {
        /* we got a new connection */
        try {
          newConnection.setSoTimeout(ForwardServerManager.CLIENT_CONNECTION_TIMEOUT);
          m_parentScheduler.handleNewConnection(new SocketConnection(newConnection));
        }
        catch (Exception e) {
        }
      }
    }
  }
  
}