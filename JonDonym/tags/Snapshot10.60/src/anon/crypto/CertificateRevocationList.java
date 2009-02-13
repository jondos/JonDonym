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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V2TBSCertListGenerator;
import org.bouncycastle.asn1.x509.X509Name;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.Base64;
import anon.util.IResourceInstantiator;
import anon.util.IXMLEncodable;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;

/**
 * This Class implements Certificate Revocation Lists (CRLs) as specified by RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 * @todo implement methods to change or update a crl.
 */
public class CertificateRevocationList implements IXMLEncodable
{	
	private static final String BASE64_TAG = "X509 CRL";
	
	private static final String XML_ELEMENT_NAME = "X509CRL";
	
	private CertificateList m_crl;
	private Date m_thisUpdate;
	private Date m_nextUpdate;
	private X509DistinguishedName m_issuer;
	private X509Extensions m_extensions;
	
	/**
	 * Creates a new crl.
	 * @param a_issuerCertificate the crl's issuer
	 * @param a_certList the vector of certificates to revoke
	 * @param a_nextUpdate the date when the next crl will be published
	 * @param a_extensions the extensions for the crl
	 */
	public CertificateRevocationList(
			PKCS12 a_issuerCertificate,
			Vector a_certList,
			Date a_nextUpdate,
			X509Extensions a_extensions)
	{
		this(new CRLGenerator(a_issuerCertificate.getSubject().getX509Name(), 
				a_certList, a_nextUpdate, a_extensions).sign(a_issuerCertificate));
	}
	
	/**
	 * Creates a new instance of CertificateRevocationList from a BC CertificateList
	 * @param a_crl a BC CertificateList
	 */
	public CertificateRevocationList(CertificateList a_crl)
	{
		m_crl = a_crl;
		m_issuer = new X509DistinguishedName(m_crl.getIssuer());
		m_extensions = new X509Extensions(m_crl.getTBSCertList().getExtensions());
		m_thisUpdate = m_crl.getThisUpdate().getDate();
		if(m_crl.getNextUpdate() != null)
		{
			m_nextUpdate = m_crl.getNextUpdate().getDate();
		}
	}
	
	/**
	 * Creates a crl from a byte array
	 * @param a_rawCRL the byte array holding the crl
	 * @return an instance of CertificateRevocationList or <code>null</code> if an error occured
	 */
	public static CertificateRevocationList getInstance(byte[] a_rawCRL)
	{
		if (a_rawCRL == null || a_rawCRL.length == 0)
		{
			return null;
		}

		/*try
		{
			ByteArrayInputStream bis = new ByteArrayInputStream(a_rawCRL);
			ASN1InputStream ais = new ASN1InputStream(bis);
			DERSequence sequence = (DERSequence)ais.readObject();
			return new CertificateRevocationList(new CertificateList(sequence));
		}*/
		
		try
		{
			ASN1Sequence crl = JAPCertificate.toASN1Sequence(a_rawCRL, XML_ELEMENT_NAME);

			return new CertificateRevocationList(CertificateList.getInstance(crl));
		}
		catch (Exception e)
		{
			//LogHolder.log(LogLevel.ERR, LogType.CRYPTO, "Error loading CRL from byte array");
			return null;
		}
	}
	
	/**
	 * Create a crl from a file.
	 * @param a_file a file containing a crl
	 * @return an instance of CertificateRevocationList or <code>null</code> if an error occured
	 */
	public static CertificateRevocationList getInstance(File a_file)
	{
		if (a_file != null)
		{
			try 
			{
				return CertificateRevocationList.getInstance(new FileInputStream(a_file));
			}
			catch (Exception e) 
			{
			}
		}
		return null;	
	}
	
	/** 
	 * Creates a crl by using an input stream.
	 * @param a_in Inputstream that holds the crl
	 * @return an instance of CertificateRevocationList or <code>null</code> if an error occured
	 */
	public static CertificateRevocationList getInstance(InputStream a_in)
	{
		byte[] bytes;

		try
		{
			bytes = ResourceLoader.getStreamAsBytes(a_in);
		}
		catch (IOException a_e)
		{
			return null;
		}

		return CertificateRevocationList.getInstance(bytes);
	}
	
