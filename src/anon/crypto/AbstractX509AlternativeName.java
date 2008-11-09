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

import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.lang.Integer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ASN1Sequence;

import anon.util.Util;
import anon.infoservice.ListenerInterface;

/**
 * The SubjectAlternativeName extension represents an alias to distinguished name (DN).
 * It may contain several values and is often used for IPs, DNS-Names, URLs and E-Mail addresses.
 * @author Rolf Wendolsky
 * @see http://www.faqs.org/rfcs/rfc2538.html
 */
public abstract class AbstractX509AlternativeName extends AbstractX509Extension
{
	public static final Integer TAG_OTHER = new Integer(0);
	public static final Integer TAG_EMAIL = new Integer(1);
	public static final Integer TAG_DNS = new Integer(2);
	//public static final Integer TAG_X400ADDRESS = new Integer(3);
	//public static final Integer TAG_DIRECTORY_NAME = new Integer(4);
	//public static final Integer TAG_EDI_PARTY_NAME = new Integer(5);
	public static final Integer TAG_URL = new Integer(6);
	public static final Integer TAG_IP = new Integer(7);
	//public static final Integer TAG_REGISTERED_ID = new Integer(8);

	private Vector m_values;
	private Vector m_tags;

	/**
	 * Constructs a new X509AlternativeName from a value. A value with commas (,) is interpreted
	 * as multiple values.
	 * @param a_identifier the identifier of this extension
	 * @param a_value a value
	 * @param a_tag the type tag for this value
	 */
	public AbstractX509AlternativeName(String a_identifier, String a_value, Integer a_tag)
	{
		this (a_identifier, Util.toVector(a_value), Util.toVector(a_tag));
	}

	/**
	 * Constructs a new X509AlternativeName from a value. A value with commas (,) is interpreted
	 * as multiple values.
	 * @param a_identifier the identifier of this extension
	 * @param a_critical true if the X509AlternativeName is critical; false otherwise
	 * @param a_value a value
	 * @param a_tag the type tag for this value
	 */
	public AbstractX509AlternativeName(String a_identifier,
									   boolean a_critical, String a_value, Integer a_tag)
	{
		this (a_identifier, a_critical, Util.toVector(a_value), Util.toVector(a_tag));
	}

	/**
	 * Constructs a new X509AlternativeName from values.
	 * @param a_identifier the identifier of this extension
	 * @param a_values values for the X509AlternativeName
	 * @param a_tags the type tags for the values
	 */
	public AbstractX509AlternativeName(String a_identifier, Vector a_values, Vector a_tags)
	{
		this (a_identifier, false, a_values, a_tags);
	}

	/**
	 * Constructs a new X509AlternativeName from values.
	 * @param a_identifier the identifier of this extension
	 * @param a_critical true if the extension is critical; false otherwise
	 * @param a_values values for the X509AlternativeName
	 * @param a_tags the type tags for the values
	 */
	public AbstractX509AlternativeName(String a_identifier,
									   boolean a_critical, Vector a_values, Vector a_tags)
	{
		super(a_identifier, a_critical, createValue(a_values, a_tags));
		m_values = (Vector)a_values.clone();
		m_tags = (Vector)a_tags.clone();
	}

	/**
	 * Creates an X509AlternativeName from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 */
	public AbstractX509AlternativeName(DERSequence a_extension)
	{
		super(a_extension);

		ASN1Sequence values;
		Enumeration enumValues;
		DERTaggedObject taggedValue;
		byte[] value;
		Integer tag;

		m_values = new Vector();
		m_tags = new Vector();
		try
		{
			values = (ASN1Sequence)(new ASN1InputStream(
				  new ByteArrayInputStream(getDEROctets()))).readObject();
		}
		catch (IOException a_e)
		{
			throw new RuntimeException("Could not read object from DER sequence!");
		}

		enumValues = values.getObjects();
		while (enumValues.hasMoreElements())
		{
			taggedValue = (DERTaggedObject)enumValues.nextElement();
			tag = new Integer(taggedValue.getTagNo());
			value = ((DEROctetString) taggedValue.getObject()).getOctets();
			if (tag.equals(TAG_IP))
			{
				String ipaddress = "";
				for (int i = 0; i < value.length; i++)
				{
					ipaddress += (255 & (int)value[i]);
					if (i + 1 < value.length)
					{
						ipaddress += ".";
					}
				}
				m_values.addElement(ipaddress);
			}
			else
			{
				m_values.addElement(new String(value));
			}
			m_tags.addElement(tag);
		}

	}

