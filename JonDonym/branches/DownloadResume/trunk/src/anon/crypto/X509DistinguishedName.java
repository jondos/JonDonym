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
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * Objects of this class represent an X509 distinguished name. The name consists of attributes,
 * that are pairs of identifiers and values. Each attribute may have exactly one value.
 * For each identifier, there also exists a string representation.
 * @author Rolf Wendolsky
 */
public final class X509DistinguishedName
{
	/** Common name : X509 identifier for the string "CN" */
	public static final String IDENTIFIER_CN = X509Name.CN.getId();
	/** Country code : X509 identifier for the string "C" */
	public static final String IDENTIFIER_C = X509Name.C.getId();
	/** State or province : X509 identifier for the string "CN" */
	public static final String IDENTIFIER_ST = X509Name.ST.getId();
	/** locality name : X509 identifier for the string "L" */
	public static final String IDENTIFIER_L = X509Name.L.getId();
	/** Organisation : X509 identifier for the string "O" */
	public static final String IDENTIFIER_O = X509Name.O.getId();
	/** Organisational Unit : X509 identifier for the string "OU" */
	public static final String IDENTIFIER_OU = X509Name.OU.getId();
	/** E-Mail : X509 identifier for the string "E" */
	public static final String IDENTIFIER_E = X509Name.E.getId();
	/** E-Mail 2: X509 identifier for the string "EmailAddress" */
	public static final String IDENTIFIER_EmailAddress = X509Name.EmailAddress.getId();
	/** Surname : Identifier for the string "SURNAME" */
	public static final String IDENTIFIER_SURNAME = X509Name.SURNAME.getId();
	/** Given name : Identifier for the string "GIVENNAME" */
	public static final String IDENTIFIER_GIVENNAME = X509Name.GIVENNAME.getId();

	public static final String LABEL_COMMON_NAME = "CN";
	public static final String LABEL_COUNTRY = "C";
	public static final String LABEL_STATE_OR_PROVINCE = "ST";
	public static final String LABEL_LOCALITY = "L";
	public static final String LABEL_ORGANISATION = "O";
	public static final String LABEL_ORGANISATIONAL_UNIT = "OU";
	public static final String LABEL_EMAIL = "E";
	public static final String LABEL_EMAIL_ADDRESS = "EmailAddress";
	public static final String LABEL_SURNAME = "SURNAME";
	public static final String LABEL_GIVENNAME = "GIVENNAME";

	private static Vector m_sortedIdentifiers;

	private X509Name m_bcX509Name;

	/**
	 * Constructs a distinguished name from a String of the form "C=DE, ST=Bavaria, ..."
	 * @param a_x509distinguishedName String
	 */
	public X509DistinguishedName(String a_x509distinguishedName)
	{
		m_bcX509Name = new X509Name(a_x509distinguishedName);
	}

