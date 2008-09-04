package anon.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.IssuerAndSerialNumber;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.pkcs.SignerInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.X509Principal;



import org.bouncycastle.asn1.ASN1InputStream;

/**
 * This was stolen from BouncyCastle and changed a little bit to get it work without BC provider...
 * Original Message was:
 *
 * Represents a PKCS#7 object - specifically the "Signed Data"
 * type.
 * <p>
 * How to use it? To verify a signature, do:
 * <pre>
 * PKCS7SignedData pkcs7 = new PKCS7SignedData(der_bytes);		// Create it
 * pkcs7.update(bytes, 0, bytes.length);	// Update checksum
 * boolean verified = pkcs7.verify();		// Does it add up?
 *
 * To sign, do this:
 * PKCS7SignedData pkcs7 = new PKCS7SignedData(privKey, certChain, "MD5");
 * pkcs7.update(bytes, 0, bytes.length);	// Update checksum
 * pkcs7.sign();				// Create digest
 *
 * bytes = pkcs7.getEncoded();			// Write it somewhere
 * </pre>
 * <p>
 * This class is pretty close to obsolete, for a much better (and more complete)
 * implementation of PKCS7 have a look at the org.bouncycastle.cms package.
 */
public class PKCS7SignedData implements PKCSObjectIdentifiers
{
	private int version, signerversion;
	private Hashtable digestalgos;
	private Vector certs /*, crls*/;
	private JAPCertificate signCert;
	private byte[] digest;
	private String digestAlgorithm, digestEncryptionAlgorithm;

	//private transient PrivateKey privKey;

	//private final String ID_PKCS7_DATA = "1.2.840.113549.1.7.1";
	//private final String ID_PKCS7_SIGNED_DATA = "1.2.840.113549.1.7.2";
	private final String ID_MD5 = "1.2.840.113549.2.5";
	private final String ID_MD2 = "1.2.840.113549.2.2";
	private final String ID_SHA1 = "1.3.14.3.2.26";
	private final String ID_RSA = "1.2.840.113549.1.1.1";
	private final String ID_DSA = "1.2.840.10040.4.1";

	/**
	 * Read an existing PKCS#7 object from a DER encoded byte array
	 */
	public PKCS7SignedData(
		byte[] in) throws SecurityException, InvalidKeyException,
		NoSuchAlgorithmException
	{
		ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(in));

		//
		// Basic checks to make sure it's a PKCS#7 SignedData Object
		//
		DERObject pkcs;

		try
		{
			pkcs = din.readObject();
		}
		catch (IOException e)
		{
			throw new SecurityException("can't decode PKCS7SignedData object");
		}

		if (! (pkcs instanceof ASN1Sequence))
		{
			throw new SecurityException("Not a valid PKCS#7 object - not a sequence");
		}

		ContentInfo content = ContentInfo.getInstance(pkcs);

		if (!content.getContentType().equals(signedData))
		{
			throw new SecurityException("Not a valid PKCS#7 signed-data object - wrong header " +
										content.getContentType().getId());
		}

		SignedData sigdata = SignedData.getInstance(content.getContent());

		certs = new Vector();

		if (sigdata.getCertificates() != null)
		{
			Enumeration ec = ASN1Set.getInstance(sigdata.getCertificates()).getObjects();

			while (ec.hasMoreElements())
			{
				JAPCertificate cert = null;
				try
				{
					cert = JAPCertificate.getInstance(X509CertificateStructure.getInstance(ec.nextElement()));
				}
				catch (Exception e)
				{}
				if (cert != null)
				{
					certs.addElement(cert);
				}
			}
		}

		/* Removed sk13!
		 crls = new ArrayList();
		  if (data.getCRLs() != null)
		  {
		   Enumeration ec = ASN1Set.getInstance(data.getCRLs()).getObjects();
		   while (ec.hasMoreElements())
		   {
		 crls.add(new X509CRLObject(CertificateList.getInstance(ec.nextElement())));
		   }
		  }
		 */
		version = sigdata.getVersion().getValue().intValue();

		//
		// Get the digest algorithm
		//
		digestalgos = new Hashtable();
		Enumeration e = sigdata.getDigestAlgorithms().getObjects();

		while (e.hasMoreElements())
		{
			ASN1Sequence s = (ASN1Sequence) e.nextElement();
			DERObjectIdentifier o = (DERObjectIdentifier) s.getObjectAt(0);
			digestalgos.put(o.getId(), o.getId());
		}

