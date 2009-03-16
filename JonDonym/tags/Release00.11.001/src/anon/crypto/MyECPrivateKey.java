package anon.crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKeyStructure;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLParseException;

public final class MyECPrivateKey extends AbstractPrivateKey implements IMyPrivateKey
{
	private BigInteger m_D;
	private MyECParams m_params;
	
	public MyECPrivateKey(ECPrivateKeyParameters params, DERObjectIdentifier curveId)
	{
		m_D = params.getD();
		m_params = new MyECParams(params.getParameters());
		m_params.setNamedCurveID(curveId);
	}
	
	/**
	 * Use ECPrivateKeyStructure defined in "SEC 1: Elliptic Curve Cryptography"
	 * to be compatible with OpenSSL
	 * @see http://www.secg.org/
	 * @param privKeyInfo
	 * @throws InvalidKeyException
	 */
	public MyECPrivateKey(PrivateKeyInfo privKeyInfo) throws InvalidKeyException
	{
		super(privKeyInfo);
		try
		{
			AlgorithmIdentifier algId = privKeyInfo.getAlgorithmId();
			ECPrivateKeyStructure ecpks = new ECPrivateKeyStructure((ASN1Sequence)privKeyInfo.getPrivateKey());
			m_D = ecpks.getKey();
			m_params = new MyECParams(X962Parameters.getInstance(algId.getParameters()));
		}

		catch (Exception e)
		{
			throw new InvalidKeyException("IOException while decoding private key");
		}
	}
	
	/**
	 * @see RFC 4050
	 */
	public MyECPrivateKey(Element a_xmlElement) throws InvalidKeyException, XMLParseException
	{
		//TODO Implement me
	}
	
	/**
	 * @see ECKeyPairGenerator
	 */
	public IMyPublicKey createPublicKey()
	{
		MyECPublicKey key;
		
		ECPoint Q = m_params.getECDomainParams().getG().multiply(m_D);
		
		key = new MyECPublicKey(new ECPublicKeyParameters(Q, m_params.getECDomainParams()));
		
		key.setNamedCurveID(m_params.getCurveID());
		
		return key;
	}
	
	/**
	 * Use ECPrivateKeyStructure defined in "SEC 1: Elliptic Curve Cryptography" section
	 * to be compatible with OpenSSL
	 * @see http://www.secg.org/
	 * @return
	 */
	public PrivateKeyInfo getAsPrivateKeyInfo()
	{
		PrivateKeyInfo info;

		DERObject derParam = m_params.getX962Params().toASN1Object();

		info = new PrivateKeyInfo(
					new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, derParam), //ID looks wrong but isn't!
					new ECPrivateKeyStructure(m_D).getDERObject());
		return info;
	}

	/**
	 * Gets a signature algorithm object for this key.
	 * @return a signature algorithm object for this key
	 */
	public ISignatureCreationAlgorithm getSignatureAlgorithm()
	{
		try
		{
			MyECDSASignature algorithm = new MyECDSASignature(); ;
			algorithm.initSign(this);
			return algorithm;
		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return null;
	}

	public String getAlgorithm()
	{
		return "Elliptic Curve Cryptography";
	}

	public String getFormat()
	{
		return "PKCS#8";
	}

	public Element toXmlElement(Document a_doc)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public ECPrivateKeyParameters getPrivateParams()
	{
		return new ECPrivateKeyParameters(m_D, m_params.getECDomainParams());
	}
}
