/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.ResourceLoader;

public class Util 
{

	public static void addDefaultCertificates(String a_certspath, int a_type, String a_ignoreCertMark)
	{
		addDefaultCertificates(a_certspath, null, a_type, a_ignoreCertMark);
	}
	
	public static void addDefaultCertificates(String a_certspath, int a_type)
	{
		addDefaultCertificates(a_certspath, null, a_type, null);
	}
	
	public static void addDefaultCertificates(String a_certspath, String[] a_singleCerts, int a_type)
	{
		addDefaultCertificates(a_certspath, a_singleCerts, a_type, null);
	}
	
	public static void addDefaultCertificates(String a_certspath, String[] a_singleCerts, int a_type, String a_ignoreCertMark)
	{
		JAPCertificate defaultRootCert = null;

		if (a_singleCerts != null)
		{
			for (int i = 0; i < a_singleCerts.length; i++)
			{
				if (a_singleCerts[i] != null && (a_ignoreCertMark == null || !a_singleCerts[i].endsWith(a_ignoreCertMark)))
				{
					defaultRootCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
							"certificates/" + a_certspath + a_singleCerts[i]));
					if (defaultRootCert == null)
					{
						continue;
					}
					SignatureVerifier.getInstance().getVerificationCertificateStore().
						addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
				}
			}
		}

		Enumeration certificates =
			JAPCertificate.getInstance("certificates/" + a_certspath, true, a_ignoreCertMark).elements();
		while (certificates.hasMoreElements())
		{
			defaultRootCert = (JAPCertificate) certificates.nextElement();
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
		}
		/* no elements were found */
		if (defaultRootCert == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading certificates of type '" + a_type + "'.");
		}	
	}	
}