	/**
	 * Constructs a distinguished name from a Hashtable of attributes. The Hashtable contains
	 * the attribute identifiers as keys and the attributes as values. Do not use the
	 * string representation of the identifiers! Commas (,) are interpreted as multiple
	 * attributes for the same identifier.
	 * @param a_attributes a Hashtable of attributes
	 * @throws IllegalCharacterException if an illegal character is contained in the DN
	 */
	public X509DistinguishedName(Hashtable a_attributes)
		throws IllegalCharacterException
	{
		if (a_attributes == null)
		{
			throw new IllegalArgumentException("Attributes must not be null!");
		}

		Enumeration oids = a_attributes.keys();
		Vector vecOIDs  = new Vector();
		Vector attributes = new Vector();
		String currentAttribute;
		Object oid;

		while (oids.hasMoreElements())
		{
			oid = oids.nextElement();

			if (a_attributes.get(oid) == null)
			{
				continue;
			}
			currentAttribute = a_attributes.get(oid).toString();
			if (currentAttribute.trim().length() == 0)
			{
				continue;
			}

			if (!(oid instanceof DERObjectIdentifier))
			{
				// compatibility to BouncyCastle
				oid = new DERObjectIdentifier(oid.toString());
			}


			// test if this is a valid attribute
			/*
			if (currentAttribute.indexOf("=") >= 0)
			{
				throw new IllegalCharacterException((DERObjectIdentifier)oid, '=');
			}
			else */
		    /*
			if (currentAttribute.indexOf(",") >= 0)
			{
				throw new IllegalCharacterException((DERObjectIdentifier)oid, ',');
			}*/


		    // interpret comma as separator

			if (oid.equals(X509Name.E) || oid.equals(X509Name.EmailAddress) || oid.equals(X509Name.OU))
			{
				StringTokenizer tokenizer = new StringTokenizer(currentAttribute, ",");
				while (tokenizer.hasMoreTokens())
				{
					vecOIDs.addElement(oid);
					attributes.addElement(tokenizer.nextToken().trim());
				}
			}
			else
			{
				vecOIDs.addElement(oid);
				attributes.addElement(currentAttribute.trim());
			}
		}

		if (attributes.size() == 0)
		{
			throw new IllegalArgumentException("Attributes are empty!");
		}
		m_bcX509Name = new X509Name(vecOIDs, attributes);
	}

	/**
	 * Constructs a distinguished name from a BouncyCastle X509Name object. For internal use only.
	 * @param a_bcX509Name a BouncyCastle X509Name object.
	 */
	public X509DistinguishedName(X509Name a_bcX509Name)
	{
		m_bcX509Name = a_bcX509Name;
	}

	/**
	 * Returns the X509 attribute label corresponding to a given attribute identifier.
	 * @param a_identifier an X509 attribute identifier
	 * @return the X509 attribute label corresponding to a given attribute identifier
	 */
	public static String getAttributeNameFromAttributeIdentifier(String a_identifier)
	{
		if (a_identifier == null)
		{
			return null;
		}

		if (a_identifier.equals(IDENTIFIER_CN))
		{
			return LABEL_COMMON_NAME;
		}
		else if (a_identifier.equals(IDENTIFIER_C))
		{
			return LABEL_COUNTRY;
		}
		else if (a_identifier.equals(IDENTIFIER_ST))
		{
			return LABEL_STATE_OR_PROVINCE;
		}
		else if (a_identifier.equals(IDENTIFIER_L))
		{
			return LABEL_LOCALITY;
		}
		else if (a_identifier.equals(IDENTIFIER_O))
		{
			return LABEL_ORGANISATION;
		}
		else if (a_identifier.equals(IDENTIFIER_OU))
		{
			return LABEL_ORGANISATIONAL_UNIT;
		}
		else if (a_identifier.equals(IDENTIFIER_E))
		{
			return LABEL_EMAIL;
		}
		else if (a_identifier.equals(IDENTIFIER_EmailAddress))
		{
			return LABEL_EMAIL_ADDRESS;
		}
		else if (a_identifier.equals(IDENTIFIER_SURNAME))
		{
			return LABEL_SURNAME;
		}
		else if (a_identifier.equals(IDENTIFIER_GIVENNAME))
		{
			return LABEL_GIVENNAME;
		}

		return a_identifier;
	}

	/**
	 * Returns the common name.
	 * @return the common name or null if not set
	 */
	public String getCommonName()
	{
		return getAttributeValue(IDENTIFIER_CN);
	}

	/**
	 * Returns the surname.
	 * @return the surname or null if not set
	 */
	public String getSurname()
	{
		return getAttributeValue(IDENTIFIER_SURNAME);
	}

	/**
	 * Returns the given name.
	 * @return the given name or null if not set
	 */
	public String getGivenName()
	{
		return getAttributeValue(IDENTIFIER_GIVENNAME);
	}

	/**
	 * Returns the country code;
	 * @return the country code or null if not set
	 */
	public String getCountryCode()
	{
		return getAttributeValue(IDENTIFIER_C);
	}

