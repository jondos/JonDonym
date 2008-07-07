/*
 Copyright (c) 2000 - 2008, The JAP-Team
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

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;

public class CertificationPathBuilder
{
	private Vector m_endEntityCertificates;
	private Vector m_pathCertificates;
	private Vector m_trustAnchorCertificates;
	
	public CertificationPathBuilder(
			Vector a_endEntityCertificates, 
			Vector a_pathCertificates, 
			Vector a_trustAnchorCertificates)
	{
		m_endEntityCertificates = a_endEntityCertificates;
		m_pathCertificates = a_pathCertificates;
		m_trustAnchorCertificates = a_trustAnchorCertificates;
	}
	
	private void doExtendedNameChaining()
	{
		Enumeration endEntityCertificates = m_endEntityCertificates.elements();
		Enumeration pathCertificates = null;
		Enumeration trustAnchorCertificates = m_endEntityCertificates.elements();
		
		if(m_pathCertificates != null)
		{
			pathCertificates = m_pathCertificates.elements();
		}
		
		JAPCertificate endEntity, pathEntity, trustAnchor;
		X509DistinguishedName currentIssuer;
		X509AuthorityKeyIdentifier x509aki;
		String aki;
		
		while(endEntityCertificates.hasMoreElements())
		{
			endEntity = (JAPCertificate)endEntityCertificates.nextElement();
			currentIssuer = endEntity.getIssuer();
			x509aki = (X509AuthorityKeyIdentifier)endEntity.getExtensions().getExtension(X509AuthorityKeyIdentifier.IDENTIFIER);
			aki = x509aki.getValueWithoutColon();
			
			if(pathCertificates != null)
			{
				
			}
		}
	}
	
	private void findIssuer(
			Enumeration a_certificates, 
			X509DistinguishedName a_name, 
			String a_keyID)
	{
		
	}
	
	
}
