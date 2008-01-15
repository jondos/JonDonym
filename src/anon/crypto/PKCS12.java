/*
 Copyright (c) 2000, The JAP-Team
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


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.crypto;

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Vector;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BEROutputStream;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import org.bouncycastle.asn1.pkcs.CertBag;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.EncryptedData;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.MacData;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.Pfx;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.SafeBag;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;

import anon.util.IMiscPasswordReader;
import anon.util.SingleStringPasswordReader;
import org.bouncycastle.asn1.DEREncodableVector;
import anon.util.ResourceLoader;
import anon.util.Base64;

/**
 * This class creates and handles PKCS12 certificates, that include a private key,
 * a public key and an X509 certificate.
 */
public final class PKCS12 implements PKCSObjectIdentifiers, X509ObjectIdentifiers, ICertificate
{
	public static final String FILE_EXTENSION = ".pfx";

	private static final int SALT_SIZE = 20;
	private static final int MIN_ITERATIONS = 100;

	private static final String BASE64_TAG = "PKCS12";

	public static final String XML_ELEMENT_NAME = "X509PKCS12";

	//
	// SHA-1 and 3-key-triple DES.
	//
	private static final String KEY_ALGORITHM = "1.2.840.113549.1.12.1.3";

	//
	// SHA-1 and 40 bit RC2.
	//
	private static final String CERT_ALGORITHM = "1.2.840.113549.1.12.1.6";

	//
	// SHA-1 HMAC
	//
	// private static final String MAC_ALGORITHM = "1.3.14.3.2.26";

	private SecureRandom random = new SecureRandom();
	private AsymmetricCryptoKeyPair m_keyPair;
	private JAPCertificate m_x509certificate;

	/**
	 * Creates a new PKCS12 certificate.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_keyPair a key pair with a private and a public key
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 */
	public PKCS12(X509DistinguishedName a_ownerAlias, AsymmetricCryptoKeyPair a_keyPair,
				  Validity a_validity)
	{
		this(a_ownerAlias, a_keyPair, a_validity, null);
	}

	/**
	 * Creates a new PKCS12 certificate.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_keyPair a key pair with a private and a public key
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @param a_extensions optional X509 extensions; may be null
	 */
	public PKCS12(X509DistinguishedName a_ownerAlias, AsymmetricCryptoKeyPair a_keyPair,
				  Validity a_validity, X509Extensions a_extensions)
	{
		m_keyPair = a_keyPair;
		m_x509certificate =
			JAPCertificate.getInstance(a_ownerAlias, a_keyPair, a_validity, a_extensions);
	}

	/**
	 * Creates a new PKCS12 certificate. This constructor is not public as is is possible
	 * that the X509 certificate owner alias and the PKCS12 owner alias differ, what they
	 * should not!
	 * @param a_keyPair a key pair with a private and a public key
	 * @param a_X509certificate an X509 certificate
	 */
	private PKCS12(AsymmetricCryptoKeyPair a_keyPair, JAPCertificate a_X509certificate)
	{
		m_keyPair = a_keyPair;
		m_x509certificate = a_X509certificate;
	}


	/**
	 * Loads a PKCS12 certificate from a byte array. The type of the encryption
	 * algorithm is recognized dynamically.
	 * @param a_bytes a byte array
	 * @param a_password a password (may be null)
	 * @see anon.crypto.IMyPrivateKey
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.AsymmetricKeyPair
	 * @return a PKCS12 certificate or null if an error occured
	 */
	public static PKCS12 getInstance(byte[] a_bytes, char[] a_password)
	{
		return getInstance(a_bytes, new SingleStringPasswordReader(a_password));
	}

	public static PKCS12 getInstance(byte[] a_bytes, String a_password)
	{
		return getInstance(a_bytes, new SingleStringPasswordReader(a_password));
	}

	public static PKCS12 getInstance(String privCertString, String password)
	{
		return getInstance(privCertString.getBytes(),password.toCharArray());
	}

