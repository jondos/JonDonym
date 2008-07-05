package anon.crypto;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;

import anon.util.Util;

public class X509CertificateIssuer extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.CertificateIssuer.getId();
	
	private X509DistinguishedName m_issuer;
	
	public X509CertificateIssuer(X509DistinguishedName a_issuer)
	{
		super(IDENTIFIER, true, createDEROctets(a_issuer));
		m_issuer = a_issuer;
	}
	
	public X509CertificateIssuer(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	private static byte[] createDEROctets(X509DistinguishedName a_issuer)
	{		
		return new GeneralNames(new GeneralName(a_issuer.getX509Name())).getDEREncoded();
	}
	
	public String getName()
	{
		return "CertificateIssuer";
	}
	
	private void createValue()
	{
		try
		{
			DERSequence seq = (DERSequence)new ASN1InputStream(new ByteArrayInputStream(getDEROctets())).readObject();
			GeneralName name = ((new GeneralNames(seq)).getNames())[0];
			if(name.getTagNo() == GeneralName.directoryName)
			{
				seq = (DERSequence)name.getName();
				m_issuer = new X509DistinguishedName(new X509Name(seq));
			}
			else
			{
				throw new Exception();
			}
			
		} catch (Exception e)
		{
			// this should never happen
			throw new RuntimeException("Could not read certificate issuer extension from byte array!");
		}
	}

	public Vector getValues()
	{
		return Util.toVector(m_issuer.toString());
	}

	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}

}
