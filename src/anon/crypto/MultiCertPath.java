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

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;

/**
 * This class takes an array of CertPaths that is associated with
 * a signed XML Document. A MultiCertPath is considered valid an verified
 * if ONE CertPath in it is both.
 * @author zenoxx
 */
public class MultiCertPath implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "MultiCertPath";
	
	private CertPath[] m_certPaths;
	private X509DistinguishedName m_subject;
	private X509DistinguishedName m_issuer;
	private int m_documentType;
	
	protected MultiCertPath(CertPath[] a_certPaths, int a_documentType)
	{
		if (a_certPaths.length != 0 && a_certPaths[0] != null)
		{
			m_subject = a_certPaths[0].getFirstCertificate().getSubject();
			m_issuer = a_certPaths[0].getFirstCertificate().getIssuer();
						
			for(int i=1; i<a_certPaths.length; i++)
			{
				//a MultiCertPath may only consist of CertPaths for the same subject and from the same issuer
				if(!m_subject.equals(a_certPaths[i].getFirstCertificate().getSubject()))
				{
					throw new IllegalArgumentException("Wrong subject in MultiCertPath!");
				}
				if(!m_issuer.equals(a_certPaths[i].getFirstCertificate().getIssuer()))
				{
					throw new IllegalArgumentException("Wrong issuer in MultiCertPath!");
				}
			}
		}
		m_documentType = a_documentType;
		m_certPaths = a_certPaths;
	}
	
	/**
	 * Returns <code>true</code> if one verifiable path is valid or
	 * in case there is no verifiable path, one unverifiable path is valid
	 * @param a_date
	 * @return if a MultiCertPath is valid for the time in question
	 */
	public boolean isValid(Date a_date)
	{
		if(!this.needsVerification())
		{
			return true;
		}
		
		synchronized (m_certPaths)
		{
			//check if we have a verified path
			boolean checkOnlyVerfied = this.getFirstVerifiedPath() != null;
			
			for(int i=0; i<m_certPaths.length; i++)
			{	
				//look for a verified (or unverified) path that is valid
				if(((checkOnlyVerfied && m_certPaths[i].verify()) || !checkOnlyVerfied) 
						&& m_certPaths[i].checkValidity(a_date))
				{
					return true;
				}
			}
			return false;
		}	
	}
	
	/**
	 * Check if the documentType that is associated with this MultiCertPath
	 * needs signature verification.
	 * @return <code>true</code> if the MultiCertPath has to be verified
	 */
	private boolean needsVerification()
	{
		return (SignatureVerifier.getInstance().isCheckSignatures() && 
				SignatureVerifier.getInstance().isCheckSignatures(m_documentType));
	}
	
	/**
	 * At the moment we try to find a single verifiable CertPath and return
	 * <code>true</code> if there is one, or if signature verification is disabled.
	 * @return if this MultiCertPath is verified
	 */
	public boolean isVerified()
	{
		if(!this.needsVerification())
		{
			return true;
		}
		return (getFirstVerifiedPath() != null);
	}
	
	/**
	 * Tries to return the first verified CertPath. If there is
	 * none the first (unverified) Path is returned.
	 * @return
	 */
	public CertPath getPath()
	{
		synchronized (m_certPaths)
		{
			CertPath path = this.getFirstVerifiedPath();
			if(path == null)
			{
				path = m_certPaths[0];
			}
			return path;
		}	
	}
	
	/**
	 * Returns all CertPath objects, no matter if verified or not.
	 * @return
	 */
	public Vector getPaths()
	{
		Vector vecPaths = new Vector();
		
		for (int i = 0; i < m_certPaths.length; i++)
		{
			vecPaths.addElement(m_certPaths[i]);
		}
		
		return vecPaths;
	}
	
	/**
	 * Gets the first verified CertPath of this MultiCertPath.
	 * @return the first verified CertPath or null if there is none
	 */
	public CertPath getFirstVerifiedPath()
	{
		synchronized (m_certPaths)
		{
			for(int i=0; i<m_certPaths.length; i++)
			{
				if(m_certPaths[i] != null && m_certPaths[i].verify())
				{
					return m_certPaths[i];
				}
			}
			return null;
		}
	}

	/**
	 * This method is used by the checkId()-methods of the database classes,
	 * that compare the id of a given entry with the SubjectKeyIdentifier of
	 * the assoicated cert(s). If there is only one cert its ski is returned, 
	 * else the XOR of all end-entity-certs' SKIs is returned.
	 * @see anon.infoservice.AbstractCertifiedDatabaseEntry.checkId()
	 * @see anon.infoservice.AbstractDistributableCertifiedDatabaseEntry.checkId()
	 * @return the xor of all end-entity-certs' SKIs
	 */
	/*public String getXORofSKIs()
	{
		synchronized (m_certPaths)
		{
			byte[] raw = new byte[20];
			JAPCertificate cert;
			
			for(int i=0; i<m_certPaths.length; i++)
			{
				cert = m_certPaths[i].getFirstCertificate();
				byte[] ski = cert.getRawSubjectKeyIdentifier();
				
				for(int j=0; j<raw.length; j++)
				{
					raw[j] = (byte) (raw[j] ^ ski[j]);
				}
			}
			return new String(Hex.encode(raw));
		}
	}*/
	
	/**
	 * Gets all successfully verified end entity Keys from this MultiCertPath.
	 * Used by the KeyExchangeManager to verifiy the signature of the symmetric Key
	 * @see anon.client.KeyExchangeManager
	 * @return all verified end-entity-keys
	 */
	public Vector getEndEntityKeys()
	{
		synchronized (m_certPaths)
		{
			Vector keys = new Vector();
			
			for(int i=0; i<m_certPaths.length; i++)
			{
				if(!this.needsVerification() || m_certPaths[i].verify())
				{
					keys.addElement(m_certPaths[i].getFirstCertificate().getPublicKey());
				}
			}
			if(keys.size() != 0)
			{
				return keys;
			}
			return null;
		}
	}
	
	/**
	 * Returns this MultiCertPath's Subject which is the same for all end-entity certs
	 * @return this MultiCertPath's Subject
	 */
	public X509DistinguishedName getSubject()
	{
		return m_subject;
	}
	
	/**
	 * Returns this MultiCertPaths Issuer which is the same for all end-entity certs
	 * @return this MultiCertPaths Issuer
	 */
	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}
	
	
	/**
	 * Returns the number of CertPaths in this MultCertPath
	 * @return the number of CertPaths in this MultCertPath
	 */
	public int countPaths()
	{
		synchronized (m_certPaths)
		{
			return m_certPaths.length;
		}
	}
	
	/**
	 * Returns the number of verified CertPaths in this MultCertPath
	 * @return the number of verified CertPaths in this MultCertPath
	 */
	public int countVerifiedPaths()
	{
		int count = 0;
		
		if(!this.needsVerification())
		{
			return countPaths();
		}
		synchronized (m_certPaths)
		{
			for(int i=0; i<m_certPaths.length; i++)
			{
				if(m_certPaths[i].verify())
				{
					count++;
				}
			}
			return count;
		}	
	}
	
	/**
	 * Returns the number of paths that are verified and timely valid.
	 * The return value is at least 1 if at least one verified path exists,
	 * no matter whether any of the paths is valid or not.
	 * @return the number of paths that are verified and timely valid
	 */
	public int countVerifiedAndValidPaths()
	{
		int count = 0;
		
		synchronized (m_certPaths)
		{
			for (int i=0; i < m_certPaths.length; i++)
			{
				if (!this.needsVerification() || m_certPaths[i].verify())
				{
					if (//count == 0 || // count at least 1 verified path
						m_certPaths[i].checkValidity(new Date()))
					{
						count++;
					}
				}
			}
		}
		
		return count;
	}
	
	public int getMaxLength() 
	{
		int maxLength = 0;
		
		synchronized (m_certPaths)
		{
			for(int i=0; i<m_certPaths.length; i++)
			{
				if(m_certPaths[i].length() > maxLength)
				{
					maxLength = m_certPaths[i].length();
				}
			}		
			return maxLength;
		}
	}
	
	public CertPathInfo[] getPathInfos()
	{
		synchronized (m_certPaths)
		{
			CertPathInfo[] infos = new CertPathInfo[m_certPaths.length];
			
			for(int i=0; i<m_certPaths.length; i++)
			{
				infos[i] = m_certPaths[i].getPathInfo();
				if(!this.needsVerification())
				{
					infos[i].setVerified(true);
				}
			}
			return infos;
		}
	}

	public Element toXmlElement(Document a_doc)
	{
		Enumeration certificates;
		Element elemMultiCertPath;
		
		if (a_doc == null)
		{
			return null;
		}
		
		elemMultiCertPath = a_doc.createElement(XML_ELEMENT_NAME);
		
		synchronized (m_certPaths)
		{
			for(int i=0; i<m_certPaths.length; i++)
			{
				certificates = m_certPaths[i].getCertificates().elements();
				while(certificates.hasMoreElements())
				{
					elemMultiCertPath.appendChild(((JAPCertificate) certificates.nextElement()).toXmlElement(a_doc));
				}
			}
		}
		return elemMultiCertPath;
	}
}