	/**
	 * Returns the state or province.
	 * @return the state or province or null if not set
	 */
	public String getStateOrProvince()
	{
		return getAttributeValue(IDENTIFIER_ST);
	}

	/**
	 * Returns the locality name.
	 * @return the locality name or null if not set
	 */
	public String getLocalityName()
	{
		return getAttributeValue(IDENTIFIER_L);
	}

	/**
	 * Returns the organisation.
	 * @return the organisation or null if not set
	 */
	public String getOrganisation()
	{
		return getAttributeValue(IDENTIFIER_O);
	}

	/**
	 * Returns the organisational unit.
	 * @return the organisational unit
	 */
	public String getOrganisationalUnit()
	{
		return getAttributeValue(IDENTIFIER_OU);
	}

	/**
	 * Returns the email address specified by the identifier "E".
	 * @return the email address or null if not set
	 */
	public String getE_EmailAddress()
	{
		return getAttributeValue(IDENTIFIER_E);
	}

	/**
	 * Returns the email address specified by the identifier "EmailAddress".
	 * @return the email address or null if not set
	 */
	public String getEmailAddress()
	{
		return getAttributeValue(IDENTIFIER_EmailAddress);
	}

	/**
	 * Returns the attribute value corresponding to a given identifier. If there is more than one
	 * attribute for this identifier, the attributes are comma-separated.
	 * @param a_identifier an attribute identifier
	 * @return String the attribute value corresponding to a given identifier or null if the
	 *                attribute is not set in this X509 name
	 */
	public String getAttributeValue(String a_identifier)
	{
		if (a_identifier == null || a_identifier.trim().length() == 0)
		{
			return null;
		}
		String attribute = null;
		DERObjectIdentifier identifier = new DERObjectIdentifier(a_identifier);
		Vector values = m_bcX509Name.getValues();
		Vector OIDs = m_bcX509Name.getOIDs();

		int index = OIDs.indexOf(identifier);

		if (index < 0)
		{
			return null;
		}
		else
		{
			attribute = (String)values.elementAt(index);
			if (attribute != null)
			{
				for (int i = index + 1; i < OIDs.size(); i++)
				{
					if (OIDs.elementAt(i).equals(identifier))
					{
						attribute = attribute.trim();
						if (attribute.length() > 0)
						{
							attribute += ", ";
						}
						attribute +=  (String)values.elementAt(i);
					}
				}
			}
		}

		return attribute;
	}



	/**
	 * Returns the identifiers of attributes in this X509 name.
	 * @return the identifiers of attributes in this X509 name
	 */
	public Vector getAttributeIdentifiers()
	{
		Vector oids = new Vector();
		Vector unsortedOIDs = m_bcX509Name.getOIDs();
		Enumeration identifiers = getSortedIdentifiers();
		int index;

		while (identifiers.hasMoreElements())
		{
			if ((index = unsortedOIDs.indexOf((identifiers.nextElement()))) >= 0)
			{
				oids.addElement(((DERObjectIdentifier)unsortedOIDs.elementAt(index)).getId());
				unsortedOIDs.removeElementAt(index);
			}
		}

		for (int i = 0; i < unsortedOIDs.size(); i++)
		{
			oids.addElement(((DERObjectIdentifier)unsortedOIDs.elementAt(i)).getId());
		}

		return oids;
	}

	/**
	 * Returns the values of attributes in this X509 name.
	 * @return the values of attributes in this X509 name
	 */
	public Vector getAttributeValues()
	{
		Vector unsortedOIDs = m_bcX509Name.getOIDs();
		Vector unsortedAttributes = m_bcX509Name.getValues();
		Vector attributes = new Vector();
		Enumeration identifiers = getSortedIdentifiers();
		int index;

		while (identifiers.hasMoreElements())
		{
			if ((index = unsortedOIDs.indexOf((identifiers.nextElement()))) >= 0)
			{
				attributes.addElement(unsortedAttributes.elementAt(index));
				unsortedOIDs.removeElementAt(index);
				unsortedAttributes.removeElementAt(index);
			}
		}
		for (int i = 0; i < unsortedAttributes.size(); i++)
		{
			attributes.addElement(unsortedAttributes.elementAt(i));
		}

		return attributes;
	}

