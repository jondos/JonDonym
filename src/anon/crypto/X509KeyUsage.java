package anon.crypto;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;

public class X509KeyUsage extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.KeyUsage.getId();
	
	public static final String DIGITAL_SIGNATURE = "digitalSignature";
	public static final String NON_REPUDIATION   = "nonRepudiation/contentCommitment";
    public static final String KEY_ENCIPHERMENT  = "keyEncipherment";
    public static final String DATA_ENCIPHERMENT = "dataEncipherment";
    public static final String KEY_AGREEMENT     = "keyAgreement";
    public static final String KEY_CERT_SIGN     = "keyCertSign";
    public static final String CRL_SIGN          = "cRLSign";
    public static final String ENCIPHER_ONLY     = "encipherOnly";
    public static final String DECIPHER_ONLY     = "decipherOnly";
    
    public static final int[] USAGES = 
       {KeyUsage.digitalSignature, KeyUsage.nonRepudiation, KeyUsage.keyEncipherment, 
    	KeyUsage.dataEncipherment, KeyUsage.keyAgreement, KeyUsage.keyCertSign,
    	KeyUsage.cRLSign, KeyUsage.encipherOnly, KeyUsage.decipherOnly};
    
    private Vector<String> m_usage;
	
	public X509KeyUsage(int usage)
	{
		super(IDENTIFIER, true, createDEROctet(usage));
		createValue();
	}
	
	public X509KeyUsage(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	private static byte[] createDEROctet(int usage)
	{
		return new KeyUsage(usage).getDEREncoded();
	}

	@Override
	public String getName()
	{
		return "KeyUsage";
	}

	@Override
	public Vector getValues()
	{
		return (Vector)m_usage.clone();
	}
	
	public String getUsageString(int a_usage)
	{
		switch(a_usage)
		{
			case KeyUsage.digitalSignature:
				return DIGITAL_SIGNATURE;
			case KeyUsage.nonRepudiation:
				return NON_REPUDIATION;
			case KeyUsage.keyEncipherment:
				return KEY_ENCIPHERMENT;
			case KeyUsage.dataEncipherment:
				return DATA_ENCIPHERMENT;
			case KeyUsage.keyAgreement:
				return KEY_AGREEMENT;
			case KeyUsage.keyCertSign:
				return KEY_CERT_SIGN;
			case KeyUsage.cRLSign:
				return CRL_SIGN;
			case KeyUsage.encipherOnly:
				return ENCIPHER_ONLY;
			case KeyUsage.decipherOnly:
				return DECIPHER_ONLY;
			default:
				return null;
		}
	}
	
	private void createValue()
	{
		try
		{
			m_usage = new Vector();
			int usage = ((DERBitString)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject()).intValue();
			for(int i=0; i<USAGES.length; i++)
			{
				if((USAGES[i] & usage) == USAGES[i])
				{
					m_usage.add(getUsageString(USAGES[i]));
				}
			}
		} catch (Exception a_e)
		{
			throw new RuntimeException("Could not read key usage from byte array!");
		}
		
	}
}
