/*
 Copyright (c) 2000- 2004, The JAP-Team
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
package anon;

import java.net.ConnectException;

import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.IMutableProxyInterface;

/** This class is used for accessing the AnonService. An instance is created
 *  via AnonServiceFactory.
 */
public interface AnonService
{
	///The version of the AnonLib
	String ANONLIB_VERSION = "00.04.002";

	/** Initializes this AnonService. Depending on the AnonService, this may establish a connection to
	 *  an AnonServer, which is described through the
	 *  anonService parameter. This method must be called before any Channels could be created.
	 *  @param anonServer AnonServer to use
	 *  @return E_SUCCESS, if the connection could be estblished
	 *  @return E_ALREADY_CONNECTED, if this AnonService is already connected to a server
	 *  @return E_INVALID_SERVICE, if anonService is not a valid server
	 *  @return E_PROTOCOL_NOT_SUPPORTED, if the chosen AnonService uses a Protocol, which this version of
	 *                                        the Anon-Lib does not understand
	 *  @return E_CONNECT, if a general connection error occured
	 *
	 */
	int initialize(AnonServerDescription anonServer, IServiceContainer a_serviceContainer);

	/** Sets the settings ofr the proxy, which should be used to establish network connections
	 * @return E_SUCCESS, if ok
	 * @return E_UNKNOWN, if an error occured
	 */
	int setProxy(IMutableProxyInterface a_Proxy);

	/** Disconnects form the server.*/
	void shutdown(boolean a_bResetTransferredBytes);

	/** Returns true if this Anon Service is connected, e.g. initialized and useable*/
	boolean isConnected();

	/** Creates a new AnonChannel, which could be used for transmitting data. There is a
	 *  limit of 50 channels per AnonService-connection, in order to prevent Denial of Service-attacks
	 *  See {@link AnonChannel AnonChannel} for more information.
	 *  @param type the type of the created channel
	 *  @return AnonChannel, usefull for data transmisson
	 *  @throws ConnectException, if the Channel could not be created
	 *  @throws ToManyOpenChannels, if there a to many open channels for this AnonService
	 */
	AnonChannel createChannel(int type) throws ConnectException;

	/** Adds an AnonServiceEventListener. This listener will receive events like:
	 *  ... For more information see {@link AnonServiceEventListener AnonServiceEventListener}.
	 *  @param l Listener to add
	 */
	void addEventListener(AnonServiceEventListener l);

	/** Removes an AnonServiceEventListener. This Listener will not receive any Events anymore.
	 *  @param l Listener, which will be removed
	 */
	void removeEventListener(AnonServiceEventListener l);

	void removeEventListeners();

}
