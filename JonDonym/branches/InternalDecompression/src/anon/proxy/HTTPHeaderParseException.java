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

import anon.infoservice.HttpResponseStructure;

public class HTTPHeaderParseException extends ChunkNotProcessableException
{
	
	private int errorCode = 0;
	private String statusMessage = "";
	private String errorDescription = "";
	
	public HTTPHeaderParseException(int errorCode, int headerType)
	{
		super();
		this.errorCode = errorCode;
		if(headerType == HTTPProxyCallback.MESSAGE_TYPE_REQUEST)
		{
			switch (errorCode)
			{
				/* right now the only one available is 400 Bad Request */
				case HttpResponseStructure.HTTP_RETURN_BAD_REQUEST:
				default:
				{
					statusMessage = HttpResponseStructure.HTTP_RETURN_BAD_REQUEST_STRING;
				}
			}
		}
		else
		{
			switch (errorCode)
			{
				/* right now the only one available for responses is 500 Internal Server Error */
				case HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR:
				default:
				{
					statusMessage = HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR_STRING;
				}
			}
		}
	}
	
	public HTTPHeaderParseException(int errorCode, int headerType, String message)
	{
		this(errorCode, headerType);
		errorDescription = message;
	}
	
	public String getMessage()
	{
		String errorResponse =
			"<html><head><title>"+statusMessage+"</title></head>"+
			"<body><h1>"+statusMessage+"</h1>" +
					(errorDescription.equals("") ? "" : "<p>"+errorDescription+"</p>")+
			"</html>";
		
		return errorResponse;
	}

	
	public byte[] getErrorResponse() 
	{
		return new HttpResponseStructure(errorCode, getMessage()).getResponseData();
	}
	
	
}
