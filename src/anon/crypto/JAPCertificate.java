/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.Base64;
import anon.util.IResourceInstantiator;
import anon.util.IXMLEncodable;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class represents an X509 certificate.
 */
public final class JAPCertificate implements IXMLEncodable, Cloneable, ICertificate
{
	/**
	 * This are the certificate type constant for root certificates. Root certificates are used to
	 * verify other certificates (at the moment only one indirection is supported, so root
	 * certificates cannot verify other root certificates).
	 * At the moment we have root certificates for Mixes and for InfoServices.
	 */
	public static final int CERTIFICATE_TYPE_ROOT_MIX = 1;
	public static final int CERTIFICATE_TYPE_ROOT_INFOSERVICE = 5;
	public static final int CERTIFICATE_TYPE_ROOT_UPDATE = 6;
	public static final int CERTIFICATE_TYPE_ROOT_PAYMENT = 8;

	/**
	 * This is the certificate type constant for mix certificates. Mix certificates are used to
	 * create or verify the signature of mix, mixcascade or cascade-state XML structures.
	 */
	public static final int CERTIFICATE_TYPE_MIX = 2;

	/**
	 * This is the certificate type constant for infoservice certificates. InfoService certificates
	 * are used to create or verify the signature of an infoservice XML structure.
	 */
	public static final int CERTIFICATE_TYPE_INFOSERVICE = 3;

	/**
	 * This is the certificate type constant for update certificates. Update certificates are used
	 * to create or verify the signature of all JAP update related structures like the minimum
	 * required JAP version or the Java WebStart files for the release or development version of
	 * JAP.
	 */
	public static final int CERTIFICATE_TYPE_UPDATE = 4;

	/** Other certificate types */
	public static final int CERTIFICATE_TYPE_PAYMENT = 7;
	public static final int CERTIFICATE_TYPE_UNKNOWN = 0;

	public static final String XML_ELEMENT_NAME = "X509Certificate";
	public static final String XML_ELEMENT_CONTAINER_NAME = "X509Data";

	private static final String BASE64_TAG = "CERTIFICATE";
	private static final String BASE64_ALTERNATIVE_TAG = "X509 " + BASE64_TAG;
	
	/** SignatureAlgorithmIdentifiers and their names as specified by RFC 3279 */
	private static final String IDENTIFIER_DSA_WITH_SHA1 = 				"1.2.840.10040.4.3";
	private static final String DSA_WITH_SHA1 = 						"dsaWithSHA1";
	
	private static final String IDENTIFIER_MD2_WITH_RSA_ENCRYPTION = 	"1.2.840.113549.1.1.2";
	private static final String MD2_WITH_RSA_ENCRYPTION = 				"md2WithRSAEncryption";
	
	private static final String IDENTIFIER_MD5_WITH_RSA_ENCRYPTION = 	"1.2.840.113549.1.1.4";
	private static final String MD5_WITH_RSA_ENCRYPTION = 				"md5WithRSAEncryption";
	
	private static final String IDENTIFIER_SHA1_WITH_RSA_ENCRYPTION =   "1.2.840.113549.1.1.5";
	private static final String SHA1_WITH_RSA_ENCRYPTION = 				"sha-1WithRSAEncryption";
	
	private static final String IDENTIFIER_ECDSA_WITH_SHA1 = 			"1.2.840.10045.4.1";
	private static final String ECDSA_WITH_SHA1 = 						"ecdsa-with-SHA1";
	

	/**
	 * The dummy private key is used to create temporary certificates.
	 */
	private static IMyPrivateKey ms_dummyPrivateKey;

	private X509CertificateStructure m_bcCertificate;
	private X509DistinguishedName m_subject;
	private X509DistinguishedName m_issuer;
	private X509Extensions m_extensions;
	private X509SubjectKeyIdentifier m_subjectKeyIdentifier;

	private IMyPublicKey m_PubKey;
	private String m_id;
	private String m_sha1Fingerprint;
	private String m_md5Fingerprint;
	private Validity m_validity;


