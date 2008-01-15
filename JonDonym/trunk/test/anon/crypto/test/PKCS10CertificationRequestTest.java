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
package anon.crypto.test;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import anon.crypto.PKCS10CertificationRequest;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import junitx.framework.extension.XtendedPrivateTestCase;

public class PKCS10CertificationRequestTest extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	public PKCS10CertificationRequestTest(String a_name)
	{
		super(a_name);
		m_random = new SecureRandom();
	}

	public void testSaveAndLoad() throws Exception
	{
		PKCS12 privateCertificate;
		PKCS10CertificationRequest request;
		X509DistinguishedName subject;
		Hashtable attributes = new Hashtable();
		byte[] requestData;
		String cn = "Test request";
		String mail = "test@mail.de";
		String org = "Secret organisation";

		attributes.put(X509DistinguishedName.IDENTIFIER_CN, cn);
		attributes.put(X509DistinguishedName.IDENTIFIER_E, mail);
		attributes.put(X509DistinguishedName.IDENTIFIER_O, org);
		subject = new X509DistinguishedName(attributes);

		m_random.setSeed(756232544);
		privateCertificate =
			new PKCS12(subject,
					   new DSATestKeyPairGenerator(m_random).createKeyPair(),
					   new Validity(new GregorianCalendar(2011, 11, 2), 1));


		// save and load the request
		request = privateCertificate.createCertifcationRequest();
		requestData = request.toByteArray(true);
		request = new PKCS10CertificationRequest(new ByteArrayInputStream(requestData));

		assertTrue(request.verify());
		assertEquals(3, request.getX509DistinguishedName().getAttributeValues().size());

		// reimport the certificate from the request
		privateCertificate.setX509Certificate(
				  request.createX509Certificate(privateCertificate,
												new Validity(new GregorianCalendar(2011, 11, 3), 2),
												request.getExtensions(), null));
	}

}
