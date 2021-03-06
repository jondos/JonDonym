/*
 Copyright (c) 2000, The JAP-Team
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

public final class ErrorCodes
{
	public final static int E_SUCCESS = 0;
	public final static int E_UNKNOWN = -1;
	public final static int E_ALREADY_CONNECTED = -4;
	public final static int E_INVALID_SERVICE = -5;
	public final static int E_CONNECT = -6;
	public final static int E_NOT_CONNECTED = -9;
	public final static int E_PROTOCOL_NOT_SUPPORTED = -10;
	public final static int E_INVALID_CERTIFICATE = -20;
	public final static int E_INVALID_KEY = -21;
	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = -22;
	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = -23;
	public final static int E_INTERRUPTED = -24;
	public final static int E_NOT_TRUSTED = -26;
	public final static int E_NOT_PARSABLE = -27;
	public final static int E_SPACE=-31;
	public final static int E_ACCOUNT_EMPTY = -32;
}
