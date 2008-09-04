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
package infoservice;

import java.net.InetAddress;

/**
 * This is the definition for all implementations handling HTTP requests from the
 * InfoServiceConnection.
 */
public interface JWSInternalCommands {

	/**
   * This is definition of the handler for processing an HTTP request.
   *
   * @param method The HTTP method used within the request from the client. See the REQUEST_METHOD
   *               constants in anon.infoservice.Constants.
   * @param a_supportedEncodings The HTTP encodings supported by the client. See the HTTP_ENCODING
   * constants in HttpResonseStructure.
   * @param command The URL requested from the client within the HTTP request. Normally this
   *                should be an absolute path with a filename.
   * @param postData The HTTP content data (maybe of size 0), if the request was an HTTP POST. If
   *                 the HTTP method was not POST, this value is always null.
   * @param a_sourceAddress The internet address from where we have received the request. It is
   *                        the address of the other end of the socket connection, so maybe it is
   *                        only the address of a proxy.
   *
   * @return The response to send back to the client. This value is null, if the request cannot
   *         be handled by the implementation (maybe because of an invalid command, ...).
	 */
  public HttpResponseStructure processCommand(int method, int a_supportedEncodings,
											  String command, byte[] postData, InetAddress a_sourceAddress);

}
