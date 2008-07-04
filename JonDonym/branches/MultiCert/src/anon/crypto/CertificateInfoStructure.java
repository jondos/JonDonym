/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package anon.crypto;

public class CertificateInfoStructure
{
  private CertPath m_certPath;

  private JAPCertificate m_parentCertificate;

  private int m_certificateType;

  private boolean m_enabled;

  private boolean m_certificateNeedsVerification;

  private boolean m_onlyHardRemovable;

	private boolean m_bNotRemovable;

	public CertificateInfoStructure(JAPCertificate a_certificate, JAPCertificate a_parentCertificate,
										int a_certificateType, boolean a_enabled,
										boolean a_certificateNeedsVerification, boolean a_onlyHardRemovable,
									boolean a_notRemovable)
	{
		this (new CertPath(a_certificate), a_parentCertificate, a_certificateType, a_enabled,
			  a_certificateNeedsVerification, a_onlyHardRemovable, a_notRemovable);
	}


	public CertificateInfoStructure(CertPath a_certPath, JAPCertificate a_parentCertificate,
									int a_certificateType, boolean a_enabled,
									boolean a_certificateNeedsVerification, boolean a_onlyHardRemovable,
									boolean a_notRemovable)
	{
		if (a_certPath == null || a_certPath.getFirstCertificate() == null)
		{
			throw new IllegalArgumentException("Invalid cert path!");
		}
		m_certPath = a_certPath;
		m_parentCertificate = a_parentCertificate;
		m_certificateType = a_certificateType;
		m_enabled = a_enabled;
		m_certificateNeedsVerification = a_certificateNeedsVerification;
		m_onlyHardRemovable = a_onlyHardRemovable;
		m_bNotRemovable = a_notRemovable;
	}

	public JAPCertificate getCertificate()
	{
		return m_certPath.getFirstCertificate();
	}

	public JAPCertificate getParentCertificate()
	{
		return m_parentCertificate;
	}


	public CertPath getCertPath()
	{
		return m_certPath;
	}

	public int getCertificateType()
	{
		return m_certificateType;
	}

	public boolean getCertificateNeedsVerification()
	{
		return m_certificateNeedsVerification;
	}

	public boolean isAvailable()
	{
		boolean returnValue = false;
		synchronized (this)
		{
			returnValue = ((!m_certificateNeedsVerification) || (m_parentCertificate != null)) && m_enabled;
		}
		return returnValue;
	}

	public boolean isOnlyHardRemovable()
	{
		return m_onlyHardRemovable;
	}

	public boolean isNotRemovable()
	{
		return m_bNotRemovable;
	}

	public boolean isEnabled()
	{
		return m_enabled;
	}

}