	/**
	 * Creates a new certificate from a valid X509 certificate structure.
	 * @param x509cert a valid X509 certificate structure
	 * @exception IllegalArgumentException if the certificate structure is invalid
	 */
	private JAPCertificate(X509CertificateStructure x509cert) throws IllegalArgumentException
	{
		//super(ASN1Sequence.getInstance(new DERTaggedObject(true, DERTags.BIT_STRING, x509cert), true));
		//m_bcCertificate = this;
		m_bcCertificate = new X509CertificateStructure(ASN1Sequence.getInstance(new DERTaggedObject(true, DERTags.BIT_STRING, x509cert), true));


		byte[] data;

		try
		{
			m_PubKey = AsymmetricCryptoKeyPair.createPublicKey(x509cert.getSubjectPublicKeyInfo());
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			throw new IllegalArgumentException(
				"Certificate structure contains invalid public key! " + a_e);
		}

		data = toByteArray();
		m_sha1Fingerprint = createFingerprint(new SHA1Digest(), data);
		m_md5Fingerprint = createFingerprint(new MD5Digest(), data);

		Calendar startDate, endDate;
		startDate = Calendar.getInstance();
		startDate.setTime(m_bcCertificate.getStartDate().getDate());
		endDate = Calendar.getInstance();
		endDate.setTime(m_bcCertificate.getEndDate().getDate());
		m_validity = new Validity(startDate, endDate);
		m_subject = new X509DistinguishedName(m_bcCertificate.getSubject());
		m_issuer = new X509DistinguishedName(m_bcCertificate.getIssuer());
		m_extensions = new X509Extensions(m_bcCertificate.getTBSCertificate().getExtensions());
		m_id = m_sha1Fingerprint + m_validity.getValidFrom() + m_validity.getValidTo();
		m_subjectKeyIdentifier = (X509SubjectKeyIdentifier)m_extensions.getExtension(X509SubjectKeyIdentifier.IDENTIFIER);
		if(m_subjectKeyIdentifier == null)
		{
			m_subjectKeyIdentifier = new X509SubjectKeyIdentifier(this.getPublicKey());
		}
		
	}

	/**
	 * Creates a new certificate from a valid X509 certificate structure.
	 * @param x509cert a valid X509 certificate structure
	 * @return null if no certificate could be created from the certificate structure
	 */
	public static JAPCertificate getInstance(X509CertificateStructure x509cert)
	{
		JAPCertificate certificate;

		try
		{
			certificate = new JAPCertificate(x509cert);
		}
		catch (IllegalArgumentException a_e)
		{
			return null;
		}

		return certificate;
	}

	/**
	 * Creates a new certificate from a valid X509 certificate.
	 * @param x509cert a valid X509 certificate
	 * @return null if no certificate could be created from the certificate
	 */
	public static JAPCertificate getInstance(JAPCertificate x509cert)
	{
		if (x509cert == null)
		{
			return null;
		}

		return getInstance(x509cert.m_bcCertificate);
	}

	/**
	 * Creates a certificate instance by using the encoded variant of the certificate.
	 * @param a_certificate Byte Array of the Certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(byte[] a_certificate)
	{
		if (a_certificate == null || a_certificate.length == 0)
		{
			return null;
		}

		try
		{
			ASN1Sequence certificate = toASN1Sequence(a_certificate, XML_ELEMENT_NAME);

			if (certificate.size() > 1
				&& certificate.getObjectAt(1) instanceof DERObjectIdentifier
				&& certificate.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
			{
				return getInstance(X509CertificateStructure.getInstance(
								new SignedData(
					ASN1Sequence.getInstance(
					(ASN1TaggedObject) certificate.getObjectAt(1),
					true)).getCertificates()
					.getObjectAt(0)));
			}

			return getInstance(new X509CertificateStructure(certificate));
		}
		catch (Exception a_e)
		{
			//LogHolder.log(LogLevel.DEBUG, LogType.MISC, a_e);
			return null;
		}
	}

	public static Hashtable getInstance(String a_strResourceSearchPath, boolean a_bRecursive,
										String a_strIgnoreCertMark)
	{
		try
		{
			return ResourceLoader.loadResources(a_strResourceSearchPath,
												new X509CertificateInstantiator(a_strIgnoreCertMark),
												a_bRecursive);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, a_e);
			return new Hashtable();
		}
	}

	/**
	 * Instantiates all certificates found in the specified relative resource path.
	 * @param a_strResourceSearchPath a relative path to a resource
	 * @param a_bRecursive if true subdirectories are searched, too
	 * @return a Hashtable with all instanciated certificates
	 */
	public static Hashtable getInstance(String a_strResourceSearchPath, boolean a_bRecursive)
	{
		return getInstance(a_strResourceSearchPath, a_bRecursive, null);
	}