		//
		// Get the SignerInfo
		//
		ASN1Set signerinfos = sigdata.getSignerInfos();
		if (signerinfos.size() != 1)
		{
			throw new SecurityException(
				"This PKCS#7 object has multiple SignerInfos - only one is supported at this time");
		}

		SignerInfo signerInfo = SignerInfo.getInstance(signerinfos.getObjectAt(0));

		signerversion = signerInfo.getVersion().getValue().intValue();

		IssuerAndSerialNumber isAnds = signerInfo.getIssuerAndSerialNumber();

		//
		// Get the signing certificate
		//
		BigInteger serialNumber = isAnds.getCertificateSerialNumber().getValue();
		X509Principal issuer = new X509Principal(isAnds.getName());

		for (Enumeration enumer = certs.elements(); enumer.hasMoreElements(); )
		{
			JAPCertificate cert = (JAPCertificate) enumer.nextElement();
			boolean bS = serialNumber.equals(cert.getSerialNumber());
			if (bS && issuer.equals(new X509Principal(cert.getIssuer().getX509Name())))
			{
				signCert = cert;
				break;
			}
		}

		if (signCert == null)
		{
			throw new SecurityException("Can't find signing certificate with serial " +
										serialNumber.toString(16));
		}

		digestAlgorithm = signerInfo.getDigestAlgorithm().getObjectId().getId();

		digest = signerInfo.getEncryptedDigest().getOctets();
		digestEncryptionAlgorithm = signerInfo.getDigestEncryptionAlgorithm().getObjectId().getId();

