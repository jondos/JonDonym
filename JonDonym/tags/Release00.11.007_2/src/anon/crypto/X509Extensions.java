/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;

import anon.util.Util;

/**
 * Stores X509 extensions as described in RFC 3280.
 * @author Rolf Wendolsky
 * @see http://www.faqs.org/rfcs/rfc3280.html
 */
public final class X509Extensions
{
	private DERObjectIdentifier X509_EXTENSIONS_IDENTIFIER =
		new DERObjectIdentifier("1.2.840.113549.1.9.14");
	
	private static final Vector KNOWN_CERTIFICATE_EXTENSIONS = new Vector();
	private static final Vector KNOWN_CRL_EXTENSIONS = new Vector();
	private static final Vector KNOWN_CRL_ENTRY_EXTENSIONS = new Vector();
	
	static 
	{ 
		//Certificate Extensions
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.AuthorityKeyIdentifier.toString());
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.SubjectKeyIdentifier.toString());
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.BasicConstraints.toString());
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.KeyUsage.toString());
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.IssuerAlternativeName.toString());
		KNOWN_CERTIFICATE_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.SubjectAlternativeName.toString());
		//CRL Extensions
		KNOWN_CRL_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.AuthorityKeyIdentifier.toString());
		KNOWN_CRL_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.IssuerAlternativeName.toString());
		KNOWN_CRL_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.IssuingDistributionPoint.toString());
		//CRL Entry Extension
		KNOWN_CRL_ENTRY_EXTENSIONS.addElement(org.bouncycastle.asn1.x509.X509Extensions.CertificateIssuer.toString());
	};

	private DERSet m_extensions;
	private Vector m_vecExtensions;

	/**
	 * Creates a new X509Extensions object from a single extension.
	 * @param a_extension an AbstractX509Extension
	 */
	public X509Extensions(AbstractX509Extension a_extension)
	{
		this(Util.toVector(a_extension));
	}

	/**
	 * Creates a new X509Extensions object that holds all extensions in the given Vector.
	 * The extensions must be of the type AbstractX509Extension.
	 * @param a_extensions a Vector with extension of the type AbstractX509Extension
	 */
	public X509Extensions(Vector a_extensions)
	{
		ASN1EncodableVector extensionWrapper;
		ASN1EncodableVector extensions = new ASN1EncodableVector();

		if (a_extensions == null)
		{
			a_extensions = new Vector();
		}

		m_vecExtensions = new Vector();
		for (int i = 0; i < a_extensions.size(); i++)
		{
			if (!(a_extensions.elementAt(i) instanceof AbstractX509Extension))
			{
				throw new IllegalArgumentException("X509 extension expected, but was: " +
					a_extensions.elementAt(i));
			}
			m_vecExtensions.addElement(a_extensions.elementAt(i));
			extensions.add(((AbstractX509Extension)a_extensions.elementAt(i)).getBCExtension());
		}

		extensionWrapper = new ASN1EncodableVector();
		extensionWrapper.add(X509_EXTENSIONS_IDENTIFIER);
		extensionWrapper.add(new DERSet(new DERSequence(extensions)));
		m_extensions = new DERSet(new DERSequence(extensionWrapper));
	}

	/**
	 * Creates an X509Extensions object from a DERTaggedObject. For internal use only.
	 * @param a_extensions a DERTaggedObject containing X509 extensions
	 */
	X509Extensions(DERSet a_extensions)
	{
		DERSequence extensionWrapper;
		DERObjectIdentifier identifier;

		m_extensions = a_extensions;
		m_vecExtensions = new Vector();

		if (m_extensions.size() == 0)
		{
			// no extensions
			return;
		}

		extensionWrapper = (DERSequence)m_extensions.getObjectAt(0);

		identifier = (DERObjectIdentifier)extensionWrapper.getObjectAt(0);
		if (!identifier.equals(X509_EXTENSIONS_IDENTIFIER))
		{
			throw new IllegalArgumentException("Wrong identifier: " + identifier.getId());
		}

		extensionWrapper = (DERSequence)((DERSet)extensionWrapper.getObjectAt(1)).getObjectAt(0);
		for (int i = 0; i < extensionWrapper.size(); i++)
		{
			m_vecExtensions.addElement(
						 AbstractX509Extension.getInstance(
								  (DERSequence)extensionWrapper.getObjectAt(i)));
		}
	}

	/**
	 * Creates an X509Extensions object from a BouncyCastle X509Extensions object.
	 * For internal use only.
	 * @param a_extensions a BouncyCastle X509Extensions object
	 */
	X509Extensions(org.bouncycastle.asn1.x509.X509Extensions a_extensions)
	{
		this(createExtensionsFromX509Extensions(a_extensions));
	}

	/**
	 * Returns the number of extensions contained in this object.
	 * @return the number of extensions contained in this object
	 */
	public int getSize()
	{
		return m_vecExtensions.size();
	}

	/**
	 * Returns the extension at the given index.
	 * @param a_index an index number
	 * @return AbstractX509Extension the extension at the given index
	 * @throws ArrayIndexOutOfBoundsException  if the index is negative or
	 * not less than the current size of this X509Extensions object
	 */
	public AbstractX509Extension getExtension(int a_index)
	{
		return (AbstractX509Extension)m_vecExtensions.elementAt(a_index);
	}

	/**
	 * Returns the extension with the specified identifier if it is contained in this
	 * X509Extensions object.
	 * @param a_identifier an X509 extension identifier
	 * @return the extension with the specified identifier or null if it is not contained in this
	 *         X509Extensions object
	 */
	public AbstractX509Extension getExtension(String a_identifier)
	{
		AbstractX509Extension currentExtension;

		for (int i = 0; i < m_vecExtensions.size(); i++)
		{
			currentExtension = (AbstractX509Extension)m_vecExtensions.elementAt(i);

			if (currentExtension.getIdentifier().equals(a_identifier))
			{
				return currentExtension;
			}
		}

		return null;
	}

	/**
	 * Returns a Vector with all X509 extensions that are contained in this object as
	 * AbstractX509Extension objects.
	 * @return a Vector with all X509 extensions that are contained in this object as
	 * AbstractX509Extension objects
	 */
	public Vector getExtensions()
	{
		return (Vector)m_vecExtensions.clone();
	}

	/**
	 * Returns the object as BouncyCastle DERSet. For internal use only.
	 * @return the object as BouncyCastle DERSet
	 */
	DERSet getBCExtensions()
	{
		return m_extensions;
	}

	/**
	 * Returns the object a BouncyCastle X509Extensions object.
	 * @return the object a BouncyCastle X509Extensions object
	 */
	org.bouncycastle.asn1.x509.X509Extensions getBCX509Extensions()
	{
		return new org.bouncycastle.asn1.x509.X509Extensions(
				  (DERSequence)
				  ((DERSet)
				   ((DERSequence)m_extensions.getObjectAt(0)).getObjectAt(1)).getObjectAt(0));
	}

	private static Vector createExtensionsFromX509Extensions(
		   org.bouncycastle.asn1.x509.X509Extensions a_extensions)
	{
		Vector vecExtensions = new Vector();
		DERSequence extensionWrapper;

		if (a_extensions == null)
		{
			return vecExtensions;
		}
		extensionWrapper = (DERSequence)a_extensions.getDERObject();

		for (int i = 0; i < extensionWrapper.size(); i++)
		{
			vecExtensions.addElement(
						 AbstractX509Extension.getInstance(
								  (DERSequence)extensionWrapper.getObjectAt(i)));
		}

		return vecExtensions;
	}
	
	protected boolean hasUnknownCriticalExtensions()
	{
		AbstractX509Extension someExtension;
		String someIdentifier;
		Enumeration extensions = m_vecExtensions.elements();
		
		while(extensions.hasMoreElements())
		{
			someExtension = (AbstractX509Extension) extensions.nextElement();
			someIdentifier = someExtension.getIdentifier();
			
			if(someExtension.isCritical())
			{
				if(!KNOWN_CERTIFICATE_EXTENSIONS.contains(someIdentifier) &&
					!KNOWN_CRL_EXTENSIONS.contains(someIdentifier) &&
					 !KNOWN_CRL_EXTENSIONS.contains(someIdentifier))
				{
				
					return true;
				}
			}
		}
		return false;
	}
}
