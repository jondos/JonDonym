package anon.crypto;

import java.io.IOException;
import java.security.InvalidKeyException;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECPoint;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class MyECPublicKey extends AbstractPublicKey implements IMyPublicKey
{
	private X9ECPoint m_Q; //Maybe use ECPoint here?! Doesn't matter ...
	private MyECParams m_params;
	
	public MyECPublicKey(ECPublicKeyParameters params)
	{
		m_Q = new X9ECPoint(params.getQ());
		m_params = new MyECParams(params.getParameters());			
	}
	
	public MyECPublicKey(SubjectPublicKeyInfo spki) throws IllegalArgumentException
	{
		try
		{
			DERBitString derBits = (DERBitString) spki.getPublicKeyData();
			ASN1OctetString derQ = new DEROctetString(derBits.getBytes());
			m_params = new MyECParams(X962Parameters.getInstance(spki.getAlgorithmId().getParameters()));
			m_Q = new X9ECPoint(m_params.getECDomainParams().getCurve(), derQ);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("invalid info structure in ECDSA public key");
		}
	}
		
	public boolean equals(Object a_publicKey)
	{
		if (a_publicKey == null)
		{
			return false;
		}
		if (! (a_publicKey instanceof ECPublicKeyParameters))
		{
			return false;
		}
		ECPublicKeyParameters e = (ECPublicKeyParameters) a_publicKey;
		//Check Q
		if(e.getQ().equals(m_Q.getPoint()))
		{
			return false;
		}
		//Check parameters
		return m_params.equals(e.getParameters());
	}

	public int hashCode()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo()
	{
		AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, m_params.getX962Params().toASN1Object());
		
		return new SubjectPublicKeyInfo(algId, m_Q.getPoint().getEncoded());
	}

	public int getKeyLength()
	{ 
		/* return the length of the order n of the base point G because this determines the 
		 * key's security better than its pure length. 
		 */
		return m_params.getECDomainParams().getN().bitLength()-1;
	}
	
	/**
	 * Gets a signature algorithm object for this key.
	 * @return a signature algorithm object for this key
	 */
	public ISignatureVerificationAlgorithm getSignatureAlgorithm()
	{
		try
		{
			MyECDSASignature algorithm = new MyECDSASignature();
			algorithm.initVerify(this);
			return algorithm;
		}
		catch (InvalidKeyException a_e)
		{
			// not possible
		}
		return null;
	}
	
	public ECPublicKeyParameters getPublicParams()
	{
		return new ECPublicKeyParameters(m_Q.getPoint(), m_params.getECDomainParams());
	}
	
	public String getAlgorithm()
	{
		return "Elliptic Curve Cryptography";
	}

	public String getFormat()
	{
		return "X509";
	}
	
	/**
	 * @see RFC 4050
	 */
	public Element toXmlElement(Document a_doc)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	protected void setNamedCurveID(DERObjectIdentifier curveIdentifier)
	{
		m_params.setNamedCurveID(curveIdentifier);
	}
}
