package anon.crypto;

import java.security.SecureRandom;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;

public class ECKeyPair extends AsymmetricCryptoKeyPair
{
	ECKeyPair(IMyPrivateKey a_privateKey)
	{
		super(a_privateKey);
	}
	
	public static ECKeyPair getInstance(SecureRandom a_secureRandom)
	{
		return getInstance(SECObjectIdentifiers.secp160r1, a_secureRandom);
	}
	
	public static ECKeyPair getInstance(DERObjectIdentifier a_namedCurve, SecureRandom a_secureRandom)
	{
		ECKeyPair keyPair;
		X9ECParameters x9params;
		ECDomainParameters domainParameters;
		ECKeyGenerationParameters keyGenParams;
		ECKeyPairGenerator keyPairGen;
		AsymmetricCipherKeyPair asymmetricCipherKeyPair;
		
		//generate EC Params
		x9params = SECNamedCurves.getByOID(a_namedCurve);
		if(x9params == null)
		{
			x9params = X962NamedCurves.getByOID(a_namedCurve);
		}
		if(x9params == null)
		{
			x9params = NISTNamedCurves.getByOID(a_namedCurve);
		}
		if(x9params == null)
		{
			x9params = TeleTrusTNamedCurves.getByOID(a_namedCurve);
		}
		if(x9params == null)
		{
			throw new IllegalArgumentException("Unknown Named Curve Identifier!");
		}
		domainParameters = new ECDomainParameters(x9params.getCurve(), x9params.getG(), x9params.getN(), x9params.getH());
		keyGenParams = new ECKeyGenerationParameters(domainParameters, a_secureRandom);
		
		//generate Key Pair
		keyPairGen = new ECKeyPairGenerator();
		keyPairGen.init(keyGenParams);
		asymmetricCipherKeyPair = keyPairGen.generateKeyPair();
		
		try
		{
			keyPair = new ECKeyPair(
				new MyECPrivateKey((ECPrivateKeyParameters)asymmetricCipherKeyPair.getPrivate(), a_namedCurve));
		}
		catch(Exception e)
		{
			return null;
		}
		
		if(!isValidKeyPair(keyPair))
		{
			return null;
		}
	
		return keyPair;
	}
}
