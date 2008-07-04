package anon.crypto;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;

public class X509BasicConstraints extends AbstractX509Extension
{
	private BasicConstraints m_BCbasicConstraints;
	
	public static final String IDENTIFIER = X509Extensions.BasicConstraints.getId();
	
	public X509BasicConstraints(boolean cA)
	{
		super(IDENTIFIER, true, createDEROctets(cA));
		createValue();
	}
	
	public X509BasicConstraints(int pathLenConstraint)
	{
		super(IDENTIFIER, true, createDEROctets(pathLenConstraint));
		createValue();
	}
	
	public X509BasicConstraints(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	private static byte[] createDEROctets(int pathLenConstraint)
	{
		return new BasicConstraints(pathLenConstraint).getDEREncoded();
	}

	private static byte[] createDEROctets(boolean cA)
	{
		return new BasicConstraints(cA).getDEREncoded();
	}
	
	private void createValue()
	{
		try
		{
			m_BCbasicConstraints = new BasicConstraints((DERSequence)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject());
		} catch (Exception a_e)
		{
			throw new RuntimeException("Could not read basic constraints from byte array!");
		}
	}
	
	public boolean isCA()
	{
		return m_BCbasicConstraints.isCA();
	}
	
	public BigInteger getPathLenConstraint()
	{
		return m_BCbasicConstraints.getPathLenConstraint();
	}

	public String getName()
	{
		return "BasicConstraints";
	}

	public Vector getValues()
	{
		Vector<String> v = new Vector();;
		
		if(m_BCbasicConstraints != null)
		{
			v.add(new String("cA="+m_BCbasicConstraints.isCA()));
			if(m_BCbasicConstraints.getPathLenConstraint() != null)
			{
				v.add(new String("pathLenConstraint="+m_BCbasicConstraints.getPathLenConstraint()));
			}
		}
		return v;
	}
}
