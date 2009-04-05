package anon.proxy;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

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
public class HTTPConnectionEvent 
{
	private HTTPConnectionHeader connectionHeader;
	private volatile long upStreamContentBytes;
	private volatile long downStreamContentBytes;
	private AnonProxyRequest anonRequest;
	private boolean m_bNeedsConfirmation = false;
	
	public HTTPConnectionEvent() 
	{
	}
	
	HTTPConnectionEvent(HTTPConnectionHeader connectionHeader, long upStreamContentBytes,
								long downStreamContentBytes, AnonProxyRequest anonRequest) 
	{
		this.connectionHeader = connectionHeader;
		this.upStreamContentBytes = upStreamContentBytes;
		this.downStreamContentBytes = downStreamContentBytes;
		this.anonRequest = anonRequest;
	}


	public HTTPConnectionHeader getConnectionHeader() 
	{
		return connectionHeader;
	}

	public void setNeedsConfirmation(boolean a_bNeedsConfirmation)
	{
		m_bNeedsConfirmation = a_bNeedsConfirmation;
	}
	
	public boolean isConfirmationNeeded()
	{
		return m_bNeedsConfirmation;
	}
	
	public void setConnectionHeader(HTTPConnectionHeader connectionHeader) 
	{
		this.connectionHeader = connectionHeader;
	}


	public long getUpStreamContentBytes() 
	{
		return upStreamContentBytes;
	}


	public void setUpStreamContentBytes(long upStreamContentBytes) 
	{
		this.upStreamContentBytes = upStreamContentBytes;
	}


	public long getDownStreamContentBytes()
	{
		return downStreamContentBytes;
	}


	public void setDownStreamContentBytes(long downStreamContentBytes) 
	{
		this.downStreamContentBytes = downStreamContentBytes;
	}


	public AnonProxyRequest getAnonRequest() 
	{
		return anonRequest;
	}


	public void setAnonRequest(AnonProxyRequest anonRequest) 
	{
		this.anonRequest = anonRequest;
	}
	
	
}
