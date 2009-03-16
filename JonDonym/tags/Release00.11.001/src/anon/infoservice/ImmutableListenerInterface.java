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
package anon.infoservice;

/**
 * Objects of this type only contain immutable methods.
 * @author Rolf Wendolsky
 */
public interface ImmutableListenerInterface
{

	/**
	 * The constant for the 'unknown' protocol.
	 */
	String PROTOCOL_STR_TYPE_UNKNOWN = "UNKNWON/UNKNOWN";
	int PROTOCOL_TYPE_UNKNOWN = -1;

	/**
	 * The constant for the HTTP protocol.
	 */
	String PROTOCOL_STR_TYPE_HTTP = "HTTP/TCP";
	int PROTOCOL_TYPE_HTTP = 1;

	/**
	 * The constant for the HTTP protocol.
	 */
	String PROTOCOL_STR_TYPE_HTTPS = "https";
	int PROTOCOL_TYPE_HTTPS = 4;
	
	/**
	 * The constant for the HTTP protocol.
	 */
	String PROTOCOL_STR_TYPE_RAW_UNIX = "RAW/UNIX";
	int PROTOCOL_TYPE_RAW_UNIX = 5;

	/**
	 * The constant for the HTTP protocol.
	 */
	String PROTOCOL_STR_TYPE_SOCKS = "socks";
	int PROTOCOL_TYPE_SOCKS = 3;

	/**
	 * The constant for a custom protocol based on TCP.
	 */
	String PROTOCOL_STR_TYPE_RAW_TCP = "RAW/TCP";
	int PROTOCOL_TYPE_RAW_TCP = 2;

	/**
	 * Gets the protocol of this ListenerInterface.
	 * @return the protocol of this ListenerInterface
	 */
	int getProtocol();

	/**
	 * Get the host (hostname or IP) of this interface as a String.
	 *
	 * @return The host of this interface.
	 */
	String getHost();

	/**
	 * Get the port of this interface.
	 * @return The port of this interface.
	 */
	int getPort();

	/**
	 * Get the validity of this interface.
	 * @return Whether this interface is valid or not.
	 */
	boolean isValid();

	/**
	 * Returns a String equal to getHost(). If getHost() is an IP, we try to find the hostname
	 * and add it in brackets. If getHost() is a hostname, we try to find the IP and add
	 * it in brackets. If we can't resolve getHost() (IP or hostname), only getHost() without
	 * the additional information is returned.
	 *
	 * @return The host of this interface with additional information.
	 */
	//String getHostAndIp();

}
