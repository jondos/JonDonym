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
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * The Authority Public Key Identifier as specified in RFC 5280 is calcluated
 * the same way as the SubjectKeyIdentifier.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class X509AuthorityKeyIdentifier extends AbstractX509KeyIdentifier {

	public static final String IDENTIFIER = X509Extensions.AuthorityKeyIdentifier.getId();
	
	/** the optional serial number of the issuing certificate */
	private BigInteger m_serial;
	/** the optional name of the issuer */
	private GeneralNames m_names;

	/**
	 * Creates a new X509AuthorityKeyIdentifier from a public key.
	 * @param a_publicKey the issuer's public key
	 */
	public X509AuthorityKeyIdentifier(IMyPublicKey a_publicKey)
	{
		super(IDENTIFIER, createDEROctets(a_publicKey, null, null));
		createValue();
	}
	
	/**
	 * Creates a new X509AuthorityKeyIdentifier from a public key, a name and
	 * a certificate serial
	 * @param a_publicKey the issuer's public key
	 * @param a_name the issuer's name
	 * @param a_serial the issuing certificate's serial number
	 */
	public X509AuthorityKeyIdentifier(IMyPublicKey a_publicKey, GeneralNames a_name, BigInteger a_serial)
	{
		super(IDENTIFIER, createDEROctets(a_publicKey, a_name, a_serial));
		createValue();
	}
	
	/**
	 * Creates a new X509AuthorityKeyIdentifier from a public key, a distinguished name and
	 * a certificate serial
	 * @param a_publicKey the issuer's public key
	 * @param a_name the issuer's distinguished name
	 * @param a_serial the issuing certificate's serial number
	 */
	public X509AuthorityKeyIdentifier(IMyPublicKey a_publicKey, X509DistinguishedName a_name, BigInteger a_serial)
	{
		super(IDENTIFIER, createDEROctets(a_publicKey, new GeneralNames(new GeneralName(a_name.getX509Name())), a_serial));
		createValue();
	}
	
	/**
	 * Creates an extension from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 */
	public X509AuthorityKeyIdentifier(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}

	/**
	 * Returns "AuthorityKeyIdentifier".
	 * @return "AuthorityKeyIdentifier"
	 */
	public String getName()
	{
		return "AuthorityKeyIdentifier";
	}
	
	/**
	 * Generates the octets to hand over to the super class 
	 * @param a_publicKey the issuer's public key
	 * @param a_name the issuer's name
	 * @param a_serial the issuing certificate's serial number
	 * @return
	 */
	private static byte[] createDEROctets(IMyPublicKey a_publicKey, GeneralNames a_name, BigInteger a_serial)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		AuthorityKeyIdentifier aki;
		
		if(a_name != null && a_serial != null)
		{
			aki = new AuthorityKeyIdentifier(a_publicKey.getAsSubjectPublicKeyInfo(), a_name, a_serial);
		}
		else
		{
			aki = new AuthorityKeyIdentifier(a_publicKey.getAsSubjectPublicKeyInfo());
		}
		
		try
		{
			new DEROutputStream(out).writeObject((DERSequence)aki.getDERObject());
		}
		catch (Exception a_e)
		{
			// should never happen
			throw new RuntimeException("Could not write DER object to bytes!");
		}

	   return out.toByteArray();
	}
	
	/**
	 * Calculate the AuthorityKeyIdentifier value. The keyIdentifier is the stored as
	 * a String in m_value
	 */
	private void createValue()
	{
		byte[] identifier;

		try
		{
			AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier((DERSequence)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject());
			
			identifier = aki.getKeyIdentifier();
			m_value = ByteSignature.toHexString(identifier);
			m_serial = aki.getAuthorityCertSerialNumber();
			m_names = aki.getAuthorityCertIssuer();
		}
		catch (Exception a_e)
		{
			// this should never happen
			throw new RuntimeException("Could not read authority key identifier from byte array!");
		}
	}
	
	/**
	 * Returns a Vector containing the keyIdentifier and if available the name of the
	 * certificate issuer und the serial number of the issuing certificate.
	 * @return Vector a Vector of Strings containing the AuthorityKeyIdentifiers values
	 */
	public Vector getValues()
	{
		Vector v = new Vector();
		v.addElement(m_value);
		if(m_names != null)
		{
			GeneralName[] names = m_names.getNames();
			for(int i=0; i<names.length; i++)
			{
				String name;
				if(names[i].getTagNo() == GeneralName.directoryName)
				{
					name = new X509Name((DERSequence)names[i].getName().getDERObject()).toString();
				}
				else
				{
					name = new String(names[i].getName().getDERObject().getDEREncoded()).trim();
					//name = name+" ("+AbstractX509AlternativeName.getTagAsString(names[i].getTagNo())+")";
				}
				v.addElement(name);	
			}
		}
		if(m_serial != null)
		{
			v.addElement("authorityCertSerialNumber: "+m_serial);
		}
		return v;
	}
}
