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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

import anon.util.ClassUtil;

/**
 * Represents and creates an X509 V3 extensions. The concrete extensions are subclasses of this
 * class and must (!) implement the IDENTIFIER String constant and a public (!) constructor that
 * takes a DERSequence.
 * @see http://www.faqs.org/rfcs/rfc3280.html
 * @author Rolf Wendolsky
 */
public abstract class AbstractX509Extension
{
	/** Each subclass must contain this individual identifier. */
	public static final String IDENTIFIER = null;

	private static Class[] AVAILABLE_EXTENSIONS = new Class[]
		{X509UnknownExtension.class, 
		X509SubjectKeyIdentifier.class, X509AuthorityKeyIdentifier.class,
		X509SubjectAlternativeName.class, X509IssuerAlternativeName.class,
		X509BasicConstraints.class, X509KeyUsage.class};

	private static Vector ms_classExtensions;

	private DERObjectIdentifier m_identifier;
	private boolean m_critical;
	private byte[] m_value;
	private DERSequence m_extension;

	/**
	 * Create a new X509 V3 extension.
	 * @param a_identifier the identifier of this extension
	 * @param a_critical boolean
	 * @param a_value the extension's value
	 */
	public AbstractX509Extension(String a_identifier, boolean a_critical, byte[] a_value)
	{
		ASN1EncodableVector extension = new ASN1EncodableVector();

		m_identifier = new DERObjectIdentifier(a_identifier);
		m_critical = a_critical;
		m_value = a_value;

		extension.add(m_identifier);
		extension.add(new DERBoolean(a_critical));
		extension.add(new DEROctetString(a_value));
		m_extension = new DERSequence(extension);
	}

	/**
	 * Creates an extension from a BouncyCastle DER sequence. For internal use only.
	 * Each subclass must contain this constructor in the following form:
	 * <PRE>
	 * X509UnknownExtension(DERSequence a_extension)
	 * {
	 *    super(a_extension);
	 * }
	 * </PRE>
	 * @param a_extension a DERSequence
	 */
	public AbstractX509Extension(DERSequence a_extension)
	{
		int indexValue = 1;

		m_extension = a_extension;
		m_identifier = (DERObjectIdentifier)a_extension.getObjectAt(0);

		if (a_extension.size() == 3)
		{
			m_critical = ((DERBoolean)a_extension.getObjectAt(1)).isTrue();
			indexValue = 2;
		}
		else
		{
			m_critical = false;
		}

		m_value = ((DEROctetString)a_extension.getObjectAt(indexValue)).getOctets();
	}

	/**
	 * Creates an extension from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 * @return a X509Extension
	 */
	static AbstractX509Extension getInstance(DERSequence a_extension)
	{
		DERObjectIdentifier identifier = (DERObjectIdentifier)a_extension.getObjectAt(0);
		Enumeration classes;
		Class classExtension;
		Object[] derSequence = new Object[1];
		derSequence[0] = a_extension;
		Class[] derEncodable = new Class[1];
		derEncodable[0] = DERSequence.class;

		if (ms_classExtensions == null)
		{
			try
			{
				ms_classExtensions = ClassUtil.findSubclasses(ClassUtil.getClassStatic());
			}
			catch (Throwable a_throwable)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, a_throwable);
			}
			if (ms_classExtensions == null)
			{
				ms_classExtensions = new Vector();
			}

			if (ms_classExtensions.size() < AVAILABLE_EXTENSIONS.length)
			{
				int level;
				if (ClassUtil.isFindSubclassesEnabled())
				{
					level = LogLevel.EXCEPTION;
				}
				else
				{
					level = LogLevel.NOTICE;
				}
				LogHolder.log(level, LogType.CRYPTO,
							  "X509 extension classes have not been loaded automatically!");
				for (int i = 0; i < AVAILABLE_EXTENSIONS.length; i++)
				{
					if (!ms_classExtensions.contains(AVAILABLE_EXTENSIONS[i]))
					{
						ms_classExtensions.addElement(AVAILABLE_EXTENSIONS[i]);
					}
				}

			}
		}


		classes = ms_classExtensions.elements();
		while (classes.hasMoreElements())
		{
			classExtension = (Class)classes.nextElement();
			try
			{
				if (classExtension.getDeclaredField("IDENTIFIER").get(null).equals(
					identifier.getId()))
				{
					return (AbstractX509Extension)classExtension.getConstructor(derEncodable).
						newInstance(derSequence);
				}
			}
			catch (Exception a_e)
			{
				// never mind
			}
		}

		return new X509UnknownExtension(a_extension);
	}

	/**
	 * Returns the human-readable name of this extension.
	 * @return the human-readable name of this extension
	 */
	public abstract String getName();

	/**
	 * Returns if the extension is critical.
	 * @return true if the extension is critical; false otherwise
	 */
	public final boolean isCritical()
	{
		return m_critical;
	}

	/**
	 * Returns the identifier of this extension.
	 * @return the identifier of this extension
	 */
	public final String getIdentifier()
	{
		return m_identifier.getId();
	}

	/**
	 * Returns the DER value of this extension in a single byte array.
	 * @return the DER value of this extension in a single byte array
	 */
	public final byte[] getDEROctets()
	{
		return m_value;
	}

	/**
	 * The hash code is derived from the identifier.
	 * @return the hash code
	 */
	public final int hashCode()
	{
		return getIdentifier().hashCode();
	}

	/**
	 * Two extensions are equal if their identifiers are equal.
	 * @param a_object an Object
	 * @return true if the object is an extension and their identifiers are equal; false otherwise
	 */
	public final boolean equals(Object a_object)
	{
		if (a_object == null || !(a_object instanceof AbstractX509Extension))
		{
			return false;
		}

		if (getIdentifier().equals(((AbstractX509Extension)a_object).getIdentifier()))
		{
			return true;
		}

		return false;
	}

	/**
	 * This method returns all values of this extension in separated String objects. The values
	 * should be human readable.
	 * @return all values of this extension
	 */
	public abstract Vector getValues();

	/**
	 * Returns the name of this extension.
	 * @return the name of this extension
	 */
	public final String toString()
	{
		return getName();
	}

	/**
	 * Returns the extension as BouncyCastle DERSequence.
	 * @return the extension as BouncyCastle DERSequence
	 */
	final DERSequence getBCExtension()
	{
		return m_extension;
	}
}
