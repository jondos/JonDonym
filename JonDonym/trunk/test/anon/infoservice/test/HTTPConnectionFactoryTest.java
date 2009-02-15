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
package anon.infoservice.test;

import HTTPClient.HTTPConnection;
import junitx.framework.PrivateTestCase;

import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;

/**
 * These are the tests for the HTTPConnectionFactory class.
 * @author Wendolsky
 */
public class HTTPConnectionFactoryTest extends PrivateTestCase
{
	public HTTPConnectionFactoryTest(String a_name)
	{
		super(a_name);
	}

	/**
	 * Configures the factory.
	 * @throws Exception if the HTTPConnection class could no be altered
	 */
	protected void setUp() throws Exception
	{
		Object[] args = new Object[1];

		args[0] = DummyHTTPConnection.class;
		// create the factory
		invoke(HTTPConnectionFactory.getInstance(), "setHTTPConnectionClass", args);
	}

	/**
	 * Tests if the timeout is set correctly.
	 */
	public void testTimeout()
	{
		HTTPConnectionFactory.getInstance().setTimeout(50);
		assertEquals(50, HTTPConnectionFactory.getInstance().getTimeout());
	}

	public void testCreateHTTPConnection() throws Exception
	{
		ListenerInterface listener =
			new ListenerInterface("testhost", 1000, ListenerInterface.PROTOCOL_TYPE_HTTPS);
		HTTPConnection connection;

		connection = HTTPConnectionFactory.getInstance().createHTTPConnection(listener);

		assertTrue(connection instanceof DummyHTTPConnection);
		assertEquals("testhost", connection.getHost());
		assertEquals(1000, connection.getPort());
		// protocol is ignored by the factory
		assertEquals("http", connection.getProtocol());
		//System.out.println(connection.toString());
	}
}
