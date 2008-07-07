/*
 Copyright (c) 2000 - 2008, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

package anon.crypto;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.ReasonFlags;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * This class implements the IssuingDistributionPoint extension for X.509-CRLs
 * as specified in RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
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
		
		super(IDENTIFIER, false, createDEROctets(distributionPoint, !onlyContainsUserCerts,
											!onlyContainsCACerts, onlySomeReasons, !indirectCRL,
											!onlyContainsAttributeCerts));
		m_issuingDistributionPoint = new IssuingDistributionPoint(distributionPoint, 
				!onlyContainsUserCerts, !onlyContainsCACerts, onlySomeReasons, 
				!indirectCRL, !onlyContainsAttributeCerts);
	}
	
	public X509IssuingDistributionPoint(boolean a_indirectCRL)
	{
		this(null, !false, !false, null, !a_indirectCRL, !false);
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
			// this should never happen
			throw new RuntimeException("Could not read issuing distribution point extension from byte array!");
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
