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
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * This class implements the KeyUsage extension for X.509-certificates
 * as specified in RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class X509KeyUsage extends AbstractX509Extension
{
	public static final String IDENTIFIER = X509Extensions.KeyUsage.getId();
	
	/** the Strings for the different KeyUsages as specified by RFC 5280 */
	public static final String DIGITAL_SIGNATURE = "digitalSignature";
	public static final String NON_REPUDIATION   = "nonRepudiation/contentCommitment";
    public static final String KEY_ENCIPHERMENT  = "keyEncipherment";
    public static final String DATA_ENCIPHERMENT = "dataEncipherment";
    public static final String KEY_AGREEMENT     = "keyAgreement";
    public static final String KEY_CERT_SIGN     = "keyCertSign";
    public static final String CRL_SIGN          = "cRLSign";
    public static final String ENCIPHER_ONLY     = "encipherOnly";
    public static final String DECIPHER_ONLY     = "decipherOnly";
    
    /** the different KeyUsage values packed into an arry for easiear Parsing */
    private static final int[] USAGES = 
       {KeyUsage.digitalSignature, KeyUsage.nonRepudiation, KeyUsage.keyEncipherment, 
    	KeyUsage.dataEncipherment, KeyUsage.keyAgreement, KeyUsage.keyCertSign,
    	KeyUsage.cRLSign, KeyUsage.encipherOnly, KeyUsage.decipherOnly};
    
    /** a vector containing */
    private int m_usage;
	
    /** 
     * Creates a new X509KeyUsage object from the specified usage integer.
     * This has to be constructet through the bitwise or ("|") from the
     * differnet available usages in the BC-Class KeyUsage.
     * @param a_usage
     * @see org.bouncycastle.asn1.x509.KeyUsage;
     */
	public X509KeyUsage(int a_usage)
	{
		super(IDENTIFIER, true, createDEROctet(a_usage));
		m_usage = a_usage;
	}
	
	/**
	 * Creates an new X509KeyUsage object from a BouncyCastle DERSequence
	 * @param a_extension the extions as DERSequence
	 */
	public X509KeyUsage(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}
	
	/**
	 * Generates the DEROctets of this extension to hand over to the super class.
	 * @param usage the allowed usages of this certificate
	 * @return the DEROctets of this extension
	 */
	private static byte[] createDEROctet(int usage)
	{
		return new KeyUsage(usage).getDEREncoded();
	}

	/**
	 * @return "KeyUsage"
	 */
	public String getName()
	{
		return "KeyUsage";
	}

	/**
	 * @return a human-readable Vector of the allowed KeyUsages.
	 */
	public Vector getValues()
	{
        Vector v = new Vector();
		
		for(int i=0; i<USAGES.length; i++)
		{
			if((USAGES[i] & m_usage) == USAGES[i])
			{
				v.addElement(getUsageString(USAGES[i]));
			}
		}
		
		return v;
	}
	
	/**
	 * Translate a single usage-bitstring (no combination) into a
	 * human-readable String.
	 * @param a_usage a single usage
	 * @return a human-readable String of the usage or null if the
	 * 	       usage is unknown. combined usages return also null!
	 */
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
	
	
	/**
	 * reads the usage-integer from the DEROctets of this extension.
	 */
	private void createValue()
	{
		try
		{
			m_usage = ((DERBitString)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject()).intValue();
		} catch (Exception a_e)
		{
			throw new RuntimeException("Could not read key usage from byte array!");
		}
	}
	
	/**
	 * @param a_usage a (combination of) usage(s) to test against the extension's allowed 
	 *        usages.
	 * @return <code>true</code> if all of the usages are allowed or <code>false</code>
	 * 		   otherwise.
	 */
	public boolean isAllowedUsage(int a_usage)
	{
		if((m_usage & a_usage) == m_usage)
		{
			return true;
		}
		return false;
	}
	
	/*
	 * Convinience methods checking for all possible usages
	 */
	
	public boolean allowsDigitalSignature()
	{
		return isAllowedUsage(KeyUsage.digitalSignature);
	}
	
	public boolean allowsNonRepudiation()
	{
		return isAllowedUsage(KeyUsage.nonRepudiation);
	}
	
	public boolean allowsKeyEncipherment()
	{
		return isAllowedUsage(KeyUsage.keyEncipherment);
	}
	
	public boolean allowsDataEncipherment()
	{
		return isAllowedUsage(KeyUsage.dataEncipherment);
	}
	
	public boolean allowsKeyAgreement()
	{
		return isAllowedUsage(KeyUsage.keyAgreement);
	}
	
	public boolean allowsKeyCertSign()
	{
		return isAllowedUsage(KeyUsage.keyCertSign);
	}
	
	public boolean allowsCRLSign()
	{
		return isAllowedUsage(KeyUsage.cRLSign);
	}
	
	public boolean allowsEncipherOnly()
	{
		return isAllowedUsage(KeyUsage.encipherOnly);
	}
	
	public boolean allowsDecipherOnly()
	{
		return isAllowedUsage(KeyUsage.decipherOnly);
	}
}
