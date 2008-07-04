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
package infoservice;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.CertPath;
import anon.infoservice.Database;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class CertificateManager implements Observer
{

	private Hashtable m_certificateLocks;

	public CertificateManager()
	{
		m_certificateLocks = new Hashtable();
		Database.getInstance(MixCascade.class).addObserver(this);
	}

	public void update(Observable a_observedObject, Object a_message)
	{
		try
		{
			if (a_observedObject == Database.getInstance(MixCascade.class))
			{
				DatabaseMessage message = (DatabaseMessage) a_message;
				switch (message.getMessageCode())
				{
					case DatabaseMessage.ENTRY_ADDED:
					{
						updateEntry( (MixCascade) (message.getMessageData()));
						break;
					}
					case DatabaseMessage.ENTRY_RENEWED:
					{
						updateEntry( (MixCascade) (message.getMessageData()));
						break;
					}
					case DatabaseMessage.ENTRY_REMOVED:
					{
						removeEntry( (MixCascade) (message.getMessageData()));
						break;
					}
					case DatabaseMessage.ALL_ENTRIES_REMOVED:
					{
						synchronized (m_certificateLocks)
						{
							Enumeration allLocks = m_certificateLocks.keys();
							while (allLocks.hasMoreElements())
							{
								removeEntry( (MixCascade) (allLocks.nextElement()));
							}
						}
						break;
					}
					case DatabaseMessage.INITIAL_OBSERVER_MESSAGE:
					{
						synchronized (m_certificateLocks)
						{
							Enumeration allEntries = ( (Vector) (message.getMessageData())).elements();
							while (allEntries.hasMoreElements())
							{
								updateEntry( (MixCascade) (allEntries.nextElement()));
							}
						}
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			/* should never occur, but so we will never throw a NullPointerException or a
			 * ClassCastException
			 */
		}
	}

	private void updateEntry(MixCascade a_newEntry)
	{
		synchronized (m_certificateLocks)
		{
			CertPath newCertificate = a_newEntry.getCertPath();
			Integer oldEntryCertificateLock = null;
			boolean removeCertificateLock = false;
			if (newCertificate != null)
			{
				int newCertificateLock = SignatureVerifier.getInstance().getVerificationCertificateStore().
					addCertificateWithVerification(newCertificate, JAPCertificate.CERTIFICATE_TYPE_MIX, false);
				if (newCertificateLock != -1)
				{
					oldEntryCertificateLock = (Integer) (m_certificateLocks.put(a_newEntry,
						new Integer(newCertificateLock)));
					if (oldEntryCertificateLock != null)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							"The lock on the certificate of the MixCascade '" +
									  a_newEntry.getId() + "' was updated in the certificate store.");
					}
					else
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							"The lock on the certificate of the MixCascade '" +
									  a_newEntry.getId() + "' was added to the certificate store.");
					}
				}
				else
				{
					removeCertificateLock = true;
				}
			}
			else
			{
				removeCertificateLock = true;
			}
			if (removeCertificateLock == true)
			{
				/* there is no appended certificate or there was an error while adding it to the certificate store */
				oldEntryCertificateLock = (Integer) (m_certificateLocks.remove(a_newEntry));
				if (oldEntryCertificateLock != null)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						"CertificateManager: updateEntry: The lock on the certificate of the MixCascade '" +
								  a_newEntry.getId() +
						"' was removed from the certificate store. Cannot add the certificate from the updated MixCascade entry.");
				}
			}
			if (oldEntryCertificateLock != null)
			{
				/* remove the old lock from the certificate store */
				SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(
					oldEntryCertificateLock.intValue());
			}
		}
	}

	private void removeEntry(MixCascade a_entryToRemove)
	{
		synchronized (m_certificateLocks)
		{
			Integer certificateLock = (Integer) (m_certificateLocks.remove(a_entryToRemove));
			if (certificateLock != null)
			{
				SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(
					certificateLock.intValue());
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					"CertificateManager: removeEntry: The lock on the certificate of the MixCascade '" +
							  a_entryToRemove.getId() + "' was removed from the certificate store.");
			}
		}
	}

}
