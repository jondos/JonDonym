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
import java.math.BigInteger;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * This class implements the BasicConstrains extension for X.509-certificates
 * as specified in RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class X509BasicConstraints extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.BasicConstraints.getId();
	
	/** <code>true</code> if the certificate belongs to a CA */
	private boolean m_cA;
	/** the maximal length of a certification path build from this cert to an end entity */
	private int m_pathLenConstraint = -1;
	
	/**
	 * Creates a new X509BasicConstrains objects with the specified value for cA.
	 * This indicates if the certificate is a CA-certificate.
	 * This Extension is by default set to non-critical according to RFC 5280.
	 * @param cA indicates if the certificate belongs to a CA.
	 */
	public X509BasicConstraints(boolean cA)
	{
		super(IDENTIFIER, true, createDEROctets(cA));
		m_cA = cA;
	}
	
	/**
	 * Creates a new X509BasicConstrains objects with pathLength limited to 
	 * the specified value. Note that cA is always set to <code>true</code>
	 * if a pathLength is specified.
	 * This Extension is by default set to non-critical according to RFC 5280.
	 * @param pathLenConstraint the maximum length of a certification Path
	 */
	public X509BasicConstraints(int pathLenConstraint)
	{
		super(IDENTIFIER, true, createDEROctets(pathLenConstraint));
		m_cA = true;
		m_pathLenConstraint = pathLenConstraint;
	}
	
	/**
	 * Creates an new X509BasicConstraints object from a BouncyCastle DERSequence
	 * @param a_extension the extions as DERSequence
	 */
	public X509BasicConstraints(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	/**
	 * Generates the DEROctets of this extension to hand over to the super class.
	 * @param pathLenConstraint the maximum length of a certification Path
	 * @return the DEROctets of this extension
	 */
	private static byte[] createDEROctets(int pathLenConstraint)
	{
		return new BasicConstraints(pathLenConstraint).getDEREncoded();
	}
	
	/**
	 * Generates the DEROctets of this extension to hand over to the super class.
	 * @param cA indicates if the certificate belongs to a CA.
	 * @return the DEROctets of this extension
	 */
	private static byte[] createDEROctets(boolean cA)
	{
		return new BasicConstraints(cA).getDEREncoded();
	}
	
	/**
	 * Instantiates a new BouncyCastle BasicConstraints from the DEROctets of this 
	 * extension an extracts the cA and pathLenConstraint values.
	 */
	private void createValue()
	{
		try
		{
			BasicConstraints bc = new BasicConstraints((DERSequence)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject());
			m_cA = bc.isCA();
			/* Note: if the pathLenConstraint in the bc object is larger than Integer.MAX_VALUE
			 * the wrong value will be extracted ... but this should be very improbable.
			 */
			BigInteger pathLenConstraint = bc.getPathLenConstraint();
			if(pathLenConstraint != null)
			{
				m_pathLenConstraint = bc.getPathLenConstraint().intValue();
			}
		} catch (Exception a_e)
		{
			throw new RuntimeException("Could not read basic constraints from byte array!");
		}
	}
	
	/**
	 * @return if the certificate belongs to a CA
	 */
	public boolean isCA()
	{
		return m_cA;
	}
	
	/**
	 * @return the maximal length of a certificate that is build from this certificate
	 * 		   to an end entity
	 */
	public int getPathLengthConstraint()
	{
		return m_pathLenConstraint;
	}
	
	/**
	 * @return "BasicConstraints"
	 */
	public String getName()
	{
		return "BasicConstraints";
	}
	
	/**
	 * @return a Vector containing the values for cA and pathLenConstraint
	 * 		   as human readable Strings.
	 */
	public Vector getValues()
	{
		Vector v = new Vector();;
		
		v.addElement(new String("cA="+m_cA));
		if(m_pathLenConstraint != -1)
		{
			v.addElement(new String("pathLenConstraint="+m_pathLenConstraint));
		}
		return v;
	}
}
