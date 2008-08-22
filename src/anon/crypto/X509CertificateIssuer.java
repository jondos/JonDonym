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
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;

import anon.util.Util;

/**
 * This class implements the CertificateIssuer Entry-Extension for X.509-CRLs
 * as specified in RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class X509CertificateIssuer extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.CertificateIssuer.getId();
	
	/** the distinguished name of the certificate's issuer */
	private X509DistinguishedName m_issuer;
	
	/**
	 * Create a new X509CertificateIssuer object from a given distinguished name.
	 * @param a_issuer the distinguished name of the certificate's issuer
	 */
	public X509CertificateIssuer(X509DistinguishedName a_issuer)
	{
		super(IDENTIFIER, true, createDEROctets(a_issuer));
		m_issuer = a_issuer;
	}
	
	/**
	 * Creates an new X509etificateIssuer object from a BouncyCastle DERSequence
	 * @param a_extension the extions as DERSequence
	 */
	public X509CertificateIssuer(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	/**
	 * Generates the DEROctets of this extension to hand over to the super class.
	 * @param a_issuer the distinguished name of the certificate's issuer
	 * @return the DEROctets of this extension
	 */
	private static byte[] createDEROctets(X509DistinguishedName a_issuer)
	{		
		return new GeneralNames(new GeneralName(a_issuer.getX509Name())).getDEREncoded();
	}
	
	/**
	 * @return "CertificateIssuer"
	 */
	public String getName()
	{
		return "CertificateIssuer";
	}
	
	/**
	 * reads the distinguished name of the certificate issuer from the DEROctets
	 */
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
    
	/**
	 * @return a human-readable vector containing the String-representation of the issuer's DN.
	 */
	public Vector getValues()
	{
		return Util.toVector(m_issuer.toString());
	}
	
	/**
	 * Returns if the issuer's DN and a given DN are equal. You may call this with
	 * a X509DistinguishedName or a BC X509Name object. Otherwise <code>false</code>
	 * is returned.
	 * @return if the issuer's DN and a given DN are equal
	 * @see X509DistinguishedName.equals()
	 */
	public boolean equalsIssuer(Object a_object)
	{
		if(a_object == null)
		{
			return false;
		}
		if(a_object instanceof X509DistinguishedName ||
				a_object instanceof X509Name)
		{
			return m_issuer.equals(a_object);
		}
		return false;
	}
	
	/**
	 * Returns the X509DistinguishedName of the issuer represented by this extension
	 * @return the issuer's X509DistinguishedName
	 */
	public X509DistinguishedName getDistinguishedName()
	{
		return m_issuer;
	}
}
