/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * based on tinySSL
 * (http://www.ritlabs.com/en/products/tinyweb/tinyssl.php)
 *
 */
package anon.crypto.tinytls;

abstract public class AbstractTLSRecord
{
	protected int m_Type;
	protected int m_dataLen;
	protected byte[] m_Data;
	protected byte[] m_Header;


	/** Retruns the original buffer of the header of this TLS record!*/
	public byte[] getHeader()
	{
		return m_Header;
	}

	/** Retruns the original buffer of the data of this TLS record!*/
	public byte[] getData()
	{
		return m_Data;
	}

	/**
	 * sets the typeof the tls record
	 * @param type
	 * type
	 */
	public void setType(int type)
	{
		m_Type = type;
		m_Header[0] = (byte) (type & 0x00FF);
	}

	/** return the type of this tls record
	 *
	 */
	public int getType()
	{
		return m_Type;
	}

	/** Return the size of the payload data of this record.*/
	public int getLength()
	{
		return m_dataLen;
	}

	abstract public int getHeaderLength();

}
