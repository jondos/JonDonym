package anon.crypto;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;

public final class MyECParams
{
	private static final DERObjectIdentifier IMPLICIT_CURVE_ID = SECObjectIdentifiers.secp160r1;
	
	ECDomainParameters m_params;
	boolean m_isImplicitlyCA = false;
	boolean m_isNamedCurve = false;
	DERObjectIdentifier m_curveIdentifier = null;
	
	public MyECParams()
	{
		this(SECNamedCurves.getByOID(IMPLICIT_CURVE_ID));
		m_isImplicitlyCA = true;
	}
	
	public MyECParams(ECDomainParameters params)
	{
		m_params = params;
	}
	
	public MyECParams(X9ECParameters x9params)
	{
		m_params =  new ECDomainParameters(x9params.getCurve(), x9params.getG(), x9params.getN(), x9params.getH());
	}
	
	public MyECParams(X962Parameters params)
	{
		X9ECParameters x9params = null;
		
		if(params.isNamedCurve())
		{
			m_isNamedCurve = true;
			m_curveIdentifier = (DERObjectIdentifier)params.getParameters();
			x9params = SECNamedCurves.getByOID(m_curveIdentifier);
			if(x9params == null)
			{
				x9params = X962NamedCurves.getByOID(m_curveIdentifier);
			}
			if(x9params == null)
			{
				x9params = NISTNamedCurves.getByOID(m_curveIdentifier);
			}
			if(x9params == null)
			{
				x9params = TeleTrusTNamedCurves.getByOID(m_curveIdentifier);
			}
			if(x9params == null)
			{
				throw new IllegalArgumentException("Unknown Named Curve Identifier!");
			}
		}
		else if(params.isImplicitlyCA())
		{
			m_isImplicitlyCA = true;
			x9params = SECNamedCurves.getByOID(IMPLICIT_CURVE_ID);
			m_curveIdentifier = IMPLICIT_CURVE_ID;
		}
		else
		{
			x9params = new X9ECParameters((ASN1Sequence)params.getParameters());
			m_isNamedCurve = false;
		}
		m_params = new ECDomainParameters(x9params.getCurve(), x9params.getG(), x9params.getN(), x9params.getH());
	}

	public boolean equals(Object o) 
	{
		if (o == null)
		{
			return false;
		}
		if (! (o instanceof ECDomainParameters))
		{
			return false;
		}
		ECDomainParameters e = (ECDomainParameters)o;
		if(e.getH().equals(m_params.getH()) && e.getN().equals(m_params.getN()))
		{
			//Check curve
			if(e.getCurve() instanceof ECCurve.F2m)
			{
				return ((ECCurve.F2m)e.getCurve()).equals(m_params.getCurve());
			}
			if(e.getCurve() instanceof ECCurve.Fp)
			{
				return ((ECCurve.Fp)e.getCurve()).equals(m_params.getCurve());
			}
		}
		return false;
	}
		
	protected ECDomainParameters getECDomainParams()
	{
		return m_params;
	}

	protected X962Parameters getX962Params()
	{
		if(m_isNamedCurve)
		{
			return new X962Parameters(m_curveIdentifier);
		}
		else if(m_isImplicitlyCA)
		{
			return new X962Parameters(new DERNull());
		}
		else
		{
			X9ECParameters x9params = new X9ECParameters(m_params.getCurve(), m_params.getG(), m_params.getN(), m_params.getH());
			return new X962Parameters(x9params);
		}
	}
	
	protected void setNamedCurveID(DERObjectIdentifier curveIdentifier)
	{
		if(curveIdentifier != null)
		{
			this.m_curveIdentifier = curveIdentifier;
			this.m_isNamedCurve = true;
		}
	}
	
	protected DERObjectIdentifier getCurveID()
	{
		return m_curveIdentifier;
	}
}
