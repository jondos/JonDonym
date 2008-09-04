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

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.IDistributable;
import anon.infoservice.HTTPConnectionFactory;

/**
 * This class is a dummy implementation and for testing purposes only.
 * @author Rolf Wendolsky
 */
public class DummyDatabaseEntry extends AbstractDatabaseEntry implements IDistributable
{
	private String m_id;
	private long m_versionNumber;
	private String m_postData;
	private String m_postFile;

	public DummyDatabaseEntry()
	{
		super(Long.MAX_VALUE);
		// do some initializations
		m_id = "";
		m_versionNumber = 0;
	}

	public byte[] getPostData()
	{
		return m_postData.getBytes();
	}

	public void setPostData(String a_postData)
	{
		m_postData = a_postData;
	}

	public int getPostEncoding()
	{
		return HTTPConnectionFactory.HTTP_ENCODING_PLAIN;
	}

	public String getPostFile()
	{
		return m_postFile;
	}

	public void setPostFile(String a_postFile)
	{
		m_postFile = a_postFile;
	}

	public String getId()
	{
		return m_id;
	}

	public void setId(String a_id)
	{
		m_id = a_id;
	}

	public long getVersionNumber()
	{
		return m_versionNumber;
	}

	public void setVersionNumber(long a_versionNumber)
	{
		m_versionNumber = a_versionNumber;
	}

	public long getLastUpdate() {
		// TODO Auto-generated method stub
		return 0;
	}

}