	/**
	 * Loads a PKCS12 certificate from a byte array. The type of the encryption
	 * algorithm is recognized dynamically.
	 * @param a_bytes a byte array
	 * @param a_password a password (may be null)
	 * @see anon.crypto.IMyPrivateKey
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.AsymmetricKeyPair
	 * @return a PKCS12 certificate or null if an error occured
	 */
	public static PKCS12 getInstance(byte[] a_bytes, IMiscPasswordReader a_passwordReader)
	{
		if (a_bytes == null)
		{
			return null;
		}

		return getInstance(new ByteArrayInputStream(a_bytes), a_passwordReader);
	}


	/**
	 * Loads a PKCS12 certificate from an input stream. The type of the encryption
	 * algorithm is recognized dynamically.
	 * @param a_stream InputStream
	 * @param password a password (may be null)
	 * @see anon.crypto.IMyPrivateKey
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.AsymmetricKeyPair
	 * @return PKCS12
	 */
	public static PKCS12 getInstance(InputStream a_stream, char[] password)
	{
		return getInstance(a_stream, new SingleStringPasswordReader(password));
	}

	public static PKCS12 getInstance(InputStream a_stream, String password)
	{
		return getInstance(a_stream,new SingleStringPasswordReader(password));
	}

	/**
	 * Loads a PKCS12 certificate from an input stream. The type of the encryption
	 * algorithm is recognized dynamically.
	 * @param a_stream InputStream
	 * @param password a password (may be null)
	 * @see anon.crypto.IMyPrivateKey
	 * @see anon.util.ClassUtil#loadClasses()
	 * @see anon.crypto.AsymmetricKeyPair
	 * @return PKCS12
	 */
	public static PKCS12 getInstance(InputStream a_stream, IMiscPasswordReader a_passwordReader)
	{
		boolean bCorrectPassword = false;
		char[] password = new char[0];
		ASN1InputStream dIn;

		if (a_passwordReader == null)
		{
			a_passwordReader = new SingleStringPasswordReader(new char[0]);
		}

		try
		{
			String alias = null;
			IMyPrivateKey privKey = null;
			X509CertificateStructure x509cert = null;

			//BERInputStream is = new BERInputStream(stream);
			ASN1Sequence asn1seq = JAPCertificate.toASN1Sequence(
				 ResourceLoader.getStreamAsBytes(a_stream), XML_ELEMENT_NAME);
			if (asn1seq == null)
			{
				// this does not seem to be a PKCS12 certificate
				return null;
			}
			ContentInfo contentInfo = new Pfx(asn1seq).getAuthSafe();
			/** @todo Check MAC */
			// Look at JDKPKCS12KeyStore.engineLoad (starting with bag.getMacData())

			if (!contentInfo.getContentType().equals(PKCSObjectIdentifiers.data))
			{
				return null;
			}
			ASN1InputStream is =
				new ASN1InputStream(new ByteArrayInputStream( ( (ASN1OctetString) contentInfo.getContent()).getOctets()));
			ContentInfo[] cinfos = (new AuthenticatedSafe( (ASN1Sequence) is.readObject())).getContentInfo();

			for (int i = 0; i < cinfos.length; i++)
			{
				ASN1Sequence cseq;
				if (cinfos[i].getContentType().equals(PKCSObjectIdentifiers.data))
				{
					ASN1InputStream dis = new ASN1InputStream(new ByteArrayInputStream(
						( (DEROctetString) cinfos[i].getContent())
						.getOctets()));
					cseq = (ASN1Sequence) dis.readObject();
				}
				else if (cinfos[i].getContentType().equals(PKCSObjectIdentifiers.encryptedData))
				{
					EncryptedData ed = new EncryptedData( (ASN1Sequence) cinfos[i].getContent());
					String algId = ed.getEncryptionAlgorithm().getObjectId().getId();
					MyCipher cipher = getCipher(algId);
					if (cipher == null)
					{
						return null;
					}
					PKCS12PBEParams pbeParams = new PKCS12PBEParams( (ASN1Sequence) ed.getEncryptionAlgorithm()
						.getParameters());
					ASN1InputStream bis = null;

					do
					{
						try
						{
							    bis = new ASN1InputStream(
								new ByteArrayInputStream(
								codeData(false, ed.getContent().getOctets(), pbeParams,
										 password, cipher.cipher, cipher.keysize)));
							cseq = (ASN1Sequence) bis.readObject();
							bCorrectPassword = true; // 'password' contains the correct password
						}
						catch (Throwable a_e)
						{
							cseq = null;
							bis.close();
							if (password.length == 0)
							{
								password = new char[]{0};
							    continue;
							}
							// throws a NullPointerException if the user canceled input
							password = a_passwordReader.readPassword(null).toCharArray();
						}
					} while (!bCorrectPassword);
				}
				else
				{
					continue;
				}

				for (int j = 0; j < cseq.size(); j++)
				{
					SafeBag sb = new SafeBag( (ASN1Sequence) cseq.getObjectAt(j));

					if (sb.getBagId().equals(PKCSObjectIdentifiers.certBag))
					{
						    dIn = new ASN1InputStream(
							new ByteArrayInputStream( ( (DEROctetString)new CertBag( (ASN1Sequence)
							sb.getBagValue()).getCertValue()).getOctets()));
						ASN1Sequence xseq = (ASN1Sequence) dIn.readObject();

						if (xseq.size() > 1
							&& xseq.getObjectAt(1) instanceof DERObjectIdentifier
							&& xseq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
						{
							x509cert = X509CertificateStructure.getInstance(
								new SignedData(
								ASN1Sequence.getInstance(
								(ASN1TaggedObject) xseq.getObjectAt(1),
								true))
								.getCertificates()
								.getObjectAt(0));
						}
						else
						{
							x509cert = X509CertificateStructure.getInstance(xseq);
						}
					}
					else if (sb.getBagId().equals(PKCSObjectIdentifiers.pkcs8ShroudedKeyBag))
					{

						EncryptedPrivateKeyInfo ePrivKey = new EncryptedPrivateKeyInfo( (ASN1Sequence) sb.
							getBagValue());
						MyCipher cipher = getCipher(ePrivKey.getEncryptionAlgorithm().getObjectId().getId());
						if (cipher == null)
						{
							return null;
						}
						PKCS12PBEParams pbeParams = new PKCS12PBEParams( (ASN1Sequence) ePrivKey
							.getEncryptionAlgorithm().getParameters());

						PrivateKeyInfo privKeyInfo;
						do
						{
							dIn = null;
							try
							{
								    dIn = new ASN1InputStream(new ByteArrayInputStream(
									codeData(false, ePrivKey.getEncryptedData(),
											 pbeParams, password, cipher.cipher, cipher.keysize)));
								privKeyInfo = new PrivateKeyInfo( (ASN1Sequence) dIn.readObject());
								bCorrectPassword = true; // 'password' contains the correct password
							}
							catch (Throwable a_e)
							{
								privKeyInfo = null;
								dIn.close();
								if (password.length == 0)
								{
									password = new char[] {0};
									continue;
								}
								// throws a NullPointerException if the user canceled input
								do {
									password = a_passwordReader.readPassword(null).toCharArray();
								}
								while (password.length == 0 ||
									   (password.length == 1 && password[0] == '0'));
							}
						}
						while (!bCorrectPassword);

						privKey = (new AsymmetricCryptoKeyPair(privKeyInfo)).getPrivate();

					}

					if (alias == null && sb.getBagAttributes() != null)
					{
						Enumeration e = sb.getBagAttributes().getObjects();
						while (e.hasMoreElements())
						{
							ASN1Sequence ba = (ASN1Sequence) e.nextElement();
							DERObjectIdentifier oid = (DERObjectIdentifier) ba.getObjectAt(0);
							DERObject att = (DERObject) ( (ASN1Set) ba.getObjectAt(1)).getObjectAt(0);
							if (oid.equals(pkcs_9_at_friendlyName))
							{
								alias = ( (DERBMPString) att).getString();
							}
						}
					}
				}
			}

			if (x509cert != null)
			{
				/*
				 * 'alias' is ignored as it is not important and contained in the public
				 * certificate as common name. Some PKCS12 certificates created by OpenSSL
				 * even do not contain an alias.
				 */
				return new PKCS12(new AsymmetricCryptoKeyPair(privKey),
								  JAPCertificate.getInstance(x509cert));
			}
		}
		catch (Throwable t)
		{
			//t.printStackTrace();
		}
		return null;
	}


	/**
	 * Converts the certificate to a byte array.
	 * @return the certificate as a byte array
	 */
	public byte[] toByteArray()
	{
		return toByteArray("".toCharArray());
	}

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

	public byte[] toByteArray(char[] a_password, boolean a_Base64Encoded)
	{
		if (a_Base64Encoded)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try
			{
				out.write(Base64.createBeginTag(BASE64_TAG).getBytes());
				out.write(Base64.encode(toByteArray(a_password), true).getBytes());
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
			return toByteArray(a_password);
		}
	}


	/**
	 * Converts the certificate to a (optionally encrypted) byte array.
	 * @param a_password a password
	 * @throws IOException
	 * @return the certificate as a byte array
	 */
	public byte[] toByteArray(char[] a_password)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			store(out, a_password);
			out.close();
		}
		catch (IOException a_e)
		{
			// I don`t think this is possible
		}

