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
package jap;

import java.util.Enumeration;
import java.util.Vector;

import anon.crypto.CertificateInfoStructure;
import anon.crypto.SignatureVerifier;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation for the certificate management savepoint. It is needed for restoring
 * an old or the default configuration, if the user presses "Cancel" or "Reset to defaults".
 */
public class JAPConfCertSavePoint implements IJAPConfSavePoint
{

	/**
	 * Stores all unverified persistent certificates of the verification certificate store. This is
	 * enough, because the user can only add unverified persistent certificates and nothing else. So
	 * it's enough to restore those.
	 */
	private Vector m_unverifiedPersisitentCertificates;

	/**
	 * Creates a new instance of JAPConfCertSavePoint. We do some initialization here.
	 */
	public JAPConfCertSavePoint()
	{
		m_unverifiedPersisitentCertificates = new Vector();
	}

	/**
	 * This method will store the current certificate configuration in this savepoint.
	 */
	public void createSavePoint()
	{
		/* clear the Vector of unverified certificates */
		m_unverifiedPersisitentCertificates.removeAllElements();
		/* store all certificates which don't need verification in the vector of unverified
		 * certificates
		 */
		Enumeration allCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAllCertificates().elements();
		while (allCertificates.hasMoreElements())
		{
			CertificateInfoStructure currentCertificate = (CertificateInfoStructure) (allCertificates.
				nextElement());
			if ( (currentCertificate.getCertificateNeedsVerification() == false) &&
				(currentCertificate.isOnlyHardRemovable() == true))
			{
				/* this is a persistent certificate without the need of verification -> store it */
				m_unverifiedPersisitentCertificates.addElement(currentCertificate);
			}
		}
		/* that's it -> now it is possible to restore all persistent certificates not depending on
		 * other certificates
		 */
	}

	/**
	 * Restores the old certificate configuration (stored with the last call of createSavePoint()).
	 */
	public void restoreSavePoint()
	{
		/* first: remove all persistent certificates, which are added without verification */
		Enumeration allCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAllCertificates().elements();
		while (allCertificates.hasMoreElements())
		{
			CertificateInfoStructure currentCertificate = (CertificateInfoStructure) (allCertificates.
				nextElement());
			if ( (currentCertificate.getCertificateNeedsVerification() == false) &&
				(currentCertificate.isOnlyHardRemovable() == true))
			{
				/* this is a persistent certificate without the need of verification -> remove it */
				SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificate(currentCertificate);
			}
		}
		/* second: add the persistent certificates which don't need verification (they were stored by
		 * the last call of createSavePoint())
		 */
		Enumeration oldCertificates = m_unverifiedPersisitentCertificates.elements();
		while (oldCertificates.hasMoreElements())
		{
			CertificateInfoStructure currentCertificate = (CertificateInfoStructure) (oldCertificates.
				nextElement());
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(currentCertificate.getCertificate(),
												  currentCertificate.getCertificateType(), true,false);
			/* also restore the enabled/disabled state */
			SignatureVerifier.getInstance().getVerificationCertificateStore().setEnabled(currentCertificate, currentCertificate.isEnabled());
		}
	}

	/**
	 * Restores the default certificate configuration.
	 */
	public void restoreDefaults()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "JAPConfCertSavePoint: restoreDefaults: Restoring default certificate settings.");
		Enumeration allCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAllCertificates().elements();
		/* first: remove all certificates, which are added without verification */
		while (allCertificates.hasMoreElements())
		{
			CertificateInfoStructure currentCertificate = (CertificateInfoStructure) (allCertificates.
				nextElement());
			if (!currentCertificate.getCertificateNeedsVerification())
			{
				/* this is a certificate without the need of verification -> remove it */
				SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificate(currentCertificate);
			}
		}
		/* second: add the JAP root certificate and the update messages certificate to the store */
		JAPController.addDefaultCertificates();
		/* that's it -> only the default certificates and all certificate which can be verified
		 * against the default root certificate are activated in the store
		 */
	}

}
