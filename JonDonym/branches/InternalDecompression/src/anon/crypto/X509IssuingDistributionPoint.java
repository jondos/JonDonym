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
	
	/**
	 * Creates an new X509IssuingDistributionPoint object with the specified values
	 * @param distributionPoint the name of the distribution point
	 * @param onlyContainsUserCerts the CRL contains only revoked UserCerts
	 * @param onlyContainsCACerts the CRL contains only revoked CA-Certs
	 * @param onlySomeReasons the CRL contains only certificates revoked because of SomeReasons
	 * @param indirectCRL the CRL contains revoked certificates issued by other CAs
	 * @param onlyContainsAttributeCerts the CRL contains only revoked AttributeCerts
	 */
	public X509IssuingDistributionPoint(
			DistributionPointName distributionPoint,
	        boolean onlyContainsUserCerts,
	        boolean onlyContainsCACerts,
	        ReasonFlags onlySomeReasons,
	        boolean indirectCRL,
	        boolean onlyContainsAttributeCerts
	        )
	{
		/*
		 * Implementation Note: There seems to be an error in the BouncyCastle class IssuingDistributionPoint,
		 * because when you call the constructor with true (for any one boolean value) its set to false.
		 * By default the values are all false, so this makes no sence. To correct this errer we negate
		 * all incoming values to have the right ones in the extension.
		 * TODO: Remove negation of values when BC class is corrected!
		 */
		super(IDENTIFIER, false, createDEROctets(distributionPoint, !onlyContainsUserCerts,
											!onlyContainsCACerts, onlySomeReasons, !indirectCRL,
											!onlyContainsAttributeCerts));
		m_issuingDistributionPoint = new IssuingDistributionPoint(distributionPoint, 
				!onlyContainsUserCerts, !onlyContainsCACerts, onlySomeReasons, 
				!indirectCRL, !onlyContainsAttributeCerts);
	}
	
	/**
	 * Creates an new X509IssuingDistributionPoint object with the specified values
	 * the CRL contains revoked certificates issued by other CAs
	 * @param a_indirectCRL
	 */
	public X509IssuingDistributionPoint(boolean a_indirectCRL)
	{
		this(null, false, false, null, a_indirectCRL, false);
	}
	
	/**
	 * Creates an new X509BasicConstraints object from a BouncyCastle DERSequence
	 * @param a_extension the extions as DERSequence
	 */
	public X509IssuingDistributionPoint(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	/**
	 * Instantiates a new BouncyCastle IssuingDistributionPoint from the DEROctets 
	 * of this extension.
	 */
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
	
	/**
	 * Generates the DEROctets of this extension to hand over to the super class.
	 * @param distributionPoint the name of the distribution point
	 * @param onlyContainsUserCerts the CRL contains only revoked UserCerts
	 * @param onlyContainsCACerts the CRL contains only revoked CA-Certs
	 * @param onlySomeReasons the CRL contains only certificates revoked because of SomeReasons
	 * @param indirectCRL the CRL contains revoked certificates issued by other CAs
	 * @param onlyContainsAttributeCerts the CRL contains only revoked AttributeCerts
	 * @return the DEROctets of this extension
	 */
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
	
	/**
	 * @return "IssuingDistributionPoint"
	 */
	public String getName()
	{
		return "IssuingDistributionPoint";
	}

	/**
	 * 
	 */
	public Vector getValues()
	{
		//TODO Implement this when there's something like a CRLDetailsDialog
		return null;
	}
	
	/**
	 * @return <code>true</code> if the CRL contains revoked certificates issued by other CAs
	 */
	public boolean isIndirectCRL()
	{
		return m_issuingDistributionPoint.isIndirectCRL();
	}
	
	/**
	 * @return the BC IssuingDistributionPoint object represented by this class
	 */
	public IssuingDistributionPoint getIssuingDistributionPoint()
	{
		//TODO: Remove this method an only allow access to the values!
		return m_issuingDistributionPoint;
	}
}
