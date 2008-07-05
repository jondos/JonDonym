package anon.crypto;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.ReasonFlags;
import org.bouncycastle.asn1.x509.X509Extensions;

public class X509IssuingDistributionPoint extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.IssuingDistributionPoint.getId();
	
	private IssuingDistributionPoint m_issuingDistributionPoint;
	
	public X509IssuingDistributionPoint(
			DistributionPointName distributionPoint,
	        boolean onlyContainsUserCerts,
	        boolean onlyContainsCACerts,
	        ReasonFlags onlySomeReasons,
	        boolean indirectCRL,
	        boolean onlyContainsAttributeCerts
			)
	{
		
		super(IDENTIFIER, false, createDEROctets(distributionPoint, onlyContainsUserCerts,
											onlyContainsCACerts, onlySomeReasons, indirectCRL,
											onlyContainsAttributeCerts));
		m_issuingDistributionPoint = new IssuingDistributionPoint(distributionPoint, 
				onlyContainsUserCerts, onlyContainsCACerts, onlySomeReasons, 
				indirectCRL, onlyContainsAttributeCerts);
	}
	
	public X509IssuingDistributionPoint(boolean a_indirectCRL)
	{
		this(null, false, false, null, a_indirectCRL, false);
	}
	
	public X509IssuingDistributionPoint(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	private void createValue()
	{
		try
		{
			m_issuingDistributionPoint = new IssuingDistributionPoint((DERSequence)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject());
		}
		catch (Exception a_e)
		{
			a_e.printStackTrace();
			// this should never happen
			//throw new RuntimeException("Could not read issuing distribution point extension from byte array!");
		}
	}

	private static byte[] createDEROctets(
			DistributionPointName distributionPoint,
			boolean onlyContainsUserCerts, 
			boolean onlyContainsCACerts,
			ReasonFlags onlySomeReasons, 
			boolean indirectCRL,
			boolean onlyContainsAttributeCerts)
	{
		return new IssuingDistributionPoint(distributionPoint, onlyContainsUserCerts,
				onlyContainsCACerts, onlySomeReasons, indirectCRL, 
				onlyContainsAttributeCerts).getDEREncoded();
	}
	
	@Override
	public String getName()
	{
		return "IssuingDistributionPoint";
	}

	@Override
	public Vector getValues()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isIndirectCRL()
	{
		return m_issuingDistributionPoint.isIndirectCRL();
	}
	
	public IssuingDistributionPoint getIssuingDistributionPoint()
	{
		return m_issuingDistributionPoint;
	}

}