		String algo = getDigestAlgorithm();
		if (!algo.equalsIgnoreCase("sha1withdsa"))
		{
			throw new NoSuchAlgorithmException("Signature Algorithm unknown!");
		}
	}

	/**
	 * Create a new PKCS#7 object from the specified key using the BC provider.
	 *
	 * @param the private key to be used for signing.
	 * @param the certifiacate chain associated with the private key.
	 * @param hashAlgorithm the hashing algorithm used to compute the message digest. Must be "MD5", "MD2", "SHA1" or "SHA"
	 */
	/*   public PKCS7SignedData(
	 PrivateKey      privKey,
	 Certificate[]   certChain,
	 String          hashAlgorithm)
	 throws SecurityException, InvalidKeyException,
	 NoSuchProviderException, NoSuchAlgorithmException
	   {
	 this(privKey, certChain, hashAlgorithm, "BC");
	   }
	 */
	/**
	 * Create a new PKCS#7 object from the specified key.
	 *
	 * @param privKey the private key to be used for signing.
	 * @param certChain the certificate chain associated with the private key.
	 * @param hashAlgorithm the hashing algorithm used to compute the message digest. Must be "MD5", "MD2", "SHA1" or "SHA"
	 * @param provider the provider to use.
	 */
	/*   public PKCS7SignedData(
	 PrivateKey      privKey,
	 Certificate[]   certChain,
	 String          hashAlgorithm,
	 String          provider)
	 throws SecurityException, InvalidKeyException,
	 NoSuchProviderException, NoSuchAlgorithmException
	   {
	 this(privKey, certChain, null, hashAlgorithm, provider);
	   }
	 */
	/**
	 * Create a new PKCS#7 object from the specified key.
	 *
	 * @param privKey the private key to be used for signing.
	 * @param certChain the certificate chain associated with the private key.
	 * @param crlList the crl list associated with the private key.
	 * @param hashAlgorithm the hashing algorithm used to compute the message digest. Must be "MD5", "MD2", "SHA1" or "SHA"
	 * @param provider the provider to use.
	 */
	/*   public PKCS7SignedData(
	 PrivateKey      privKey,
	 Certificate[]   certChain,
	 CRL[]           crlList,
	 String          hashAlgorithm,
	 String          provider)
	 throws SecurityException, InvalidKeyException,
	 NoSuchProviderException, NoSuchAlgorithmException
	   {
	 this.privKey = privKey;

	 if (hashAlgorithm.equals("MD5"))
	 {
	  digestAlgorithm = ID_MD5;
	 }
	 else if (hashAlgorithm.equals("MD2"))
	 {
	  digestAlgorithm = ID_MD2;
	 }
	 else if (hashAlgorithm.equals("SHA"))
	 {
	  digestAlgorithm = ID_SHA1;
	 }
	 else if (hashAlgorithm.equals("SHA1"))
	 {
	  digestAlgorithm = ID_SHA1;
	 }
	 else
	 {
	  throw new NoSuchAlgorithmException("Unknown Hash Algorithm "+hashAlgorithm);
	 }

	 version = signerversion = 1;
	 certs = new ArrayList();
	  //  crls = new ArrayList();
	 digestalgos = new HashSet();
	 digestalgos.add(digestAlgorithm);

	 //
	 // Copy in the certificates and crls used to sign the private key.
	 //
	 signCert = (X509Certificate)certChain[0];
	 for (int i = 0;i < certChain.length;i++)
	 {
	  certs.add(certChain[i]);
	 }

	 if (crlList != null)
	 {
	  for (int i = 0;i < crlList.length;i++)
	  {
	   crls.add(crlList[i]);
	  }
	 }

	 //
	 // Now we have private key, find out what the digestEncryptionAlgorithm is.
	 //
	 digestEncryptionAlgorithm = privKey.getAlgorithm();
	 if (digestEncryptionAlgorithm.equals("RSA"))
	 {
	  digestEncryptionAlgorithm = ID_RSA;
	 }
	 else if (digestEncryptionAlgorithm.equals("DSA"))
	 {
	  digestEncryptionAlgorithm = ID_DSA;
	 }
	 else
	 {
	  throw new NoSuchAlgorithmException("Unknown Key Algorithm "+digestEncryptionAlgorithm);
	 }

	 sig = Signature.getInstance(getDigestAlgorithm(), provider);

	 sig.initSign(privKey);
	   }
	 */
	/**
	 * Get the algorithm used to calculate the message digest
	 */
	public String getDigestAlgorithm()
	{
		String da = digestAlgorithm;
		String dea = digestEncryptionAlgorithm;

		if (digestAlgorithm.equals(ID_MD5))
		{
			da = "MD5";
		}
		else if (digestAlgorithm.equals(ID_MD2))
		{
			da = "MD2";
		}
		else if (digestAlgorithm.equals(ID_SHA1))
		{
			da = "SHA1";
		}

		if (digestEncryptionAlgorithm.equals(ID_RSA))
		{
			dea = "RSA";
		}
		else if (digestEncryptionAlgorithm.equals(ID_DSA))
		{
			dea = "DSA";
		}

		return da + "with" + dea;
	}

	/**
	 * Get the X.509 certificates associated with this PKCS#7 object
	 */
	public JAPCertificate[] getCertificates()
	{
		JAPCertificate[] tmp = new JAPCertificate[certs.size()];
		certs.copyInto(tmp);
		return tmp;
	}

	/**
	 * Get the X.509 certificate revocation lists associated with this PKCS#7 object
	 */
	/*  removed sk13 public Collection getCRLs()
	  {
	   return crls;
	  }*/

	/**
	 * Get the X.509 certificate actually used to sign the digest.
	 */
	public JAPCertificate getSigningCertificate()
	{
		return signCert;
	}

	/**
	 * Get the version of the PKCS#7 object. Always 1
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * Get the version of the PKCS#7 "SignerInfo" object. Always 1
	 */
	public int getSigningInfoVersion()
	{
		return signerversion;
	}

	/**
	 * Update the digest with the specified byte. This method is used both for signing and verifying
	 */
	/*    public void update(byte buf)
	  throws SignatureException
	 {
	  sig.update(buf);
	 }
	 */
	/**
	 * Update the digest with the specified bytes. This method is used both for signing and verifying
	 */
	/*   public void update(byte[] buf, int off, int len)
	 throws SignatureException
	   {
	 sig.update(buf, off, len);
	   }
	 */
	/**
	 * Verify the digest
	 */
	public boolean verify(byte[] msg) throws SignatureException
	{
		return ByteSignature.verify(msg, digest, signCert.getPublicKey());
	}

	/**
	 * Get the "issuer" from the TBSCertificate bytes that are passed in
	 */
	private DERObject getIssuer(byte[] enc)
	{
		try
		{
			ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
			ASN1Sequence seq = (ASN1Sequence) in.readObject();
			return (DERObject) seq.getObjectAt(seq.getObjectAt(0) instanceof DERTaggedObject ? 3 : 2);
		}
		catch (IOException e)
		{
			throw new Error("IOException reading from ByteArray: " + e);
		}
	}

}