	/** Creates a certificate by using an input stream.
	 *
	 * @param a_in Inputstream that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(InputStream a_in)
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

		return getInstance(bytes);
	}

	/**
	 * Creates a certificate instance by using a XML Node as input.
	 * @param a_NodeRoot <X509Certificate> XML Node
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(Node a_NodeRoot)
	{
		try
		{
			if (!a_NodeRoot.getNodeName().equals(XML_ELEMENT_NAME))
			{
				return null;
			}
			Element elemX509Cert = (Element) a_NodeRoot;
			String strValue = XMLUtil.parseValue(elemX509Cert, (String)null);
			byte[] bytecert = Base64.decode(strValue);
			return getInstance(bytecert);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/** Creates a certificate instance by using a file (either DER encoded or PEM).
	 *
	 * @param a_file File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(File a_file)
	{
		if (a_file == null)
		{
			return null;
		}
		byte[] buff = null;
		try
		{
			buff = new byte[ (int) a_file.length()];
			FileInputStream fin = new FileInputStream(a_file);
			fin.read(buff);
			fin.close();
		}
		catch (Exception e)
		{
			return null;
		}

		return JAPCertificate.getInstance(buff);
	}

	/** Creates a certificate instance by using a file name.
	 *
	 * @param a_strFileName Name of File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(String a_certString)
	{
		try
		{
			return getInstance(a_certString.getBytes());
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Creates an X509 certificate with a short validity from a public key.
	 * The certificate has no owner an no valid signature, and it is not enabled.
	 * But this method is useful if there is a trusted public key, but no corresponding
	 * certificate available. Use this method with care, and enable the certificate only if the
	 * public key is really trusted!
	 * @param a_publicKey IMyPublicKey
	 * @param a_validFrom The date from which the certificate is valid.
	 * @return JAPCertificate
	 */
	public static final JAPCertificate getInstance(IMyPublicKey a_publicKey, Calendar a_validFrom)
	{
		return getInstance(new X509DistinguishedName("CN=void"),
						   new X509DistinguishedName("CN=void"),
                                   getDummyPrivateKey(), a_publicKey,
                                   new Validity(a_validFrom, -1), null, new BigInteger("1"));
	}

	/**
	 * Creates an X509 certificate from a key pair. This method is used to create a self-signed
	 * public certificate.
	 * @param a_ownerAlias The owner of the certificate.
	 * @param a_keyPair a key pair
	 * @param a_validity the validity period of this certificate
	 * @return an X509 certificate
	 */
	public static JAPCertificate getInstance(X509DistinguishedName a_ownerAlias,
											 AsymmetricCryptoKeyPair a_keyPair, Validity a_validity)
	{
		return getInstance(a_ownerAlias, a_keyPair, a_validity, null);
	}

	/**
	 * Creates an X509 certificate from a key pair. This method is used to create a self-signed
	 * public certificate.
	 * @param a_ownerAlias The owner of the certificate.
	 * @param a_keyPair a key pair
	 * @param a_validity the validity period of this certificate
	 * @param a_extensions some X509 extensions (may be null)
	 * @return an X509 certificate
	 */
	public static JAPCertificate getInstance(X509DistinguishedName a_ownerAlias,
											 AsymmetricCryptoKeyPair a_keyPair,
											 Validity a_validity, X509Extensions a_extensions)
	{
		return getInstance(a_ownerAlias, a_ownerAlias, a_keyPair.getPrivate(),
						   a_keyPair.getPublic(), a_validity, a_extensions,
						   new BigInteger("1"));
	}
	
	public static String calculateXORofSKIs(Vector a_certificates)
	{
		byte[] raw;
		Enumeration enumCertificates;
		String strXoredID;
		
		if (a_certificates == null)
		{
			return null;
		}
		
		synchronized (a_certificates)
		{
			if (a_certificates.size() == 0)
			{
				return null;
			}
		
			raw = new byte[20];
			enumCertificates = a_certificates.elements();
			while(enumCertificates.hasMoreElements())
			{
				byte[] ski = ((JAPCertificate) enumCertificates.nextElement()).getRawSubjectKeyIdentifier();
				
				if (ski == null)
				{
					continue;
				}
				for (int j=0; j<raw.length; j++)
				{
					raw[j] = (byte) (raw[j] ^ ski[j]);
				}
			}
			strXoredID = new String(Hex.encode(raw));
		}
		
		return strXoredID.toUpperCase();
	}

	public boolean equals(Object a_certificate)
	{
		if (this == a_certificate)
		{
			return true;
		}

		if (a_certificate == null || ! (a_certificate instanceof JAPCertificate))
		{
			return false;
		}

		// ok, this is a certificate; compare the IDs
		return getId().equals(((JAPCertificate) a_certificate).getId());
	}

	/**
	 * The hash code is derived from the certificate`s id.
	 * @return the hash code
	 */
	public int hashCode()
	{
		return getId().hashCode();
	}

	public Object clone()
	{
		return JAPCertificate.getInstance(m_bcCertificate);
	}