	/**
	 * Method to get instances of CertificateRevocationList from files in the specified path
	 * @param a_strResourceSearchPath
	 * @param a_bRecursive
	 * @param a_ignoreCertMark
	 * @return
	 */
	public static Hashtable getInstance(String a_strResourceSearchPath, boolean a_bRecursive, String a_ignoreCertMark)
	{
		try
		{
			return ResourceLoader.loadResources(a_strResourceSearchPath,
							new CRLInstantiator(a_ignoreCertMark), a_bRecursive);
		}
		catch (Exception a_e)
		{
			return new Hashtable();
		}
	}
	
	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}
	
	public boolean isIndirectCRL()
	{
		X509IssuingDistributionPoint idp = 
			(X509IssuingDistributionPoint) m_extensions.getExtension(X509IssuingDistributionPoint.IDENTIFIER);
		if(idp != null)
		{
			return idp.isIndirectCRL();
		}
		return false;
	}
	
	public Date getThisUpdate()
	{
		return m_thisUpdate;
	}
	
	public Date getNextUpdate()
	{
		return m_nextUpdate;
	}
	
	public X509Extensions getExtensions()
	{
		return m_extensions;
	}
	
	/** 
	 * Creates a vector of RevokedCertificates from the CRLEntries on this crl.
	 * @return a vector of RevokedCertificates
	 */
	public Vector getRevokedCertificates()
	{
		Vector v = new Vector();
		TBSCertList.CRLEntry[]  crlEntries = m_crl.getRevokedCertificates();
		for(int i=0; i<crlEntries.length; i++)
		{
			v.addElement(new RevokedCertificate(crlEntries[i]));
		}
		return v;
	}
	
	/**
	 * Convertes the crl to a byte array.
	 * @return the crl as a byte array
	 */
	public byte[] toByteArray()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(bos).writeObject(this.m_crl);
		}
		catch(Exception e)
		{
			//should be impossible
		}
		return bos.toByteArray();
	}
	
	/**
	 * Converts the crl to a byte array.
	 * @param a_Base64Encoded if the crl is converted to a Base64 encoded form.
	 * @throws IOException
	 * @return the crl as a byte array
	 */
	public byte[] toByteArray(boolean a_Base64Encoded)
	{
		if (a_Base64Encoded)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try
			{
				out.write(Base64.createBeginTag(BASE64_TAG).getBytes());
				out.write(Base64.encode(toByteArray(), true).getBytes());
				out.write(Base64.createEndTag(BASE64_TAG).getBytes());
			}
			catch (IOException a_e)
			{
				// should not be possible
			}

			return out.toByteArray();
		}
		else
		{
			return toByteArray();
		}
	}
	
	/**
	 * Verifiy the crl with the given cert
	 * @param a_cert the cert for the verification
	 * @return <code>true</code> if the signature on the crl could be verified
	 *         with the cert's public key or <code>false</code> otherwise.
	 */
	public boolean verifiy(JAPCertificate a_cert)
	{
		if(a_cert == null)
		{
			return false;
		}
		
		try
		{
			ByteArrayOutputStream bArrOStream = new ByteArrayOutputStream();
				(new DEROutputStream(bArrOStream)).writeObject(m_crl.getTBSCertList());

			return ByteSignature.verify(bArrOStream.toByteArray(),
										m_crl.getSignature().getBytes(), a_cert.getPublicKey());
		}
		catch (IOException a_e)
		{
			// should not happen
		}
		
		return false;
	}
	
	/**
	 * Creates XML element of crl consisting of:
	 * <X509CRL>
	 *  Base64 encocded crl
	 * </X509CRL>
	 * @param a_doc The XML document, which is the environment for the created XML element.
	 * @return CRL as XML element.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemX509Crl = a_doc.createElement(XML_ELEMENT_NAME);
		elemX509Crl.setAttribute("xml:space", "preserve");
		XMLUtil.setValue(elemX509Crl, Base64.encode(toByteArray(), true));
		return elemX509Crl;
	}
			
	/**
	 * This class is used to generate, sign and modify CRLs.
	 * @author Robert Hirschberger
	 */
	private static final class CRLGenerator extends V2TBSCertListGenerator
	{
		public CRLGenerator(
				X509Name a_issuer,
				Vector a_certList,
				Date a_nextUpdate,
				X509Extensions a_extensions)
		{
			setIssuer(a_issuer);
			setThisUpdate(new Time(new Date()));
			if(a_nextUpdate != null)
			{
				setNextUpdate(new Time(a_nextUpdate));
			}
			setExtensions(a_extensions.getBCX509Extensions());
			if(a_certList != null)
			{
				X509Extensions entryExtensions;
				Enumeration certificates = a_certList.elements();
				while(certificates.hasMoreElements())
				{
					entryExtensions = null;
					JAPCertificate currentCertificate = (JAPCertificate)certificates.nextElement();
					if(!currentCertificate.getIssuer().equals(a_issuer))
					{
						entryExtensions = new X509Extensions(new X509CertificateIssuer(currentCertificate.getIssuer()));
					}
					RevokedCertificate revCert = new RevokedCertificate(currentCertificate, new Date(), entryExtensions);
					addCRLEntry(revCert.toASN1Sequence());
				}
			}
		}
		
		public CertificateList sign(PKCS12 a_privateIssuerCertificate)
		{
			return sign(a_privateIssuerCertificate.getPrivateKey());
		}

		public CertificateList sign(IMyPrivateKey a_privateKey)
		{
			try
			{
				TBSCertList tbsList;
				DEREncodableVector seqv;
				ByteArrayOutputStream bOut;
				byte[] signature;
				
				setSignature(a_privateKey.getSignatureAlgorithm().getIdentifier());

				/* generate signature */
				bOut = new ByteArrayOutputStream();
				tbsList = generateTBSCertList();
				(new DEROutputStream(bOut)).writeObject(tbsList);
				signature = ByteSignature.sign(bOut.toByteArray(), a_privateKey);

				/* construct crl */
				seqv = new ASN1EncodableVector();
				seqv.add(tbsList);
				seqv.add(a_privateKey.getSignatureAlgorithm().getIdentifier());
				seqv.add(new DERBitString(signature));

				return new CertificateList(new DERSequence(seqv));
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, t);
				return null;
			}
		}
	}
	
	private static final class CRLInstantiator implements IResourceInstantiator
	{
		private String m_ignoreCRLMark;

		public CRLInstantiator(String a_strIgnoreCertMark)
		{
			m_ignoreCRLMark = a_strIgnoreCertMark;
		}

		public Object getInstance(File a_file, File directory) throws Exception
		{
			if (a_file == null || (m_ignoreCRLMark != null && a_file.getName().endsWith(m_ignoreCRLMark)))
			{
				return null;
			}
			return CertificateRevocationList.getInstance(a_file);
		}

		public Object getInstance(ZipEntry a_entry, ZipFile a_file)
				throws Exception
		{
			if (a_file == null || (m_ignoreCRLMark != null && a_file.getName().endsWith(m_ignoreCRLMark)))
			{
				return null;
			}

			return CertificateRevocationList.getInstance(a_file.getInputStream(a_entry));
		}
		
		public Object getInstance(InputStream a_inputStream)
		{
			return CertificateRevocationList.getInstance(a_inputStream);
		}
	}
}