		return out.toByteArray();
	}


	/**
	 * Writes the certificate to an output stream.
	 * @param stream an output stream
	 * @param password a password; may be null
	 * @throws IOException if an I/O error occurs
	 */
	public void store(OutputStream stream, char[] password) throws IOException
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut;

		if (password == null)
		{
			password = new char[0];
		}

		//
		// handle the key
		//
		BERConstructedOctetString keyString;
		{

			byte[] kSalt = new byte[SALT_SIZE];
			random.nextBytes(kSalt);
			PKCS12PBEParams kParams = new PKCS12PBEParams(kSalt, MIN_ITERATIONS);
			byte[] kBytes = codeData(true,
									 m_keyPair.getPrivate().getEncoded(),
									 kParams,
									 password,
									 new DESedeEngine(),
									 192);
			AlgorithmIdentifier kAlgId = new AlgorithmIdentifier(
				new DERObjectIdentifier(KEY_ALGORITHM),
				kParams.getDERObject());
			EncryptedPrivateKeyInfo kInfo = new EncryptedPrivateKeyInfo(kAlgId, kBytes);

			//
			// set a default friendly name (from the key id) and local id
			//
			DERSequence[] seqs = new DERSequence[2];
			DEREncodableVector kSeq = new DEREncodableVector();
			kSeq.add(pkcs_9_at_localKeyId);
			kSeq.add(new DERSet(createSubjectKeyId()));
			seqs[0] = new DERSequence(kSeq);
			//kName.addObject(new DERSequence(kSeq));
			kSeq = new DEREncodableVector();
			kSeq.add(pkcs_9_at_friendlyName);
			kSeq.add(new DERSet(new DERBMPString(getAlias())));
			seqs[1] = new DERSequence(kSeq);
			DERSet kName = new DERSet(seqs);
			//kName.addObject(new DERSequence(kSeq));

			keyString = new BERConstructedOctetString(new DERSequence(
				new SafeBag(pkcs8ShroudedKeyBag, kInfo.getDERObject(), kName)));
		}

		//
		// certficate processing
		//
		EncryptedData cInfo;
		{
			byte[] cSalt = new byte[SALT_SIZE];
			random.nextBytes(cSalt);
			PKCS12PBEParams cParams = new PKCS12PBEParams(cSalt, MIN_ITERATIONS);
			AlgorithmIdentifier cAlgId = new AlgorithmIdentifier(new DERObjectIdentifier(CERT_ALGORITHM),
				cParams);
			CertBag cBag = new CertBag(x509certType, new DEROctetString(
						 m_x509certificate.getBouncyCastleCertificate()));
			DEREncodableVector fSeq = new DEREncodableVector();
			fSeq.add(pkcs_9_at_localKeyId);
			fSeq.add(new DERSet(createSubjectKeyId()));
			DERSequence[] seqs = new DERSequence[2];
			seqs[0] = new DERSequence(fSeq);


			fSeq = new DEREncodableVector();
			fSeq.add(pkcs_9_at_friendlyName);
			fSeq.add(new DERSet(new DERBMPString(getAlias())));
			seqs[1] = new DERSequence(fSeq);
			DERSet fName = new DERSet(seqs);

			SafeBag sBag = new SafeBag(certBag, cBag.getDERObject(), fName);

			bOut.reset();
			dOut = new DEROutputStream(bOut);
			dOut.writeObject(new DERSequence(sBag));
			dOut.close();

			byte[] certBytes = codeData(true, bOut.toByteArray(), cParams, password, new RC2Engine(), 40);
			cInfo = new EncryptedData(data, cAlgId, new BERConstructedOctetString(certBytes));
		}

		ContentInfo[] c = new ContentInfo[2];
		c[0] = new ContentInfo(data, keyString);
		c[1] = new ContentInfo(encryptedData, cInfo);

		/*
		 bOut.reset();
		 AuthenticatedSafe auth = new AuthenticatedSafe(c);
		 BEROutputStream berOut = new BEROutputStream(bOut);
		 berOut.writeObject(auth);
		 byte[] pkg = bOut.toByteArray();

		 ContentInfo mainInfo =
		   new ContentInfo(data, new BERConstructedOctetString(pkg));
		 */
		ContentInfo mainInfo = new ContentInfo(data, new BERConstructedOctetString(new AuthenticatedSafe(c)));
		//
		// create the mac
		//
		byte[] mSalt = new byte[SALT_SIZE];
		int itCount = MIN_ITERATIONS;
		random.nextBytes(mSalt);
		byte[] contentData = ( (DEROctetString) mainInfo.getContent()).getOctets();
		MacData mData = null;

		try
		{
			Mac certMac = new HMac(new SHA1Digest());
			CipherParameters cParam = makePBEMacParameters(password, new PKCS12PBEParams(mSalt, itCount), 160);
			certMac.init(cParam);
			certMac.update(contentData, 0, contentData.length);
			byte[] my_res = new byte[certMac.getMacSize()];
			certMac.doFinal(my_res, 0);
			AlgorithmIdentifier my_algId = new AlgorithmIdentifier(id_SHA1, null);
			DigestInfo my_dInfo = new DigestInfo(my_algId, my_res);

			mData = new MacData(my_dInfo, mSalt, itCount);
		}
		catch (Exception e)
		{
			throw new IOException("error constructing MAC: " + e.toString());
		}

		//
		// output the Pfx
		//
		Pfx pfx = new Pfx(mainInfo, mData);
		BEROutputStream berOut = new BEROutputStream(stream);
		berOut.writeObject(pfx);
	}

	public String getAlias()
	{
		Vector aliases = new Vector();
		X509DistinguishedName subject = getSubject();

		aliases.addElement(subject.getCommonName());
		aliases.addElement(subject.getEmailAddress());
		aliases.addElement(subject.getOrganisation());

		for (int i = 0; i < aliases.size(); i++)
		{
			if (aliases.elementAt(i) != null &&
				( (String) aliases.elementAt(i)).trim().length() != 0)
			{
				return (String) aliases.elementAt(i);
			}

		}
		return "alias unknown";
	}

	public X509Extensions getExtensions()
	{
		return m_x509certificate.getExtensions();
	}

	public X509DistinguishedName getSubject()
	{
		return m_x509certificate.getSubject();
	}

	public X509DistinguishedName getIssuer()
	{
		return m_x509certificate.getIssuer();
	}


	/**
	 * Returns the private key of this certificate.
	 * @return the private key of this certificate
	 */
	public IMyPrivateKey getPrivateKey()
	{
		return m_keyPair.getPrivate();
	}

	/**
	 * Returns the public key of this certificate.
	 * @return the public key of this certificate
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_keyPair.getPublic();
	}

	/**
	 * Returns the key pair of this certificate.
	 * @return the key pair of this certificate
	 */
	public AsymmetricCryptoKeyPair getKeyPair()
	{
		return m_keyPair;
	}

	/**
	 * Returns the X509 certificate corresponding to this PKCS12 certificate.
	 * The certificate is enabled by default
	 * @return the X509 certificate corresponding to this PKCS12 certificate
	 */
	public JAPCertificate getX509Certificate()
	{
		return m_x509certificate;
	}

	/**
	 * Creates a certification request from this private certificate.
	 * @return a new certification request
	 */
	public PKCS10CertificationRequest createCertifcationRequest()
	{
		return new PKCS10CertificationRequest(this);
	}

	/**
	 * Replaces the current X509 certificate by a clone of the given certificate if the given
	 * certificate has the same public key as the current certificate.
	 * @param a_X509certificate JAPCertificate
	 * @return true if the current X509 certificate has been replaced; false otherwise
	 */
	public boolean setX509Certificate(JAPCertificate a_X509certificate)
	{
		if (a_X509certificate != null &&
			m_x509certificate.getPublicKey().equals(a_X509certificate.getPublicKey()))
		{
			m_x509certificate = (JAPCertificate)a_X509certificate.clone();
			return true;
		}

		return false;
	}

	/**
	 * Signs the coresponding X509 certificate with an other pkcs12 certificate.
	 * Any previous signature is removed. If the signature was self-signed before, it is no
	 * more (but it can still be verified by this PKCS12 certificate via the public key).
	 * @param a_pkcs12Certificate a PKCS12 certificate
	 */
	public void sign(PKCS12 a_pkcs12Certificate)
	{
		m_x509certificate = m_x509certificate.sign(a_pkcs12Certificate);
	}


	/**
	 * Signs the coresponding X509 certificate with an other pkcs12 certificate.
	 * Any previous signature is removed. If the signature was self-signed before, it is no
	 * more (but it can still be verified by this PKCS12 certificate via the public key).
	 * With this method it is possible to change some certificate attributes (validity, extensions,
	 * serial number).
	 * @param a_signerCertificate the PKCS12 certificate of the signer
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @param a_extensions some X509 extensions (may be null)
	 * @param a_serialNumber the serial number for this certificate (may be null)
	 */
	public void sign(PKCS12 a_signerCertificate, Validity a_validity,
					 X509Extensions a_extensions, BigInteger a_serialNumber)
	{
		m_x509certificate = m_x509certificate.sign(a_signerCertificate,
			a_validity, a_extensions, a_serialNumber);
	}

	private static byte[] codeData(
		boolean encrypt,
		byte[] data,
		PKCS12PBEParams pbeParams,
		char[] password,
		BlockCipher cipher,
		int keySize) throws IOException
	{
		byte[] my_out;

		try
		{
			BufferedBlockCipher my_cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(cipher));
			CipherParameters my_param = makePBEParameters(password,
				pbeParams,
				my_cipher.getUnderlyingCipher().getAlgorithmName(),
				keySize,
				64);

			my_param = new ParametersWithRandom(my_param, new SecureRandom());
			my_cipher.init(encrypt, my_param);

			byte[] my_input = data;
			int my_inputlen = my_input.length;
			int my_len = 0;
			byte[] my_tmp = new byte[my_cipher.getOutputSize(my_inputlen)];

			if (my_inputlen != 0)
			{
				my_len = my_cipher.processBytes(my_input, 0, my_inputlen, my_tmp, 0);

			}
			try
			{
				my_len += my_cipher.doFinal(my_tmp, my_len);
			}
			catch (Exception e)
			{
			}

			my_out = new byte[my_len];
			System.arraycopy(my_tmp, 0, my_out, 0, my_len);
		}
		catch (Exception e)
		{
			throw new IOException(
				"exception encrypting data - " + e.toString());
		}

		return my_out;
	}

	private static CipherParameters makePBEMacParameters(char[] password, PKCS12PBEParams pbeParams,
		int keySize)
	{
		PBEParametersGenerator generator = makePBEGenerator();
		byte[] key = PBEParametersGenerator.PKCS12PasswordToBytes(password);
		CipherParameters param;

		generator.init(key, pbeParams.getIV(), pbeParams.getIterations().intValue());

		param = generator.generateDerivedMacParameters(keySize);

		for (int i = 0; i != key.length; i++)
		{
			key[i] = 0;
		}

		return param;
	}

	private static CipherParameters makePBEParameters(char[] password, PKCS12PBEParams pbeParams,
		String targetAlgorithm, int keySize, int ivSize)
	{
		PBEParametersGenerator generator = makePBEGenerator();
		byte[] key = PBEParametersGenerator.PKCS12PasswordToBytes(password);
		CipherParameters param;

		generator.init(key, pbeParams.getIV(), pbeParams.getIterations().intValue());

		if (ivSize != 0)
		{
			param = generator.generateDerivedParameters(keySize, ivSize);
		}
		else
		{
			param = generator.generateDerivedParameters(keySize);
		}

		if (targetAlgorithm.startsWith("DES"))
		{
			if (param instanceof ParametersWithIV)
			{
				KeyParameter kParam = (KeyParameter) ( (ParametersWithIV) param).getParameters();
				DESParameters.setOddParity(kParam.getKey());
			}
			else
			{
				KeyParameter kParam = (KeyParameter) param;
				DESParameters.setOddParity(kParam.getKey());
			}
		}

		for (int i = 0; i != key.length; i++)
		{
			key[i] = 0;
		}

		return param;
	}


	private static PBEParametersGenerator makePBEGenerator()
	{
		return new PKCS12ParametersGenerator(new SHA1Digest());
	}


	private static MyCipher getCipher(String algId)
	{
		if (algId.equals("1.2.840.113549.1.12.1.3"))
		{

			// PBE with SHA and 3-Key TripleDES-CBC
			return new MyCipher(new DESedeEngine(), 192);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.4"))
		{

			// PBE with SHA and 2-Key TripleDES-CBC
			return new MyCipher(new DESedeEngine(), 128);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.5"))
		{

			// PBE with SHA and 128 Bit-RC2-CBC
			return new MyCipher(new RC2Engine(), 128);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.6"))
		{

			// PBE with SHA and 40 Bit-RC2-CBC
			return new MyCipher(new RC2Engine(), 40);
		}
		else
		{
			return null;
		}
	}

	private static class MyCipher
	{
		public BlockCipher cipher;
		public int keysize;
		MyCipher(BlockCipher c, int ks)
		{
			cipher = c;
			keysize = ks;
		}
	};


	private SubjectKeyIdentifier createSubjectKeyId()
	{
		try
		{
			return new SubjectKeyIdentifier(m_x509certificate.getBouncyCastleSubjectPublicKeyInfo());
			/*			ByteArrayInputStream bIn =
			 new ByteArrayInputStream(pubKey.getEncoded());
			   SubjectPublicKeyInfo info =
			 new SubjectPublicKeyInfo(
			 (ASN1Sequence)new DERInputStream(bIn).readObject());
			   return new SubjectKeyIdentifier(info);*/
		}
		catch (Exception e)
		{
			throw new RuntimeException("error creating key");
		}
	}
}