	/**
	 * Returns a unique id for this certificate.
	 * @return a unique id for this certificate
	 */
	public String getId()
	{
		return m_id;
	}

	/** Returns the public key of the certificate.
	 *
	 * @return public key
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_PubKey;
	}

	/**
	 * Returns the certificate's X509 extensions.
	 * @return the certificate's X509 extensions
	 */
	public X509Extensions getExtensions()
	{
		return m_extensions;
	}

	/**
	 * Returns the String representation of the X509SubjectKeyIdentifier
	 * @return this certificate's SubjectKeyIdentifier as a string
	 */
	public String getSubjectKeyIdentifier()
	{
		return m_subjectKeyIdentifier.getValue();
	}
	
	public String getSubjectKeyIdentifierConcatenated()
	{
		return m_subjectKeyIdentifier.getValueWithoutColon();
	}
	
	/**
	 * Returns the byte representation of the X509SubjectKeyIdentifier
	 * @return this certificate's SubjectKeyIdentifier as a raw byte-array
	 */
	public byte[] getRawSubjectKeyIdentifier()
	{
		String ski = m_subjectKeyIdentifier.getValueWithoutColon();
		if (ski == null)
		{
			return null;
		}
		return Hex.decode(ski);
	}
	
	/**
	 * Converts the ObjectIdentifier from the certificate's signature algorithm
	 * into its human-readable Name as specified by RFC 3279.
	 * @return the human-readable Name of the algorithm the certificate was signed
	 * 		   with or the ObjectIdentifier as String, if the algorithm is unknown.
	 */
	public String getSignatureAlgorithmName()
	{
		String id = m_bcCertificate.getSignatureAlgorithm().getObjectId().getId();
		
		if(id.equals(IDENTIFIER_DSA_WITH_SHA1))
		{
			return DSA_WITH_SHA1;
		}
		if(id.equals(IDENTIFIER_SHA1_WITH_RSA_ENCRYPTION))
		{
			return SHA1_WITH_RSA_ENCRYPTION;
		}
		if(id.equals(IDENTIFIER_MD5_WITH_RSA_ENCRYPTION))
		{
			return MD5_WITH_RSA_ENCRYPTION;
		}
		if(id.equals(IDENTIFIER_MD2_WITH_RSA_ENCRYPTION))
		{
			return MD2_WITH_RSA_ENCRYPTION;
		}
		if(id.equals(IDENTIFIER_ECDSA_WITH_SHA1))
		{
			return ECDSA_WITH_SHA1;
		}
		
		return id;		
	}
	