	/**
	 * Returns the distinguished name as Hashtable (identifier-attribute).
	 * @return the distinguished name as Hashtable (identifier-attribute)
	 */
	public Hashtable getDistinguishedName()
	{
		Hashtable name = new Hashtable();
		Vector identifiers = getAttributeIdentifiers();
		Vector attributes = getAttributeValues();
		for (int i = 0; i < identifiers.size(); i++)
		{
			name.put(identifiers.elementAt(i), attributes.elementAt(i));
		}

		return name;
	}

	public int hashCode()
	{
		return m_bcX509Name.hashCode();
	}

	public boolean equals(Object a_object)
	{
		if (a_object == null)
		{
			return false;
		}
		if (a_object instanceof X509DistinguishedName)
		{
			return m_bcX509Name.equals( ( (X509DistinguishedName) a_object).m_bcX509Name);
		}
		else if (a_object instanceof X509Name)
		{
			return m_bcX509Name.equals((X509Name) a_object);
		}
		return false;
	}

	/**
	 * Writes the distinguished name as a single String. Identifiers and values are
	 * separated by "=", the whole attributes are comma-separated.
	 * @return String
	 */
	public String toString()
	{
		Vector identifiers = getAttributeIdentifiers();
		Vector attributes = getAttributeValues();
		String name = "";

		for (int i = 0; i < identifiers.size(); i++)
		{
 			name += getAttributeNameFromAttributeIdentifier((String)identifiers.elementAt(i)) +
				"=" +
				attributes.elementAt(i);
			if (i + 1 < identifiers.size())
			{
				name += ", ";
			}
		}

		return name;
	}

	/**
	 * Represents an error that is thrown when an illegal character is used in a distinguished name
	 * attribute.
	 * @author Rolf Wendolsky
	 */
	public class IllegalCharacterException extends IllegalArgumentException
	{
		private char m_character;
		private String m_attribute;

		private IllegalCharacterException(DERObjectIdentifier a_identifier, char a_character)
		{
			super("'" + a_character + "' characters are not allowed!");

			m_attribute = getAttributeNameFromAttributeIdentifier(a_identifier.getId());
			m_character = a_character;
		}

		/**
		 * Returns the illegal character.
		 * @return the illegal character
		 */
		public char getCharacter()
		{
			return m_character;
		}

		/**
		 * Returns the attribute in that the illegal character was found.
		 * @return the attribute in that the illegal character was found
		 */
		public String getAttribute()
		{
			return m_attribute;
		}
	}

	/**
	 * Returns the BouncyCastle X509Name representation of this object. For internal use only.
	 * @return the BouncyCastle X509Name representation of this object
	 */
	X509Name getX509Name()
	{
		return m_bcX509Name;
	}

	/**
	 * Returns the most important BC object identifiers sorted in a reasonable way.
	 * @return an Enumeration of the the most important BC object identifiers sorted in a reasonable way
	 */
	private static Enumeration getSortedIdentifiers()
	{
		if (m_sortedIdentifiers == null)
		{
			m_sortedIdentifiers = new Vector();
			m_sortedIdentifiers.addElement(X509Name.CN);
			m_sortedIdentifiers.addElement(X509Name.SURNAME);
			m_sortedIdentifiers.addElement(X509Name.GIVENNAME);
			m_sortedIdentifiers.addElement(X509Name.O);
			m_sortedIdentifiers.addElement(X509Name.OU);
			m_sortedIdentifiers.addElement(X509Name.L);
			m_sortedIdentifiers.addElement(X509Name.ST);
			m_sortedIdentifiers.addElement(X509Name.C);
			m_sortedIdentifiers.addElement(X509Name.E);
			m_sortedIdentifiers.addElement(X509Name.EmailAddress);
		}
		return m_sortedIdentifiers.elements();
	}
}