	/**
	 * Verifies if a given String is a valid IP address (IPv4 or IPv6)
	 * @param a_ipAddress an IP address as String
	 * @return if a given String is a valid IP address; false otherwise
	 */
	public static boolean isValidIP(String a_ipAddress)
	{
		return ListenerInterface.isValidIP(a_ipAddress);
	}

	/**
	 * Verifies if a given String is a valid email address (IPv4 or IPv6). There may be more than
	 * one addresses in the string, separated by commas (,).
	 * @param a_email an email address as String
	 * @return if a given String is a valid email address; false otherwise
	 */
	public static boolean isValidEMail(String a_email)
	{
		if (a_email == null)
		{
			return false;
		}

		StringTokenizer tokenizer = new StringTokenizer(a_email, ",");
		String email;

		if (!tokenizer.hasMoreElements())
		{
			return false;
		}
		while (tokenizer.hasMoreElements())
		{
			email = tokenizer.nextToken().trim();
			if (email.length() == 0)
			{
				return false;
			}

			int dot = email.lastIndexOf('.');
			int len = email.length();
			int at = email.indexOf('@');

			if (len == 0 || at == -1 || dot == -1 || at == 0 || dot < at)
			{
				return false;
			}
			if (!((dot + 2) < len))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns all values of this X509AlternativeName.
	 * @return all values of this X509AlternativeName
	 */
	public Vector getValues()
	{
		return (Vector)m_values.clone();
	}

	/**
	 * Returns the tags corresponding to the values as Integer objects.
	 * @return the tags corresponding to the values as Integer objects
	 */
	public Vector getTags()
	{
		return (Vector)m_tags.clone();
	}

	private static byte[] createValue(Vector a_values, Vector a_tags)
	{
		ByteArrayOutputStream out;
		DEREncodableVector values;
		Integer tag;
		byte[] value = null;
		String strValue;
		Vector tempTags, tempValues;
		StringTokenizer tokenizer;

		values = new DEREncodableVector();

		if (a_values != null && a_values.size() != 0)
		{
			if (a_tags == null || a_values.size() != a_tags.size())
			{
				throw new IllegalArgumentException("Tags have an invalid size!");
			}
			// interpret comma-separated values as multiple values
			tempTags = new Vector();
			tempValues = new Vector();
			for (int i = 0; i < a_values.size(); i++)
			{
				if (! (a_values.elementAt(i) instanceof String))
				{
					throw new IllegalArgumentException("Values must be Strings!");
				}
				tokenizer = new StringTokenizer((String)a_values.elementAt(i), ",");
				while (tokenizer.hasMoreTokens())
				{
					tempTags.addElement(a_tags.elementAt(i));
					tempValues.addElement(tokenizer.nextToken().trim());
				}
			}
			a_tags = tempTags;
			a_values = tempValues;

			for (int i = 0; i < a_values.size(); i++)
			{
				strValue = (String) a_values.elementAt(i);
				if (strValue == null || strValue.length() == 0)
				{
					continue;
				}

				if (a_tags.elementAt(i) == null || ! (a_tags.elementAt(i) instanceof Integer))
				{
					throw new IllegalArgumentException("Unsupported tag: " + a_tags.elementAt(i));
				}
				tag = (Integer) a_tags.elementAt(i);

				if (tag.equals(TAG_IP))
				{
					if (!isValidIP(strValue))
					{
						throw new IllegalArgumentException("Invalid IP address: " + strValue);
					}
					try
					{
						value = InetAddress.getByName(strValue).getAddress();
					}
					catch (java.net.UnknownHostException a_e)
					{
						// should be impossible
						throw new RuntimeException("An IP address was not recognized as such!");
					}
				}
				else if (tag.equals(TAG_EMAIL))
				{
					if (!isValidEMail(strValue))
					{
						throw new IllegalArgumentException("Invalid email address: " + strValue);
					}
				}
				else if (tag.equals(TAG_URL))
				{
					try
					{
						new URL(strValue);
					}
					catch (Exception a_e)
					{
						throw new IllegalArgumentException(a_e.getMessage());
					}
				}
				else if (tag.equals(TAG_DNS))
				{
					// todo...
				}
				else if (tag.equals(TAG_OTHER))
				{
					// OK, you may write everything...
				}
				else
				{
					throw new IllegalArgumentException("Unsupported tag: " + tag);
				}

				if (value == null)
				{
					value = strValue.getBytes();
				}

				values.add(new DERTaggedObject(tag.intValue(), new DEROctetString(value)));
				value = null;
			}
		}
		out = new ByteArrayOutputStream();
		try
		{
			new DEROutputStream(out).writeObject(new DERSequence(values));
		}
		catch (IOException a_e)
		{
			// should be impossible
			throw new RuntimeException("Error while writing object to byte array.");
		}

		return out.toByteArray();
	}
}