	public BigInteger getSerialNumber()
	{
		return m_bcCertificate.getSerialNumber().getPositiveValue();
	}

	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}

	/**
	 * Returns the distinguished name.
	 * @return the distinguished name.
	 */
	public X509DistinguishedName getSubject()
	{
		return m_subject;
	}

	/**
	 * getAnyEmailAddress: checks all reasonably likely places in the certificate for an email address
	 *
	 * @return String: the first email address found, or null if none exists
	 */
	public String getAnyEmailAddress()
	{
		try
		{
			String emailAddress;
			X509DistinguishedName disName = this.getSubject();
			//first, try E
			emailAddress = disName.getE_EmailAddress();
			if (emailAddress != null)
			{
				return emailAddress;
			}
			//then, EmailAddress
			emailAddress = disName.getEmailAddress();
			if (emailAddress != null)
			{
				return emailAddress;
			}
			//check SubjectAlternativeName
			X509Extensions allExtensions = this.getExtensions();
			Vector allExtensionsVector = allExtensions.getExtensions();
			AbstractX509Extension curExt;
			X509SubjectAlternativeName name;
			for (int idx = 0; idx < allExtensionsVector.size(); idx++)
			{
				curExt = (AbstractX509Extension) allExtensionsVector.elementAt(idx);
				if (curExt instanceof X509SubjectAlternativeName)
				{
					name = (X509SubjectAlternativeName) curExt;
					Vector tags = name.getTags();
					Integer tag;
					for (int i = 0; i < tags.size(); i++)
					{

						tag = (Integer) tags.elementAt(i);
						if (tag.equals(AbstractX509AlternativeName.TAG_EMAIL))
						{
							emailAddress = (String) name.getValues().elementAt(tag.intValue());
							if (emailAddress != null)
							{
								return emailAddress;
							}
						}
					}
				}
			}
		} catch (Exception e)
		{
			//lots of class casts etc, better check for exceptions
			return null; //success is not guaranteed for this function anyway
		}
		//give up and return null
		return null;
	}

	/**
	 * Returns a reference to this certificate.
	 * @return a reference to this certificate
	 */
	public JAPCertificate getX509Certificate()
	{
		return this;
	}

	/**
	 * Gets a human readable SHA1 fingerprint for this certificate. This fingerprint may be
	 * compared by a user with an other certificate's fingerprint to proof their equality.
	 * @return a human readable SHA1 fingerprint for this certificate
	 */
	public String getSHA1Fingerprint()
	{
		return m_sha1Fingerprint;
	}

	/**
	 * Gets a human readable MD5 fingerprint for this certificate. This fingerprint may be
	 * compared by a user with an other certificate's fingerprint to proof their equality.
	 * @return a human readable MD5 fingerprint for this certificate
	 */
	public String getMD5Fingerprint()
	{
		return m_md5Fingerprint;
	}

	/**
	 * Converts the certificate to a byte array.
	 * @throws IOException
	 * @return the certificate as a byte array
	 */
	public byte[] toByteArray()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(out).writeObject(this.m_bcCertificate);
		}
		catch (IOException a_e)
		{
			// I don`t think this is possible
		}

		return out.toByteArray();
	}

	/**
	 * Converts the certificate to a byte array.
	 * @param a_Base64Encoded if the certificate is converted to a Base64 encoded form.
	 * @throws IOException
	 * @return the certificate as a byte array
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
	 * Writes this certificate to an output stream.
	 * @param a_ostream a OutputStream
	 * @exception IOException if the certificate could not be written to the output stream
	 */
	public void store(OutputStream a_ostream) throws IOException
	{
		DEROutputStream derOutputStream = new DEROutputStream(a_ostream);
		derOutputStream.writeObject(this.m_bcCertificate);
	}

	/**
	 * Writes this certificate to an output stream.
	 * @param a_ostream a OutputStream
	 * @param a_bBase64Encoded if the certificate should be written Base64 encoded.
	 * @exception IOException if the certificate could not be written to the output stream
	 */
	public void store(OutputStream a_ostream, boolean a_bBase64Encoded) throws IOException
	{
		a_ostream.write(toByteArray(a_bBase64Encoded));
	}

	/**
	 * Returns the validity period of this certificate.
	 * @return the validity period of this certificate
	 */
	public Validity getValidity()
	{
		return m_validity;
	}

	/**
	 * Checks if a given Certificate could be directly verified against a set of other certificates.
	 * @param a_verifyingCertificates A Vector of JAPCertificates to verify this JAPCertificate.
	 * @return True, if this certificate could be verified.
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(Vector a_verifyingCertificates)
	{
		return verify(a_verifyingCertificates.elements());
	}

	/**
	 * Checks if a given Certificate could be directly verified against a set of other certificates.
	 * @param a_verifyingCertificates A Hashtable of JAPCertificates to verify this JAPCertificate.
	 * @return True, if this certificate could be verified.
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(Hashtable a_verifyingCertificates)
	{
		return verify(a_verifyingCertificates.elements());
	}

	/**
	 * Checks if a given Certificate could be directly verified against a set of other certificates.
	 * @param a_verifyingCertificates An Enumeration of JAPCertificates to verify this JAPCertificate.
	 * @return True, if this certificate could be verified.
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(Enumeration a_verifyingCertificates)
	{
		if (a_verifyingCertificates == null)
		{
			return false;
		}

		synchronized(a_verifyingCertificates)
		{
			while (a_verifyingCertificates.hasMoreElements())
			{
				JAPCertificate currentCertificate =
					(JAPCertificate) (a_verifyingCertificates.nextElement());

				if (verify(currentCertificate))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifies the certificate using an other X509 certificate.
	 *
	 * @param a_certificate an X509 certificate
	 * @return true if it could be verified; false otherwise
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(JAPCertificate a_certificate)
	{
		if (a_certificate == null)
		{
			return false;
		}
		return (verify(a_certificate.getPublicKey()));
	}

	/** Verifies the certificate by using the public key.
	 * @param a_publicKey given public key
	 * @return true if it could be verified; false otherwise
	 */
	public synchronized boolean verify(IMyPublicKey a_publicKey)
	{
		if (a_publicKey == null)
		{
			return false;
		}
		
		AlgorithmIdentifier aid1 = a_publicKey.getSignatureAlgorithm().getIdentifier();
		AlgorithmIdentifier aid2 = this.m_bcCertificate.getSignatureAlgorithm();
		if(aid1.equals(aid2))
		{
			try
			{
				ByteArrayOutputStream bArrOStream = new ByteArrayOutputStream();
				(new DEROutputStream(bArrOStream)).writeObject(m_bcCertificate.getTBSCertificate());
	
				return ByteSignature.verify(bArrOStream.toByteArray(),
											m_bcCertificate.getSignature().getBytes(), a_publicKey);
			}
			catch (IOException a_e)
			{
				// should not happen
			}
		}

		return false;
	}

	/**
	 * Creates a duplicate of this certificate that is signed with a the PKCS12 certificate.
	 * A certificate can have only one signature.
	 * @param a_signerCertificate the PKCS12 certificate of the signer
	 * @return a duplicate of this certificate that is signed with a the PKCS12 certificate
	 */
	public JAPCertificate sign(PKCS12 a_signerCertificate)
	{
		JAPCertificate certificate;
		X509CertificateStructure x509cert;
		X509CertificateGenerator certgen =
			new X509CertificateGenerator(m_bcCertificate.getTBSCertificate());
		x509cert = certgen.sign(a_signerCertificate);
		certificate = getInstance(x509cert);
		return certificate;
	}

	/**
	 * Creates a duplicate of this certificate that is signed with a the PKCS12 certificate.
	 * A certificate can have only one signature.
	 * @param a_signerCertificate the PKCS12 certificate of the signer
	 * @param a_validity the validity period of this certificate
	 * @param a_extensions some X509 extensions (may be null)
	 * @param a_serialNumber the serial number for this certificate (may be null)
	 * @return a duplicate of this certificate that is signed with a the PKCS12 certificate
	 */
	public JAPCertificate sign(PKCS12 a_signerCertificate, Validity a_validity,
							   X509Extensions a_extensions, BigInteger a_serialNumber)
	{
		return JAPCertificate.getInstance(
				  new X509DistinguishedName(m_bcCertificate.getSubject()),
				  a_signerCertificate.getSubject(), a_signerCertificate.getPrivateKey(),
				  getPublicKey(), a_validity, a_extensions, a_serialNumber);
	}

	/**
	 * Creates an X509 certificate.
	 * @param a_ownerAlias The owner of the certificate.
	 * @param a_issuer The issuer and signer of this X509 certificate.
	 * @param a_privateKey a private key
	 * @param a_publicKey a public key
	 * @param a_validity the validity period of this certificate
	 * @param a_extensions some X509 extensions (may be null)
	 * @param a_serialNumber the serial number for this certificate (may be null)
	 * @return an X509 certificate
	 */
	public static JAPCertificate getInstance(X509DistinguishedName a_ownerAlias,
											  X509DistinguishedName a_issuer,
											  IMyPrivateKey a_privateKey,
											  IMyPublicKey a_publicKey,
											  Validity a_validity,
											  X509Extensions a_extensions,
											  BigInteger a_serialNumber)
	{
		X509CertificateGenerator v3CertGen;

		v3CertGen = new X509CertificateGenerator(a_ownerAlias, a_validity.getValidFrom(),
												 a_validity.getValidTo(), a_publicKey,
												 a_extensions, a_serialNumber);

		return getInstance(v3CertGen.sign(a_issuer.getX509Name(), a_privateKey));
	}

	/**
	 * Creates XML element of certificate consisting of:
	 * <X509Certificate>
	 *  Base64 encocded cert
	 * </X509Certificate>
	 *
	 * @param a_doc The XML document, which is the environment for the created XML element.
	 *
	 * @return Certificate as XML element.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemX509Cert = a_doc.createElement(XML_ELEMENT_NAME);
		elemX509Cert.setAttribute("xml:space", "preserve");
		XMLUtil.setValue(elemX509Cert, Base64.encode(toByteArray(), true));
		return elemX509Cert;
	}

	DEREncodable getBouncyCastleCertificate()
	{
		return m_bcCertificate;
	}

	SubjectPublicKeyInfo getBouncyCastleSubjectPublicKeyInfo()
	{
		return m_bcCertificate.getSubjectPublicKeyInfo();
	}

	/**
	 * Converts a DER or BER encoded byte array into an ASN1 sequence. The array may additionally
	 * be Base64 encoded.
	 * @param a_bytes an array of bytes
	 * @param a_xmlElementName the name of the containing XML element or null if the object is not
	 * expected to be in an XML element
	 * @return the byte array as ASN1Sequence
	 */
	static ASN1Sequence toASN1Sequence(byte[] a_bytes, String a_xmlElementName)
	{
		ByteArrayInputStream bin = null;
		if (a_bytes == null || a_bytes.length == 0)
		{
			return null;
		}

		try
		{
			if (a_bytes[0] != (ASN1InputStream.SEQUENCE | ASN1InputStream.CONSTRUCTED))
			{
				/*
				 * Probably a Base64 encoded certificate; might be given in a single line, use tokenizer to
				 * correct this (transform whitespaces to newlines).
				 */
				String certString = new String(a_bytes);
				StringTokenizer tokenizer = new StringTokenizer(certString);
				StringBuffer sbuf = new StringBuffer();
				String line;
				int tagIndex;
				boolean endTagFound = false;

				if (a_xmlElementName != null)
				{
					if (a_xmlElementName.trim().length() == 0 ||
						new StringTokenizer(a_xmlElementName).countTokens() > 1)
					{
						a_xmlElementName = null;
					}
				}

				beginLoop:
				while (tokenizer.hasMoreTokens())
				{
					line = tokenizer.nextToken();
					if (line.startsWith(Base64.BEGIN_TAG.trim()))
					{
						do
						{
							if (line.endsWith(Base64.TAG_END_SEQUENCE))
							{
								break beginLoop;
							}
						}
						while (tokenizer.hasMoreTokens() && (line = tokenizer.nextToken()) != null);
					}
					else if (a_xmlElementName != null &&
							 (tagIndex = line.indexOf("<" + a_xmlElementName)) >= 0)
					{
						int endTagIndex = certString.indexOf(">");
						if (tagIndex >= endTagIndex)
						{
							continue;
						}
						tagIndex = endTagIndex + 1;
						endTagIndex = certString.indexOf("</" + a_xmlElementName + ">");
						if (endTagIndex >= 0)
						{
							// this is a certificate enclosed in an XML element
							endTagFound = true;
							sbuf.append(certString.substring(certString.indexOf(">") + 1, endTagIndex));
							break;
						}
					}
				}

				if (!endTagFound)
				{
					if (!tokenizer.hasMoreTokens())
					{
						throw new Exception();
					}

					endLoop:while (tokenizer.hasMoreTokens())
					{
						line = tokenizer.nextToken();
						if (line.startsWith(Base64.END_TAG.trim()))
						{
							do
							{
								if (line.endsWith(Base64.TAG_END_SEQUENCE))
								{
									endTagFound = true;
									break endLoop;
								}
							}
							while (tokenizer.hasMoreTokens() && (line = tokenizer.nextToken()) != null);
						}
						sbuf.append(line);
					}
				}

				if (!endTagFound)
				{
					throw new Exception();
				}

				bin = new ByteArrayInputStream(Base64.decode(sbuf.toString()));
			}

			if (bin == null && a_bytes[1] == 0x80)
			{
				//a BER encoded certificate
				ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(a_bytes));
				return (ASN1Sequence) in.readObject();
			}
			else
			{
				if (bin == null)
				{
					bin = new ByteArrayInputStream(a_bytes);
				}
				return (ASN1Sequence) (new ASN1InputStream(bin)).readObject();
			}
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte array is no valid ASN1 sequence data!");
		}
	}

	/**
	 * Creates a human readable fingerprint for this certificate. This fingerprint may be
	 * compared by a user with an other certificate's fingerprint to proof their equality.
	 * @param a_digestGenerator a digest generator
	 * @param a_data the data to be 'fingerprinted'
	 * @return the fingerprint
	 */
	protected static String createFingerprint(GeneralDigest a_digestGenerator, byte[] a_data)
	{
		byte[] digestData;

		digestData = new byte[a_digestGenerator.getDigestSize()];
		a_digestGenerator.update(a_data, 0, a_data.length);
		a_digestGenerator.doFinal(digestData, 0);
		
		return ByteSignature.toHexString(digestData);
	}

	/**
	 * Gets a dummy private key for creating X509 certificates from trusted public keys.
	 * @return a dummy private key for creating X509 certificates from trusted public keys
	 */
	private static IMyPrivateKey getDummyPrivateKey()
	{
		SecureRandom random;

		if (ms_dummyPrivateKey == null)
		{
			random = new SecureRandom();
			random.setSeed(58921787);
			ms_dummyPrivateKey = DSAKeyPair.getInstance(random, 256, 100).getPrivate();
		}

		return ms_dummyPrivateKey;
	}

	/**
	 * This class is used to create, duplicate and sign X509 certificates.
	 */
	private static final class X509CertificateGenerator extends V3TBSCertificateGenerator
	{
		/**
		 * Prepares a new X509 certificate from an owner alias and a public key.
		 * The SubjectKeyIdentifier extension is added and calculated using a SHA1 hash over
		 * the BIT STRING from SubjectPublicKeyInfo as defined in RFC2459.
		 * @param a_ownerAlias the certificate`s common name; an alias to the public key`s owner
		 * @param a_validFrom the date from which the certificate is valid
		 * @param a_validTo the date on which the certificate looses validity
		 * @param a_publicKey the public key that is enclosed in the certificate
		 * @param a_extensions some X509 extensions (may be null)
		 * @param a_serialNumber the serial number for this certificate (may be null)
		 * @throws IOException if the public key`s encoding is invalid
		 */
		public X509CertificateGenerator(X509DistinguishedName a_ownerAlias,
										Date a_validFrom, Date a_validTo,
										IMyPublicKey a_publicKey,
										X509Extensions a_extensions,
										BigInteger a_serialNumber)
		{
			setStartDate(new DERUTCTime(a_validFrom));
			setEndDate(new DERUTCTime(a_validTo));
			if (a_serialNumber == null)
			{
				setSerialNumber(new DERInteger(1));
			}
			else
			{
				setSerialNumber(new DERInteger(a_serialNumber));
			}
			setSubject(a_ownerAlias.getX509Name());
			setSubjectPublicKeyInfo(a_publicKey.getAsSubjectPublicKeyInfo());

			if (a_extensions != null && a_extensions.getSize() > 0)
			{
				setExtensions(a_extensions.getBCX509Extensions());
			}
			else
			{
				setExtensions(new X509Extensions(new Vector()).getBCX509Extensions());
			}
		}

		/**
		 * Prepares a new X509 certificate from an existing X509 certificate.
		 * @param tbs X509CertificateStructure
		 */
		public X509CertificateGenerator(TBSCertificateStructure tbs)
		{
			setStartDate(tbs.getStartDate());
			setEndDate(tbs.getEndDate());
			setSerialNumber(tbs.getSerialNumber());
			setSubject(tbs.getSubject());
			setSubjectPublicKeyInfo(tbs.getSubjectPublicKeyInfo());
			setExtensions(tbs.getExtensions());
			setIssuer(tbs.getIssuer());
			setSignature(tbs.getSignature());
		}

		public X509CertificateStructure sign(PKCS12 a_pkcs12Certificate)
		{
			return sign(a_pkcs12Certificate.getX509Certificate().m_bcCertificate.getSubject(),
						a_pkcs12Certificate.getPrivateKey());
		}

		public X509CertificateStructure sign(X509Name a_issuer, IMyPrivateKey a_privateKey)
		{
			try
			{
				TBSCertificateStructure tbsCert;
				DEREncodableVector seqv;
				ByteArrayOutputStream bOut;
				byte[] signature;

				setIssuer(a_issuer);
				setSignature(a_privateKey.getSignatureAlgorithm().getIdentifier());

				/* generate signature */
				bOut = new ByteArrayOutputStream();
				tbsCert = generateTBSCertificate();
				(new DEROutputStream(bOut)).writeObject(tbsCert);
				signature = ByteSignature.sign(bOut.toByteArray(), a_privateKey);

				/* construct certificate */
				seqv = new ASN1EncodableVector();
				seqv.add(tbsCert);
				seqv.add(a_privateKey.getSignatureAlgorithm().getIdentifier());
				seqv.add(new DERBitString(signature));

				return new X509CertificateStructure(new DERSequence(seqv));
			}

			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, t);
				return null;
			}
		}
	}

	private static final class X509CertificateInstantiator implements IResourceInstantiator
	{
		private String m_strIgnoreCertMark;

		public X509CertificateInstantiator(String a_strIgnoreCertMark)
		{
			m_strIgnoreCertMark = a_strIgnoreCertMark;
		}

		public Object getInstance(File a_file, File a_topDirectory) throws IOException
		{
			if (a_file == null || (m_strIgnoreCertMark != null && a_file.getName().endsWith(m_strIgnoreCertMark)))
			{
				return null;
			}
			return JAPCertificate.getInstance(new FileInputStream(a_file));
		}

		public Object getInstance(ZipEntry a_entry, ZipFile a_file) throws IOException
		{
			if (a_file == null || (m_strIgnoreCertMark != null && a_file.getName().endsWith(m_strIgnoreCertMark)))
			{
				return null;
			}

			return JAPCertificate.getInstance(a_file.getInputStream(a_entry));
		}
		
		public Object getInstance(InputStream a_inputStream)
		{
			return JAPCertificate.getInstance(a_inputStream);
		}
	}
	
	/**
	 * Returns <code>true</code> if this cert is self-signed.
	 * @return if this cert is self-signed.
	 */
	public boolean isSelfSigned()
	{
		return this.verify(this.getPublicKey());
	}
	
	public boolean isRevoked()
	{
		return RevokedCertifcateStore.getInstance().isCertificateRevoked(this);
	}
}
