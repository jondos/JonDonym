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
package anon.proxy;

import java.util.Enumeration;
import java.util.Vector;

/**
 * 
 * @author Simon Pecher
 * Experimental framework class to filter or modify data before it is
 * transferred via the gateway. Customized Callback-Objects can 
 * be registered. 
 */
public class ProxyCallbackHandler 
{
	private Vector callbacks = null;
	
	public ProxyCallbackHandler()
	{
		callbacks = new Vector();
	}
	
	public synchronized void deliverUpstream(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer) 
		throws ProxyCallbackNotProcessableException, ProxyCallbackDelayException
	{
		int currentStatus = ProxyCallback.STATUS_PROCESSABLE;
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			currentStatus = callback.handleUpstreamChunk(anonRequest, buffer);
			if(currentStatus == ProxyCallback.STATUS_DELAY)
			{
				throw new ProxyCallbackDelayException();
			}
		}
	}
	
	
	public synchronized void deliverDownstream(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer) 
		throws ProxyCallbackNotProcessableException, ProxyCallbackDelayException
	{
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		
		int currentStatus = ProxyCallback.STATUS_PROCESSABLE;
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			currentStatus = callback.handleDownstreamChunk(anonRequest, buffer);
			if(currentStatus == ProxyCallback.STATUS_DELAY)
			{
				throw new ProxyCallbackDelayException();
			}
		}
	}
	
	public synchronized void closeRequest(AnonProxyRequest anonRequest)
	{
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			callback.closeRequest(anonRequest);
		}
	}
	
	public synchronized void registerProxyCallback(ProxyCallback callback)
	{
		if(! callbacks.contains(callback))
		{
			callbacks.addElement(callback);
		}
	}
	
	public synchronized void removeCallback(ProxyCallback callback)
	{
		callbacks.removeElement(callback);
	}
}
