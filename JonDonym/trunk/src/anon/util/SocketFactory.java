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
package anon.util;

import java.net.Socket;
import java.util.Vector;

/**
 * This is an implementation to make the Socket creation process interruptable which offers also
 * the opportunity to set a timeout for this process. Both possibilities are not available in the
 * default Java Socket implementation.
 */
public class SocketFactory {
  
  /**
   * Creates a new Socket. A timeout can be set for this operation and also it is interruptable.
   * If there occured an Exception while creating the Socket, this Exception is thrown. If the
   * creation process was interrupted from outside, an InterrupedException is thrown (but the
   * interrupted flag is not set on the thread). If a timeout was specified and reached, a normal
   * Exception is thrown.
   *
   * @param a_hostName The hostname of the target or null for the loopback interface
   *                   (see java.net.Socket).
   * @param a_port The port of the target.
   * @param a_createTimeout The timeout for creating the new Socket in milliseconds. A timeout of
   *                        0 means to wait forever (until Socket creation is ready).
   *
   * @return The created Socket.
   */
  public static Socket createSocket(final String a_hostName, final int a_port, long a_createTimeout) throws Exception {
    final Vector socketStore = new Vector();
    final Vector exceptionStore = new Vector();
    Thread createThread = new Thread(new Runnable() {
      /**
       * This is the implementation for the thread creating the new Socket.
       */
      public void run() {
        try {
          Socket serverConnection = new Socket(a_hostName, a_port);
          synchronized (socketStore) {
            if (Thread.interrupted()) {
              /* we are to late -> close the socket */
              serverConnection.close();
            }
            else {
              /* we are in time */
              socketStore.addElement(serverConnection);
            }
          }
        }
        catch (Exception e) {
          synchronized (exceptionStore) {
            exceptionStore.addElement(e);
          }
        }
      }
    });
    createThread.setDaemon(true);
    createThread.start();
    try {
      createThread.join(a_createTimeout);
    }
    catch (Exception e) {
      /* first try to interrupt the create thread, maybe later Java versions will stop the
       * socket operation then, also if the creation of the Socket will be successful, the
       * create thread will close the now useless Socket immediately
       */
      createThread.interrupt();
      synchronized (socketStore) {
        /* maybe it was to late and the Socket was already created -> close it */
        if (socketStore.size() > 0) {
          try {
            ((Socket)(socketStore.firstElement())).close();
          }
          catch (Exception e2) {
            /* do nothing */
          }
        }
      }
      /* now throw the InterruptedException */
      throw e;
    }
    /* if we reach this point, the socket create thread cam to the end or the time was up */
    Socket createdSocket = null;
    synchronized (socketStore) {
      synchronized (exceptionStore) {
        if (exceptionStore.size() > 0) {
          /* an exception occured while creating the socket -> throw it */
          throw ((Exception)(exceptionStore.firstElement()));
        }
        if (socketStore.size() > 0) {
          /* socket was created successfully */
          createdSocket = (Socket)(socketStore.firstElement());
        }
        else {
          /* no exception, no socket -> create thread not ready -> timeout occured -> throw an
           * exception
           */
          /* first try to interrupt the create thread, maybe later Java versions will stop the
           * socket operation then, also if the creation of the Socket will be successful, the
           * create thread will close the now useless Socket immediately
           */
          createThread.interrupt();
          throw (new Exception("SocketFactory: createSocket: Timeout occured."));
        }
      }
    }
    return createdSocket;
  }
  
}
