/*
 Copyright (c) 2008 The JAP-Team, JonDos GmbH

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation and/or
      other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
      the JonDos GmbH, nor the names of their contributors may be used to endorse or
      promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package infoservice.performance;

import org.w3c.dom.Document;

import infoservice.HttpResponseStructure;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.infoservice.Database;
import anon.crypto.SignatureVerifier;

/**
 * Handles an incoming <code>PerformanceRequest</code> or
 * <code>PerformanceTokenRequest</code>.
 * 
 * @author Christian Banse
 */
public class PerformanceRequestHandler 
{
	/**
	 * Magic bytes for the IPv4 address.
	 */
	public static byte[] MAGIC_BYTES_IPV4 = { 0x49, 0x50, 0x56, 0x34 }; // IPV4
	
	/**
	 * Magic bytes for the IPv6 address.
	 */
	public static byte[] MAGIC_BYTES_IPV6 = { 0x49, 0x50, 0x56, 0x36 }; // IPV6
	
	/**
	 * Retrieves the XML structure of a PerformanceTokenRequest and
	 * issues a new token to the <code>InfoService</code>.
	 * 
	 * @param a_postData The XML data.
	 * @return A <code>HttpResponseStructure</code> with the issued token.
	 */
	public HttpResponseStructure handlePerformanceTokenRequest(byte[] a_postData)
	{
		Document doc = null;
		PerformanceTokenRequest request = null;
		
		try
		{
			doc = XMLUtil.toXMLDocument(a_postData);
			request = new PerformanceTokenRequest(doc.getDocumentElement());
		}
		catch(XMLParseException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Error while processing PerformanceTokenRequest: " + ex.getMessage());
			
			return new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "InfoService " + request.getInfoServiceId() + " is requesting a performance token.");
	
		// generate a new token
		PerformanceToken token = new PerformanceToken();
		Database.getInstance(PerformanceToken.class).update(token);
		
		// sign the token
		doc = XMLUtil.toSignedXMLDocument(token, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(doc));
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Token " + token.getId() + " issued.");
		
		return httpResponse;
	}
	
	/**
	 * Retrieves the XML structure of a PerformanceRequest and
	 * sends random data back to the <code>InfoService</code>.
	 * 
	 * This requires a valid token id.
	 * 
	 * @param a_postData The XML data.
	 * @return A <code>HttpResponseStructure</code> with the issued token.
	 */
	public HttpResponseStructure handlePerformanceRequest(InetAddress a_address, byte[] a_postData)
	{
		Document doc = null;
		PerformanceRequest request = null;
		
		try
		{
			doc = XMLUtil.toXMLDocument(a_postData);
			
			// the PerformanceRequest constructor will throw an exception
			// if the info service did not specify a valid token id
			request = new PerformanceRequest(doc.getDocumentElement());
		}
		catch(XMLParseException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Error while processing PerformanceTokenRequest: " + ex.getMessage());
			
			return new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		
		// we seem to have a valid token
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Token " + request.getTokenId() + " is requesting " + request.getDataSize() + " bytes of random data.");
		
		// generate random data
		byte[] data = new byte[request.getDataSize()];
		new java.util.Random().nextBytes(data);
		
		if(a_address instanceof Inet4Address)
		{
			System.arraycopy(MAGIC_BYTES_IPV4, 0, data, 0, 4);
		
			byte[] ip = a_address.getAddress();
			System.arraycopy(ip, 0, data, 4, 4);
		} 
		else if(a_address instanceof Inet6Address)
		{
			System.arraycopy(MAGIC_BYTES_IPV6, 0, data, 0, 4);
			
			byte[] ip = a_address.getAddress();
			System.arraycopy(ip, 0, data, 4, 16);			
		}
		
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, data);
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, data.length + " bytes sent. Removed token.");
		
		// remove the token from the database
		Database.getInstance(PerformanceToken.class).remove(request.getTokenId());
		
		return httpResponse;
	}
}
