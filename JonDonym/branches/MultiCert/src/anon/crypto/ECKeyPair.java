package anon.crypto;

import java.security.SecureRandom;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
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
		ECKeyPair keyPair;
		
		//generate EC Params
		DERObjectIdentifier curveIdentifier = SECObjectIdentifiers.secp160r1; //X962NamedCurves.getByOID(X9ObjectIdentifiers.prime256v1);
		X9ECParameters x9params = SECNamedCurves.getByOID(curveIdentifier);
		ECDomainParameters domainParameters = new ECDomainParameters(x9params.getCurve(), x9params.getG(), x9params.getN(), x9params.getH());
		ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(domainParameters, new SecureRandom());
		
		//generate Key Pair
		ECKeyPairGenerator keyPairGen = new ECKeyPairGenerator();
		keyPairGen.init(keyGenParams);
		AsymmetricCipherKeyPair asymmetricCipherKeyPair = keyPairGen.generateKeyPair();
		
		try
		{
			keyPair = new ECKeyPair(
				new MyECPrivateKey((ECPrivateKeyParameters)asymmetricCipherKeyPair.getPrivate(), curveIdentifier));
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
