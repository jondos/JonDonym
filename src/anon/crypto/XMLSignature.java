/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.Util;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * This class stores and creates signatures of XML nodes. The signing and verification processes
 * and the underlying XML signature structure are completely transparent to the using code.
 * Therefore, the XML_ELEMENT_NAME is not public. Just sign and verify what you want, you do not
 * need to know how it works! It is not allowed to change the structure of an element`s signature
 * node for other code than methods of this class. Otherwise, some methods could give false
 * results.
 * XMLSignature objects can only be created by signing or verifying XML nodes, or by getting an
 * unverified signature from an XML node.
 * @author Rolf Wendolsky
 * @see http://www.w3.org/TR/xmldsig-core/
 */
public final class XMLSignature implements IXMLEncodable
{
	private static final String XML_ELEMENT_NAME = "Signature";
	private static final String ELEM_CANONICALIZATION_METHOD = "CanonicalizationMethod";
	private static final String ELEM_SIGNATURE_METHOD = "SignatureMethod";
	private static final String ELEM_SIGNATURE_VALUE = "SignatureValue";
	private static final String ELEM_KEY_INFO = "KeyInfo";
	private static final String ELEM_SIGNED_INFO = "SignedInfo";
	private static final String ELEM_REFERENCE = "Reference";
	private static final String ELEM_DIGEST_VALUE = "DigestValue";
	private static final String ELEM_DIGEST_METHOD = "DigestMethod";
	private static final String ATTR_URI = "URI";
	private static final String ATTR_ALGORITHM = "Algorithm";

	private static final String DIGEST_METHOD_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#sha1";

	private Element m_elemSignature;
	private String m_signatureMethod;
	private String m_signatureValue;
	private String m_referenceURI;
	private String m_digestMethod;
	private String m_digestValue;
	private byte[] m_signedInfoCanonical;
	/**
	 * This hashtable contains the appended certificates in the form
	 * <dl compact>
	 * <dt> <b> key </b> </dt> <dd> <i> JAPCertificate </i> </dd>
	 * <dt> <b> value </b> </dt> <dd> <i> certificate XML element </i> </dd>
	 * </dl>
	 */
	//private Hashtable m_appendedCertificates;

	/**
	 * Stores all appended certificates
	 * It is very important, that whenever this Vector is changed,
	 * we also have to change m_appendedCertXMLElements, because
	 * the values have to be at the same index of the Vectors
	 */
	private Vector m_appendedCerts;
	/** Stores the XML representation of the appended certificates */
	private Vector m_appendedCertXMLElements;
	/** Stores the certification Path of this XMLSignature */
	private CertPath m_certPath;
	/** Indicates if the Signature was verfied already */
	private boolean m_bVerified;

	/**
	 * Creates a new and empty signature.
	 */
	private XMLSignature()
	{
		m_appendedCerts = new Vector();
		m_appendedCertXMLElements = new Vector();
		m_bVerified = false;
	}

	/**
	 * Creates a new signature from a signature element.
	 * @param a_element an XML Element
	 * @exception XMLParseException if the element is no valid signature element
	 */
	private XMLSignature(Element a_element) throws XMLParseException
	{
		Node node, subnode;

		if (a_element == null || !a_element.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, "This is no signature element!");
		}

		m_elemSignature = a_element;
		setCertificates(m_elemSignature);

		node = XMLUtil.getFirstChildByName(m_elemSignature, ELEM_SIGNED_INFO);
		if (node == null)
		{

			m_signedInfoCanonical = toCanonicalDeprecated(m_elemSignature);
			if (m_signedInfoCanonical == null)
			{
				throw new XMLParseException(ELEM_SIGNED_INFO);
			}
		}
		else
		{
			m_signedInfoCanonical = toCanonical(node);
			/** @todo SIGNATURE_METHOD is optional due to compatibility reasons; make this mandatory */
			subnode = XMLUtil.getFirstChildByName(node, ELEM_SIGNATURE_METHOD);
			m_signatureMethod = XMLUtil.parseValue(subnode, "");

			node = XMLUtil.getFirstChildByName(node, ELEM_REFERENCE);
			if (node == null)
			{
				throw new XMLParseException(ELEM_REFERENCE);
			}
			m_referenceURI = XMLUtil.parseAttribute( (Element) node, ATTR_URI, "");

			/** @todo DIGEST_METHOD is optional due to compatibility reasons; make this mandatory */
			subnode = XMLUtil.getFirstChildByName(node, ELEM_DIGEST_METHOD);
			m_digestMethod = XMLUtil.parseValue(subnode, "");

			node = XMLUtil.getFirstChildByName(node, ELEM_DIGEST_VALUE);
			if (node == null)
			{
				throw new XMLParseException(ELEM_DIGEST_VALUE);
			}
			m_digestValue = XMLUtil.parseValue(node, "");
		}

		node = XMLUtil.getFirstChildByName(m_elemSignature, ELEM_SIGNATURE_VALUE);
		if (node == null)
		{
			throw new XMLParseException(ELEM_SIGNATURE_VALUE);
		}
		m_signatureValue = XMLUtil.parseValue(node, "");
		m_bVerified = false;
	}

	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. Also, the public X509 certificate
	 * from the PKCS12 certificate is added to the signature (and the node, respective).
	 * If an error occurs while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_certificate a certificate to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	public static XMLSignature sign(Node a_node, PKCS12 a_certificate) throws XMLParseException
	{
		XMLSignature signature = signInternal(a_node, a_certificate.getPrivateKey());

		if (signature != null)
		{
			signature.addCertificate(a_certificate.getX509Certificate());
			signature.m_certPath = new CertPath(a_certificate.getX509Certificate());
		}

		return signature;
	}

	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. No certificate is appended by default;
	 * if certificates need to be appended, they must be appended after signing. If an error occurs
	 * while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKey a private key to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	public static XMLSignature sign(Node a_node, IMyPrivateKey a_privateKey) throws XMLParseException
	{
		return signInternal(a_node, a_privateKey);
	}

	/**
	 * getHashValueOfElement: takes an XML node and returns its hash value
	 *
	 * @param nodeToHash Node
	 * @return String the SHA1 hash value of the node (might be null if an exception occured)
	 */
	public static String getHashValueOfElement(Node nodeToHash)
	{
		byte[] digestValue = null;
		try
		{
			digestValue = MessageDigest.getInstance("SHA-1").digest(toCanonical(nodeToHash));
		}
		catch (Exception ex)
		{
			  LogHolder.log(LogLevel.WARNING, LogType.PAY, "could not create hash value of node");
			  return null;
		}
		return Base64.encode(digestValue, false);
	}
	/**
	 * Same method as getHashValueOfElement,
	 * except the String returned is already Base64-encoded
	 *
	 * necessary to avoid discrepancies between the results of getHashValueOfElement
	 * between the BI(Java) and PIG (Ruby/Java-bridge)
	 *
	 * @param nodeToHash Node
	 * @return String
	 */
	public static String getEncodedHashValue(Element nodeToHash)
	{
		return getHashValueOfElement(nodeToHash);
	}


	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. If an error occurs while signing, the
	 * old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKey a private key to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	private static XMLSignature signInternal(Node a_node, IMyPrivateKey a_privateKey) throws
		XMLParseException
	{
		byte[] digestValue;
		byte[] signatureValue;
		Element elementToSign;
		XMLSignature xmlSignature;
		Element oldSignatureNode;

		if (a_node == null || a_privateKey == null)
		{
			return null;
		}
		else if (a_node instanceof Document)
		{
			elementToSign = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			elementToSign = (Element) a_node;
		}
		else
		{
			return null;
		}

		// create an empty XMLSignature; it will be 'filled' while signing the node
		xmlSignature = new XMLSignature();

		/* if there are any Signature nodes, remove them --> we create a new one */
		oldSignatureNode = removeSignatureFromInternal(elementToSign);

		try
		{
			/* calculate a message digest for the node; this digest is signed later on */
			try
			{
				digestValue = MessageDigest.getInstance("SHA-1").digest(toCanonical(elementToSign));
			}
			catch (NoSuchAlgorithmException a_e)
			{
				return null;
			}

			xmlSignature.m_referenceURI = ""; // no URI is set
			xmlSignature.m_digestMethod = DIGEST_METHOD_ALGORITHM;
			xmlSignature.m_digestValue = new String(Base64.encode(digestValue, false));

			/* now build the SignedInfo node tree */
			Document doc = elementToSign.getOwnerDocument();
			Element signedInfoNode = doc.createElement(ELEM_SIGNED_INFO);
			/** @todo the actual type of the canonicalization method is not known... */
			Element canonicalizationNode = doc.createElement(ELEM_CANONICALIZATION_METHOD);
			Element signatureMethodNode = doc.createElement(ELEM_SIGNATURE_METHOD);
			String signatureMethod =
				a_privateKey.getSignatureAlgorithm().getXMLSignatureAlgorithmReference();
			if (signatureMethod != null)
			{
				xmlSignature.m_signatureMethod = signatureMethod;
				XMLUtil.setAttribute(signatureMethodNode, ATTR_ALGORITHM, signatureMethod);
			}
			else
			{
				xmlSignature.m_signatureMethod = "";
			}

			Element referenceNode = doc.createElement(ELEM_REFERENCE);
			if (xmlSignature.getReferenceURI().length() > 0)
			{
				referenceNode.setAttribute(ATTR_URI, xmlSignature.getReferenceURI());
			}
			Element digestMethodNode = doc.createElement(ELEM_DIGEST_METHOD);
			XMLUtil.setAttribute(digestMethodNode, ATTR_ALGORITHM, DIGEST_METHOD_ALGORITHM);
			Element digestValueNode = doc.createElement(ELEM_DIGEST_VALUE);
			XMLUtil.setValue(digestValueNode, xmlSignature.m_digestValue);
			referenceNode.appendChild(digestMethodNode);
			referenceNode.appendChild(digestValueNode);
			signedInfoNode.appendChild(canonicalizationNode);
			signedInfoNode.appendChild(signatureMethodNode);
			signedInfoNode.appendChild(referenceNode);

			xmlSignature.m_signedInfoCanonical = toCanonical(signedInfoNode);

			/* now we sign the SignedInfo node tree */
			signatureValue = ByteSignature.sign(xmlSignature.m_signedInfoCanonical, a_privateKey);
			signatureValue =
				a_privateKey.getSignatureAlgorithm().encodeForXMLSignature(signatureValue);
			if (signatureValue == null)
			{
				// An error occured while signing or encoding
				return null;
			}
			xmlSignature.m_signatureValue = new String(Base64.encode(signatureValue, false));

			/* create the SignatureValue node and build the Signature tree */
			Element signatureValueNode = doc.createElement(ELEM_SIGNATURE_VALUE);
			signatureValueNode.appendChild(doc.createTextNode(xmlSignature.m_signatureValue));
			Element signatureNode = doc.createElement(XML_ELEMENT_NAME);
			signatureNode.appendChild(signedInfoNode);
			signatureNode.appendChild(signatureValueNode);

			/* now add the Signature node as a child to our toSign node */
			elementToSign.appendChild(signatureNode);
			xmlSignature.m_elemSignature = signatureNode;
			xmlSignature.m_bVerified = true;

			return xmlSignature;
		}
		catch (XMLParseException a_e)
		{
			// restore the old signature if present
			if (oldSignatureNode != null)
			{
				elementToSign.appendChild(oldSignatureNode);
			}
			throw a_e;
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, "Could not sign XML document!", a_e);

			// restore the old signature if present
			if (oldSignatureNode != null)
			{
				elementToSign.appendChild(oldSignatureNode);
			}
			return null;
		}
	}

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature.
	 * @param a_node an XML node
	 * @param a_certificate a certificate to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	public static XMLSignature verify(Node a_node, JAPCertificate a_certificate) throws XMLParseException
	{
		return verify(a_node, Util.toVector(a_certificate));
	}

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature.
	 * @param a_node an XML node
	 * @param a_certificateList certificates to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	public static XMLSignature verify(Node a_node, Vector a_certificateList) throws XMLParseException
	{
		// the certificates can be used as root certificates or directly on the signature
		//return verify(a_node, a_certificateList, a_certificateList);
		Vector vecCertPaths = new Vector(a_certificateList.size());
		for (int i = 0; i < a_certificateList.size(); i++)
		{
			vecCertPaths.addElement(new CertPath( (JAPCertificate) a_certificateList.elementAt(i)));
		}

		XMLSignature signature = getVerified(a_node, a_certificateList, vecCertPaths, false);
		if (signature != null && signature.isVerified())
		{
			return signature;
		}
		else
		{
			return null;
		}
	}

	/**
	 * New Implementation of the verify()-method. This one can also verify a
	 * chain of certificats to verify the Signature of an XML node.
	 * While trying to verify the Signature the certification Path is builded.
	 * The certification Path is null if the signature could not be verified.
	 *
	 * @param a_node Node A signed XML node.
	 * @param a_rootCertificates Vector A Vector of trusted root certificates which is used to verify
	 *                                  the last(or only) certificate appended at the signature
	 * @param a_directCertificates A Vector of CertPaths to verify the signature, if there are no
	 *                             appended certificates
	 * @param a_bCheckValidity If this is true, the validity of the certs is checked and expired
	 *                      certs are treated as invalid.
	 * @return XMLSignature of the node, if there is one.
	 *         The signature is also returned if the verification was NOT successfull.
	 *         to get the result of the verification call isVerified() on the returned
	 *         XMLSignature object
	 * @throws XMLParseException if a signature element exists, but the element
	 *                           has an invalid structure
	 * @todo if the signature could not be verified, the certpath should contain the appended certificate
	 *       so that the user can check (in a panel f.e.) wich appended certs are invalid
	 * @todo remove the check of the first certificate when the certPath could not be verified
	 *       this is only implemented because of compatibility reasons
	 */
	public static XMLSignature getVerified(Node a_node, Vector a_rootCertificates,
										   Vector a_directCertificatePaths, boolean a_bCheckValidity) throws
		XMLParseException
	{
		XMLSignature signature;
		Enumeration certificates;
		boolean oneCertAppended = false;
		JAPCertificate currentCertificate;
		JAPCertificate nextCertificate = null;

		// find the signature (this call could throw an XMLParseException)
		signature = findXMLSignature(a_node);
		if (signature == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Could not find the <Signature> node!");
			return null;
		}

		//start verification
		try
		{
			//get the included certificates
			//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Looking for appended certificates...");
			certificates = signature.getCertificates().elements();
			if ( (certificates != null) && (certificates.hasMoreElements()))
			{
				//we found at least one certificate
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Found " + signature.countCertificates() + " appended certificates!");
				currentCertificate = (JAPCertificate) certificates.nextElement();
				signature.m_certPath = new CertPath(currentCertificate);

				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Trying to build certification path...");
				while (certificates.hasMoreElements())
				{
					//take the next certificate an try to verify the previous
					nextCertificate = (JAPCertificate) certificates.nextElement();
					//check validity if checking is set
					if (!currentCertificate.verify(nextCertificate.getPublicKey())
						|| (a_bCheckValidity && ! (nextCertificate.getValidity().isValid(new Date()))))
					{
						//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
						//			  "Trying to build certification path -stopped!");
						break; //the building of the certPath stops here
					}
					//the cert that was used for verification is now verified
					signature.m_certPath.add(nextCertificate);
					currentCertificate = nextCertificate;
					if (!certificates.hasMoreElements()) //we reached the last cert in the path
					{
						LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
									  "Trying to build certification path -success!");
					}
				}
				//the certspath was traversed as far as possible or there was only one certificate
				if (nextCertificate == null)
				{
					oneCertAppended = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to build certification path -only one certificate appended!");
				}

				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify signature against first certifcate...");
				currentCertificate = signature.getCertPath().getFirstCertificate();
				if (currentCertificate != null)
				{
					// check validity if neccessary
					if (!verify(a_node, signature, currentCertificate.getPublicKey())
						|| (a_bCheckValidity && ! (currentCertificate.getValidity().isValid(new Date()))))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
									  "Trying to verify signature against first certifcate -failed");
						//the verification failed, the found CertPath is set
						return signature;
					}
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify signature against first certifcate -success!");
				}

				//the first (=Mix) cert could verify the signature
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify last certificate against root certificates...");
				currentCertificate = signature.m_certPath.getLatestAddedCertificate();
				if (currentCertificate != null && currentCertificate.verify(a_rootCertificates))
				{
					signature.m_bVerified = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify last certificate against root certificates -success");
				}
				else
				{
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify last certificate against root certificates -failed");
				}
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "No appended certificates found!");
			}

			if (!signature.isVerified())
			{
				CertPath certPathNew;

				/**
				 * Either no appended certificates were found, or the appended certificate path could not
				 * be verified against the root certificates.
				 * Try to verify the signature using the direct certificates
				 */
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify signature against direct certificates...");

				certPathNew = getVerifier(a_node, signature, a_directCertificatePaths, a_bCheckValidity);
				if (certPathNew != null)
				{
					signature.m_certPath = certPathNew;
					signature.m_bVerified = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify signature against direct certificates -success");
				}
				else
				{
					if (signature.m_certPath == null)
					{
						signature.m_certPath = new CertPath( (JAPCertificate)null);
					}
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify signature against direct certificates...-failed");
				}
			}

			if (!signature.isVerified() && !oneCertAppended) //the last certificate in the Path could not be verified
			{ //now try to verify the first cert against the root certs if there are more certs in the signature
				//this is for maintaining compatibility to older systems. it can be removed when the systems are adapted
				LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
							  "Trying to verify first certificate against root certificates...");
				currentCertificate = signature.m_certPath.getFirstCertificate();
				if (currentCertificate != null && currentCertificate.verify(a_rootCertificates))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify first certificate against root certificates -success");
					//signature.m_certPath.add(verifyingCertificate);
					signature.m_bVerified = true;
				}
				else //the first certificate could not be verified against the rootCerts
				{ //verification ends here, the signature is returned but could not be verified
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify first certificate against root certificates -failed");
				}
			}
		} //end of verification
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, e);
		}
		if (signature.isVerified())
		{
			a_directCertificatePaths.addElement(signature.getCertPath());
		}

		return signature;
	}

	/**
	 * Only verifies the signature of an XML node.
	 * @param a_node an XML node
	 * @param a_publicKey a public key to verify the signature
	 * @return true if the signatue was ok, false otherwise
	 */
	public static boolean verifyFast(Node a_node, IMyPublicKey a_publicKey)
	{
		try
		{
			return verify(a_node, findXMLSignature(a_node), a_publicKey);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature. This method is not as fast as verify(Node, X509Certificate) as a temporary
	 * certificate has to be created from the public key. Therefore, it is not recommended.
	 * @param a_node an XML node
	 * @param a_publicKey a public key to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	public static XMLSignature verify(Node a_node, IMyPublicKey a_publicKey) throws XMLParseException
	{
		JAPCertificate certificate;

		// transform the public key into a temporary certificate
		certificate = JAPCertificate.getInstance(a_publicKey, new GregorianCalendar());

		return verify(a_node, certificate);
	}

	/**
	 * Gets the signature from a node if present. The signature is not verified.
	 * @param a_node an XML node
	 * @throws XMLParseException if the signature is present but has an invalid XML structure
	 * @return the node`s XMLSignature or null if no signature was found
	 */
	public static XMLSignature getUnverified(Node a_node) throws XMLParseException
	{
		XMLSignature signature;

		if (a_node == null)
		{
			return null;
		}

		signature = findXMLSignature(a_node);
		if (signature != null)
		{
			signature.m_bVerified = false;
		}
		return signature;
	}

	/**
	 * Removes the signature from an XML node if a signature exists.
	 * @param a_node an XML Node
	 * @return true if the signature has been removed; false if the node did not have any signature
	 */
	public static boolean removeSignatureFrom(Node a_node)
	{
		if (removeSignatureFromInternal(a_node) == null)
		{
			return false;
		}

		return true;
	}

	/**
	 * Returns all X509 certificates that are embedded in this XMLSignature.
	 * @return all X509 certificates that are emmbeded in this XMLSignature;
	 */
	public synchronized Vector getCertificates()
	{
		Vector certificates = new Vector(m_appendedCerts.size());
		Enumeration enumCerts = m_appendedCerts.elements();
		while (enumCerts.hasMoreElements())
		{
			certificates.addElement(enumCerts.nextElement());
		}
		return certificates;
	}

	/**
	 * Returns if the specified certificate is already contained in this signature element.
	 * @param a_certificate an X509 certificate
	 * @return true if the specified certificate is already contained in this signature element;
	 *         false otherwise
	 */
	public synchronized boolean containsCertificate(JAPCertificate a_certificate)
	{
		return m_appendedCerts.contains(a_certificate);
	}

	/**
	 * Returns the number of certificates appended to this signature.
	 * @return the number of certificates appended to this signature
	 */
	public synchronized int countCertificates()
	{
		return m_appendedCerts.size();
	}

	/**
	 * Deletes all certificates from this signature.
	 */
	public synchronized void clearCertificates()
	{
		Enumeration certificates = m_appendedCertXMLElements.elements();
		Element currentElemCertificate;
		Node parentNode;

		while (certificates.hasMoreElements())
		{
			currentElemCertificate = (Element) certificates.nextElement();
			parentNode = currentElemCertificate.getParentNode();
			if (parentNode != null)
			{
				parentNode.removeChild(currentElemCertificate);
			}
		}
		m_appendedCertXMLElements.removeAllElements();
		m_appendedCerts.removeAllElements();
	}

	/**
	 * Removes a certificate from this signature.
	 * @param a_certificate an X509 certificate
	 * @return true if the certificate has been removed; false otherwise
	 */
	public synchronized boolean removeCertificate(JAPCertificate a_certificate)
	{
		int index = m_appendedCerts.indexOf(a_certificate);
		if (index >= 0)
		{
			m_appendedCerts.removeElementAt(index);
			if (! (index < m_appendedCertXMLElements.size()))
			{
				m_appendedCertXMLElements.removeElementAt(index);
				//the certificate was removed from both Vectors
				return true;
			}
		}
		//Item was not found
		return false;
	}

	/**
	 * Adds a certificate to the signature. The certificate is not added if the signature cannot
	 * be verified with it, or if the signature already contains the specified certificate.
	 * @param a_certificate JAPCertificate
	 * @return true if the certificate was added; false otherwise
	 */
	public synchronized boolean addCertificate(JAPCertificate a_certificate)
	{
		Element elemCertificate;
		Node nodeKeyInfo;
		Node nodeCertificateContainer;

		if (a_certificate == null)
		{
			return false;
		}

		// there are certificates to add; create the certificate structures if not available
		nodeKeyInfo = XMLUtil.getFirstChildByName(getSignatureElement(), ELEM_KEY_INFO);
		if (nodeKeyInfo == null)
		{
			nodeKeyInfo =
				getSignatureElement().getOwnerDocument().createElement(ELEM_KEY_INFO);
			getSignatureElement().appendChild(nodeKeyInfo);
		}

		nodeCertificateContainer = XMLUtil.getFirstChildByName(nodeKeyInfo,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (nodeCertificateContainer == null)
		{
			nodeCertificateContainer =
				getSignatureElement().getOwnerDocument().createElement(
					JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
			nodeKeyInfo.appendChild(nodeCertificateContainer);
		}

		/* test if the signature already contains the certificate and
		 * if the certificate is suitable to verify the signature
		 */
		if (m_appendedCerts.contains(a_certificate) ||
			!checkSignature(this, a_certificate.getPublicKey()))
		{
			return false;
		}

		// create a new certificate element
		elemCertificate = a_certificate.toXmlElement(getSignatureElement().getOwnerDocument());

		// add the certificate to the two vectors
		m_appendedCerts.addElement(a_certificate);
		m_appendedCertXMLElements.addElement(elemCertificate);

		// add the certificate to the signature element
		nodeCertificateContainer.appendChild(elemCertificate);

		return true;
	}

	/**
	 * Appends this XMLSignature to an XML node. If the node already has a signature, it is removed
	 * first. The signature is only appended to the node if the node`s message digest is equal to
	 * the signature`s stored message digest. If the new signature could not be appended, the old
	 * signature is not removed (if present).
	 * @param a_node an XML node
	 * @return true if the signature has been appended; false otherwise
	 */
	public boolean appendSignatureTo(Node a_node)
	{
		Document doc;
		Element element;
		Node elemOldSignature;
		Element elemNewSignature;

		if (a_node instanceof Document)
		{
			doc = (Document) a_node;
			element = doc.getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			element = (Element) a_node;
			doc = element.getOwnerDocument();
		}
		else
		{
			return false;
		}

		// check if this is a valid signature for this element!
		try
		{
			if (!checkMessageDigest(element, this))
			{
				return false;
			}
		}
		catch (XMLParseException a_e)
		{
			return false;
		}

		// create the signature element
		elemNewSignature = toXmlElementInternal(doc);

		// remove any existing signatures
		while ( (elemOldSignature = XMLUtil.getFirstChildByName(element, XML_ELEMENT_NAME)) != null)
		{
			element.removeChild(elemOldSignature);
		}

		// append this signature element
		element.appendChild(elemNewSignature);

		return true;
	}

	/**
	 * Creates a new XML element from this signature. The element is not connected with this
	 * XMLSignature object and should be used with care (or better: it should never be used,
	 * as it is not necessary...)
	 * @param a_doc an XML document
	 * @return the signature as XML element
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemSignature = toXmlElementInternal(a_doc);

		if (getSignatureElement() == elemSignature)
		{
			// create a new signature element
			elemSignature = (Element) elemSignature.cloneNode(true);
		}

		return elemSignature;
	}

	/**
	 * Returns the signature method that was used for creating this signature.
	 * @return the signature method that was used for creating this signature
	 */
	public String getSignatureMethod()
	{
		return m_signatureMethod;
	}

	/**
	 * Returns the digest method that was used for creating this signature.
	 * @return the digest method that was used for creating this signature
	 */
	public String getDigestMethod()
	{
		return m_digestMethod;
	}

	/**
	 * Returns the reference attribute URI.
	 * @return the reference attribute URI
	 */
	public String getReferenceURI()
	{
		return m_referenceURI.trim();
	}

	/**
	 * Transforms this XMLSignature to an XML element. If the given XML document
	 * already is the owner document of the signature element kept by this XMLSignature,
	 * this signature element is returned. Otherwise, a new element is created.
	 * @param a_doc an XML document
	 * @return the signature as XML element
	 */
	private Element toXmlElementInternal(Document a_doc)
	{
		if (m_elemSignature.getOwnerDocument() == a_doc)
		{
			return m_elemSignature;
		}

		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_elemSignature, true);
		}
		catch (Exception a_e)
		{
			return null;
		}
	}

	/**
	 * Removes the signature from an XML node if a signature exists.
	 * @param a_node an XML Node
	 * @return the removed signature node or null if the node did not have any signature
	 */
	private static Element removeSignatureFromInternal(Node a_node)
	{
		Element signatureNode = null;
		Node nextRemovedNode;
		Element element;

		if (a_node instanceof Document)
		{
			element = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			element = (Element) a_node;
		}
		else
		{
			return null;
		}

		// remove any existing signatures
		while ( (nextRemovedNode = XMLUtil.getFirstChildByName(element, XML_ELEMENT_NAME)) != null)
		{
			try
			{
				signatureNode = (Element) element.removeChild(nextRemovedNode);
			}
			catch (ClassCastException a_e)
			{
				// should not happen
			}
		}

		return signatureNode;
	}

	/**
	 * Gets the signature element held by this XMLSignature.
	 * @return the signature elements held by this XMLSignature
	 */
	private Element getSignatureElement()
	{
		return m_elemSignature;
	}

	/**
	 * Returns the canonical representation of the SIGNED_INFO element.
	 * @return the canonical representation of the SIGNED_INFO element
	 */
	private byte[] getSignedInfoCanonical()
	{
		return m_signedInfoCanonical;
	}

	/**
	 * Returns the Base64 encoded digest value.
	 * @return the Base64 encoded digest value
	 */
	private String getDigestValue()
	{
		return m_digestValue;
	}

	/**
	 * Returns the signature value as Base64 encoded, (and r-s encoded) String.
	 * @return the signature value as Base64 encoded, (and r-s encoded) String
	 */
	private String getSignatureValue()
	{
		return m_signatureValue;
	}

	/**
	 * @return true if the verification of the Signature was successful, false otherwise
	 *         or if no verification was done
	 */
	public boolean isVerified()
	{
		return m_bVerified;
	}

	public void setVerified(boolean a_bVerified)
	{
		m_bVerified = a_bVerified;
	}

	public CertPath getCertPath()
	{
		return m_certPath;
	}

	/**
	 * Finds the signature element of the given node if present. This signature element is only found
	 * if it is a direct child of a_node. The signature is not verified.
	 * @param a_node an XML Node
	 * @return the node`s XMLSignature or null if no signature node was found
	 * @exception XMLParseException if the node has an invalid valid XML signature element structure
	 */
	private static XMLSignature findXMLSignature(Node a_node) throws XMLParseException
	{
		XMLSignature signature;
		Element elementVerified;
		Node signatureNode;

		if (a_node == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}

		if (a_node instanceof Document)
		{
			elementVerified = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			elementVerified = (Element) a_node;
		}
		else
		{
			return null;

		}

		signatureNode = XMLUtil.getFirstChildByName(elementVerified, XML_ELEMENT_NAME);
		if (signatureNode == null)
		{
			return null;
		}

		try
		{
			// this call could throw an XMLParseException if the structure is invalid
			signature = new XMLSignature(
				(Element) XMLUtil.getFirstChildByName(elementVerified, XML_ELEMENT_NAME));
		}
		catch (ClassCastException a_e)
		{
			// should not happen
			signature = null;
		}

		return signature;
	}

	private synchronized void setCertificates(Element a_xmlSignature)
	{
		m_appendedCerts = new Vector();
		m_appendedCertXMLElements = new Vector();
		JAPCertificate currentCertificate;
		Element elemContainer;
		Node nodeCertificate;

		elemContainer = (Element) XMLUtil.getFirstChildByName(a_xmlSignature, ELEM_KEY_INFO);
		if (elemContainer == null)
		{
			return;
		}

		elemContainer = (Element) XMLUtil.getFirstChildByName(elemContainer,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (elemContainer == null)
		{
			return;
		}

		nodeCertificate = XMLUtil.getFirstChildByName(elemContainer, JAPCertificate.XML_ELEMENT_NAME);
		while (nodeCertificate != null)
		{
			try
			{
				currentCertificate = JAPCertificate.getInstance( (Element) nodeCertificate);
				if (currentCertificate != null)
				{
					m_appendedCerts.addElement(currentCertificate);
					m_appendedCertXMLElements.addElement(nodeCertificate);
				}
			}
			catch (ClassCastException a_e)
			{
				// the node is non XML element; should not happen...
			}
			nodeCertificate = nodeCertificate.getNextSibling();
		}
	}

	/**
	 * Returns all certificates that are appended to the given signature element.
	 * @param a_xmlSignature an XML signature Element
	 * @return all certificates that are appended to the given signature node
	 */
	private static Hashtable findCertificates(Element a_xmlSignature)
	{
		Hashtable certificates = new Hashtable();
		JAPCertificate currentCertificate;
		Element elemContainer;
		Node nodeCertificate;

		elemContainer = (Element) XMLUtil.getFirstChildByName(a_xmlSignature, ELEM_KEY_INFO);
		if (elemContainer == null)
		{
			return certificates;
		}

		elemContainer = (Element) XMLUtil.getFirstChildByName(elemContainer,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (elemContainer == null)
		{
			return certificates;
		}

		nodeCertificate = XMLUtil.getFirstChildByName(elemContainer, JAPCertificate.XML_ELEMENT_NAME);
		while (nodeCertificate != null)
		{
			try
			{
				currentCertificate = JAPCertificate.getInstance( (Element) nodeCertificate);
				if (currentCertificate != null)
				{
					certificates.put(currentCertificate, nodeCertificate);
				}
			}
			catch (ClassCastException a_e)
			{
				// the node not an XML element; should not happen...
			}

			nodeCertificate = nodeCertificate.getNextSibling();
		}

		return certificates;
	}

	private static byte[] toCanonical(Node a_inputNode, Node a_excludeNode) throws XMLParseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (makeCanonical(a_inputNode, out, false, a_excludeNode) == -1)
		{
			throw new XMLParseException(a_inputNode.getNodeName(),
										"Could not make the node canonical!");
		}

		try
		{
			out.flush();
		}
		catch (IOException a_e)
		{
		}

		return out.toByteArray();

	}

	/**
	 * Is only used if no digest value is found. Then, the entire document is verified against the signature.
	 * @todo mark this as deprecated and remove
	 * @param a_inputNode Node
	 * @return byte[]
	 */
	private static byte[] toCanonicalDeprecated(Node a_inputNode)
	{
		if (a_inputNode == null || a_inputNode.getPreviousSibling() == null)
		{
			return null;
		}

		byte[] dataToVerify;
		Node parent;

		parent = a_inputNode.getParentNode();
		parent.removeChild(a_inputNode);
		dataToVerify = XMLUtil.toByteArray(parent.getOwnerDocument());
		parent.appendChild(a_inputNode);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		try
		{
			dataOut.writeShort(dataToVerify.length);
			dataOut.flush();
			out.write(dataToVerify);
			out.flush();
			return out.toByteArray();
		}
		catch (IOException a_e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Could not make xml data canonical!", a_e);
			return null;
		}
	}

	public static byte[] toCanonical(Node inputNode) throws XMLParseException
	{
		return toCanonical(inputNode, false);
	}

	/**
	 * Creates a byte array from an XML node tree.
	 * @param inputNode The node (incl. the whole tree) which is flattened to a byte array.
	 *
	 * @return the node as a byte array (incl. the whole tree).
	 * @exception XMLParseException if the node could not be properly transformed into bytes
	 */
	public static byte[] toCanonical(Node inputNode, boolean a_bKeepSpaces) throws XMLParseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (makeCanonical(inputNode, out, false, null, a_bKeepSpaces) == -1)
		{
			throw new XMLParseException(inputNode.getNodeName(), "Could not make the node canonical!");
		}

		try
		{
			out.flush();
		}
		catch (IOException a_e)
		{
		}
		return out.toByteArray();

	}
	/**
	 * same as toCanonical(Node):byte[], except returning a String
	 * only necessary for use in Ruby (since handling a Java byte array in Ruby wouldnt work)
	 *
	 * @param inputNode Node
	 * @return String
	 * @throws XMLParseException
	 */
	public static String toCanonicalString(Element input)
	{
		try
		{
			byte[] canonicalBytes = toCanonical(input);
			return new String(canonicalBytes);
		} catch (Exception e)
		{
			//nothing to be done from ruby
			return ("canonicalization error");
		}
	}

	private static int makeCanonical(Node node, OutputStream o, boolean bSiblings, Node excludeNode)
	{
		return makeCanonical(node, o, bSiblings, excludeNode, false);
	}


	/**
	 * @todo find a better way to get the data of the node as a bytestream, for
	 *       compatibility reasons we use this now; it cannot be verifed that this canonicalization
	 *       method is compatible to one of the methods defined by w3c
	 * @param node Node
	 * @param o OutputStream
	 * @param bSiblings boolean
	 * @param excludeNode Node
	 * @return int
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-CanonicalizationMethod
	 * @see http://www.w3.org/TR/xml-c14n
	 */
	private static int makeCanonical(Node node, OutputStream o, boolean bSiblings, Node excludeNode, boolean a_bKeepSpaces)
	{
		try
		{
			if (node == null)
			{
				return 0;
			}
			if (node instanceof Document)
			{
				if (a_bKeepSpaces)
				{
					o.write(XMLUtil.createDocumentStructure());
					o.write('\n');
				}
				node = ( (Document) node).getDocumentElement();
			}

			if (node.equals(excludeNode))
			{
				return 0;
			}
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element elem = (Element) node;
				o.write('<');
				o.write(elem.getNodeName().getBytes());
				NamedNodeMap attr = elem.getAttributes();
				if (attr.getLength() > 0)
				{
					// sort attributes by name
					String[] nodeNames = new String[attr.getLength()];
					String[] nodeValues = new String[attr.getLength()];
					for (int i = 0; i < attr.getLength(); i++)
					{
						nodeNames[i] = attr.item(i).getNodeName();
						nodeValues[i] = attr.item(i).getNodeValue();
					}
					Util.sort(nodeNames, nodeValues);

					for (int i = 0; i < attr.getLength(); i++)
					{
						o.write(' ');
						o.write(nodeNames[i].getBytes());
						o.write('=');
						o.write('\"');
						o.write(nodeValues[i].getBytes());
						o.write('\"');
					}
				}
				o.write('>');
				if (elem.hasChildNodes())
				{
					if (makeCanonical(elem.getFirstChild(), o, true, excludeNode, a_bKeepSpaces) == -1)
					{
						return -1;
					}
				}
				o.write('<');
				o.write('/');
				o.write(elem.getNodeName().getBytes());
				o.write('>');
				if (bSiblings && makeCanonical(elem.getNextSibling(), o, true, excludeNode, a_bKeepSpaces) == -1)
				{
					return -1;
				}
			}
			else if (node.getNodeType() == Node.TEXT_NODE)
			{
				if (a_bKeepSpaces)
				{
					o.write(node.getNodeValue().getBytes());
				}
				else
				{
					o.write(node.getNodeValue().trim().getBytes());
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludeNode, a_bKeepSpaces) == -1)
				{
					return -1;
				}
				return 0;
			}
			else if (node.getNodeType() == Node.COMMENT_NODE)
			{
				if (a_bKeepSpaces)
				{
					o.write("<!--".getBytes());
					o.write(node.getNodeValue().getBytes());
					o.write("-->\n".getBytes());
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludeNode, a_bKeepSpaces) == -1)
				{
					return -1;
				}
				return 0;
			}
			else
			{
				return -1;
			}
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	/**
	 * This method is used to verify a node with a previously created XMLSignature.
	 * @param a_node an XML node
	 * @param a_signature an XMLSignature
	 * @param a_verifyingCertificatePaths a Vector of CertPaths to verify the signature
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 * @return the certificate that verified this signature; null if it could not be verified
	 *
	 */
	private static CertPath getVerifier(Node a_node, XMLSignature a_signature,
										Vector a_verifyingCertificatePaths,
										boolean a_bCheckValidity) throws XMLParseException
	{
		Enumeration certificates = a_verifyingCertificatePaths.elements();
		CertPath currentCertificate = null;
		boolean bVerified = false;
		while (!bVerified && certificates.hasMoreElements())
		{
			currentCertificate = (CertPath) certificates.nextElement();
			if (currentCertificate.getFirstCertificate() != null)
			{
				bVerified = verify(a_node, a_signature,
								   currentCertificate.getFirstCertificate().getPublicKey())
					&&
					(!a_bCheckValidity || (currentCertificate.getFirstCertificate().getValidity().isValid(new Date())));
			}
		}
		//if the verification was successfull return the cert that verified, else return null
		return bVerified ? currentCertificate : null;
	}

	/**
	 * This method is used to verify a node with a previously created XMLSignature.
	 * @param a_node an XML node
	 * @param a_signature an XMLSignature
	 * @param a_publicKey a public key
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 * @return true if the node could be verified with this signature; false otherwise
	 */
	private static boolean verify(Node a_node, XMLSignature a_signature, IMyPublicKey a_publicKey) throws
		XMLParseException
	{

		if (a_publicKey == null || a_node == null || a_signature == null)
		{
			return false;
		}

		if (!checkMessageDigest(a_node, a_signature))
		{
			a_signature.m_certPath = new CertPath( (JAPCertificate)null);
			return false;
		}

		if (!checkSignature(a_signature, a_publicKey))
		{
			return false;
		}

		return true;
	}

	/**
	 * Checks if the signature of the XMLSignature`s SIGNED_INFO is valid.
	 * @param a_signature an XMLSignature
	 * @param a_publicKey a public key
	 * @return true if the signature of the XMLSignature`s SIGNED_INFO is valid; false otherwise
	 */
	private static boolean checkSignature(XMLSignature a_signature, IMyPublicKey a_publicKey)
	{
		byte[] buff;

		buff = Base64.decode(a_signature.getSignatureValue());
		buff = a_publicKey.getSignatureAlgorithm().decodeForXMLSignature(buff);
		if (buff == null)
		{
			// an error occured while decoding the signature
			return false;
		}

		//testing Signature....
		if (!ByteSignature.verify(a_signature.getSignedInfoCanonical(), buff, a_publicKey))
		{
			return false;
		}

		return true;
	}

	private static boolean checkMessageDigest(Node a_node, XMLSignature a_signature) throws XMLParseException
	{
		MessageDigest sha1;
		byte[] digest;

		try
		{
			if (a_signature.getDigestMethod() == null)
			{
				// no digest was used; message is verified directly
				return true;
			}
			else
			{
				sha1 = MessageDigest.getInstance("SHA-1");
			}
		}
		catch (NoSuchAlgorithmException a_e)
		{
			return false;
		}
		byte[] buff = toCanonical(a_node, a_signature.getSignatureElement());
		digest = sha1.digest(buff);
		if (!MessageDigest.isEqual(Base64.decode(a_signature.getDigestValue()), digest))
		{
			return false;
		}

		return true;
	}
}
