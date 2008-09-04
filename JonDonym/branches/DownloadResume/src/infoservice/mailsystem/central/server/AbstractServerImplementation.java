/*
 Copyright (c) 2000 - 2005 The JAP-Team
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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import infoservice.mailsystem.central.server.util.SocketContainer;

/**
 * This is a generic implementation for handling client requests which have reached the server.
 */
public abstract class AbstractServerImplementation implements Runnable {
  
  /**
   * Stores the SocketContainer which includes the Socket connected to the client and maybe some
   * alternative datastreams.
   */
  private SocketContainer m_socketContainer;
  
  /**
   * Stores whether this AbstractServerImplementation is already in use.
   */
  private boolean m_alreadyInUse;
  
  /**
   * This object is used for synchronization purposes to prevent using this server implementation
   * more than once.
   */
  private Object m_synchronizer;

  
  /**
   * Creates a new AbstractServerImplementation. This constructor is called automatically from the
   * children of this class. Only some initialization is done here.
   */
  protected AbstractServerImplementation() {
    m_socketContainer = null;
    m_synchronizer = new Object();
    m_alreadyInUse = false;
  }
  
  
  /**
   * This will set the SocketContainer with the connection to the client. This method must only be
   * called from the ConnectionHandle class. It is called once before the handling-thread calls
   * the run() method.
   *
   * @param a_socketContainer The SocketContainer with the connection to the client.
   */
  public final void setClientConnection(SocketContainer a_socketContainer) {
    synchronized (m_synchronizer) {
      if (m_alreadyInUse == true) {
        LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "AbstractServerImplementation: setClientConnection: This server implementation is already in use. Cannot switch the client connection.");
      }
      else {
        m_socketContainer = a_socketContainer;
      }
    }
  }  
  
  /**
   * This is the generic implementation for handling the connection. It will check, whether
   * everything is defined correctly and will then start the specific server implementation of the
   * children of this class by calling the handleClientRequest() method.
   */
  public final void run() {
    boolean canHandleRequest = false;
    synchronized (m_synchronizer) {
      if (m_socketContainer == null) {
        LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "AbstractServerImplementation: run: No client connection is specified.");
      }
      else {  
        /* connection to the client is set */
        if (m_alreadyInUse == true) {
          LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "AbstractServerImplementation: run: This server implementation is already in use.");
        }
        else {
          /* this server implementations wasn't used until yet */
          m_alreadyInUse = true;
          canHandleRequest = true;
        }
      }
    }
    if (canHandleRequest == true) {
      handleClientRequest();
    }
  }
      
  
  /**
   * Returns the SocketContainer with the connection to the client. This method can be called by
   * children of this class, if they need access to the client connection.
   *
   * @return The SocketContainer with the connection to the client.
   */    
  protected final SocketContainer getSocketContainer() {
    return m_socketContainer;
  }

  /**
   * This method must be implemented by the children of this class. The request handling must be
   * done here. It's called automatically by the request-handling thread. After it has finished
   * this method, the connection to the client is closed automatically.
   */
  protected abstract void handleClientRequest();
  
}