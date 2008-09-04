/*
 Copyright (c) 2000 - 2004 The JAP-Team
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
package infoservice.tor;

/**
 * This is the implementation of the tor directory server structure, which holds the information
 * how we can get the tor nodes list from the directory server (host, port of the server and
 * HTTP filename of the nodes list).
 */ 
public class TorDirectoryServerUrl {
  
  /**
   * Stores the host (IP or hostname) of the tor directory server.
   */
  private String m_host;
  
  /**
   * Stores the port number of the tor directory server.
   */
  private int m_port;
  
  /**
   * Stores the filename of the tor nodes list at the directory server (including the full path).
   */
  private String m_fileName;
  
  
  /**
   * Creates a new TorDirectoryServerUrl.
   *
   * @param a_host The host (hostname or IP) of the tor directory server.
   * @param a_port The port number of the tor directory server.
   * @param a_fileName The filename of the tor nodes list at the tor directory server (including
   *                   the full path).
   */
  public TorDirectoryServerUrl(String a_host, int a_port, String a_fileName) {
    m_host = a_host;
    m_port = a_port;
    m_fileName = a_fileName;
  }

  
  /**
   * Returns the host (hostname or IP) of the tor directory server.
   *
   * @return The tor directory server host.
   */
  public String getHost() {
    return m_host;
  }
  
  /**
   * Returns the port number of the tor directory server.
   *
   * @return The tor directory server port number.
   */
  public int getPort() {
    return m_port;
  }
  
  /**
   * Returns the filename of the tor nodes list at the tor directory server (a simple "/" for
   * an original tor server or "/tornodes" at a JAP infoservice).
   *
   * @return The HTTP filename of the tor nodes list at the tor directory server.
   */
  public String getFileName() {
    return m_fileName;
  }
  
}