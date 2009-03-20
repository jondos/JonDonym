/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/**
 * This interface defines the methods for a ServerManager. A ServerManager manages the server part
 * of the forwarding code, e.g. a ServerSocket where clients can connect to.
 */
public interface IServerManager {

  /**
   * Returns the ID of this ServerManager. This ID must be unique for the instance of
   * ServerManager within all possible ServerManagers, e.g. a String of the type
   * "CLASSNAME%INSTANCE_ID" would be possible.
   *
   * @return A unique identifier Object for the ServerManager.
   */
  public Object getId();
   
  /**
   * This starts the ServerManager.
   *
   * @param a_parentScheduler The ForwardScheduler where all new connections are reported to.
   */
  public void startServerManager(ForwardScheduler a_parentScheduler) throws Exception;
  
  /**
   * This method must be called, if the ServerManager shall come to an end. This method should
   * block until everything is down (e.g. open sockets, internal threads, ...).
   */
  public void shutdown();
    
}